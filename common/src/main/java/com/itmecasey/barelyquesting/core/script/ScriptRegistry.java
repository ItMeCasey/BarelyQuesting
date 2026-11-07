package com.itmecasey.barelyquesting.core.script;

import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.task.TaskEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Callbacks that scripts register by name. {@code scripted} tasks, rewards and locks in the quest files refer to these names, and the
 * ModJS and KubeJS bridges fill them from script functions. Everything here is plain Java, so tests can use it directly.
 */
public final class ScriptRegistry {
    /** Progress logic of a scripted task. */
    public interface TaskCallback {
        default long poll(Def task, PlayerView player, long current) {
            return current;
        }

        default long onEvent(Def task, TaskEvent event) {
            return 0;
        }
    }

    /** What a scripted reward does when it is claimed. */
    public interface RewardCallback {
        void give(Def reward, PlayerView player);
    }

    /** Whether a script lock is closed for a team (any online member decides is up to the engine). */
    public interface LockCallback {
        boolean locked(Def lock, String teamId, String questId);
    }

    private static final Map<String, TaskCallback> TASKS = new ConcurrentHashMap<>();
    private static final Map<String, RewardCallback> REWARDS = new ConcurrentHashMap<>();
    private static final Map<String, LockCallback> LOCKS = new ConcurrentHashMap<>();

    private ScriptRegistry() {
    }

    public static void task(String name, TaskCallback cb) {
        TASKS.put(name, cb);
    }

    public static TaskCallback task(String name) {
        return TASKS.get(name);
    }

    public static void reward(String name, RewardCallback cb) {
        REWARDS.put(name, cb);
    }

    public static RewardCallback reward(String name) {
        return REWARDS.get(name);
    }

    public static void lock(String name, LockCallback cb) {
        LOCKS.put(name, cb);
    }

    public static LockCallback lock(String name) {
        return LOCKS.get(name);
    }

    public static void clear() {
        TASKS.clear();
        REWARDS.clear();
        LOCKS.clear();
    }
}
