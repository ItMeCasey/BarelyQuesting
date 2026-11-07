package com.itmecasey.barelyquesting.core.task;

import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.script.ScriptRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** The registry of task types and the built-in ones. Scripts add more with {@link #register}. */
public final class TaskTypes {
    private static final Map<String, TaskType> TYPES = new LinkedHashMap<>();

    private TaskTypes() {
    }

    public static synchronized void register(TaskType type) {
        TYPES.put(type.id(), type);
    }

    public static synchronized TaskType get(String id) {
        return TYPES.get(id);
    }

    public static synchronized Collection<TaskType> all() {
        return new java.util.ArrayList<>(TYPES.values());
    }

    /** Removes everything scripts added, keeping the built-ins (used on /reload of scripts). */
    public static synchronized void resetToBuiltins() {
        TYPES.clear();
        registerBuiltins();
    }

    static {
        registerBuiltins();
    }

    private static void registerBuiltins() {
        register(new ItemTask());
        register(new EventItemTask("craft", "craft"));
        register(new ObtainTask());
        register(new KillTask());
        register(new AdvancementTask());
        register(new StatisticTask());
        register(new LocationTask());
        register(new SimplePolled("dimension") {
            @Override
            boolean test(Def t, PlayerView p) {
                return Matchers.idMatches("dimension", t.str("dimension", "minecraft:overworld"), p.dimension());
            }

            @Override
            public String describe(Def t) {
                return "Visit " + t.str("dimension", "minecraft:overworld");
            }
        });
        register(new SimplePolled("biome") {
            @Override
            boolean test(Def t, PlayerView p) {
                return Matchers.idMatches("biome", t.str("biome", ""), p.biome());
            }

            @Override
            public String describe(Def t) {
                return "Visit biome " + t.str("biome", "?");
            }
        });
        register(new SimplePolled("structure") {
            @Override
            boolean test(Def t, PlayerView p) {
                return p.inStructure(t.str("structure", ""));
            }

            @Override
            public String describe(Def t) {
                return "Find " + t.str("structure", "?");
            }
        });
        register(new CheckmarkTask());
        register(new ScriptedTask());
    }

    private static ItemSpec itemOf(Def t) {
        ItemSpec s = t.item("item");
        return s.count() <= 0 ? s.withCount(1) : s;
    }

    /** Hold items (polled) or hand them in (consume). */
    private static final class ItemTask implements TaskType {
        @Override
        public String id() {
            return "item";
        }

        @Override
        public long target(Def t) {
            return Math.max(1, itemOf(t).count());
        }

        @Override
        public boolean polled() {
            return true;
        }

        @Override
        public long poll(Def t, PlayerView p, long current) {
            if (t.bool("consume", false)) {
                return current;
            }
            return Math.min(target(t), p.countItems(itemOf(t)));
        }

        @Override
        public boolean submits(Def t) {
            return t.bool("consume", false);
        }

        @Override
        public String describe(Def t) {
            return (t.bool("consume", false) ? "Submit " : "Have ") + itemOf(t);
        }
    }

    /** Counts items matching an event kind (craft). */
    private static class EventItemTask implements TaskType {
        private final String id;
        private final String kind;

        EventItemTask(String id, String kind) {
            this.id = id;
            this.kind = kind;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public long target(Def t) {
            return Math.max(1, itemOf(t).count());
        }

        @Override
        public long onEvent(Def t, TaskEvent e) {
            if (e.item == null || !accepts(e.kind) || !Matchers.itemMatches(itemOf(t).withCount(1), e.item)) {
                return 0;
            }
            return e.amount;
        }

        boolean accepts(String eventKind) {
            return kind.equals(eventKind);
        }

        @Override
        public String describe(Def t) {
            return "Craft " + itemOf(t);
        }
    }

    /** Obtain: any way of getting the item counts (picking up or crafting), and it is not consumed. */
    private static final class ObtainTask extends EventItemTask {
        ObtainTask() {
            super("obtain", "pickup");
        }

        @Override
        boolean accepts(String eventKind) {
            return eventKind.equals("pickup") || eventKind.equals("craft");
        }

        @Override
        public String describe(Def t) {
            return "Obtain " + itemOf(t);
        }
    }

    private static final class KillTask implements TaskType {
        @Override
        public String id() {
            return "kill";
        }

        @Override
        public long onEvent(Def t, TaskEvent e) {
            return e.kind.equals("kill") && Matchers.idMatches("entity", t.str("entity", ""), e.key) ? e.amount : 0;
        }

        @Override
        public String describe(Def t) {
            return "Kill " + t.num("count", 1) + " " + t.str("entity", "any mob");
        }
    }

    private static final class AdvancementTask implements TaskType {
        @Override
        public String id() {
            return "advancement";
        }

        @Override
        public long target(Def t) {
            return 1;
        }

        @Override
        public boolean polled() {
            return true;
        }

        @Override
        public long poll(Def t, PlayerView p, long current) {
            return p.hasAdvancement(t.str("advancement", "")) ? 1 : current;
        }

        @Override
        public long onEvent(Def t, TaskEvent e) {
            return e.kind.equals("advancement") && e.key.equals(t.str("advancement", "")) ? 1 : 0;
        }

        @Override
        public String describe(Def t) {
            return "Earn " + t.str("advancement", "?");
        }
    }

    private static final class StatisticTask implements TaskType {
        @Override
        public String id() {
            return "statistic";
        }

        @Override
        public long target(Def t) {
            return Math.max(1, t.num("value", 1));
        }

        @Override
        public boolean polled() {
            return true;
        }

        @Override
        public long poll(Def t, PlayerView p, long current) {
            return Math.min(target(t), p.statistic(t.str("stat_type", "custom"), t.str("stat", "")));
        }

        @Override
        public String describe(Def t) {
            return t.str("stat", "?") + " " + t.num("value", 1);
        }
    }

    private static final class LocationTask extends SimplePolled {
        LocationTask() {
            super("location");
        }

        @Override
        boolean test(Def t, PlayerView p) {
            String dim = t.str("dimension", "");
            if (!dim.isEmpty() && !dim.equals(p.dimension())) {
                return false;
            }
            double r = Math.max(1, t.num("radius", 5));
            double dx = p.x() - t.num("x", 0);
            double dy = p.y() - t.num("y", 64);
            double dz = p.z() - t.num("z", 0);
            return dx * dx + dy * dy + dz * dz <= r * r;
        }

        @Override
        public String describe(Def t) {
            return "Go to " + t.num("x", 0) + " " + t.num("y", 64) + " " + t.num("z", 0);
        }
    }

    /** A yes/no task that polls the player. */
    private abstract static class SimplePolled implements TaskType {
        private final String id;

        SimplePolled(String id) {
            this.id = id;
        }

        abstract boolean test(Def t, PlayerView p);

        @Override
        public String id() {
            return id;
        }

        @Override
        public long target(Def t) {
            return 1;
        }

        @Override
        public boolean polled() {
            return true;
        }

        @Override
        public long poll(Def t, PlayerView p, long current) {
            return test(t, p) ? 1 : current;
        }
    }

    private static final class CheckmarkTask implements TaskType {
        @Override
        public String id() {
            return "checkmark";
        }

        @Override
        public long target(Def t) {
            return 1;
        }

        @Override
        public boolean manual(Def t) {
            return true;
        }

        @Override
        public String describe(Def t) {
            return t.str("text", "Check this off");
        }
    }

    /** A task whose progress is decided by a script: by name, through callbacks or by the script adding progress. */
    private static final class ScriptedTask implements TaskType {
        @Override
        public String id() {
            return "scripted";
        }

        @Override
        public long target(Def t) {
            return Math.max(1, t.num("count", 1));
        }

        @Override
        public boolean polled() {
            return true;
        }

        @Override
        public long poll(Def t, PlayerView p, long current) {
            ScriptRegistry.TaskCallback cb = ScriptRegistry.task(t.str("name", ""));
            return cb == null ? current : cb.poll(t, p, current);
        }

        @Override
        public long onEvent(Def t, TaskEvent e) {
            ScriptRegistry.TaskCallback cb = ScriptRegistry.task(t.str("name", ""));
            return cb == null ? 0 : cb.onEvent(t, e);
        }

        @Override
        public String describe(Def t) {
            return t.str("text", t.str("name", "Scripted task"));
        }
    }
}
