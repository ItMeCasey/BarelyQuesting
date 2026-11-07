package com.itmecasey.barelyquesting.core.progress;

import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.script.ScriptRegistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reward and lock type registries with the built-in types. */
public final class Rewards {
    /** What a reward type does. */
    public interface RewardType {
        String id();

        void give(Def reward, PlayerView player);

        default String describe(Def reward) {
            return id();
        }
    }

    /** Whether a lock closes a quest for a team. */
    public interface LockType {
        String id();

        boolean locked(Def lock, QuestEngine engine, String teamId, String questId);

        default String describe(Def lock) {
            return id();
        }
    }

    private static final Map<String, RewardType> REWARDS = new LinkedHashMap<>();
    private static final Map<String, LockType> LOCKS = new LinkedHashMap<>();

    private Rewards() {
    }

    public static synchronized void register(RewardType t) {
        REWARDS.put(t.id(), t);
    }

    public static synchronized RewardType reward(String id) {
        return REWARDS.get(id);
    }

    public static synchronized Collection<RewardType> rewards() {
        return new java.util.ArrayList<>(REWARDS.values());
    }

    public static synchronized void registerLock(LockType t) {
        LOCKS.put(t.id(), t);
    }

    public static synchronized LockType lock(String id) {
        return LOCKS.get(id);
    }

    public static synchronized Collection<LockType> locks() {
        return new java.util.ArrayList<>(LOCKS.values());
    }

    public static synchronized void resetToBuiltins() {
        REWARDS.clear();
        LOCKS.clear();
        builtins();
    }

    static {
        builtins();
    }

    private static void builtins() {
        register(new RewardType() {
            @Override
            public String id() {
                return "item";
            }

            @Override
            public void give(Def r, PlayerView p) {
                ItemSpec s = r.item("item");
                if (!s.isEmpty() && !s.isTag()) {
                    p.giveItem(s);
                }
            }

            @Override
            public String describe(Def r) {
                return r.item("item").toString();
            }
        });
        register(new RewardType() {
            @Override
            public String id() {
                return "xp";
            }

            @Override
            public void give(Def r, PlayerView p) {
                if (r.str("unit", "points").equals("levels")) {
                    p.addExperienceLevels((int) r.num("amount", 1));
                } else {
                    p.addExperiencePoints((int) r.num("amount", 1));
                }
            }

            @Override
            public String describe(Def r) {
                return r.num("amount", 1) + " XP " + r.str("unit", "points");
            }
        });
        register(new RewardType() {
            @Override
            public String id() {
                return "command";
            }

            @Override
            public void give(Def r, PlayerView p) {
                String cmd = r.str("command", "");
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                if (!cmd.isEmpty()) {
                    p.runCommand(cmd.replace("{player}", p.name()).replace("@p", p.name()));
                }
            }

            @Override
            public String describe(Def r) {
                return "Command: " + r.str("command", "");
            }
        });
        register(new RewardType() {
            @Override
            public String id() {
                return "scripted";
            }

            @Override
            public void give(Def r, PlayerView p) {
                ScriptRegistry.RewardCallback cb = ScriptRegistry.reward(r.str("name", ""));
                if (cb != null) {
                    cb.give(r, p);
                }
            }

            @Override
            public String describe(Def r) {
                return r.str("text", r.str("name", "Scripted reward"));
            }
        });

        registerLock(new LockType() {
            @Override
            public String id() {
                return "quest";
            }

            @Override
            public boolean locked(Def l, QuestEngine e, String teamId, String questId) {
                return !e.isComplete(teamId, l.str("quest", ""));
            }

            @Override
            public String describe(Def l) {
                return "Needs quest " + l.str("quest", "?");
            }
        });
        registerLock(new LockType() {
            @Override
            public String id() {
                return "time";
            }

            @Override
            public boolean locked(Def l, QuestEngine e, String teamId, String questId) {
                long now = e.nowMillis();
                long after = parseTime(l.str("after", ""), Long.MIN_VALUE);
                long before = parseTime(l.str("before", ""), Long.MAX_VALUE);
                return now < after || now > before;
            }

            @Override
            public String describe(Def l) {
                return "Time window " + l.str("after", "") + " .. " + l.str("before", "");
            }
        });
        registerLock(new LockType() {
            @Override
            public String id() {
                return "script";
            }

            @Override
            public boolean locked(Def l, QuestEngine e, String teamId, String questId) {
                ScriptRegistry.LockCallback cb = ScriptRegistry.lock(l.str("name", ""));
                return cb != null && cb.locked(l, teamId, questId);
            }

            @Override
            public String describe(Def l) {
                return "Script condition " + l.str("name", "?");
            }
        });
    }

    /** ISO-8601 instant ({@code 2026-12-01T00:00:00Z}), a date ({@code 2026-12-01}) or epoch milliseconds. */
    static long parseTime(String text, long def) {
        if (text == null || text.isBlank()) {
            return def;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ignored) {
            // try dates
        }
        try {
            return java.time.Instant.parse(text.trim()).toEpochMilli();
        } catch (java.time.format.DateTimeParseException ignored) {
            // try a plain date
        }
        try {
            return java.time.LocalDate.parse(text.trim()).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (java.time.format.DateTimeParseException e) {
            return def;
        }
    }
}
