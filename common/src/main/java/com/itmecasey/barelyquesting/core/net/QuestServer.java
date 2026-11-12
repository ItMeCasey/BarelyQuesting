package com.itmecasey.barelyquesting.core.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.DefaultBook;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.Progress;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.script.ScriptEvents;
import com.itmecasey.barelyquesting.core.store.QuestStore;
import com.itmecasey.barelyquesting.core.team.TeamManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Everything on the server side that is not the game itself: loads and saves the files, answers client messages, applies edits,
 * keeps the clients in sync, and ticks the engine. The loaders only forward events, ticks and bytes to it.
 */
public final class QuestServer implements QuestEngine.Listener {
    /** Sends a message to one player. */
    public interface ServerNet {
        void send(UUID player, byte[] message);
    }

    private static final Logger LOG = Logger.getLogger("barelyquesting");
    private static final int POLL_TICKS = 20;
    private static final int SYNC_TICKS = 10;
    private static final int SAVE_TICKS = 1200;

    private final QuestStore store;
    private final Path worldDir;
    private java.util.Set<UUID> welcomed;
    private final ServerNet net;
    private final QuestEngine.Host host;
    private final QuestEngine engine;
    private long lastSave;
    private boolean bookDirty;

    public QuestServer(Path configDir, Path worldDir, ServerNet net, QuestEngine.Host host) {
        this.store = new QuestStore(configDir);
        this.worldDir = worldDir;
        this.net = net;
        this.host = host;
        this.engine = new QuestEngine(new Book(), new TeamManager(), new Progress(), host);
        this.engine.addListener(this);
    }

    public QuestEngine engine() {
        return engine;
    }

    public QuestStore store() {
        return store;
    }

    /** Loads the book (writing the starter book on first run), the teams and the progress. */
    public void start() {
        loadBook();
        try {
            engine.teams().load(worldDir.resolve("teams.json"));
            engine.progress().load(worldDir.resolve("progress.json"));
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.SEVERE, "Could not read teams or progress", e);
        }
    }

    private void loadBook() {
        Book book;
        try {
            if (!store.exists()) {
                book = DefaultBook.create();
                store.save(book);
            } else {
                book = store.load();
            }
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.SEVERE, "Could not read the quest files, using an empty book", e);
            book = new Book();
        }
        engine.setBook(book);
    }

    /** Re-reads the quest files and sends the new book to everyone (the /barelyquesting reload command). */
    public void reload() {
        loadBook();
        for (PlayerView p : host.players()) {
            sendInfo(p);
            sendBook(p.uuid());
            sendTeamAndProgress(p);
        }
    }

    public void stop() {
        saveAll();
    }

    public void saveAll() {
        try {
            if (bookDirty) {
                store.save(engine.book());
                bookDirty = false;
            }
            engine.teams().save(worldDir.resolve("teams.json"));
            engine.progress().save(worldDir.resolve("progress.json"));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not save quests", e);
        }
    }

    public boolean canEdit(PlayerView p) {
        Book b = engine.book();
        if (!b.editing) {
            return false;
        }
        return p.isOperator() || b.editors.contains(p.name().toLowerCase(java.util.Locale.ROOT)) || (b.creativeEdits && p.isCreative());
    }

    // ---- joining and syncing ----

    public void onJoin(PlayerView p) {
        engine.teams().rememberName(p.uuid(), p.name());
        sendInfo(p);
        sendBook(p.uuid());
        sendTeamAndProgress(p);
        if (markWelcomed(p.uuid())) {
            Book b = engine.book();
            if (b.giveBookOnJoin) {
                p.giveItem(ItemSpec.of("barelyquesting:quest_book"));
            }
            if (b.openBookOnJoin) {
                open(p, null);
            }
        }
    }

    private void sendInfo(PlayerView p) {
        net.send(p.uuid(), new Wire.Out(Msg.S_INFO).str(p.uuid().toString()).bool(canEdit(p)).done());
    }

    /** Remembers that the player has joined this world; true only the first time, so a rejoin or a death never repeats a welcome. */
    private boolean markWelcomed(UUID id) {
        if (welcomed == null) {
            welcomed = new java.util.HashSet<>();
            try {
                Path file = worldDir.resolve("welcomed.json");
                if (Files.exists(file)) {
                    for (JsonElement e : Json.parse(Files.readString(file, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray()) {
                        welcomed.add(UUID.fromString(e.getAsString()));
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOG.log(Level.WARNING, "Could not read welcomed.json", e);
            }
        }
        if (!welcomed.add(id)) {
            return false;
        }
        try {
            com.google.gson.JsonArray all = new com.google.gson.JsonArray();
            welcomed.stream().map(UUID::toString).sorted().forEach(all::add);
            Files.createDirectories(worldDir);
            Files.writeString(worldDir.resolve("welcomed.json"), Json.compact(all), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save welcomed.json", e);
        }
        return true;
    }

    private void sendBook(UUID to) {
        byte[] data = Wire.gzip(Json.compact(engine.book().toJson()));
        int total = Math.max(1, (data.length + Msg.CHUNK - 1) / Msg.CHUNK);
        for (int i = 0; i < total; i++) {
            int from = i * Msg.CHUNK;
            int end = Math.min(data.length, from + Msg.CHUNK);
            byte[] part = java.util.Arrays.copyOfRange(data, from, end);
            net.send(to, new Wire.Out(Msg.S_BOOK).i(i).i(total).blob(part).done());
        }
    }

    private void sendTeamAndProgress(PlayerView p) {
        TeamManager.Team t = engine.teams().teamOf(p.uuid());
        net.send(p.uuid(), new Wire.Out(Msg.S_TEAM).str(teamJson(p.uuid(), t)).done());
        net.send(p.uuid(), new Wire.Out(Msg.S_PROGRESS).str(t.id).str(progressJson(t.id)).done());
    }

    /** A team's progress plus {@code $locked}: the quests the server says are closed (dependencies, time and script locks). */
    private String progressJson(String teamId) {
        JsonObject o = engine.progress().teamJson(teamId);
        JsonArray locked = new JsonArray();
        for (Quest q : engine.book().allQuests()) {
            if (!engine.isComplete(teamId, q.id) && !engine.isUnlocked(teamId, q)) {
                locked.add(q.id);
            }
        }
        o.add("$locked", locked);
        return Json.compact(o);
    }

    private String teamJson(UUID viewer, TeamManager.Team t) {
        JsonObject o = new JsonObject();
        o.addProperty("id", t.id);
        o.addProperty("name", t.name);
        o.addProperty("solo", t.solo);
        o.addProperty("owner", t.owner.toString());
        JsonArray members = new JsonArray();
        for (UUID m : t.members) {
            JsonObject mo = new JsonObject();
            mo.addProperty("uuid", m.toString());
            mo.addProperty("name", engine.teams().nameOf(m));
            members.add(mo);
        }
        o.add("members", members);
        JsonArray invites = new JsonArray();
        for (TeamManager.Team inv : engine.teams().invitationsFor(viewer)) {
            JsonObject io = new JsonObject();
            io.addProperty("id", inv.id);
            io.addProperty("name", inv.name);
            invites.add(io);
        }
        o.add("invites", invites);
        return Json.compact(o);
    }

    /** Called every server tick with the current game time. */
    public void tick(long gameTime) {
        if (gameTime % POLL_TICKS == 0) {
            for (PlayerView p : host.players()) {
                engine.poll(p);
            }
        }
        if (gameTime % 600 == 0) {
            // Time locks open without any progress change, so refresh the lock list now and then.
            for (PlayerView p : host.players()) {
                engine.markTeamDirty(engine.teams().teamOf(p.uuid()).id);
            }
        }
        if (gameTime % SYNC_TICKS == 0) {
            flush();
        }
        if (gameTime - lastSave >= SAVE_TICKS) {
            lastSave = gameTime;
            if (engine.takeProgressDirty() || bookDirty || engine.teams().takeDirty()) {
                saveAll();
            }
        }
    }

    /** Sends changed progress and team info to the players who see it. */
    public void flush() {
        boolean teamsChanged = engine.teams().takeDirty();
        List<String> dirty = engine.takeDirtyTeams();
        if (teamsChanged) {
            for (PlayerView p : host.players()) {
                sendTeamAndProgress(p);
            }
            return;
        }
        for (String teamId : dirty) {
            TeamManager.Team t = engine.teams().team(teamId);
            if (t == null) {
                continue;
            }
            String json = progressJson(teamId);
            for (UUID m : t.members) {
                if (host.online(m) != null) {
                    net.send(m, new Wire.Out(Msg.S_PROGRESS).str(teamId).str(json).done());
                }
            }
        }
    }

    // ---- client messages ----

    /** Handles one message from a client. Malformed data is dropped. */
    public void handle(PlayerView sender, byte[] message) {
        try {
            Wire.In in = new Wire.In(message);
            switch (in.id()) {
                case Msg.C_HELLO:
                    onJoin(sender);
                    break;
                case Msg.C_CLAIM: {
                    String quest = in.str();
                    String reward = in.str();
                    int n = engine.claim(sender, quest, reward.isEmpty() ? null : reward);
                    if (n == 0) {
                        result(sender, "error", "Nothing to claim");
                    }
                    break;
                }
                case Msg.C_SUBMIT: {
                    int n = engine.submit(sender, in.str(), in.str());
                    if (n == 0) {
                        result(sender, "error", "No matching items to submit");
                    }
                    break;
                }
                case Msg.C_CHECK:
                    engine.check(sender, in.str(), in.str());
                    break;
                case Msg.C_EDIT:
                    edit(sender, in.str());
                    break;
                case Msg.C_TEAM:
                    team(sender, in.str(), in.str());
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Dropped a bad message from " + sender.name(), e);
        }
    }

    private void result(PlayerView p, String kind, String text) {
        net.send(p.uuid(), new Wire.Out(Msg.S_RESULT).str(kind).str(text).done());
    }

    /** Applies an edit from an editor, saves, and tells every client. */
    public void edit(PlayerView sender, String opJson) {
        if (!canEdit(sender)) {
            result(sender, "error", "You may not edit quests");
            return;
        }
        applyEdit(sender.uuid(), Json.parseObject(opJson), sender);
    }

    /** Applies an operation as the given sender (scripts and the importer pass null for the sender). */
    public boolean applyEdit(UUID senderId, JsonObject op, PlayerView sender) {
        EditOps.Result r = EditOps.apply(engine.book(), op);
        if (!r.ok) {
            if (sender != null) {
                result(sender, "error", "Edit failed: " + r.error);
            }
            return false;
        }
        bookDirty = true;
        try {
            store.save(engine.book());
            bookDirty = false;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not save quests", e);
        }
        String message = senderId == null ? "" : senderId.toString();
        byte[] packet = new Wire.Out(Msg.S_OP).str(message).str(Json.compact(op)).done();
        for (PlayerView p : host.players()) {
            net.send(p.uuid(), packet);
        }
        // New or removed quests change which progress entries matter; resend progress so statuses stay right.
        for (TeamManager.Team t : engine.teams().all()) {
            engine.markTeamDirty(t.id);
        }
        return true;
    }

    private void team(PlayerView p, String action, String arg) {
        TeamManager tm = engine.teams();
        String err;
        switch (action) {
            case "create":
                err = tm.create(p.uuid(), arg);
                break;
            case "leave":
                err = tm.leave(p.uuid());
                break;
            case "disband":
                err = tm.disband(p.uuid());
                break;
            case "invite": {
                UUID target = findOnline(arg);
                err = target == null ? "player_not_found" : tm.invite(p.uuid(), target);
                break;
            }
            case "join":
                err = tm.join(p.uuid(), arg);
                break;
            case "kick": {
                UUID target = findByName(arg);
                err = target == null ? "player_not_found" : tm.kick(p.uuid(), target);
                break;
            }
            default:
                err = "unknown_action";
        }
        if (err != null) {
            result(p, "error", "Team: " + err.replace('_', ' '));
        } else {
            result(p, "info", "Team updated");
        }
        for (PlayerView o : host.players()) {
            engine.markTeamDirty(engine.teams().teamOf(o.uuid()).id);
        }
    }

    private UUID findOnline(String name) {
        for (PlayerView p : host.players()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p.uuid();
            }
        }
        return null;
    }

    private UUID findByName(String name) {
        UUID online = findOnline(name);
        if (online != null) {
            return online;
        }
        for (TeamManager.Team t : engine.teams().all()) {
            for (UUID m : t.members) {
                if (engine.teams().nameOf(m).equalsIgnoreCase(name)) {
                    return m;
                }
            }
        }
        return null;
    }

    // ---- engine events: tell scripts and the player ----

    @Override
    public void questStarted(String teamId, Quest quest) {
        ScriptEvents.fire("questStarted", teamId, quest, null, null);
    }

    @Override
    public void taskProgressed(String teamId, Quest quest, Def task, long value, long target) {
        ScriptEvents.fire("taskProgressed", teamId, quest, task, null);
    }

    @Override
    public void questCompleted(String teamId, Quest quest) {
        ScriptEvents.fire("questCompleted", teamId, quest, null, null);
        TeamManager.Team t = engine.teams().team(teamId);
        if (t != null) {
            for (UUID m : t.members) {
                PlayerView p = host.online(m);
                if (p != null) {
                    net.send(m, new Wire.Out(Msg.S_TOAST).str(quest.title).str("Quest complete").done());
                }
            }
        }
    }

    @Override
    public void questClaimed(String teamId, UUID player, Quest quest, Def reward) {
        ScriptEvents.fire("questClaimed", teamId, quest, reward, player);
    }

    /** Opens the book on a player's client. */
    public void open(PlayerView p, String questId) {
        net.send(p.uuid(), new Wire.Out(Msg.S_OPEN).str(questId == null ? "" : questId).done());
    }

    public void sendToast(PlayerView p, String title, String text) {
        net.send(p.uuid(), new Wire.Out(Msg.S_TOAST).str(title).str(text).done());
    }
}
