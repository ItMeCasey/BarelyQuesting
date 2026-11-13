package com.itmecasey.barelyquesting.core;

import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;

/** The starter book written the first time the mod runs, so a new world has something to show and to edit. */
public final class DefaultBook {
    private DefaultBook() {
    }

    public static Book create() {
        Book book = new Book();
        book.title = "Quest Book";

        Chapter start = new Chapter("getting_started");
        start.title = "Getting Started";
        start.icon = ItemSpec.of("minecraft:oak_sapling");
        book.chapters.add(start);

        Quest wood = quest(start, "wood", "Gather Wood", "Punch a tree and **collect 4 logs**.",
                "minecraft:oak_log", 0, 0);
        wood.tasks.add(Def.of("t0", "obtain").with("item", "4 #minecraft:logs"));
        wood.rewards.add(Def.of("r0", "item").with("item", "8 minecraft:apple"));

        Quest bench = quest(start, "bench", "Crafting Table", "Turn the wood into planks, then craft a **crafting table**.",
                "minecraft:crafting_table", 2, 0);
        bench.dependencies.add("wood");
        bench.tasks.add(Def.of("t0", "craft").with("item", "minecraft:crafting_table"));
        bench.rewards.add(Def.of("r0", "xp").with("amount", 5).with("unit", "levels"));

        Quest pick = quest(start, "pickaxe", "Wooden Pickaxe", "Craft a pickaxe and head underground.", "minecraft:wooden_pickaxe", 4, 0);
        pick.dependencies.add("bench");
        pick.tasks.add(Def.of("t0", "craft").with("item", "minecraft:wooden_pickaxe"));

        Quest stone = quest(start, "stone", "Stone Age", "Mine and **submit 16 cobblestone**.", "minecraft:cobblestone", 6, -1);
        stone.dependencies.add("pickaxe");
        stone.tasks.add(Def.of("t0", "item").with("item", "16 minecraft:cobblestone").with("consume", true));
        stone.rewards.add(Def.of("r0", "item").with("item", "minecraft:stone_pickaxe"));

        Quest zombie = quest(start, "zombie", "Night Watch", "Defeat 3 zombies after dark.", "minecraft:rotten_flesh", 6, 1);
        zombie.dependencies.add("pickaxe");
        zombie.tasks.add(Def.of("t0", "kill").with("entity", "minecraft:zombie").with("count", 3));
        zombie.rewards.add(Def.of("r0", "xp").with("amount", 100));

        Chapter world = new Chapter("exploring");
        world.title = "Exploring";
        world.icon = ItemSpec.of("minecraft:compass");
        book.chapters.add(world);

        Quest nether = quest(world, "nether", "Into the Nether", "Build a portal and **step through**.", "minecraft:obsidian", 0, 0);
        nether.tasks.add(Def.of("t0", "dimension").with("dimension", "minecraft:the_nether"));

        Quest forest = quest(world, "forest", "Forest Walk", "Find a forest biome.", "minecraft:oak_leaves", 2, 0);
        forest.tasks.add(Def.of("t0", "biome").with("biome", "minecraft:forest"));

        Quest village = quest(world, "village", "Neighbors", "Find a village.", "minecraft:bell", 4, 0);
        village.dependencies.add("forest");
        village.tasks.add(Def.of("t0", "structure").with("structure", "minecraft:village_plains"));

        Quest done = quest(world, "check", "Take a Break", "Take a break.", "minecraft:clock", 2, 2);
        done.tasks.add(Def.of("t0", "checkmark").with("text", "I took a break"));
        return book;
    }

    private static Quest quest(Chapter c, String id, String title, String text, String icon, int x, int y) {
        Quest q = new Quest(id);
        q.title = title;
        q.description = text;
        q.icon = ItemSpec.of(icon);
        q.x = x;
        q.y = y;
        c.quests.add(q);
        return q;
    }
}
