package com.itmecasey.barelyquesting;

import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.progress.PlayerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A scriptable player for tests. */
public class FakePlayer implements PlayerView {
    public final UUID id;
    public final String name;
    public final Map<String, Integer> inventory = new HashMap<>();
    public final List<String> given = new ArrayList<>();
    public final List<String> commands = new ArrayList<>();
    public final List<String> messages = new ArrayList<>();
    public final List<String> toasts = new ArrayList<>();
    public int xpPoints;
    public int xpLevels;
    public String dimension = "minecraft:overworld";
    public String biome = "minecraft:plains";
    public double x;
    public double y = 64;
    public double z;
    public boolean op;
    public boolean creative;
    public final Set<String> advancements = new HashSet<>();
    public final Set<String> structures = new HashSet<>();
    public final Map<String, Long> stats = new HashMap<>();

    public FakePlayer(String name) {
        this.name = name;
        this.id = UUID.nameUUIDFromBytes(name.getBytes());
    }

    @Override public UUID uuid() { return id; }
    @Override public String name() { return name; }
    @Override public int countItems(ItemSpec spec) { return inventory.getOrDefault(spec.id(), 0); }
    @Override public int takeItems(ItemSpec spec, int max) {
        int have = inventory.getOrDefault(spec.id(), 0);
        int take = Math.min(have, max);
        inventory.put(spec.id(), have - take);
        return take;
    }
    @Override public void giveItem(ItemSpec spec) { given.add(spec.toString()); }
    @Override public void addExperiencePoints(int points) { xpPoints += points; }
    @Override public void addExperienceLevels(int levels) { xpLevels += levels; }
    @Override public void runCommand(String command) { commands.add(command); }
    @Override public String dimension() { return dimension; }
    @Override public String biome() { return biome; }
    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public double z() { return z; }
    @Override public boolean inStructure(String s) { return structures.contains(s); }
    @Override public boolean hasAdvancement(String a) { return advancements.contains(a); }
    @Override public long statistic(String type, String key) { return stats.getOrDefault(type + ":" + key, 0L); }
    @Override public void message(String text) { messages.add(text); }
    @Override public boolean isOperator() { return op; }
    @Override public boolean isCreative() { return creative; }
    @Override public void toast(String title, String text) { toasts.add(title); }
}
