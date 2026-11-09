package com.itmecasey.barelyquesting.compat;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.task.Matchers;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Minecraft 26.1.2: the calls the shared code needs that other versions spell differently. ResourceLocations are {@code ResourceLocation},
 * registries answer {@code getValue}, and item data is a component patch.
 */
public final class Mc {
    /** Sends a message to one player; set by the loader. */
    public interface ServerSender {
        void send(ServerPlayer player, byte[] message);
    }

    public static volatile ServerSender toPlayer = (p, m) -> { };
    /** Sends a message to the server; set by the loader (client only). */
    public static volatile java.util.function.Consumer<byte[]> toServer = m -> { };
    public static volatile MinecraftServer server;
    public static volatile java.nio.file.Path configDir = java.nio.file.Path.of("config");

    private Mc() {
    }

    public static ResourceLocation id(String s) {
        return ResourceLocation.parse(s.contains(":") ? s : "minecraft:" + s);
    }

    public static ResourceLocation id(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
    }

    // ---- items ----

    public static Item item(String id) {
        ResourceLocation rl = id(id);
        return BuiltInRegistries.ITEM.containsKey(rl) ? BuiltInRegistries.ITEM.get(rl) : null;
    }

    public static String entityTypeId(net.minecraft.world.entity.Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    public static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** The item data (component patch) as SNBT text, or null when there is none. */
    public static String dataOf(ItemStack stack, RegistryAccess access) {
        DataComponentPatch patch = stack.getComponentsPatch();
        if (patch.isEmpty()) {
            return null;
        }
        DataResult<Tag> r = DataComponentPatch.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, access), patch);
        return r.result().map(Tag::toString).orElse(null);
    }

    public static ItemSpec specOf(ItemStack stack, RegistryAccess access) {
        return new ItemSpec(itemId(stack), stack.getCount(), false, dataOf(stack, access), false);
    }

    /** A stack for a spec; tags show a rotating member. Returns an empty stack for unknown items. */
    public static ItemStack stack(ItemSpec spec, RegistryAccess access, long millis) {
        if (spec.isTag()) {
            TagKey<Item> key = TagKey.create(Registries.ITEM, id(spec.id()));
            List<Item> members = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(key).forEach(h -> members.add(h.value()));
            if (members.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(members.get((int) ((millis / 1000) % members.size())), Math.max(1, spec.count()));
        }
        Item item = item(spec.id());
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, Math.max(1, spec.count()));
        if (spec.data() != null && access != null) {
            try {
                DataComponentPatch patch = DataComponentPatch.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, access), TagParser.parseTag(spec.data()))
                        .result().orElse(null);
                if (patch != null) {
                    stack.applyComponents(patch);
                }
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                // keep the plain stack
            }
        }
        return stack;
    }

    /** Whether a stack satisfies a spec (id or tag, and data when the spec asks for it). */
    public static boolean matches(ItemSpec spec, ItemStack stack, RegistryAccess access) {
        if (stack.isEmpty()) {
            return false;
        }
        if (spec.isTag()) {
            if (!stack.is(TagKey.create(Registries.ITEM, id(spec.id())))) {
                return false;
            }
        } else if (!itemId(stack).equals(spec.id())) {
            return false;
        }
        if (spec.matchData()) {
            String actual = dataOf(stack, access);
            return actual != null && dataMatches(spec.data(), actual);
        }
        return true;
    }

    public static boolean dataMatches(String wanted, String actual) {
        try {
            Tag w = TagParser.parseTag(wanted);
            Tag a = TagParser.parseTag(actual);
            return NbtUtils.compareNbt(w, a, true);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            return wanted.equals(actual);
        }
    }

    // ---- registries ----

    public static List<String> itemIds() {
        List<String> out = new ArrayList<>();
        BuiltInRegistries.ITEM.keySet().forEach(k -> out.add(k.toString()));
        java.util.Collections.sort(out);
        return out;
    }

    public static List<String> entityIds() {
        List<String> out = new ArrayList<>();
        BuiltInRegistries.ENTITY_TYPE.keySet().forEach(k -> out.add(k.toString()));
        java.util.Collections.sort(out);
        return out;
    }

    public static <T> List<String> keys(RegistryAccess access, ResourceKey<? extends Registry<T>> registry) {
        List<String> out = new ArrayList<>();
        if (access != null) {
            Optional<? extends Registry<T>> reg = access.registry(registry);
            reg.ifPresent(r -> r.keySet().forEach(k -> out.add(k.toString())));
        }
        java.util.Collections.sort(out);
        return out;
    }

    public static ResourceKey<? extends Registry<net.minecraft.world.level.biome.Biome>> biomeRegistry() {
        return Registries.BIOME;
    }

    /** Installs tag and data matching that needs the game's registries. */
    public static void installMatchers() {
        Matchers.install((kind, tag, id) -> {
            ResourceLocation tagId = id(tag);
            ResourceLocation target = id(id);
            try {
                switch (kind) {
                    case "item": {
                        Item item = item(id);
                        return item != null && BuiltInRegistries.ITEM.wrapAsHolder(item).is(TagKey.create(Registries.ITEM, tagId));
                    }
                    case "entity": {
                        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(target)) {
                            return false;
                        }
                        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(target);
                        return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(TagKey.create(Registries.ENTITY_TYPE, tagId));
                    }
                    case "biome":
                        return tagged(Registries.BIOME, tagId, target);
                    case "structure":
                        return tagged(Registries.STRUCTURE, tagId, target);
                    default:
                        return false;
                }
            } catch (RuntimeException e) {
                return false;
            }
        }, Mc::dataMatches);
    }

    private static <T> boolean tagged(ResourceKey<? extends Registry<T>> registry, ResourceLocation tag, ResourceLocation id) {
        MinecraftServer s = server;
        if (s == null) {
            return false;
        }
        Registry<T> reg = s.registryAccess().registryOrThrow(registry);
        Optional<Holder.Reference<T>> holder = reg.getHolder(ResourceKey.create(registry, id));
        if (holder.isEmpty()) {
            return false;
        }
        Optional<HolderSet.Named<T>> set = reg.getTag(TagKey.create(registry, tag));
        return set.isPresent() && set.get().contains(holder.get());
    }

    /** Runs a consumer over each key of the item registry's tag, for the editor's tag preview (unused when empty). */
    public static void forEachTagMember(String tag, BiConsumer<String, Item> out) {
        BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, id(tag))).forEach(h -> out.accept(BuiltInRegistries.ITEM.getKey(h.value()).toString(), h.value()));
    }

    public static CompoundTag emptyTag() {
        return new CompoundTag();
    }
}
