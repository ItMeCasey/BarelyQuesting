package com.itmecasey.barelyquesting.ui;

import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.Def;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.core.model.Quest;
import com.itmecasey.barelyquesting.core.progress.QuestEngine;
import com.itmecasey.barelyquesting.core.progress.QuestEngine.Status;
import com.itmecasey.barelyquesting.core.progress.Rewards;
import com.itmecasey.barelyquesting.core.task.TaskType;
import com.itmecasey.barelyquesting.core.task.TaskTypes;
import com.itmecasey.barelyquesting.theme.Theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** The right-hand panel: the selected quest for players, and its fields for editors. */
final class QuestPanel {
    private final BookUi book;
    private final Map<String, TextEditor> fields = new HashMap<>();
    private int scroll;
    private int contentH;
    private int px;
    private int py;
    private int pw;
    private int ph;
    private boolean previewing;

    QuestPanel(BookUi book) {
        this.book = book;
    }

    void reset() {
        scroll = 0;
        fields.clear();
    }

    boolean ownsFocusAt(int x, int y) {
        return false;
    }

    private String url;

    void render(int x, int y, int w, int h) {
        Quest q = book.selectedQuest();
        if (q == null) {
            return;
        }
        Ui ui = book.ui;
        Gfx g = ui.g;
        Theme th = ui.th;
        px = x;
        py = y;
        pw = w;
        ph = h;
        th.draw(g, "panel_inset", x, y, w, h);
        boolean edit = book.editing && !previewing;

        // title row
        g.item(q.icon, x + 5, y + 5, 16);
        g.text(ui.fit(q.title, w - 70, Gfx.BOLD), x + 25, y + 9, th.color("title"), Gfx.BOLD | Gfx.SHADOW);
        int bx = x + w - 15;
        ui.button("x", bx, y + 4, 12, 14, true, () -> book.select(""));
        bx -= 14;
        boolean pinned = book.st.pinned.contains(q.id);
        ui.toggle("*", bx, y + 4, 12, 14, pinned, () -> book.st.togglePin(q.id));
        if (ui.hover(bx, y + 4, 12, 14)) {
            g.tooltip(List.of(pinned ? "Unpin from the HUD" : "Pin to the HUD"), ui.mx, ui.my);
        }
        if (book.editing) {
            bx -= 14;
            ui.toggle(previewing ? "v" : "e", bx, y + 4, 12, 14, !previewing, () -> previewing = !previewing);
            if (ui.hover(bx, y + 4, 12, 14)) {
                g.tooltip(List.of(previewing ? "Back to editing" : "Preview as a player"), ui.mx, ui.my);
            }
        }
        g.fill(x + 4, y + 23, w - 8, 1, th.color("panel_edge"));

        int vx = x + 3;
        int vy = y + 26;
        int vw = w - 6;
        int vh = h - 28;
        ui.clip(vx, vy, vw, vh);
        ui.zone(vx, vy, vw, vh, null, d -> scroll = Ui.clamp(scroll - (int) (d * 20), 0, Math.max(0, contentH - vh)));
        int cy = vy - scroll;
        url = null;
        if (edit) {
            cy = editor(q, vx + 2, cy, vw - 6);
        } else {
            cy = view(q, vx + 2, cy, vw - 6);
        }
        contentH = cy - (vy - scroll) + 6;
        ui.unclip();
        scroll = Ui.clamp(scroll, 0, Math.max(0, contentH - vh));
        ui.scrollbar(x + w - 5, vy, vh, vh, contentH, scroll);
    }

    // ================= player view =================

    private int view(Quest q, int x, int y, int w) {
        Ui ui = book.ui;
        Gfx g = ui.g;
        Theme th = ui.th;
        QuestEngine.Status s = book.st.status(q);
        String label = s == Status.COMPLETE ? "Complete: claim your rewards" : s == Status.CLAIMED ? "Complete" : s == Status.LOCKED ? "Locked"
                : s == Status.IN_PROGRESS ? "In progress" : "Available";
        int color = s == Status.LOCKED ? th.color("danger") : s == Status.COMPLETE ? th.color("accent")
                : s == Status.CLAIMED ? th.color("success") : th.color("text_muted");
        g.text(label, x, y + 2, color, 0);
        y += 14;
        if (!q.description.isEmpty()) {
            List<List<RichText.Piece>> lines = RichText.layout(g, RichText.parse(q.description), w);
            String[] hover = new String[1];
            y += RichText.draw(g, lines, x, y, th.color("text"), ui.mx, ui.my, hover);
            url = hover[0];
            if (url != null) {
                g.tooltip(List.of(url, "(link: click to copy)"), ui.mx, ui.my);
                final String u = url;
                ui.zone(px, py, pw, ph, b -> g.setClipboard(u), null);
            }
            y += 6;
        }
        if (s == Status.LOCKED) {
            for (String dep : q.dependencies) {
                Quest d = book.st.book.quest(dep);
                if (d != null && !book.st.engine().isComplete(book.st.teamId, dep)) {
                    g.text(ui.fit("Needs: " + d.title, w, 0), x, y, th.color("danger"), 0);
                    y += 11;
                }
            }
            for (Def lock : q.locks) {
                Rewards.LockType lt = Rewards.lock(lock.type);
                g.text(ui.fit(lt == null ? lock.type : lt.describe(lock), w, 0), x, y, th.color("danger"), 0);
                y += 11;
            }
            y += 4;
        }
        y = heading(g, th, "Tasks", x, y, w);
        for (Def t : q.tasks) {
            y = taskRow(q, t, x, y, w, s);
        }
        if (q.tasks.isEmpty()) {
            g.text("Nothing to do here.", x, y, th.color("text_muted"), 0);
            y += 12;
        }
        if (!q.rewards.isEmpty()) {
            y += 4;
            y = heading(g, th, "Rewards", x, y, w);
            boolean anyLeft = false;
            for (Def r : q.rewards) {
                y = rewardRow(q, r, x, y, w, s);
                anyLeft |= !book.st.isClaimed(q, r);
            }
            if (s == Status.COMPLETE && anyLeft) {
                y += 2;
                ui.button("Claim all", x, y, w, 16, true, () -> book.st.claim(q, null));
                y += 20;
            }
        }
        return y;
    }

    private int heading(Gfx g, Theme th, String text, int x, int y, int w) {
        g.text(text, x, y, th.color("accent"), Gfx.BOLD);
        g.fill(x, y + 10, w, 1, th.color("panel_edge"));
        return y + 14;
    }

    private int taskRow(Quest q, Def t, int x, int y, int w, Status s) {
        Ui ui = book.ui;
        Gfx g = ui.g;
        Theme th = ui.th;
        TaskType type = TaskTypes.get(t.type);
        long have = book.st.taskProgress(q, t);
        long need = QuestEngine.targetOf(t);
        boolean done = have >= need;
        ItemSpec icon = itemOf(t);
        int tx = x;
        if (icon != null) {
            ui.slot(x, y, 18);
            g.item(icon, x + 1, y + 1, 16);
            itemClicks(icon, x, y, 18);
            tx = x + 22;
        }
        ui.checkbox(x + w - 12, y + 2, 10, done, th.color("success"));
        String text = TaskText.task(g, t);
        g.text(ui.fit(text, w - (tx - x) - 16, 0), tx, y + 1, th.color(done ? "success" : "text"), 0);
        if (need > 1) {
            ui.bar(tx, y + 11, w - (tx - x) - 4, 7, (double) have / need);
            String n = have + "/" + need;
            g.text(n, tx + (w - (tx - x) - 4 - g.textWidth(n, 0)) / 2, y + 10, th.color("text"), Gfx.SHADOW);
        }
        int h = icon != null ? 22 : (need > 1 ? 21 : 13);
        boolean open = s != Status.LOCKED && !done;
        if (open && type != null && type.submits(t)) {
            ui.button("Submit items", x + (tx - x), y + h, w - (tx - x) - 4, 14, true, () -> book.st.submit(q, t));
            h += 17;
        } else if (open && type != null && type.manual(t)) {
            ui.button("Check", x + (tx - x), y + h, 60, 14, true, () -> book.st.check(q, t));
            h += 17;
        }
        return y + h + 3;
    }

    private int rewardRow(Quest q, Def r, int x, int y, int w, Status s) {
        Ui ui = book.ui;
        Gfx g = ui.g;
        Theme th = ui.th;
        Rewards.RewardType type = Rewards.reward(r.type);
        ItemSpec icon = r.type.equals("item") ? r.item("item") : null;
        boolean claimed = book.st.isClaimed(q, r);
        int tx = x;
        if (icon != null && !icon.isEmpty()) {
            ui.slot(x, y, 18);
            g.item(icon, x + 1, y + 1, 16);
            itemClicks(icon, x, y, 18);
            tx = x + 22;
        }
        String text = TaskText.reward(g, r);
        g.text(ui.fit(text, w - (tx - x) - 4, 0), tx, y + 5, claimed ? th.color("text_muted") : th.color("text"), 0);
        int h = 20;
        if (s == Status.COMPLETE && !claimed) {
            ui.button("Claim", x + w - 44, y, 42, 16, true, () -> book.st.claim(q, r));
        } else if (claimed) {
            g.text("claimed", x + w - 40, y + 5, th.color("success"), 0);
        }
        return y + h;
    }

    private static ItemSpec itemOf(Def t) {
        if (!t.params.has("item")) {
            return null;
        }
        ItemSpec s = t.item("item");
        return s.isEmpty() && s.id().equals("minecraft:air") ? null : s;
    }

    /** Left click: recipes, right click: uses, in the installed recipe viewer. */
    private void itemClicks(ItemSpec spec, int x, int y, int size) {
        Ui ui = book.ui;
        ItemSpec single = spec.withCount(Math.max(1, spec.count()));
        if (ui.hover(x, y, size, size)) {
            if (spec.isTag()) {
                ui.g.tooltip(List.of("#" + spec.id(), "Any item with this tag"), ui.mx, ui.my);
            } else {
                ui.g.itemTooltip(single, ui.mx, ui.my);
            }
        }
        ui.zone(x, y, size, size, b -> {
            ClientEnv env = ClientEnv.Holder.get();
            boolean ok = b == 1 ? env.showUses(single) : env.showRecipes(single);
            if (!ok && !env.viewers().isEmpty()) {
                book.say("The recipe viewer has nothing for this item", true);
            }
        }, null);
    }

    // ================= editor =================

    private void edit(Consumer<Quest> change) {
        Quest q = book.selectedQuest();
        Chapter c = q == null ? null : book.st.book.chapterOf(q.id);
        if (q == null || c == null) {
            return;
        }
        Quest copy = q.copy();
        change.accept(copy);
        if (!copy.toJson().equals(q.toJson())) {
            book.st.edit(EditOps.putQuest(c.id, copy, null));
        }
    }

    private TextEditor field(String key, String value, boolean multiline, int x, int y, int w, int h, Consumer<String> commit) {
        Ui ui = book.ui;
        Theme th = ui.th;
        String full = book.selected + "/" + key;
        TextEditor ed = fields.computeIfAbsent(full, k -> new TextEditor(multiline));
        ed.onCommit = commit;
        if (!ed.focused && !ed.text().equals(value)) {
            ed.set(value);
        }
        th.draw(ui.g, "slot", x, y, w, h);
        ed.draw(ui.g, x, y, w, h, th.color("text"), th.color("text_muted"), ui.now);
        ui.zone(x, y, w, h, b -> {
            book.setFocus(ed);
            ed.click(ui.g, ui.mx - x, ui.my - y);
        }, multiline ? d -> ed.scroll(-(int) Math.signum(d)) : null);
        return ed;
    }

    private int editor(Quest q, int x, int y, int w) {
        Ui ui = book.ui;
        Gfx g = ui.g;
        Theme th = ui.th;
        g.text("Title", x, y, th.color("text_muted"), 0);
        y += 10;
        field("title", q.title, false, x, y, w, 16, v -> edit(c -> c.title = v));
        y += 20;
        g.text("Description", x, y, th.color("text_muted"), 0);
        y += 10;
        field("desc", q.description, true, x, y, w, 62, v -> edit(c -> c.description = v));
        y += 66;

        // icon and position
        ui.slot(x, y, 20);
        g.item(q.icon, x + 2, y + 2, 16);
        ui.zone(x, y, 20, 20, b -> book.open(new Modals.ItemPick(q.icon, false, spec -> edit(c -> c.icon = spec.withCount(1)))), null);
        if (ui.hover(x, y, 20, 20)) {
            g.tooltip(List.of("Icon", "Click to choose"), ui.mx, ui.my);
        }
        int bx = x + 26;
        g.text("X", bx, y + 6, th.color("text_muted"), 0);
        ui.button("-", bx + 8, y + 2, 12, 16, true, () -> edit(c -> c.x--));
        g.text(String.valueOf(q.x), bx + 23, y + 6, th.color("text"), 0);
        ui.button("+", bx + 36, y + 2, 12, 16, true, () -> edit(c -> c.x++));
        bx += 56;
        g.text("Y", bx, y + 6, th.color("text_muted"), 0);
        ui.button("-", bx + 8, y + 2, 12, 16, true, () -> edit(c -> c.y--));
        g.text(String.valueOf(q.y), bx + 23, y + 6, th.color("text"), 0);
        ui.button("+", bx + 36, y + 2, 12, 16, true, () -> edit(c -> c.y++));
        y += 26;

        int third = (w - 4) / 3;
        ui.toggle("Optional", x, y, third, 14, q.optional, () -> edit(c -> c.optional = !c.optional));
        ui.toggle("Hidden", x + third + 2, y, third, 14, q.hidden, () -> edit(c -> c.hidden = !c.hidden));
        ui.toggle("Any task", x + 2 * (third + 2), y, third, 14, q.anyTask, () -> edit(c -> c.anyTask = !c.anyTask));
        y += 20;

        // dependencies
        y = heading(g, th, "Needs", x, y, w);
        for (int i = 0; i < q.dependencies.size(); i++) {
            final int idx = i;
            Quest d = book.st.book.quest(q.dependencies.get(i));
            g.text(ui.fit(d == null ? q.dependencies.get(i) : d.title, w - 18, 0), x + 2, y + 2, th.color("text"), 0);
            ui.button("x", x + w - 14, y, 12, 13, true, () -> edit(c -> c.dependencies.remove(idx)));
            y += 15;
        }
        ui.button("+ Needs quest", x, y, w, 14, true, () -> {
            List<String> ids = new ArrayList<>();
            for (Quest o : book.st.book.allQuests()) {
                if (!o.id.equals(q.id) && !q.dependencies.contains(o.id)) {
                    ids.add(o.id);
                }
            }
            book.open(new Modals.ListPick("Needs quest", ids, id -> {
                Quest o = book.st.book.quest(id);
                return o == null ? id : o.title;
            }, false, id -> {
                if (book.st.book.reaches(id, q.id, new java.util.HashSet<>())) {
                    book.say("That would make a loop", true);
                } else {
                    edit(c -> c.dependencies.add(id));
                }
            }));
        });
        y += 20;

        y = defs("Tasks", "task", q, q.tasks, x, y, w);
        y = defs("Rewards", "reward", q, q.rewards, x, y, w);
        y = defs("Locks", "lock", q, q.locks, x, y, w);

        y += 4;
        ui.button("Delete quest", x, y, w, 16, true, () -> book.open(new Modals.Confirm("Delete \"" + q.title + "\"?",
                "Other quests that need it lose that requirement.", () -> {
                    book.st.edit(EditOps.removeQuest(q.id));
                    book.select("");
                })));
        y += 22;
        return y;
    }

    private List<Def> listOf(Quest q, String kind) {
        return kind.equals("task") ? q.tasks : kind.equals("reward") ? q.rewards : q.locks;
    }

    private List<Schemas.Param> schema(String kind, Def d) {
        List<Schemas.Param> p = kind.equals("task") ? Schemas.task(d.type) : kind.equals("reward") ? Schemas.reward(d.type) : Schemas.lock(d.type);
        return p.isEmpty() ? Schemas.generic(d.params) : p;
    }

    private List<String> typeIds(String kind) {
        List<String> ids = new ArrayList<>();
        if (kind.equals("task")) {
            for (TaskType t : TaskTypes.all()) {
                ids.add(t.id());
            }
        } else if (kind.equals("reward")) {
            for (Rewards.RewardType t : Rewards.rewards()) {
                ids.add(t.id());
            }
        } else {
            for (Rewards.LockType t : Rewards.locks()) {
                ids.add(t.id());
            }
        }
        return ids;
    }

    private int defs(String title, String kind, Quest q, List<Def> list, int x, int y, int w) {
        Ui ui = book.ui;
        Gfx g = ui.g;
        Theme th = ui.th;
        y += 4;
        y = heading(g, th, title, x, y, w);
        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            Def d = list.get(i);
            ui.button(d.type, x, y, w - 30, 14, true, () -> book.open(new Modals.ListPick("Change type", typeIds(kind), s -> s, false, type -> edit(c -> {
                Def target = listOf(c, kind).get(idx);
                target.type = type;
                target.params = new JsonObject();
                Schemas.applyDefaults(target, schema(kind, target));
            }))));
            ui.button("x", x + w - 14, y, 12, 14, true, () -> edit(c -> listOf(c, kind).remove(idx)));
            if (i > 0) {
                ui.button("^", x + w - 28, y, 12, 14, true, () -> edit(c -> java.util.Collections.swap(listOf(c, kind), idx, idx - 1)));
            }
            y += 16;
            for (Schemas.Param p : schema(kind, d)) {
                y = param(q, kind, idx, d, p, x + 4, y, w - 4);
            }
            y += 3;
        }
        ui.button("+ " + title.substring(0, title.length() - 1), x, y, w, 14, true, () -> book.open(new Modals.ListPick("Add " + kind, typeIds(kind), s -> s, false, type -> edit(c -> {
            Def d = Def.of(freshId(listOf(c, kind), kind.substring(0, 1)), type);
            Schemas.applyDefaults(d, schema(kind, d));
            listOf(c, kind).add(d);
        }))));
        return y + 18;
    }

    private static String freshId(List<Def> list, String prefix) {
        int n = list.size();
        while (true) {
            String id = prefix + n;
            boolean used = false;
            for (Def d : list) {
                used |= d.id.equals(id);
            }
            if (!used) {
                return id;
            }
            n++;
        }
    }

    private int param(Quest q, String kind, int idx, Def d, Schemas.Param p, int x, int y, int w) {
        Ui ui = book.ui;
        Gfx g = ui.g;
        Theme th = ui.th;
        String cur = p.kind == Schemas.Kind.ITEM ? d.item(p.key).toString() : d.params.has(p.key) && d.params.get(p.key).isJsonPrimitive()
                ? d.params.get(p.key).getAsString() : "";
        int lw = Math.min(w / 2 - 4, g.textWidth(p.label, 0) + 4);
        g.text(ui.fit(p.label, w / 2 - 6, 0), x, y + 3, th.color("text_muted"), 0);
        int vx = x + w / 2 - 2;
        int vw = w - w / 2 - 2;
        boolean over = ui.hover(vx, y, vw, 14);
        th.draw(g, over ? "button_hover" : "slot", vx, y, vw, 14);
        if (p.kind == Schemas.Kind.ITEM) {
            ItemSpec s = d.item(p.key);
            g.item(s, vx + 1, y - 1, 16);
            g.text(ui.fit(s.toString(), vw - 20, 0), vx + 18, y + 3, th.color("text"), 0);
        } else if (p.kind == Schemas.Kind.BOOL) {
            ui.checkbox(vx + 3, y + 2, 10, d.bool(p.key, false), th.color("success"));
        } else {
            g.text(ui.fit(cur.isEmpty() ? "-" : cur, vw - 6, 0), vx + 3, y + 3, th.color("text"), 0);
        }
        ui.zone(vx, y, vw, 14, b -> editParam(q, kind, idx, d, p), null);
        return y + 16;
    }

    private void editParam(Quest q, String kind, int idx, Def d, Schemas.Param p) {
        switch (p.kind) {
            case BOOL:
                edit(c -> {
                    Def t = listOf(c, kind).get(idx);
                    t.params.addProperty(p.key, !t.bool(p.key, false));
                });
                break;
            case CHOICE: {
                String[] opts = p.ref.split(",");
                String cur = d.str(p.key, p.def);
                int at = 0;
                for (int i = 0; i < opts.length; i++) {
                    if (opts[i].equals(cur)) {
                        at = i;
                    }
                }
                final String next = opts[(at + 1) % opts.length];
                edit(c -> listOf(c, kind).get(idx).params.addProperty(p.key, next));
                break;
            }
            case NUMBER:
                book.open(new Modals.Prompt(p.label, d.params.has(p.key) ? d.str(p.key, "") : p.def, true, text -> edit(c -> {
                    Def t = listOf(c, kind).get(idx);
                    try {
                        t.params.addProperty(p.key, Long.parseLong(text.trim().split("\\.")[0]));
                    } catch (NumberFormatException e) {
                        t.params.remove(p.key);
                    }
                })));
                break;
            case ITEM:
                book.open(new Modals.ItemPick(d.item(p.key), true, spec -> edit(c -> listOf(c, kind).get(idx).params.add(p.key, spec.toJson()))));
                break;
            case REGISTRY:
                book.open(new Modals.ListPick(p.label, ClientEnv.Holder.get().ids(p.ref), s -> s, true,
                        v -> edit(c -> {
                            Def t = listOf(c, kind).get(idx);
                            if (v.isEmpty()) {
                                t.params.remove(p.key);
                            } else {
                                t.params.addProperty(p.key, v);
                            }
                        })));
                break;
            case QUEST: {
                List<String> ids = new ArrayList<>();
                for (Quest o : book.st.book.allQuests()) {
                    if (!o.id.equals(q.id)) {
                        ids.add(o.id);
                    }
                }
                book.open(new Modals.ListPick(p.label, ids, id -> {
                    Quest o = book.st.book.quest(id);
                    return o == null ? id : o.title;
                }, false, v -> edit(c -> listOf(c, kind).get(idx).params.addProperty(p.key, v))));
                break;
            }
            default:
                book.open(new Modals.Prompt(p.label, d.str(p.key, ""), false, text -> edit(c -> {
                    Def t = listOf(c, kind).get(idx);
                    if (text.isEmpty()) {
                        t.params.remove(p.key);
                    } else {
                        t.params.addProperty(p.key, text);
                    }
                })));
        }
    }
}
