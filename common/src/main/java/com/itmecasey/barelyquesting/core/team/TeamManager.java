package com.itmecasey.barelyquesting.core.team;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Teams share quest progress. Every player always has a team: until they join one, a one-member "solo" team made for them, so the engine
 * has a single code path. Creating, inviting, joining, leaving and kicking never touch the progress of the team that is left behind.
 */
public final class TeamManager {
    /** A team of players that share progress. */
    public static final class Team {
        public final String id;
        public String name;
        public UUID owner;
        public final Set<UUID> members = new LinkedHashSet<>();
        public final Set<UUID> invited = new LinkedHashSet<>();
        public final boolean solo;

        Team(String id, String name, UUID owner, boolean solo) {
            this.id = id;
            this.name = name;
            this.owner = owner;
            this.solo = solo;
        }
    }

    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<UUID, String> teamByPlayer = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();
    private boolean dirty;

    public static String soloId(UUID player) {
        return "solo-" + player;
    }

    public void rememberName(UUID player, String name) {
        if (!name.equals(names.put(player, name))) {
            dirty = true;
        }
        Team t = teamOf(player);
        if (t.solo) {
            t.name = name;
        }
    }

    public String nameOf(UUID player) {
        String n = names.get(player);
        return n == null ? player.toString().substring(0, 8) : n;
    }

    /** The player's team; makes the solo team on first use. */
    public Team teamOf(UUID player) {
        String id = teamByPlayer.get(player);
        Team t = id == null ? null : teams.get(id);
        if (t == null) {
            t = new Team(soloId(player), nameOf(player), player, true);
            t.members.add(player);
            teams.put(t.id, t);
            teamByPlayer.put(player, t.id);
            // Solo teams are made on demand and not saved, so creating one is not a change.
        }
        return t;
    }

    public Team team(String id) {
        return teams.get(id);
    }

    public Collection<Team> all() {
        return teams.values();
    }

    public String create(UUID owner, String name) {
        Team current = teamOf(owner);
        if (!current.solo) {
            return "already_in_team";
        }
        String clean = name.trim();
        if (clean.isEmpty() || clean.length() > 32) {
            return "bad_name";
        }
        for (Team t : teams.values()) {
            if (!t.solo && t.name.equalsIgnoreCase(clean)) {
                return "name_taken";
            }
        }
        String id = "team-" + java.lang.Long.toHexString(System.nanoTime()) + Integer.toHexString(teams.size());
        Team t = new Team(id, clean, owner, false);
        t.members.add(owner);
        teams.put(id, t);
        teamByPlayer.put(owner, id);
        dirty = true;
        return null;
    }

    public String invite(UUID owner, UUID target) {
        Team t = teamOf(owner);
        if (t.solo || !t.owner.equals(owner)) {
            return "not_owner";
        }
        if (t.members.contains(target)) {
            return "already_member";
        }
        t.invited.add(target);
        dirty = true;
        return null;
    }

    /** Teams that have invited this player. */
    public List<Team> invitationsFor(UUID player) {
        List<Team> out = new ArrayList<>();
        for (Team t : teams.values()) {
            if (t.invited.contains(player)) {
                out.add(t);
            }
        }
        return out;
    }

    public String join(UUID player, String teamId) {
        Team t = teams.get(teamId);
        if (t == null || t.solo || !t.invited.contains(player)) {
            return "no_invite";
        }
        Team current = teamOf(player);
        if (!current.solo) {
            return "already_in_team";
        }
        t.invited.remove(player);
        t.members.add(player);
        teamByPlayer.put(player, t.id);
        dirty = true;
        return null;
    }

    public String leave(UUID player) {
        Team t = teamOf(player);
        if (t.solo) {
            return "not_in_team";
        }
        t.members.remove(player);
        teamByPlayer.remove(player);
        if (t.members.isEmpty()) {
            teams.remove(t.id);
        } else if (t.owner.equals(player)) {
            t.owner = t.members.iterator().next();
        }
        dirty = true;
        teamOf(player);
        return null;
    }

    public String kick(UUID owner, UUID target) {
        Team t = teamOf(owner);
        if (t.solo || !t.owner.equals(owner)) {
            return "not_owner";
        }
        if (owner.equals(target) || !t.members.contains(target)) {
            return "not_member";
        }
        t.members.remove(target);
        teamByPlayer.remove(target);
        dirty = true;
        teamOf(target);
        return null;
    }

    public String disband(UUID owner) {
        Team t = teamOf(owner);
        if (t.solo || !t.owner.equals(owner)) {
            return "not_owner";
        }
        for (UUID m : new ArrayList<>(t.members)) {
            teamByPlayer.remove(m);
        }
        teams.remove(t.id);
        for (UUID m : t.members) {
            teamOf(m);
        }
        dirty = true;
        return null;
    }

    public boolean takeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray list = new JsonArray();
        for (Team t : teams.values()) {
            if (t.solo) {
                continue;
            }
            JsonObject to = new JsonObject();
            to.addProperty("id", t.id);
            to.addProperty("name", t.name);
            to.addProperty("owner", t.owner.toString());
            JsonArray m = new JsonArray();
            t.members.forEach(u -> m.add(u.toString()));
            to.add("members", m);
            JsonArray inv = new JsonArray();
            t.invited.forEach(u -> inv.add(u.toString()));
            to.add("invited", inv);
            list.add(to);
        }
        o.add("teams", list);
        JsonObject n = new JsonObject();
        names.forEach((k, v) -> n.addProperty(k.toString(), v));
        o.add("names", n);
        return o;
    }

    public void fromJson(JsonObject o) {
        teams.clear();
        teamByPlayer.clear();
        names.clear();
        JsonObject n = Json.obj(o, "names");
        if (n != null) {
            for (String k : n.keySet()) {
                names.put(UUID.fromString(k), n.get(k).getAsString());
            }
        }
        for (JsonElement e : Json.arr(o, "teams")) {
            JsonObject to = e.getAsJsonObject();
            Team t = new Team(Json.str(to, "id", ""), Json.str(to, "name", "Team"), UUID.fromString(Json.str(to, "owner", new UUID(0, 0).toString())), false);
            for (JsonElement m : Json.arr(to, "members")) {
                UUID u = UUID.fromString(m.getAsString());
                t.members.add(u);
                teamByPlayer.put(u, t.id);
            }
            for (JsonElement m : Json.arr(to, "invited")) {
                t.invited.add(UUID.fromString(m.getAsString()));
            }
            if (!t.members.isEmpty()) {
                teams.put(t.id, t);
            }
        }
        dirty = false;
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
