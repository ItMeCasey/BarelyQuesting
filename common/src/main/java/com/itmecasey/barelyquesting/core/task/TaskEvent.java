package com.itmecasey.barelyquesting.core.task;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.progress.PlayerView;

/**
 * Something a player did. {@code kind} is {@code kill}, {@code craft}, {@code pickup} or {@code advancement}; {@code key} is the entity
 * id, item id or advancement id. Items carry their data text so tasks can match item data.
 */
public final class TaskEvent {
    public final String kind;
    public final String key;
    public final long amount;
    public final PlayerView player;
    public final ItemSpec item;

    public TaskEvent(String kind, String key, long amount, PlayerView player, ItemSpec item) {
        this.kind = kind;
        this.key = key;
        this.amount = amount;
        this.player = player;
        this.item = item;
    }

    public static TaskEvent kill(PlayerView p, String entityId) {
        return new TaskEvent("kill", entityId, 1, p, null);
    }

    public static TaskEvent craft(PlayerView p, ItemSpec item) {
        return new TaskEvent("craft", item.id(), item.count(), p, item);
    }

    public static TaskEvent pickup(PlayerView p, ItemSpec item) {
        return new TaskEvent("pickup", item.id(), item.count(), p, item);
    }

    public static TaskEvent advancement(PlayerView p, String id) {
        return new TaskEvent("advancement", id, 1, p, null);
    }
}
