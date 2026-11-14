package com.itmecasey.barelyquesting.neoforge;

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

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The mod's settings as ModConfigSpec files (barelyquesting-startup.toml for the server side, barelyquesting-client.toml for the player), built from
 * the one key list in {@link ConfigKeys}. Loaded and reloaded values are pushed into {@link Settings}; values the mod changes itself go
 * back to the file through {@link #write}. Settings that older versions kept in JSON are migrated first (see {@link ConfigMigration}).
 */
@EventBusSubscriber(modid = "barelyquesting")
public final class QuestConfig {
    private static final Logger LOG = Logger.getLogger("barelyquesting");
    private static final Map<String, ModConfigSpec.ConfigValue<?>> VALUES = new HashMap<>();
    private static final ModConfigSpec COMMON = build(ConfigKeys.Side.COMMON);
    private static final ModConfigSpec CLIENT = build(ConfigKeys.Side.CLIENT);

    private QuestConfig() {
    }

    private static ModConfigSpec build(ConfigKeys.Side side) {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
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
                    VALUES.put(k.name(), b.defineListAllowEmpty(k.name(), (List<String>) k.def(), () -> "", o -> o instanceof String));
                    break;
            }
        }
        return b.build();
    }

    /** Migrates the old JSON settings if there are any, then registers both specs and the file writer. */
    static void register(ModContainer context) {
        ConfigMigration.run(FMLPaths.CONFIGDIR.get(), "barelyquesting-startup.toml");
        context.registerConfig(ModConfig.Type.STARTUP, COMMON);
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
        Set<ModConfigSpec> dirty = new HashSet<>();
        for (Map.Entry<String, Object> e : changes.entrySet()) {
            ModConfigSpec.ConfigValue<Object> value = (ModConfigSpec.ConfigValue<Object>) VALUES.get(e.getKey());
            if (value == null) {
                continue;
            }
            ModConfigSpec spec = ConfigKeys.key(e.getKey()).side() == ConfigKeys.Side.COMMON ? COMMON : CLIENT;
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
        for (ModConfigSpec spec : dirty) {
            spec.save();
        }
    }
}
