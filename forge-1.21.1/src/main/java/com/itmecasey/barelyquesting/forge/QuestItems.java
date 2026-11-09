package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.compat.QuestBookItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registers the Quest Book item and its creative tab. */
final class QuestItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "barelyquesting");
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "barelyquesting");

    static final RegistryObject<Item> QUEST_BOOK = ITEMS.register("quest_book", () -> new QuestBookItem(new Item.Properties().stacksTo(1)));
    static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
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
