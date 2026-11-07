package com.itmecasey.barelyquesting.core.task;

import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.progress.PlayerView;

/**
 * Behavior of one task type. A task has a numeric progress that never goes down and is done at {@link #target}. Types either react to
 * game events ({@link #onEvent}), look at the player now and then ({@link #poll}), are submitted by the player, or are set by scripts.
 */
public interface TaskType {
    String id();

    /** Progress at which the task is complete. */
    default long target(Def task) {
        return Math.max(1, task.num("count", 1));
    }

    /** Types that look at the player about once a second. */
    default boolean polled() {
        return false;
    }

    /** New progress for a polled type (the engine keeps the higher of this and the old value). */
    default long poll(Def task, PlayerView player, long current) {
        return current;
    }

    /** Progress to add for a game event (0 when the event does not match). */
    default long onEvent(Def task, TaskEvent event) {
        return 0;
    }

    /** True if the player can hand items in with the Submit button. */
    default boolean submits(Def task) {
        return false;
    }

    /** True if the player ticks the task off by hand. */
    default boolean manual(Def task) {
        return false;
    }

    /** Short text for the quest detail, e.g. "Kill 5 minecraft:zombie". */
    default String describe(Def task) {
        return id();
    }
}
