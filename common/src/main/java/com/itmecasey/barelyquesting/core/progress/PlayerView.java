package com.itmecasey.barelyquesting.core.progress;

import com.itmecasey.barelyquesting.core.model.ItemSpec;

import java.util.UUID;

/**
 * What the quest engine may ask of a player. The version modules implement it over {@code ServerPlayer}; tests implement it with a
 * fake. Nothing here uses Minecraft types, so the engine and every task type can be tested without a game.
 */
public interface PlayerView {
    UUID uuid();

    String name();

    /** Items in the inventory that match {@code spec} (tag, id and data as the spec says); the spec's own count is ignored. */
    int countItems(ItemSpec spec);

    /** Removes up to {@code max} matching items and returns how many were taken. */
    int takeItems(ItemSpec spec, int max);

    /** Gives items; what does not fit is dropped at the player's feet. */
    void giveItem(ItemSpec spec);

    void addExperiencePoints(int points);

    void addExperienceLevels(int levels);

    /** Runs a command as the server with this player as the executor (so {@code @s} is the player). */
    void runCommand(String command);

    String dimension();

    String biome();

    double x();

    double y();

    double z();

    boolean inStructure(String structureId);

    boolean hasAdvancement(String id);

    /** A statistic value: {@code type} is {@code custom}, {@code mined}, {@code crafted}, {@code used}, {@code killed}, ... */
    long statistic(String type, String key);

    void message(String text);

    boolean isOperator();

    boolean isCreative();

    /** Tells the player's client that a quest was completed (toast). */
    default void toast(String questTitle, String text) {
    }
}
