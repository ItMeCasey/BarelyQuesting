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

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The one scripting surface. The ModJS and KubeJS bridges expose an instance of this class to scripts as {@code BarelyQuesting}, and
 * Java mods can use it directly. All methods use plain strings, numbers and JSON text so any script engine can call them.
 *
 * <p>Changes made here go through the same edit operations as the in-game editor, so they are saved and synced to every player.
 */
public final class QuestApi {
    private static volatile QuestServer server;

    /** Used by the loader once the server is running. */
    public static void attach(QuestServer s) {
        server = s;
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
        Chapter c = book().chapter(id);
        Chapter n = new Chapter(id);
        n.title = title;
        n.icon = ItemSpec.of(icon == null || icon.isEmpty() ? "minecraft:book" : icon);
        if (c != null) {
            n.group = c.group;
        }
        require().applyEdit(null, EditOps.putChapter(n, null), null);
        return id;
    }

    /**
     * Adds or replaces a quest from JSON text, in the quest file format ({@code title}, {@code icon}, {@code x}, {@code y},
     * {@code dependencies}, {@code tasks}, {@code rewards}, ...). An empty {@code id} makes a new random one. Returns the quest id.
     */
    public String quest(String chapterId, String questJson) {
        JsonObject o = Json.parseObject(questJson);
        if (Json.str(o, "id", "").isEmpty()) {
            o.addProperty("id", book().newQuestId());
        }
        Quest q = Quest.fromJson(o);
        if (!require().applyEdit(null, EditOps.putQuest(chapterId, q, null), null)) {
            throw new IllegalArgumentException("Could not add quest to chapter " + chapterId);
        }
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
