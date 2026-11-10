package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.Quest;
import com.mojang.blaze3d.platform.InputConstants;
import com.itmecasey.barelyquesting.ui.ClientState;
import com.itmecasey.barelyquesting.ui.QuestFinder;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Ctrl+J over an item in a recipe viewer (BEI, EMI, JEI, REI) or an inventory slot opens the quest that asks for, rewards or shows that item.
 * Viewers are reached through reflection only, so none is needed to build or run the mod; a viewer that changes its API just stops answering.
 * The key is polled each client tick because screens swallow key bindings.
 */
public final class ItemQuestKey {
    private static final Logger LOG = Logger.getLogger("barelyquesting");
    private static boolean wasDown;

    private ItemQuestKey() {
    }

    @FunctionalInterface
    private interface Hover {
        ItemStack get() throws ReflectiveOperationException;
    }

    /** Called every client tick. */
    public static void tick() {
        Screen screen = McGui.screen();
        boolean down = screen != null && !(screen instanceof QuestScreen) && McGui.keyDown(InputConstants.KEY_J)
                && (McGui.keyDown(InputConstants.KEY_LCONTROL) || McGui.keyDown(InputConstants.KEY_RCONTROL));
        if (down && !wasDown) {
            String id = hoveredItem(Minecraft.getInstance(), screen);
            if (id != null) {
                openForItem(id);
            }
        }
        wasDown = down;
    }

    /** Opens the book at the first quest that mentions the item, or says there is none. Returns whether a quest was found. */
    public static boolean openForItem(String itemId) {
        Minecraft mc = Minecraft.getInstance();
        ClientState st = ClientState.get();
        if (!st.connected) {
            st.hello();
        }
        List<Quest> found = QuestFinder.forItem(st.book, itemId);
        if (found.isEmpty()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("No quest mentions " + itemId), true);
            }
            return false;
        }
        ClientHooks.openBook(found.get(0).id);
        return true;
    }

    /** The item id under the mouse on the current screen, or null. */
    public static String hoveredItemId() {
        return hoveredItem(Minecraft.getInstance(), McGui.screen());
    }

    private static String hoveredItem(Minecraft mc, Screen screen) {
        Hover[] sources = {() -> bei(mc, screen), ItemQuestKey::emi, ItemQuestKey::jei, ItemQuestKey::rei, () -> slot(screen)};
        for (Hover source : sources) {
            try {
                ItemStack stack = source.get();
                if (stack != null && !stack.isEmpty()) {
                    return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                LOG.fine("Barely Questing: a viewer did not answer the hover lookup: " + e);
            }
        }
        return null;
    }

    private static boolean has(String className) {
        try {
            Class.forName(className, false, ItemQuestKey.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static ItemStack slot(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> container) {
            Slot slot = container.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) {
                return slot.getItem();
            }
        }
        return null;
    }

    private static ItemStack bei(Minecraft mc, Screen screen) throws ReflectiveOperationException {
        if (!(screen instanceof AbstractContainerScreen<?>) || !has("com.itmecasey.bei.client.BrowserPanel")) {
            return null;
        }
        double x = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double y = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
        Method hovered = Class.forName("com.itmecasey.bei.client.BrowserPanel").getMethod("hovered", AbstractContainerScreen.class, double.class, double.class);
        return (ItemStack) hovered.invoke(null, screen, x, y);
    }

    private static ItemStack emi() throws ReflectiveOperationException {
        if (!has("dev.emi.emi.api.EmiApi")) {
            return null;
        }
        Object hovered = Class.forName("dev.emi.emi.api.EmiApi").getMethod("getHoveredStack", boolean.class).invoke(null, true);
        if (hovered == null) {
            return null;
        }
        Object stack = Class.forName("dev.emi.emi.api.stack.EmiStackInteraction").getMethod("getStack").invoke(hovered);
        if (stack == null) {
            return null;
        }
        return (ItemStack) Class.forName("dev.emi.emi.api.stack.EmiStack").getMethod("getItemStack").invoke(stack);
    }

    private static ItemStack jei() throws ReflectiveOperationException {
        if (!has("mezz.jei.api.runtime.IJeiRuntime") || !has("mezz.jei.common.Internal")) {
            return null;
        }
        Object runtime = unwrap(Class.forName("mezz.jei.common.Internal").getMethod("getJeiRuntime").invoke(null));
        if (runtime == null) {
            return null;
        }
        Class<?> runtimeType = Class.forName("mezz.jei.api.runtime.IJeiRuntime");
        Class<?> ingredientType = Class.forName("mezz.jei.api.ingredients.IIngredientType");
        Object itemType = Class.forName("mezz.jei.api.constants.VanillaTypes").getField("ITEM_STACK").get(null);
        String[][] sources = {
                {"getIngredientListOverlay", "mezz.jei.api.runtime.IIngredientListOverlay"},
                {"getBookmarkOverlay", "mezz.jei.api.runtime.IBookmarkOverlay"},
                {"getRecipesGui", "mezz.jei.api.runtime.IRecipesGui"}};
        for (String[] source : sources) {
            Object holder = runtimeType.getMethod(source[0]).invoke(runtime);
            if (holder == null) {
                continue;
            }
            Object value = unwrap(Class.forName(source[1]).getMethod("getIngredientUnderMouse", ingredientType).invoke(holder, itemType));
            if (value instanceof ItemStack stack && !stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }

    private static ItemStack rei() throws ReflectiveOperationException {
        if (!has("me.shedaniel.rei.api.client.REIRuntime")) {
            return null;
        }
        Class<?> runtimeType = Class.forName("me.shedaniel.rei.api.client.REIRuntime");
        Object overlay = unwrap(runtimeType.getMethod("getOverlay").invoke(runtimeType.getMethod("getInstance").invoke(null)));
        if (overlay == null) {
            return null;
        }
        Class<?> overlayType = Class.forName("me.shedaniel.rei.api.client.overlay.ScreenOverlay");
        Class<?> listType = Class.forName("me.shedaniel.rei.api.client.overlay.OverlayListWidget");
        Object[] lists = {overlayType.getMethod("getEntryList").invoke(overlay), unwrap(overlayType.getMethod("getFavoritesList").invoke(overlay))};
        for (Object list : lists) {
            if (list == null) {
                continue;
            }
            Object entry = listType.getMethod("getFocusedStack").invoke(list);
            if (entry == null) {
                continue;
            }
            Object value = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack").getMethod("getValue").invoke(entry);
            if (value instanceof ItemStack stack && !stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }

    private static Object unwrap(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }
}
