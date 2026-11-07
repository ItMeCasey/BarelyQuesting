package com.itmecasey.barelyquesting.ui;

import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.progress.Rewards;
import com.itmecasey.barelyquesting.core.task.TaskType;
import com.itmecasey.barelyquesting.core.task.TaskTypes;

import java.util.Locale;

/** Readable one-line texts for tasks and rewards, using the game's item names where the task names an item. */
public final class TaskText {
    private TaskText() {
    }

    /** {@code minecraft:the_nether} becomes {@code The Nether}. */
    public static String pretty(String id) {
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder sb = new StringBuilder();
        for (String part : s.replace('/', ' ').replace('_', ' ').split(" ")) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return sb.toString();
    }

    private static String itemText(Gfx g, ItemSpec s) {
        String name = s.isTag() ? "any " + pretty(s.id()) : g.itemName(s.withCount(1));
        return (s.count() > 1 ? s.count() + "x " : "") + name;
    }

    public static String task(Gfx g, Def t) {
        TaskType type = TaskTypes.get(t.type);
        switch (t.type) {
            case "item":
                return (t.bool("consume", false) ? "Submit " : "Have ") + itemText(g, t.item("item"));
            case "craft":
                return "Craft " + itemText(g, t.item("item"));
            case "obtain":
                return "Obtain " + itemText(g, t.item("item"));
            case "kill":
                return "Kill " + t.num("count", 1) + " " + (t.str("entity", "").isEmpty() ? "mobs" : pretty(t.str("entity", "")));
            case "advancement":
                return "Earn " + pretty(t.str("advancement", "?"));
            case "dimension":
                return "Visit " + pretty(t.str("dimension", "?"));
            case "biome":
                return "Visit the " + pretty(t.str("biome", "?")) + " biome";
            case "structure":
                return "Find a " + pretty(t.str("structure", "?"));
            case "statistic":
                return pretty(t.str("stat", "?")) + ": " + t.num("value", 1);
            default:
                return type == null ? t.type : type.describe(t);
        }
    }

    public static String reward(Gfx g, Def r) {
        Rewards.RewardType type = Rewards.reward(r.type);
        String text = r.type.equals("item") ? itemText(g, r.item("item")) : type == null ? r.type : type.describe(r);
        return QuestEngineScope.isTeam(r) ? text + " (team)" : text;
    }

    /** Keeps the scope check in one place without the UI depending on the engine class. */
    private static final class QuestEngineScope {
        static boolean isTeam(Def r) {
            return r.str("scope", "player").equals("team");
        }
    }
}
