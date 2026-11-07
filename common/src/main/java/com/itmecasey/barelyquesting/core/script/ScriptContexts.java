package com.itmecasey.barelyquesting.core.script;

import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.task.TaskEvent;

/**
 * Objects handed to script functions. They hold only strings, numbers and simple methods, so any script engine (Rhino in ModJS and
 * KubeJS) can use them without extra wrapping.
 */
public final class ScriptContexts {
    private ScriptContexts() {
    }

    /** Base: the player and the task, reward or lock parameters. */
    public static class Base {
        protected final PlayerView player;
        protected final Def def;

        Base(PlayerView player, Def def) {
            this.player = player;
            this.def = def;
        }

        public String playerId() {
            return player == null ? "" : player.uuid().toString();
        }

        public String playerName() {
            return player == null ? "" : player.name();
        }

        /** A text parameter of the quest file entry ({@code "name"}, {@code "count"}, your own keys). */
        public String param(String key, String fallback) {
            return def == null ? fallback : def.str(key, fallback);
        }

        public double number(String key, double fallback) {
            return def == null ? fallback : def.num(key, (long) fallback);
        }

        public String paramsJson() {
            return def == null ? "{}" : def.params.toString();
        }

        public JsonObject params() {
            return def == null ? new JsonObject() : def.params;
        }
    }

    /** For {@code scripted} and custom tasks: look at the player and return the progress to set. */
    public static final class TaskContext extends Base {
        private final long current;

        public TaskContext(PlayerView player, Def def, long current) {
            super(player, def);
            this.current = current;
        }

        /** The progress so far. Return a higher number to advance. */
        public long current() {
            return current;
        }

        public long target() {
            return Math.max(1, def.num("count", 1));
        }

        public int count(String itemSpec) {
            return player.countItems(ItemSpec.parse(itemSpec));
        }

        public String dimension() {
            return player.dimension();
        }

        public String biome() {
            return player.biome();
        }

        public double x() {
            return player.x();
        }

        public double y() {
            return player.y();
        }

        public double z() {
            return player.z();
        }

        public boolean hasAdvancement(String id) {
            return player.hasAdvancement(id);
        }

        public double stat(String type, String key) {
            return player.statistic(type, key);
        }
    }

    /** For event driven tasks: what happened. Return the progress to add. */
    public static final class EventContext extends Base {
        private final TaskEvent event;

        public EventContext(PlayerView player, Def def, TaskEvent event) {
            super(player, def);
            this.event = event;
        }

        /** {@code kill}, {@code craft}, {@code pickup} or {@code advancement}. */
        public String kind() {
            return event.kind;
        }

        /** The entity, item or advancement id. */
        public String key() {
            return event.key;
        }

        public double amount() {
            return event.amount;
        }
    }

    /** For scripted rewards: what to give. */
    public static final class RewardContext extends Base {
        public RewardContext(PlayerView player, Def def) {
            super(player, def);
        }

        public void giveItem(String itemSpec) {
            player.giveItem(ItemSpec.parse(itemSpec));
        }

        public void addXp(int points) {
            player.addExperiencePoints(points);
        }

        public void addLevels(int levels) {
            player.addExperienceLevels(levels);
        }

        public void command(String command) {
            player.runCommand(command.startsWith("/") ? command.substring(1) : command);
        }

        public void message(String text) {
            player.message(text);
        }
    }

    /** For script locks: which quest and team are being checked. */
    public static final class LockContext {
        private final Def def;
        private final String teamId;
        private final String questId;

        public LockContext(Def def, String teamId, String questId) {
            this.def = def;
            this.teamId = teamId;
            this.questId = questId;
        }

        public String teamId() {
            return teamId;
        }

        public String questId() {
            return questId;
        }

        public String param(String key, String fallback) {
            return def.str(key, fallback);
        }
    }
}
