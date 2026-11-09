package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.ui.ClientEnv;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** Minecraft 26.1.2: what the client-side book needs from the game. */
public final class McEnv implements ClientEnv {
    private static final List<String> ADVANCEMENTS = Arrays.asList(
            "minecraft:story/root", "minecraft:story/mine_stone", "minecraft:story/upgrade_tools", "minecraft:story/smelt_iron",
            "minecraft:story/obtain_armor", "minecraft:story/lava_bucket", "minecraft:story/iron_tools", "minecraft:story/deflect_arrow",
            "minecraft:story/form_obsidian", "minecraft:story/mine_diamond", "minecraft:story/enter_the_nether", "minecraft:story/shiny_gear",
            "minecraft:story/enchant_item", "minecraft:story/cure_zombie_villager", "minecraft:story/follow_ender_eye",
            "minecraft:story/enter_the_end", "minecraft:nether/root", "minecraft:nether/return_to_sender", "minecraft:nether/find_fortress",
            "minecraft:nether/obtain_blaze_rod", "minecraft:nether/get_wither_skull", "minecraft:nether/summon_wither",
            "minecraft:nether/all_potions", "minecraft:nether/create_beacon", "minecraft:end/root", "minecraft:end/kill_dragon",
            "minecraft:end/enter_end_gateway", "minecraft:end/respawn_dragon", "minecraft:end/find_end_city", "minecraft:end/elytra",
            "minecraft:adventure/root", "minecraft:adventure/kill_a_mob", "minecraft:adventure/trade", "minecraft:adventure/sleep_in_bed",
            "minecraft:adventure/shoot_arrow", "minecraft:adventure/voluntary_exile", "minecraft:adventure/totem_of_undying",
            "minecraft:adventure/hero_of_the_village", "minecraft:husbandry/root", "minecraft:husbandry/breed_an_animal",
            "minecraft:husbandry/tame_an_animal", "minecraft:husbandry/plant_seed", "minecraft:husbandry/balanced_diet",
            "minecraft:husbandry/fishy_business", "minecraft:husbandry/bred_all_animals", "minecraft:husbandry/obtain_netherite_hoe");
    private static final List<String> STRUCTURES = Arrays.asList(
            "minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna", "minecraft:village_snowy",
            "minecraft:village_taiga", "minecraft:stronghold", "minecraft:mineshaft", "minecraft:fortress", "minecraft:bastion_remnant",
            "minecraft:end_city", "minecraft:ancient_city", "minecraft:monument", "minecraft:mansion", "minecraft:desert_pyramid",
            "minecraft:jungle_pyramid", "minecraft:igloo", "minecraft:swamp_hut", "minecraft:pillager_outpost", "minecraft:shipwreck",
            "minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm", "minecraft:ruined_portal", "minecraft:buried_treasure",
            "minecraft:trail_ruins", "minecraft:trial_chambers");
    private static final List<String> DIMENSIONS = Arrays.asList("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");

    private static volatile ResourceManager resources;

    public static void setResources(ResourceManager manager) {
        resources = manager;
    }

    @Override
    public List<String> itemIds() {
        return Mc.itemIds();
    }

    @Override
    public List<String> ids(String kind) {
        Minecraft mc = Minecraft.getInstance();
        RegistryAccess access = mc.level == null ? null : mc.level.registryAccess();
        switch (kind) {
            case "entity":
                return Mc.entityIds();
            case "biome":
                return Mc.keys(access, Registries.BIOME);
            case "structure": {
                List<String> out = new ArrayList<>(Mc.keys(access, Registries.STRUCTURE));
                for (String s : STRUCTURES) {
                    if (!out.contains(s)) {
                        out.add(s);
                    }
                }
                return out;
            }
            case "dimension": {
                List<String> out = new ArrayList<>(DIMENSIONS);
                for (String s : Mc.keys(access, Registries.DIMENSION)) {
                    if (!out.contains(s)) {
                        out.add(s);
                    }
                }
                return out;
            }
            case "advancement":
                return ADVANCEMENTS;
            default:
                return new ArrayList<>();
        }
    }

    @Override
    public void sendToServer(byte[] message) {
        Mc.toServer.accept(message);
    }

    @Override
    public Path configDir() {
        return Mc.configDir.resolve("barelyquesting");
    }

    @Override
    public boolean showRecipes(ItemSpec item) {
        return Viewers.show(item, false);
    }

    @Override
    public boolean showUses(ItemSpec item) {
        return Viewers.show(item, true);
    }

    @Override
    public List<String> viewers() {
        return Viewers.installed();
    }

    @Override
    public Map<String, String> themeFiles(String name) {
        Map<String, String> out = new LinkedHashMap<>();
        ResourceManager rm = resources != null ? resources : Minecraft.getInstance().getResourceManager();
        for (String ns : rm.getNamespaces()) {
            Optional<Resource> r = rm.getResource(Mc.id(ns, "barelyquesting/themes/" + name));
            if (r.isPresent()) {
                try (InputStream in = r.get().open()) {
                    out.put(ns, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    // skip unreadable files
                }
            }
        }
        return out;
    }

    @Override
    public void openUrl(String url) {
        net.minecraft.util.Util.getPlatform().openUri(url);
    }

    @Override
    public String playerName() {
        return Minecraft.getInstance().getUser().getName();
    }
}
