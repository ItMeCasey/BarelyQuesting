package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * Opens recipe and usage pages in an installed recipe viewer: EMI, REI or JEI (tried in that order). Everything goes through
 * reflection, so no viewer is needed to build or run the mod, and a viewer that changes its API only makes this return false.
 * BEI has no item lookup in its public API yet, so it is not driven from here.
 */
public final class Viewers {
    private static final Logger LOG = Logger.getLogger("barelyquesting");

    private Viewers() {
    }

    private static boolean has(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static List<String> installed() {
        List<String> out = new ArrayList<>();
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
        try {
            if (emi(stack, uses) || rei(stack, uses) || jei(stack, uses)) {
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOG.warning("Barely Questing: the recipe viewer call failed: " + e);
        }
        return false;
    }

    private static Method find(Class<?> type, String name, int params) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == params) {
                return m;
            }
        }
        return null;
    }

    private static boolean emi(ItemStack stack, boolean uses) throws ReflectiveOperationException {
        if (!has("dev.emi.emi.api.EmiApi")) {
            return false;
        }
        Object emiStack = Class.forName("dev.emi.emi.api.stack.EmiStack").getMethod("of", ItemStack.class).invoke(null, stack);
        Method show = find(Class.forName("dev.emi.emi.api.EmiApi"), uses ? "displayUses" : "displayRecipes", 1);
        if (show == null) {
            return false;
        }
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
        Method add = find(builderType, uses ? "addUsagesFor" : "addRecipesFor", 1);
        if (add == null) {
            return false;
        }
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
        Object helpers = runtime.getClass().getMethod("getJeiHelpers").invoke(runtime);
        Object focusFactory = helpers.getClass().getMethod("getFocusFactory").invoke(helpers);
        Class<?> role = Class.forName("mezz.jei.api.recipe.RecipeIngredientRole");
        Object roleValue = role.getField(uses ? "INPUT" : "OUTPUT").get(null);
        Object type = Class.forName("mezz.jei.api.constants.VanillaTypes").getField("ITEM_STACK").get(null);
        Method create = find(focusFactory.getClass(), "createFocus", 3);
        if (create == null) {
            return false;
        }
        Object focus = create.invoke(focusFactory, roleValue, type, stack);
        Object gui = runtime.getClass().getMethod("getRecipesGui").invoke(runtime);
        Method show = find(gui.getClass(), "show", 1);
        if (show == null) {
            return false;
        }
        show.invoke(gui, focus);
        return true;
    }
}
