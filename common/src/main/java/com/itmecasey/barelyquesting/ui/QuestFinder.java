package com.itmecasey.barelyquesting.ui;

import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;
import java.util.ArrayList;
import java.util.List;

/** Finds the quests that mention an item, for the "open the quest for this item" shortcut in recipe viewers and inventories. */
public final class QuestFinder {
    private QuestFinder() {
    }

    /** Quests whose item, craft or obtain tasks ask for the item come first, then quests that reward it, then quests that use it as their icon. */
    public static List<Quest> forItem(Book book, String itemId) {
        List<Quest> byTask = new ArrayList<>();
        List<Quest> byReward = new ArrayList<>();
        List<Quest> byIcon = new ArrayList<>();
        if (book == null || itemId == null) {
            return byTask;
        }
        String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        for (Quest q : book.allQuests()) {
            if (anyMatch(q.tasks, id, "item", "craft", "obtain")) {
                byTask.add(q);
            } else if (anyMatch(q.rewards, id, "item")) {
                byReward.add(q);
            } else if (!q.icon.isTag() && q.icon.id().equals(id)) {
                byIcon.add(q);
            }
        }
        byTask.addAll(byReward);
        byTask.addAll(byIcon);
        return byTask;
    }

    private static boolean anyMatch(List<Def> defs, String id, String... types) {
        for (Def d : defs) {
            boolean typeOk = false;
            for (String t : types) {
                typeOk |= t.equals(d.type);
            }
            if (!typeOk || d.params == null || !d.params.has("item")) {
                continue;
            }
            ItemSpec s = d.item("item");
            if (!s.isTag() && s.id().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
