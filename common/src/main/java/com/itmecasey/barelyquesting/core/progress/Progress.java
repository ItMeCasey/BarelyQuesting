package com.itmecasey.barelyquesting.core.progress;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Everything players have done: per team, per quest, which tasks have how much progress and which rewards were claimed. */
public final class Progress {
    /** One team's state for one quest. */
    public static final class QuestProgress {
        public final Map<String, Long> tasks = new LinkedHashMap<>();
        public boolean started;
        public boolean complete;
        public long completedAt;
        /** Reward ids claimed for the whole team. */
        public final Set<String> teamClaimed = new HashSet<>();
        /** Reward ids claimed per player (uuid string). */
        public final Map<String, Set<String>> playerClaimed = new HashMap<>();

        public long task(String id) {
            return tasks.getOrDefault(id, 0L);
        }

        public boolean claimed(String player, String reward, boolean teamScope) {
            if (teamScope) {
                return teamClaimed.contains(reward);
            }
            Set<String> s = playerClaimed.get(player);
            return s != null && s.contains(reward);
        }

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            if (!tasks.isEmpty()) {
                JsonObject t = new JsonObject();
                tasks.forEach(t::addProperty);
                o.add("tasks", t);
            }
            if (started) {
                o.addProperty("started", true);
            }
            if (complete) {
                o.addProperty("complete", true);
                o.addProperty("completed_at", completedAt);
            }
            if (!teamClaimed.isEmpty()) {
                JsonArray a = new JsonArray();
                teamClaimed.stream().sorted().forEach(a::add);
                o.add("team_claimed", a);
            }
            if (!playerClaimed.isEmpty()) {
                JsonObject p = new JsonObject();
                playerClaimed.keySet().stream().sorted().forEach(k -> {
                    JsonArray a = new JsonArray();
                    playerClaimed.get(k).stream().sorted().forEach(a::add);
                    p.add(k, a);
                });
                o.add("player_claimed", p);
            }
            return o;
        }

        static QuestProgress fromJson(JsonObject o) {
            QuestProgress q = new QuestProgress();
            JsonObject t = Json.obj(o, "tasks");
            if (t != null) {
                for (String k : t.keySet()) {
                    q.tasks.put(k, t.get(k).getAsLong());
                }
            }
            q.started = Json.bool(o, "started", false);
            q.complete = Json.bool(o, "complete", false);
            q.completedAt = Json.lng(o, "completed_at", 0);
            for (JsonElement e : Json.arr(o, "team_claimed")) {
                q.teamClaimed.add(e.getAsString());
            }
            JsonObject p = Json.obj(o, "player_claimed");
            if (p != null) {
                for (String k : p.keySet()) {
                    Set<String> s = new HashSet<>();
                    for (JsonElement e : p.get(k).getAsJsonArray()) {
                        s.add(e.getAsString());
                    }
                    q.playerClaimed.put(k, s);
                }
            }
            return q;
        }
    }

    private final Map<String, Map<String, QuestProgress>> teams = new LinkedHashMap<>();

    public Map<String, QuestProgress> forTeam(String teamId) {
        return teams.computeIfAbsent(teamId, k -> new LinkedHashMap<>());
    }

    public QuestProgress get(String teamId, String questId) {
        Map<String, QuestProgress> m = teams.get(teamId);
        return m == null ? null : m.get(questId);
    }

    public QuestProgress getOrCreate(String teamId, String questId) {
        return forTeam(teamId).computeIfAbsent(questId, k -> new QuestProgress());
    }

    public void remove(String teamId, String questId) {
        Map<String, QuestProgress> m = teams.get(teamId);
        if (m != null) {
            m.remove(questId);
        }
    }

    public void clearTeam(String teamId) {
        teams.remove(teamId);
    }

    public void clearAll() {
        teams.clear();
    }

    /** One team's progress as JSON (also the sync format sent to that team's players). */
    public JsonObject teamJson(String teamId) {
        JsonObject o = new JsonObject();
        Map<String, QuestProgress> m = teams.get(teamId);
        if (m != null) {
            m.forEach((k, v) -> o.add(k, v.toJson()));
        }
        return o;
    }

    public void setTeamFromJson(String teamId, JsonObject o) {
        Map<String, QuestProgress> m = forTeam(teamId);
        m.clear();
        for (String k : o.keySet()) {
            m.put(k, QuestProgress.fromJson(o.get(k).getAsJsonObject()));
        }
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        for (String team : teams.keySet()) {
            o.add(team, teamJson(team));
        }
        return o;
    }

    public void fromJson(JsonObject o) {
        teams.clear();
        for (String team : o.keySet()) {
            setTeamFromJson(team, o.get(team).getAsJsonObject());
        }
    }

    public void load(Path file) throws IOException {
        if (Files.exists(file)) {
            fromJson(Json.parseObject(Files.readString(file, StandardCharsets.UTF_8)));
        }
    }

    public void save(Path file) throws IOException {
        Json.writeAtomic(file, Json.pretty(toJson()));
    }
}
