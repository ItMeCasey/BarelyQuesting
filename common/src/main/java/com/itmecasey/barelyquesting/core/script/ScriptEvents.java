package com.itmecasey.barelyquesting.core.script;

import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.Quest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Events that scripts can listen to: {@code questStarted}, {@code taskProgressed}, {@code questCompleted}, {@code questClaimed}. */
public final class ScriptEvents {
    /** What a listener receives. {@code task} is set for taskProgressed, {@code reward} and {@code player} for questClaimed. */
    public static final class Event {
        public final String name;
        public final String team;
        public final Quest quest;
        public final Def def;
        public final UUID player;

        Event(String name, String team, Quest quest, Def def, UUID player) {
            this.name = name;
            this.team = team;
            this.quest = quest;
            this.def = def;
            this.player = player;
        }

        public String questId() {
            return quest.id;
        }

        public String questTitle() {
            return quest.title;
        }

        public String teamId() {
            return team;
        }

        public String taskId() {
            return def == null ? "" : def.id;
        }

        public String playerId() {
            return player == null ? "" : player.toString();
        }
    }

    private static final Map<String, List<Consumer<Event>>> LISTENERS = new ConcurrentHashMap<>();

    private ScriptEvents() {
    }

    public static void on(String name, Consumer<Event> listener) {
        LISTENERS.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static void fire(String name, String team, Quest quest, Def def, UUID player) {
        List<Consumer<Event>> list = LISTENERS.get(name);
        if (list == null) {
            return;
        }
        Event e = new Event(name, team, quest, def, player);
        for (Consumer<Event> l : list) {
            try {
                l.accept(e);
            } catch (RuntimeException ex) {
                java.util.logging.Logger.getLogger("barelyquesting").warning("Script listener for " + name + " failed: " + ex);
            }
        }
    }

    public static void clear() {
        LISTENERS.clear();
    }
}
