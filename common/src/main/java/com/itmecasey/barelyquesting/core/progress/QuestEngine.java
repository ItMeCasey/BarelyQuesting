package com.itmecasey.barelyquesting.core.progress;

import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.task.TaskEvent;
import com.itmecasey.barelyquesting.core.task.TaskType;
import com.itmecasey.barelyquesting.core.task.TaskTypes;
import com.itmecasey.barelyquesting.core.team.TeamManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The server-side rules: which quests are open, how tasks advance, when a quest is complete, and what claiming does. It talks to the
 * game only through {@link PlayerView} and {@link Host}, so everything here runs in unit tests.
 */
public final class QuestEngine {
    /** The few things the engine needs from the running server. */
    public interface Host {
        /** The online player with this id, or null. */
        PlayerView online(UUID id);

        long nowMillis();

        /** Every online player. */
        List<PlayerView> players();
    }

    /** Reacts to engine events (the network layer, scripts, the HUD). All calls come from the server thread. */
    public interface Listener {
        default void questStarted(String teamId, Quest quest) {
        }

        default void taskProgressed(String teamId, Quest quest, Def task, long value, long target) {
        }

        default void questCompleted(String teamId, Quest quest) {
        }

        default void questClaimed(String teamId, UUID player, Quest quest, Def reward) {
        }
    }

    public enum Status { LOCKED, AVAILABLE, IN_PROGRESS, COMPLETE, CLAIMED }

    private Book book;
    private final TeamManager teams;
    private final Progress progress;
    private final Host host;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Set<String> dirtyTeams = new HashSet<>();
    private boolean progressDirty;

    public QuestEngine(Book book, TeamManager teams, Progress progress, Host host) {
        this.book = book;
        this.teams = teams;
        this.progress = progress;
        this.host = host;
    }

    public Book book() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
        markAllDirty();
    }

    public TeamManager teams() {
        return teams;
    }

    public Progress progress() {
        return progress;
    }

    public long nowMillis() {
        return host.nowMillis();
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    // ---- queries ----

    public boolean isComplete(String teamId, String questId) {
        Progress.QuestProgress p = progress.get(teamId, questId);
        return p != null && p.complete;
    }

    public long taskProgress(String teamId, String questId, String taskId) {
        Progress.QuestProgress p = progress.get(teamId, questId);
        return p == null ? 0 : p.task(taskId);
    }

    public static long targetOf(Def task) {
        TaskType type = TaskTypes.get(task.type);
        return type == null ? 1 : type.target(task);
    }

    /** True if every dependency is complete and no lock is closed. */
    public boolean isUnlocked(String teamId, Quest q) {
        for (String dep : q.dependencies) {
            Quest d = book.quest(dep);
            if (d != null && !d.optional && !isComplete(teamId, dep)) {
                return false;
            }
        }
        for (Def lock : q.locks) {
            Rewards.LockType type = Rewards.lock(lock.type);
            if (type != null && type.locked(lock, this, teamId, q.id)) {
                return false;
            }
        }
        return true;
    }

    public Status status(String teamId, UUID player, Quest q) {
        Progress.QuestProgress p = progress.get(teamId, q.id);
        if (p != null && p.complete) {
            return allClaimed(teamId, player, q, p) ? Status.CLAIMED : Status.COMPLETE;
        }
        if (!isUnlocked(teamId, q)) {
            return Status.LOCKED;
        }
        boolean any = p != null && p.tasks.values().stream().anyMatch(v -> v > 0);
        return any ? Status.IN_PROGRESS : Status.AVAILABLE;
    }

    private boolean allClaimed(String teamId, UUID player, Quest q, Progress.QuestProgress p) {
        for (Def r : q.rewards) {
            if (!p.claimed(player == null ? "" : player.toString(), r.id, isTeamScope(r))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isTeamScope(Def reward) {
        return reward.str("scope", "player").equals("team");
    }

    /** Whether the quest should show on a client for this team (hidden quests wait for their dependencies). */
    public boolean isVisible(String teamId, Quest q) {
        if (!q.hidden) {
            return true;
        }
        for (String dep : q.dependencies) {
            if (!isComplete(teamId, dep)) {
                return false;
            }
        }
        return true;
    }

    // ---- progress changes ----

    private boolean isOpen(String teamId, Quest q) {
        Progress.QuestProgress p = progress.get(teamId, q.id);
        return (p == null || !p.complete) && isUnlocked(teamId, q);
    }

    /** Applies a game event for the player's team to every open quest with a matching task. */
    public void event(TaskEvent e) {
        String teamId = teams.teamOf(e.player.uuid()).id;
        for (Quest q : book.allQuests()) {
            if (!isOpen(teamId, q)) {
                continue;
            }
            for (Def task : q.tasks) {
                TaskType type = TaskTypes.get(task.type);
                if (type == null) {
                    continue;
                }
                long add = type.onEvent(task, e);
                if (add > 0) {
                    addProgress(teamId, q, task, add);
                }
            }
        }
    }

    /** Checks the polled tasks for the player; call about once a second per player. */
    public void poll(PlayerView player) {
        String teamId = teams.teamOf(player.uuid()).id;
        for (Quest q : book.allQuests()) {
            if (!isOpen(teamId, q)) {
                continue;
            }
            for (Def task : q.tasks) {
                TaskType type = TaskTypes.get(task.type);
                if (type == null || !type.polled()) {
                    continue;
                }
                long cur = taskProgress(teamId, q.id, task.id);
                long now = type.poll(task, player, cur);
                if (now > cur) {
                    setProgress(teamId, q, task, now);
                }
            }
        }
    }

    public void addProgress(String teamId, Quest q, Def task, long add) {
        setProgress(teamId, q, task, taskProgress(teamId, q.id, task.id) + add);
    }

    /** Sets a task's progress (capped at its target, never lowered) and completes the quest when its tasks are done. */
    public void setProgress(String teamId, Quest q, Def task, long value) {
        long target = targetOf(task);
        long old = taskProgress(teamId, q.id, task.id);
        long v = Math.min(target, Math.max(old, value));
        if (v == old && old != 0) {
            return;
        }
        Progress.QuestProgress p = progress.getOrCreate(teamId, q.id);
        if (v == old && p.tasks.containsKey(task.id)) {
            return;
        }
        p.tasks.put(task.id, v);
        markDirty(teamId);
        if (!p.started && v > 0) {
            p.started = true;
            for (Listener l : listeners) {
                l.questStarted(teamId, q);
            }
        }
        for (Listener l : listeners) {
            l.taskProgressed(teamId, q, task, v, target);
        }
        checkComplete(teamId, q);
    }

    private void checkComplete(String teamId, Quest q) {
        Progress.QuestProgress p = progress.get(teamId, q.id);
        if (p == null || p.complete || q.tasks.isEmpty()) {
            return;
        }
        boolean done = !q.anyTask;
        for (Def t : q.tasks) {
            boolean taskDone = p.task(t.id) >= targetOf(t);
            if (q.anyTask && taskDone) {
                done = true;
                break;
            }
            if (!q.anyTask && !taskDone) {
                done = false;
                break;
            }
        }
        // Quests without tasks never complete by themselves; commands and scripts complete them.
        if (done) {
            finish(teamId, q);
        }
    }

    private void finish(String teamId, Quest q) {
        Progress.QuestProgress p = progress.getOrCreate(teamId, q.id);
        if (p.complete) {
            return;
        }
        p.complete = true;
        p.started = true;
        p.completedAt = host.nowMillis();
        markDirty(teamId);
        TeamManager.Team team = teams.team(teamId);
        if (team != null) {
            for (UUID m : team.members) {
                PlayerView pv = host.online(m);
                if (pv != null) {
                    pv.toast(q.title, "Quest complete");
                }
            }
        }
        for (Listener l : listeners) {
            l.questCompleted(teamId, q);
        }
        // Rewards marked auto are given at once to the online members (team scope: to the first online member).
        if (team != null) {
            for (Def r : q.rewards) {
                if (!r.bool("auto", false)) {
                    continue;
                }
                for (UUID m : team.members) {
                    PlayerView pv = host.online(m);
                    if (pv != null) {
                        claim(pv, q.id, r.id);
                        if (isTeamScope(r)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /** Completes a quest without its tasks (admin command, scripts, the checkmark of a task-less quest). */
    public boolean forceComplete(String teamId, String questId) {
        Quest q = book.quest(questId);
        if (q == null || isComplete(teamId, questId)) {
            return false;
        }
        for (Def t : q.tasks) {
            Progress.QuestProgress p = progress.getOrCreate(teamId, q.id);
            p.tasks.put(t.id, targetOf(t));
        }
        finish(teamId, q);
        return true;
    }

    /** Clears a quest (or all quests when {@code questId} is null) for a team, including claims. */
    public void reset(String teamId, String questId) {
        if (questId == null) {
            progress.clearTeam(teamId);
        } else {
            progress.remove(teamId, questId);
        }
        markDirty(teamId);
    }

    /** The player hands in items for a submit task; returns how many were taken. */
    public int submit(PlayerView player, String questId, String taskId) {
        Quest q = book.quest(questId);
        if (q == null) {
            return 0;
        }
        Def task = q.task(taskId);
        String teamId = teams.teamOf(player.uuid()).id;
        TaskType type = task == null ? null : TaskTypes.get(task.type);
        if (type == null || !type.submits(task) || !isOpen(teamId, q)) {
            return 0;
        }
        long need = type.target(task) - taskProgress(teamId, q.id, task.id);
        if (need <= 0) {
            return 0;
        }
        int taken = player.takeItems(task.item("item"), (int) need);
        if (taken > 0) {
            addProgress(teamId, q, task, taken);
        }
        return taken;
    }

    /** The player ticks off a manual task. */
    public boolean check(PlayerView player, String questId, String taskId) {
        Quest q = book.quest(questId);
        if (q == null) {
            return false;
        }
        Def task = q.task(taskId);
        String teamId = teams.teamOf(player.uuid()).id;
        TaskType type = task == null ? null : TaskTypes.get(task.type);
        if (type == null || !type.manual(task) || !isOpen(teamId, q)) {
            return false;
        }
        setProgress(teamId, q, task, targetOf(task));
        return true;
    }

    /** Claims one reward ({@code rewardId}) or every unclaimed reward (null) of a completed quest; returns how many were given. */
    public int claim(PlayerView player, String questId, String rewardId) {
        Quest q = book.quest(questId);
        String teamId = teams.teamOf(player.uuid()).id;
        Progress.QuestProgress p = progress.get(teamId, questId);
        if (q == null || p == null || !p.complete) {
            return 0;
        }
        int given = 0;
        for (Def r : q.rewards) {
            if (rewardId != null && !rewardId.equals(r.id)) {
                continue;
            }
            boolean team = isTeamScope(r);
            String who = player.uuid().toString();
            if (p.claimed(who, r.id, team)) {
                continue;
            }
            Rewards.RewardType type = Rewards.reward(r.type);
            if (type == null) {
                continue;
            }
            if (team) {
                p.teamClaimed.add(r.id);
            } else {
                p.playerClaimed.computeIfAbsent(who, k -> new HashSet<>()).add(r.id);
            }
            type.give(r, player);
            given++;
            for (Listener l : listeners) {
                l.questClaimed(teamId, player.uuid(), q, r);
            }
        }
        if (given > 0) {
            markDirty(teamId);
        }
        return given;
    }

    // ---- dirty tracking for sync and saving ----

    private void markDirty(String teamId) {
        dirtyTeams.add(teamId);
        progressDirty = true;
    }

    private void markAllDirty() {
        for (TeamManager.Team t : teams.all()) {
            dirtyTeams.add(t.id);
        }
    }

    /** Team ids whose progress changed since the last call (the network layer then sends them to their players). */
    public List<String> takeDirtyTeams() {
        List<String> out = new ArrayList<>(dirtyTeams);
        dirtyTeams.clear();
        return out;
    }

    public boolean takeProgressDirty() {
        boolean d = progressDirty;
        progressDirty = false;
        return d;
    }

    public void markTeamDirty(String teamId) {
        dirtyTeams.add(teamId);
    }
}
