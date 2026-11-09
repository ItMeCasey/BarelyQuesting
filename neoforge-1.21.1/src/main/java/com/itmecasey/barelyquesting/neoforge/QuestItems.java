package com.itmecasey.barelyquesting.neoforge;

import com.itmecasey.barelyquesting.compat.QuestBookItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the Quest Book item and its creative tab. */
final class QuestItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("barelyquesting");
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "barelyquesting");

    static final DeferredItem<Item> QUEST_BOOK = ITEMS.registerItem("quest_book", QuestBookItem::new, new Item.Properties().stacksTo(1));
    static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.barelyquesting"))
            .icon(() -> QUEST_BOOK.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(QUEST_BOOK.get()))
            .build());

    private QuestItems() {
    }

    static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
