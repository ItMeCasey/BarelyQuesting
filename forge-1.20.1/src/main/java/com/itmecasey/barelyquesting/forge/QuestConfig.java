package com.itmecasey.barelyquesting.forge;

import com.itmecasey.barelyquesting.core.config.ConfigKeys;
import com.itmecasey.barelyquesting.core.config.ConfigMigration;
import com.itmecasey.barelyquesting.core.config.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * The mod's settings as ForgeConfigSpec files (barelyquesting-common.toml for the server side, barelyquesting-client.toml for the player), built from
 * the one key list in {@link ConfigKeys}. Loaded and reloaded values are pushed into {@link Settings}; values the mod changes itself go
 * back to the file through {@link #write}. Settings that older versions kept in JSON are migrated first (see {@link ConfigMigration}).
 */
@Mod.EventBusSubscriber(modid = "barelyquesting", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class QuestConfig {
    private static final Logger LOG = Logger.getLogger("barelyquesting");
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> VALUES = new HashMap<>();
    private static final ForgeConfigSpec COMMON = build(ConfigKeys.Side.COMMON);
    private static final ForgeConfigSpec CLIENT = build(ConfigKeys.Side.CLIENT);

    private QuestConfig() {
    }

    private static ForgeConfigSpec build(ConfigKeys.Side side) {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        for (ConfigKeys.Key k : ConfigKeys.keys(side)) {
            b.comment(k.comment());
            switch (k.type()) {
                case BOOL:
                    VALUES.put(k.name(), b.define(k.name(), ((Boolean) k.def()).booleanValue()));
                    break;
                case STRING:
                    VALUES.put(k.name(), b.define(k.name(), (String) k.def()));
                    break;
                case CHOICE:
                    VALUES.put(k.name(), b.defineInList(k.name(), (String) k.def(), k.choices()));
                    break;
                default:
                    VALUES.put(k.name(), b.defineListAllowEmpty(List.of(k.name()), (List<String>) k.def(), o -> o instanceof String));
                    break;
            }
        }
        return b.build();
    }

    /** Migrates the old JSON settings if there are any, then registers both specs and the file writer. */
    static void register() {
        ModLoadingContext context = ModLoadingContext.get();
        ConfigMigration.run(FMLPaths.CONFIGDIR.get(), "barelyquesting-common.toml");
        context.registerConfig(ModConfig.Type.COMMON, COMMON);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT);
        Settings.writer(QuestConfig::write);
    }

    @SubscribeEvent
    public static void loaded(ModConfigEvent.Loading event) {
        push(event.getConfig().getSpec());
    }

    @SubscribeEvent
    public static void reloaded(ModConfigEvent.Reloading event) {
        push(event.getConfig().getSpec());
    }

    private static void push(Object spec) {
        ConfigKeys.Side side = spec == COMMON ? ConfigKeys.Side.COMMON : spec == CLIENT ? ConfigKeys.Side.CLIENT : null;
        if (side == null) {
            return;
        }
        Map<String, Object> values = new HashMap<>();
        for (ConfigKeys.Key k : ConfigKeys.keys(side)) {
            Object v = VALUES.get(k.name()).get();
            values.put(k.name(), v instanceof List ? new ArrayList<Object>((List<?>) v) : v);
        }
        Settings.load(values);
    }

    @SuppressWarnings("unchecked")
    private static void write(Map<String, Object> changes) {
        Set<ForgeConfigSpec> dirty = new HashSet<>();
        for (Map.Entry<String, Object> e : changes.entrySet()) {
            ForgeConfigSpec.ConfigValue<Object> value = (ForgeConfigSpec.ConfigValue<Object>) VALUES.get(e.getKey());
            if (value == null) {
                continue;
            }
            ForgeConfigSpec spec = ConfigKeys.key(e.getKey()).side() == ConfigKeys.Side.COMMON ? COMMON : CLIENT;
            if (!spec.isLoaded()) {
                continue;
            }
            try {
                Object v = e.getValue();
                value.set(v instanceof List ? new ArrayList<Object>((List<?>) v) : v);
                dirty.add(spec);
            } catch (RuntimeException ex) {
                LOG.warning("Barely Questing: could not store " + e.getKey() + " in the config file: " + ex);
            }
        }
        for (ForgeConfigSpec spec : dirty) {
            spec.save();
        }
    }
}
