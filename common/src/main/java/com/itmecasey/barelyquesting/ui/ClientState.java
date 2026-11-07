package com.itmecasey.barelyquesting.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Book;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.net.Msg;
import com.itmecasey.barelyquesting.core.net.Wire;
import com.itmecasey.barelyquesting.core.progress.PlayerView;
import com.itmecasey.barelyquesting.core.progress.Progress;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.team.TeamManager;
import com.itmecasey.barelyquesting.theme.ThemeManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The client's copy of what the server sent (book, own team's progress, team info, edit permission) plus the player's own settings.
 * The screen and the HUD read from here; actions go out through {@link ClientEnv#sendToServer}.
 */
public final class ClientState {
    /** A message shown for a few seconds. */
    public static final class Toast {
        public final String title;
        public final String text;
        public final long shownAt;

        Toast(String title, String text, long shownAt) {
            this.title = title;
            this.text = text;
            this.shownAt = shownAt;
        }
    }

    private static final ClientState INSTANCE = new ClientState();

    public static ClientState get() {
        return INSTANCE;
    }

    public Book book = new Book();
    public final Progress progress = new Progress();
    public String teamId = "";
    public UUID self = new UUID(0, 0);
    public boolean canEdit;
    public JsonObject team = new JsonObject();
    public final Set<String> serverLocked = new HashSet<>();
    public final Deque<Toast> toasts = new ArrayDeque<>();
    public String note = "";
    public boolean messageIsError;
    public long messageAt;
    public boolean connected;

    /** Quest the server asked the client to open the book on (empty string: just open), or null. */
    public volatile String pendingOpen;

    // settings (client.json)
    public String view = "graph";
    public String lastChapter = "";
    public boolean hud = true;
    public final Set<String> pinned = new LinkedHashSet<>();

    private QuestEngine engine = newEngine(book);
    private final List<byte[]> chunks = new ArrayList<>();
    private final Deque<JsonObject> undo = new ArrayDeque<>();
    private final Deque<JsonObject> redo = new ArrayDeque<>();
    private final Deque<Character> pendingOwn = new ArrayDeque<>();
    private int bookChunks;
    private int chunksSeen;

    private QuestEngine newEngine(Book b) {
        return new QuestEngine(b, new TeamManager(), progress, new QuestEngine.Host() {
            @Override
            public PlayerView online(UUID id) {
                return null;
            }

            @Override
            public long nowMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public List<PlayerView> players() {
                return new ArrayList<>();
            }
        });
    }

    /** Forgets everything from the server (on disconnect). */
    public void reset() {
        book = new Book();
        engine = newEngine(book);
        progress.clearAll();
        teamId = "";
        canEdit = false;
        team = new JsonObject();
        serverLocked.clear();
        undo.clear();
        redo.clear();
        pendingOwn.clear();
        toasts.clear();
        connected = false;
    }

    public QuestEngine engine() {
        return engine;
    }

    public QuestEngine.Status status(Quest q) {
        QuestEngine.Status s = engine.status(teamId, self, q);
        if ((s == QuestEngine.Status.AVAILABLE || s == QuestEngine.Status.IN_PROGRESS) && serverLocked.contains(q.id)) {
            return QuestEngine.Status.LOCKED;
        }
        return s;
    }

    public long taskProgress(Quest q, Def task) {
        return engine.taskProgress(teamId, q.id, task.id);
    }

    public boolean isClaimed(Quest q, Def reward) {
        Progress.QuestProgress p = progress.get(teamId, q.id);
        return p != null && p.claimed(self.toString(), reward.id, QuestEngine.isTeamScope(reward));
    }

    public boolean isVisible(Quest q) {
        return canEdit || engine.isVisible(teamId, q);
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    // ---- messages from the server ----

    public void handle(byte[] message) {
        try {
            Wire.In in = new Wire.In(message);
            switch (in.id()) {
                case Msg.S_INFO:
                    self = UUID.fromString(in.str());
                    canEdit = in.bool();
                    connected = true;
                    break;
                case Msg.S_BOOK:
                    onBookChunk(in.i(), in.i(), in.blob());
                    break;
                case Msg.S_PROGRESS: {
                    teamId = in.str();
                    JsonObject o = Json.parseObject(in.str());
                    serverLocked.clear();
                    JsonElement locked = o.remove("$locked");
                    if (locked != null && locked.isJsonArray()) {
                        locked.getAsJsonArray().forEach(e -> serverLocked.add(e.getAsString()));
                    }
                    progress.setTeamFromJson(teamId, o);
                    break;
                }
                case Msg.S_OP:
                    onOp(in.str(), Json.parseObject(in.str()));
                    break;
                case Msg.S_TEAM:
                    team = Json.parseObject(in.str());
                    break;
                case Msg.S_TOAST:
                    toasts.addLast(new Toast(in.str(), in.str(), ClientEnv.Holder.get().millis()));
                    while (toasts.size() > 4) {
                        toasts.removeFirst();
                    }
                    playSound("complete");
                    break;
                case Msg.S_OPEN:
                    pendingOpen = in.str();
                    break;
                case Msg.S_RESULT: {
                    String kind = in.str();
                    note = in.str();
                    messageIsError = kind.equals("error");
                    messageAt = ClientEnv.Holder.get().millis();
                    if (messageIsError && note.startsWith("Edit failed") || note.startsWith("You may not edit")) {
                        pendingOwn.pollFirst();
                    }
                    break;
                }
                default:
                    break;
            }
        } catch (RuntimeException e) {
            java.util.logging.Logger.getLogger("barelyquesting").warning("Bad message from server: " + e);
        }
    }

    private void onBookChunk(int index, int total, byte[] data) {
        if (index == 0) {
            chunks.clear();
            bookChunks = total;
            chunksSeen = 0;
        }
        chunks.add(data);
        chunksSeen++;
        if (chunksSeen >= bookChunks) {
            int size = 0;
            for (byte[] c : chunks) {
                size += c.length;
            }
            byte[] all = new byte[size];
            int at = 0;
            for (byte[] c : chunks) {
                System.arraycopy(c, 0, all, at, c.length);
                at += c.length;
            }
            chunks.clear();
            book = Book.fromJson(Json.parseObject(Wire.gunzip(all)));
            engine.setBook(book);
            undo.clear();
            redo.clear();
            pendingOwn.clear();
        }
    }

    private void onOp(String sender, JsonObject op) {
        EditOps.Result r = EditOps.apply(book, op);
        if (!sender.isEmpty() && sender.equals(self.toString())) {
            Character kind = pendingOwn.pollFirst();
            if (r.ok && kind != null) {
                if (kind == 'u') {
                    redo.push(r.inverse);
                } else if (kind == 'r') {
                    undo.push(r.inverse);
                } else {
                    undo.push(r.inverse);
                    redo.clear();
                }
            }
        }
    }

    // ---- actions to the server ----

    private void send(byte[] message) {
        ClientEnv.Holder.get().sendToServer(message);
    }

    public void hello() {
        send(new Wire.Out(Msg.C_HELLO).done());
    }

    public void claim(Quest q, Def reward) {
        send(new Wire.Out(Msg.C_CLAIM).str(q.id).str(reward == null ? "" : reward.id).done());
        playSound("claim");
    }

    public void submit(Quest q, Def task) {
        send(new Wire.Out(Msg.C_SUBMIT).str(q.id).str(task.id).done());
    }

    public void check(Quest q, Def task) {
        send(new Wire.Out(Msg.C_CHECK).str(q.id).str(task.id).done());
    }

    public void teamAction(String action, String arg) {
        send(new Wire.Out(Msg.C_TEAM).str(action).str(arg).done());
    }

    /** Sends an edit; the change shows up when the server echoes it back. */
    public void edit(JsonObject op) {
        pendingOwn.addLast('n');
        send(new Wire.Out(Msg.C_EDIT).str(Json.compact(op)).done());
    }

    public void undo() {
        JsonObject op = undo.poll();
        if (op != null) {
            pendingOwn.addLast('u');
            send(new Wire.Out(Msg.C_EDIT).str(Json.compact(op)).done());
        }
    }

    public void redo() {
        JsonObject op = redo.poll();
        if (op != null) {
            pendingOwn.addLast('r');
            send(new Wire.Out(Msg.C_EDIT).str(Json.compact(op)).done());
        }
    }

    // ---- sounds are played by the screen through Gfx; this only picks the id ----

    public volatile String pendingSound;

    public void playSound(String key) {
        pendingSound = ThemeManager.current().sound(key);
    }

    // ---- settings file ----

    private Path settingsFile() {
        return ClientEnv.Holder.get().configDir().resolve("client.json");
    }

    public void loadSettings() {
        try {
            Path f = settingsFile();
            if (Files.exists(f)) {
                JsonObject o = Json.parseObject(Files.readString(f, StandardCharsets.UTF_8));
                view = Json.str(o, "view", view);
                lastChapter = Json.str(o, "chapter", lastChapter);
                hud = Json.bool(o, "hud", true);
                String theme = Json.str(o, "theme", "");
                if (!theme.isEmpty()) {
                    ThemeManager.select(theme);
                }
                pinned.clear();
                Json.arr(o, "pinned").forEach(e -> pinned.add(e.getAsString()));
            }
        } catch (IOException | RuntimeException e) {
            java.util.logging.Logger.getLogger("barelyquesting").warning("Could not read client.json: " + e);
        }
    }

    public void saveSettings() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("view", view);
            o.addProperty("chapter", lastChapter);
            o.addProperty("hud", hud);
            o.addProperty("theme", ThemeManager.selectedId());
            com.google.gson.JsonArray pins = new com.google.gson.JsonArray();
            pinned.forEach(pins::add);
            o.add("pinned", pins);
            Json.writeAtomic(settingsFile(), Json.pretty(o));
        } catch (IOException e) {
            java.util.logging.Logger.getLogger("barelyquesting").warning("Could not write client.json: " + e);
        }
    }

    public void togglePin(String questId) {
        if (!pinned.remove(questId)) {
            pinned.add(questId);
        }
        saveSettings();
    }
}
