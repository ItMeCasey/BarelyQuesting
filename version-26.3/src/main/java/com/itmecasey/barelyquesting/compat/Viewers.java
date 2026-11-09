package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import java.lang.reflect.Method;
import com.itmecasey.barelyquesting.core.script.ViewerDispatch;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * Opens recipe and usage pages in an installed recipe viewer: BEI, EMI, REI or JEI (tried in that order). Everything goes through
 * reflection, so no viewer is needed to build or run the mod, and a viewer that changes its API only makes this return false.
 * BEI currently exposes its lookup through an internal popup; a container host makes it available from the quest screen.
 */
public final class Viewers {
    private Viewers() {
    }

    private static boolean has(String className) {
        try {
            Class.forName(className, false, Viewers.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static List<String> installed() {
        List<String> out = new ArrayList<>();
        if (has("com.itmecasey.bei.client.RecipePopup")) out.add("BEI");
        if (has("dev.emi.emi.api.EmiApi")) {
            out.add("EMI");
        }
        if (has("me.shedaniel.rei.api.client.view.ViewSearchBuilder")) {
            out.add("REI");
        }
        if (has("mezz.jei.api.runtime.IJeiRuntime")) {
            out.add("JEI");
        }
        return out;
    }

    public static boolean show(ItemSpec spec, boolean uses) {
        ItemStack stack = Mc.stack(spec, Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.registryAccess(),
                System.currentTimeMillis());
        if (stack.isEmpty()) {
            return false;
        }
        return ViewerDispatch.first(() -> bei(stack, uses), () -> emi(stack, uses), () -> rei(stack, uses), () -> jei(stack, uses));
    }

    private static boolean bei(ItemStack stack, boolean uses) throws ReflectiveOperationException {
        if (!has("com.itmecasey.bei.client.RecipePopup")) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        // Resolve the optional API before switching screens; restore the quest screen if the call fails.
        Method open = Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("open", net.minecraft.world.item.Item.class, boolean.class);
        Method close = Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("close");
        Method isOpen = Class.forName("com.itmecasey.bei.client.RecipePopup").getMethod("isOpen");
        var previous = McGui.screen();
        McGui.setScreen(new net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<net.minecraft.world.inventory.InventoryMenu>(
                mc.player.inventoryMenu, mc.player.getInventory(), net.minecraft.network.chat.Component.literal("Recipes")) {
            @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, float tick) {
                if (previous != null) previous.extractRenderState(graphics, x, y, tick);
            }
            @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int x, int y, float tick) { }
            @Override protected void containerTick() {
                boolean open = ViewerDispatch.first(() -> (Boolean) isOpen.invoke(null));
                if (!open) onClose();
            }
            @Override public void onClose() { McGui.setScreen(previous); }
            @Override public void removed() {
                super.removed();
                ViewerDispatch.first(() -> { close.invoke(null); return true; });
            }
        });
        try {
            open.invoke(null, stack.getItem(), uses);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            McGui.setScreen(previous);
            throw e;
        }
    }

    private static boolean emi(ItemStack stack, boolean uses) throws ReflectiveOperationException {
        if (!has("dev.emi.emi.api.EmiApi")) {
            return false;
        }
        Object emiStack = Class.forName("dev.emi.emi.api.stack.EmiStack").getMethod("of", ItemStack.class).invoke(null, stack);
        Method show = Class.forName("dev.emi.emi.api.EmiApi").getMethod(uses ? "displayUses" : "displayRecipes",
                Class.forName("dev.emi.emi.api.stack.EmiIngredient"));
        show.invoke(null, emiStack);
        return true;
    }

    private static boolean rei(ItemStack stack, boolean uses) throws ReflectiveOperationException {
        if (!has("me.shedaniel.rei.api.client.view.ViewSearchBuilder")) {
            return false;
        }
        Object entry = Class.forName("me.shedaniel.rei.api.common.util.EntryStacks").getMethod("of", ItemStack.class).invoke(null, stack);
        Class<?> builderType = Class.forName("me.shedaniel.rei.api.client.view.ViewSearchBuilder");
        Object builder = builderType.getMethod("builder").invoke(null);
        Class<?> entryType = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");
        Method add = builderType.getMethod(uses ? "addUsagesFor" : "addRecipesFor", entryType);
        add.invoke(builder, entry);
        Object result = builderType.getMethod("open").invoke(builder);
        return !(result instanceof Boolean) || (Boolean) result;
    }

    private static boolean jei(ItemStack stack, boolean uses) throws ReflectiveOperationException {
        if (!has("mezz.jei.api.runtime.IJeiRuntime") || !has("mezz.jei.common.Internal")) {
            return false;
        }
        Object runtime = Class.forName("mezz.jei.common.Internal").getMethod("getJeiRuntime").invoke(null);
        if (runtime instanceof java.util.Optional) {
            runtime = ((java.util.Optional<?>) runtime).orElse(null);
        }
        if (runtime == null) {
            return false;
        }
        Object helpers = Class.forName("mezz.jei.api.runtime.IJeiRuntime").getMethod("getJeiHelpers").invoke(runtime);
        Object focusFactory = Class.forName("mezz.jei.api.helpers.IJeiHelpers").getMethod("getFocusFactory").invoke(helpers);
        Class<?> role = Class.forName("mezz.jei.api.recipe.RecipeIngredientRole");
        Object roleValue = role.getField(uses ? "INPUT" : "OUTPUT").get(null);
        Object type = Class.forName("mezz.jei.api.constants.VanillaTypes").getField("ITEM_STACK").get(null);
        Method create = Class.forName("mezz.jei.api.recipe.IFocusFactory").getMethod("createFocus",
                role, Class.forName("mezz.jei.api.ingredients.IIngredientType"), Object.class);
        Object focus = create.invoke(focusFactory, roleValue, type, stack);
        Object gui = Class.forName("mezz.jei.api.runtime.IJeiRuntime").getMethod("getRecipesGui").invoke(runtime);
        Method show = Class.forName("mezz.jei.api.runtime.IRecipesGui").getMethod("show",
                Class.forName("mezz.jei.api.recipe.IFocus"));
        show.invoke(gui, focus);
        return true;
    }
}
