package com.itmecasey.barelyquesting.core.task;

import com.itmecasey.barelyquesting.core.model.ItemSpec;

/**
 * Tag lookups need the game's registries, so the version module installs them here at startup. Without them (unit tests) tags never match
 * and ids are compared directly.
 */
public final class Matchers {
    /** Whether {@code id} (an item, entity type, biome or structure id) is in the tag {@code tagId}. */
    public interface TagLookup {
        boolean inTag(String kind, String tagId, String id);
    }

    /** Whether item data text {@code actual} contains everything in {@code wanted} (a subset test on the data tree). */
    public interface DataMatch {
        boolean matches(String wanted, String actual);
    }

    private static volatile TagLookup tags = (kind, tag, id) -> false;
    private static volatile DataMatch data = (wanted, actual) -> wanted.equals(actual);

    private Matchers() {
    }

    public static void install(TagLookup lookup, DataMatch dataMatch) {
        tags = lookup;
        data = dataMatch;
    }

    /** Matches a pattern like {@code minecraft:zombie} or {@code #minecraft:skeletons} of a kind against an id. */
    public static boolean idMatches(String kind, String pattern, String id) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        if (pattern.startsWith("#")) {
            return tags.inTag(kind, pattern.substring(1), id);
        }
        String p = pattern.contains(":") ? pattern : "minecraft:" + pattern;
        return p.equals(id);
    }

    /** Whether a stack described by {@code actual} satisfies {@code wanted} (ids, tags and, if asked, data). */
    public static boolean itemMatches(ItemSpec wanted, ItemSpec actual) {
        boolean idOk = wanted.isTag() ? tags.inTag("item", wanted.id(), actual.id()) : wanted.id().equals(actual.id());
        if (!idOk) {
            return false;
        }
        if (wanted.matchData()) {
            return actual.data() != null && data.matches(wanted.data(), actual.data());
        }
        return true;
    }
}
