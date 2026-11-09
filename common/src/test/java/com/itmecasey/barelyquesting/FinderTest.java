package com.itmecasey.barelyquesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itmecasey.barelyquesting.core.DefaultBook;
import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.ui.QuestFinder;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinderTest {
    @Test
    void craftTaskItemFindsItsQuest() {
        List<Quest> found = QuestFinder.forItem(DefaultBook.create(), "minecraft:crafting_table");
        assertEquals("bench", found.get(0).id);
    }

    @Test
    void rewardItemFindsItsQuestAndBareNamesWork() {
        List<Quest> found = QuestFinder.forItem(DefaultBook.create(), "stone_pickaxe");
        assertTrue(found.stream().anyMatch(q -> q.id.equals("stone")));
    }

    @Test
    void taskMatchesComeBeforeRewardMatches() {
        List<Quest> found = QuestFinder.forItem(DefaultBook.create(), "minecraft:wooden_pickaxe");
        assertEquals("pickaxe", found.get(0).id);
    }

    @Test
    void unknownItemAndEmptyBook() {
        assertTrue(QuestFinder.forItem(DefaultBook.create(), "minecraft:bedrock").isEmpty());
        assertTrue(QuestFinder.forItem(new Book(), "minecraft:stone").isEmpty());
        assertTrue(QuestFinder.forItem(null, "minecraft:stone").isEmpty());
    }
}
