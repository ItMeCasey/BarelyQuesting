package com.itmecasey.barelyquesting.core.script;

import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.net.QuestServer;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.progress.Rewards;
import com.itmecasey.barelyquesting.core.task.TaskEvent;
import com.itmecasey.barelyquesting.core.task.TaskType;
import com.itmecasey.barelyquesting.core.task.TaskTypes;
import com.itmecasey.barelyquesting.core.team.TeamManager;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * The one scripting surface. The ModJS and KubeJS bridges expose an instance of this class to scripts as {@code BarelyQuesting}, and
 * Java mods can use it directly. All methods use plain strings, numbers and JSON text so any script engine can call them.
 *
 * <p>Changes made here go through the same edit operations as the in-game editor, so they are saved and synced to every player.
 */
public final class QuestApi {
    private static volatile QuestServer server;

    private static final java.util.List<Runnable> PENDING = new java.util.ArrayList<>();

    /** Used by the loader once the server is running. Work queued by startup scripts runs now. */
    public static void attach(QuestServer s) {
        server = s;
        List<Runnable> work;
        synchronized (PENDING) {
            work = new java.util.ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (Runnable r : work) {
            try {
                r.run();
            } catch (RuntimeException e) {
                java.util.logging.Logger.getLogger("barelyquesting").warning("A queued script change failed: " + e);
            }
        }
    }

    /** Runs now when the server is up, otherwise when it starts (so startup scripts can create quests). */
    private void whenReady(Runnable r) {
        if (server != null) {
            r.run();
        } else {
            synchronized (PENDING) {
                PENDING.add(r);
            }
        }
    }

    public static void detach() {
        server = null;
    }

    private QuestServer require() {
        QuestServer s = server;
        if (s == null) {
            throw new IllegalStateException("BarelyQuesting: the server is not running yet; use this from a server script or an event");
        }
        return s;
    }

    public boolean isReady() {
        return server != null;
    }

    private Book book() {
        return require().engine().book();
    }

    // ---- creating content ----

    /** Adds a chapter (or updates its title and icon when the id exists). Returns the chapter id. */
    public String chapter(String id, String title, String icon) {
        whenReady(() -> {
            Chapter c = book().chapter(id);
            Chapter n = new Chapter(id);
            n.title = title;
            n.icon = ItemSpec.of(icon == null || icon.isEmpty() ? "minecraft:book" : icon);
            if (c != null) {
                n.group = c.group;
                if (title == null || title.isEmpty()) {
                    n.title = c.title;
                }
            }
            require().applyEdit(null, EditOps.putChapter(n, null), null);
        });
        return id;
    }

    /**
     * Adds or replaces a quest from JSON text, in the quest file format ({@code title}, {@code icon}, {@code x}, {@code y},
     * {@code dependencies}, {@code tasks}, {@code rewards}, ...). An empty {@code id} makes a new random one. Returns the quest id.
     */
    public String quest(String chapterId, String questJson) {
        JsonObject o = Json.parseObject(questJson);
        if (Json.str(o, "id", "").isEmpty()) {
            o.addProperty("id", com.itmecasey.barelyquesting.core.model.Book.randomId(8));
        }
        Quest q = Quest.fromJson(o);
        whenReady(() -> {
            if (!require().applyEdit(null, EditOps.putQuest(chapterId, q, null), null)) {
                throw new IllegalArgumentException("Could not add quest to chapter " + chapterId);
            }
        });
        return q.id;
    }

    public boolean removeQuest(String questId) {
        return require().applyEdit(null, EditOps.removeQuest(questId), null);
    }

    public boolean removeChapter(String chapterId) {
        return require().applyEdit(null, EditOps.removeChapter(chapterId), null);
    }

    // ---- custom types ----

    /** Registers a task type by name that quests can use as {@code {"type": "<name>"}}. */
    public void taskType(String name, long defaultTarget, ScriptRegistry.TaskCallback callback) {
        TaskTypes.register(new TaskType() {
            @Override
            public String id() {
                return name;
            }

            @Override
            public long target(Def task) {
                return Math.max(1, task.num("count", defaultTarget));
            }

            @Override
            public boolean polled() {
                return true;
            }

            @Override
            public long poll(Def task, PlayerView player, long current) {
                return callback.poll(task, player, current);
            }

            @Override
            public long onEvent(Def task, TaskEvent event) {
                return callback.onEvent(task, event);
            }
        });
    }

    /** Registers a callback for {@code scripted} tasks with {@code "name": name}. */
    public void scriptedTask(String name, ScriptRegistry.TaskCallback callback) {
        ScriptRegistry.task(name, callback);
    }

    /** Registers a reward type by name that quests can use as {@code {"type": "<name>"}}. */
    public void rewardType(String name, ScriptRegistry.RewardCallback callback) {
        Rewards.register(new Rewards.RewardType() {
            @Override
            public String id() {
                return name;
            }

            @Override
            public void give(Def reward, PlayerView player) {
                callback.give(reward, player);
            }
        });
    }

    public void scriptedReward(String name, ScriptRegistry.RewardCallback callback) {
        ScriptRegistry.reward(name, callback);
    }

    public void scriptedLock(String name, ScriptRegistry.LockCallback callback) {
        ScriptRegistry.lock(name, callback);
    }

    // ---- script-friendly registrations (one functional interface each, so JavaScript functions convert cleanly) ----

    /** A scripted task: {@code {"type": "scripted", "name": name}}. Your function returns the progress to set (higher than now to advance). */
    public void registerTask(String name, ToLongFunction<ScriptContexts.TaskContext> poll) {
        ScriptRegistry.task(name, new ScriptRegistry.TaskCallback() {
            @Override
            public long poll(Def task, PlayerView player, long current) {
                return poll.applyAsLong(new ScriptContexts.TaskContext(player, task, current));
            }
        });
    }

    /** A scripted task driven by events (kill, craft, pickup, advancement): your function returns the progress to add. */
    public void registerTaskEvents(String name, ToLongFunction<ScriptContexts.EventContext> onEvent) {
        ScriptRegistry.task(name, new ScriptRegistry.TaskCallback() {
            @Override
            public long onEvent(Def task, TaskEvent event) {
                return onEvent.applyAsLong(new ScriptContexts.EventContext(event.player, task, event));
            }
        });
    }

    /** A scripted reward: {@code {"type": "scripted", "name": name}}. */
    public void registerReward(String name, Consumer<ScriptContexts.RewardContext> give) {
        ScriptRegistry.reward(name, (reward, player) -> give.accept(new ScriptContexts.RewardContext(player, reward)));
    }

    /** A script lock: {@code {"type": "script", "name": name}}. Return true to keep the quest closed. */
    public void registerLock(String name, Predicate<ScriptContexts.LockContext> locked) {
        ScriptRegistry.lock(name, (lock, team, quest) -> locked.test(new ScriptContexts.LockContext(lock, team, quest)));
    }

    /** A whole new task type usable as {@code {"type": typeName}}, polled like {@link #registerTask}. */
    public void newTaskType(String typeName, long defaultTarget, ToLongFunction<ScriptContexts.TaskContext> poll) {
        taskType(typeName, defaultTarget, new ScriptRegistry.TaskCallback() {
            @Override
            public long poll(Def task, PlayerView player, long current) {
                return poll.applyAsLong(new ScriptContexts.TaskContext(player, task, current));
            }
        });
    }

    /** A whole new reward type usable as {@code {"type": typeName}}. */
    public void newRewardType(String typeName, Consumer<ScriptContexts.RewardContext> give) {
        rewardType(typeName, (reward, player) -> give.accept(new ScriptContexts.RewardContext(player, reward)));
    }

    // ---- events ----

    public void onQuestStarted(Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on("questStarted", listener);
    }

    public void onTaskProgressed(Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on("taskProgressed", listener);
    }

    public void onQuestCompleted(Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on("questCompleted", listener);
    }

    public void onQuestClaimed(Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on("questClaimed", listener);
    }

    // ---- reading and changing progress ----

    private String teamOf(String playerUuid) {
        return require().engine().teams().teamOf(UUID.fromString(playerUuid)).id;
    }

    public boolean isComplete(String playerUuid, String questId) {
        return require().engine().isComplete(teamOf(playerUuid), questId);
    }

    public long taskProgress(String playerUuid, String questId, String taskId) {
        return require().engine().taskProgress(teamOf(playerUuid), questId, taskId);
    }

    /** Completes a quest for the player's team without its tasks. */
    public boolean complete(String playerUuid, String questId) {
        return require().engine().forceComplete(teamOf(playerUuid), questId);
    }

    /** Adds progress to a task of the player's team (for scripted tasks). */
    public void addProgress(String playerUuid, String questId, String taskId, long amount) {
        QuestEngine e = require().engine();
        Quest q = e.book().quest(questId);
        Def t = q == null ? null : q.task(taskId);
        if (t != null) {
            e.addProgress(teamOf(playerUuid), q, t, amount);
        }
    }

    public void reset(String playerUuid, String questId) {
        require().engine().reset(teamOf(playerUuid), questId);
    }

    public String teamName(String playerUuid) {
        TeamManager.Team t = require().engine().teams().teamOf(UUID.fromString(playerUuid));
        return t.name;
    }

    public String bookJson() {
        return Json.compact(book().toJson());
    }
}
