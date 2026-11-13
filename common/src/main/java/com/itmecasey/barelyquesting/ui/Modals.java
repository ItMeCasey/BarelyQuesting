package com.itmecasey.barelyquesting.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itmecasey.barelyquesting.core.Json;
import com.itmecasey.barelyquesting.core.edit.EditOps;
import com.itmecasey.barelyquesting.core.model.Chapter;
import com.itmecasey.barelyquesting.core.model.ItemSpec;
import com.itmecasey.barelyquesting.theme.Theme;
import com.itmecasey.barelyquesting.theme.ThemeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/** Dialogs drawn over the book: text prompt, confirmation, pickers, team panel, theme list and chapter settings. */
public final class Modals {
    private Modals() {
    }

    /** A centered box with a title and a close button. */
    public abstract static class Modal {
        final String title;
        final int w;
        final int h;

        Modal(String title, int w, int h) {
            this.title = title;
            this.w = w;
            this.h = h;
        }

        abstract void content(BookUi b, Ui ui, int x, int y, int w, int h);

        boolean key(BookUi b, Gfx g, int key, int mods) {
            return false;
        }

        boolean chr(char c) {
            return false;
        }

        void render(BookUi b, Ui ui) {
            Gfx g = ui.g;
            Theme th = ui.th;
            int mw = Math.min(w, g.width() - 12);
            int mh = Math.min(h, g.height() - 12);
            int x = (g.width() - mw) / 2;
            int y = (g.height() - mh) / 2;
            // swallow clicks outside the box so nothing underneath reacts
            ui.zone(0, 0, g.width(), g.height(), bt -> { }, d -> { });
            th.draw(g, "panel", x, y, mw, mh);
            g.text(ui.fit(title, mw - 40, Gfx.BOLD), x + 8, y + 7, th.color("title"), Gfx.BOLD | Gfx.SHADOW);
            g.fill(x + 6, y + 20, mw - 12, 1, th.color("panel_edge"));
            // The box-wide swallow zone goes first: zones registered later sit on top, so the x and the content stay clickable.
            ui.zone(x, y, mw, mh, bt -> { }, d -> { });
            ui.button("x", x + mw - 18, y + 4, 14, 14, true, b::closeModal);
            content(b, ui, x + 8, y + 26, mw - 16, mh - 32);
        }
    }

    // ---------------------------------------------------------------

    public static final class Prompt extends Modal {
        private final TextEditor editor;
        private final Consumer<String> ok;

        public Prompt(String title, String initial, boolean numeric, Consumer<String> ok) {
            super(title, 240, 84);
            this.editor = new TextEditor(false);
            this.editor.numeric = numeric;
            this.editor.set(initial);
            this.editor.focus();
            this.ok = ok;
        }

        @Override
        void content(BookUi b, Ui ui, int x, int y, int w, int h) {
            ui.th.draw(ui.g, "slot", x, y, w, 16);
            editor.draw(ui.g, x, y, w, 16, ui.color("text"), ui.color("text_muted"), ui.now);
            ui.zone(x, y, w, 16, bt -> editor.click(ui.g, ui.mx - x, ui.my - y), null);
            ui.button("OK", x, y + 24, w / 2 - 2, 16, true, () -> done(b));
            ui.button("Cancel", x + w / 2 + 2, y + 24, w / 2 - 2, 16, true, b::closeModal);
        }

        private void done(BookUi b) {
            b.closeModal();
            ok.accept(editor.text());
        }

        @Override
        boolean key(BookUi b, Gfx g, int key, int mods) {
            if (key == Keys.ENTER || key == Keys.KP_ENTER) {
                done(b);
                return true;
            }
            if (key == Keys.ESCAPE) {
                return false;
            }
            return editor.keyPressed(key, mods, g);
        }

        @Override
        boolean chr(char c) {
            return editor.charTyped(c);
        }
    }

    public static final class Confirm extends Modal {
        private final String text;
        private final Runnable yes;

        public Confirm(String title, String text, Runnable yes) {
            super(title, 260, 84);
            this.text = text;
            this.yes = yes;
        }

        @Override
        void content(BookUi b, Ui ui, int x, int y, int w, int h) {
            ui.g.text(ui.fit(text, w, 0), x, y + 2, ui.color("text"), 0);
            ui.button("Yes", x, y + 24, w / 2 - 2, 16, true, () -> {
                b.closeModal();
                yes.run();
            });
            ui.button("No", x + w / 2 + 2, y + 24, w / 2 - 2, 16, true, b::closeModal);
        }
    }

    // ---------------------------------------------------------------

    /** Pick one string from a searchable list; optionally accept typed text. */
    public static final class ListPick extends Modal {
        private final List<String> options;
        private final Function<String, String> label;
        private final boolean allowCustom;
        private final Consumer<String> choose;
        private final TextEditor search = new TextEditor(false);
        private int scroll;

        public ListPick(String title, List<String> options, Function<String, String> label, boolean allowCustom, Consumer<String> choose) {
            super(title, 280, 240);
            this.options = options;
            this.label = label;
            this.allowCustom = allowCustom;
            this.choose = choose;
            search.placeholder = "Search";
            search.focus();
        }

        @Override
        void content(BookUi b, Ui ui, int x, int y, int w, int h) {
            Gfx g = ui.g;
            ui.th.draw(g, "slot", x, y, w, 16);
            search.draw(g, x, y, w, 16, ui.color("text"), ui.color("text_muted"), ui.now);
            ui.zone(x, y, w, 16, bt -> search.click(g, ui.mx - x, ui.my - y), null);
            String f = search.text().trim().toLowerCase(Locale.ROOT);
            List<String> shown = new ArrayList<>();
            if (allowCustom) {
                shown.add("");
            }
            for (String o : options) {
                if (f.isEmpty() || o.toLowerCase(Locale.ROOT).contains(f) || label.apply(o).toLowerCase(Locale.ROOT).contains(f)) {
                    shown.add(o);
                }
            }
            if (allowCustom && !f.isEmpty() && !options.contains(search.text().trim())) {
                shown.add(0, search.text().trim());
            }
            int top = y + 20;
            int viewH = h - 24;
            int row = 14;
            scroll = Ui.clamp(scroll, 0, Math.max(0, shown.size() * row - viewH));
            ui.clip(x, top, w, viewH);
            ui.zone(x, top, w, viewH, null, d -> scroll = Ui.clamp(scroll - (int) (d * 28), 0, Math.max(0, shown.size() * row - viewH)));
            int cy = top - scroll;
            for (String s : shown) {
                boolean over = ui.hover(x, cy, w - 6, row);
                if (over) {
                    g.fill(x, cy, w - 6, row, ui.color("button_hover"));
                }
                String text = s.isEmpty() ? "(none)" : label.apply(s);
                if (allowCustom && !s.isEmpty() && !options.contains(s)) {
                    text = "Use \"" + s + "\"";
                }
                g.text(ui.fit(text, w - 12, 0), x + 3, cy + 3, ui.color("text"), 0);
                final String value = s;
                ui.zone(x, cy, w - 6, row, bt -> {
                    b.closeModal();
                    choose.accept(value);
                }, null);
                cy += row;
            }
            ui.unclip();
            ui.scrollbar(x + w - 4, top, viewH, viewH, shown.size() * row, scroll);
        }

        @Override
        boolean key(BookUi b, Gfx g, int key, int mods) {
            if (key == Keys.ESCAPE) {
                return false;
            }
            if ((key == Keys.ENTER || key == Keys.KP_ENTER) && allowCustom && !search.text().trim().isEmpty()) {
                b.closeModal();
                choose.accept(search.text().trim());
                return true;
            }
            return search.keyPressed(key, mods, g);
        }

        @Override
        boolean chr(char c) {
            return search.charTyped(c);
        }
    }

    // ---------------------------------------------------------------

    private static final Map<String, String> NAME_CACHE = new HashMap<>();

    /** Pick an item (or a tag), and optionally a count. */
    public static final class ItemPick extends Modal {
        private static final int COLS = 11;
        private final ItemSpec current;
        private final boolean withCount;
        private final Consumer<ItemSpec> choose;
        private final TextEditor search = new TextEditor(false);
        private final TextEditor count = new TextEditor(false);
        private final TextEditor tag = new TextEditor(false);
        private boolean tagMode;
        private int scroll;
        private List<String> filtered = new ArrayList<>();
        private String lastFilter = null;

        public ItemPick(ItemSpec current, boolean withCount, Consumer<ItemSpec> choose) {
            super("Pick an item", 280, 250);
            this.current = current;
            this.withCount = withCount;
            this.choose = choose;
            search.placeholder = "Search items";
            search.focus();
            count.numeric = true;
            count.maxLength = 5;
            count.set(String.valueOf(Math.max(1, current.count())));
            tag.set(current.isTag() ? current.id() : "");
            tagMode = current.isTag();
        }

        private TextEditor active() {
            return tagMode ? tag : search;
        }

        @Override
        void content(BookUi b, Ui ui, int x, int y, int w, int h) {
            Gfx g = ui.g;
            Theme th = ui.th;
            ui.toggle("Item", x, y, 40, 14, !tagMode, () -> {
                tagMode = false;
                search.focus();
                tag.blur();
            });
            ui.toggle("Tag", x + 42, y, 40, 14, tagMode, () -> {
                tagMode = true;
                tag.focus();
                search.blur();
            });
            if (withCount) {
                g.text("Count", x + w - 84, y + 3, th.color("text_muted"), 0);
                th.draw(g, "slot", x + w - 44, y - 1, 44, 16);
                count.draw(g, x + w - 44, y - 1, 44, 16, th.color("text"), th.color("text_muted"), ui.now);
                ui.zone(x + w - 44, y - 1, 44, 16, bt -> {
                    count.focus();
                    search.blur();
                    tag.blur();
                    count.click(g, ui.mx - (x + w - 44), 0);
                }, null);
            }
            int sy = y + 18;
            th.draw(g, "slot", x, sy, w, 16);
            TextEditor ed = active();
            ed.draw(g, x, sy, w, 16, th.color("text"), th.color("text_muted"), ui.now);
            ui.zone(x, sy, w, 16, bt -> {
                ed.focus();
                if (ed != count) {
                    count.blur();
                }
                ed.click(g, ui.mx - x, ui.my - sy);
            }, null);
            if (tagMode) {
                ui.button("Use tag", x, sy + 38, w, 16, !tag.text().trim().isEmpty(), () -> {
                    b.closeModal();
                    choose.accept(new ItemSpec(tag.text().trim().replace("#", ""), amount(), true, null, false));
                });
                return;
            }
            refilter(g);
            int top = sy + 22;
            int viewH = h - (top - y) - 4;
            int slot = 20;
            int rows = (filtered.size() + COLS - 1) / COLS;
            scroll = Ui.clamp(scroll, 0, Math.max(0, rows * slot - viewH));
            ui.clip(x, top, w, viewH);
            ui.zone(x, top, w, viewH, null, d -> scroll = Ui.clamp(scroll - (int) (d * 30), 0, Math.max(0, rows * slot - viewH)));
            ItemSpec hovered = null;
            for (int i = 0; i < filtered.size(); i++) {
                int cx = x + (i % COLS) * slot;
                int cy = top + (i / COLS) * slot - scroll;
                if (cy + slot < top || cy > top + viewH) {
                    continue;
                }
                ui.slot(cx, cy, slot);
                ItemSpec spec = ItemSpec.of(filtered.get(i));
                g.item(spec, cx + 2, cy + 2, 16);
                if (ui.hover(cx, cy, slot, slot)) {
                    hovered = spec;
                }
                final ItemSpec pick = spec;
                ui.zone(cx, cy, slot, slot, bt -> {
                    b.closeModal();
                    choose.accept(new ItemSpec(pick.id(), amount(), false, current.data(), current.matchData()));
                }, null);
            }
            ui.unclip();
            ui.scrollbar(x + w - 3, top, viewH, viewH, rows * slot, scroll);
            if (hovered != null) {
                g.tooltip(List.of(g.itemName(hovered), hovered.id()), ui.mx, ui.my);
            }
            if (filtered.isEmpty()) {
                g.text("No items match.", x + 4, top + 6, th.color("text_muted"), 0);
            }
        }

        private int amount() {
            if (!withCount) {
                return Math.max(1, current.count());
            }
            try {
                return Math.max(1, Math.min(99999, Integer.parseInt(count.text().trim())));
            } catch (NumberFormatException e) {
                return 1;
            }
        }

        private void refilter(Gfx g) {
            String f = search.text().trim().toLowerCase(Locale.ROOT);
            if (f.equals(lastFilter)) {
                return;
            }
            lastFilter = f;
            filtered = new ArrayList<>();
            for (String id : ClientEnv.Holder.get().itemIds()) {
                if (id.equals("minecraft:air")) {
                    continue;
                }
                if (f.isEmpty() || id.contains(f) || name(g, id).toLowerCase(Locale.ROOT).contains(f)) {
                    filtered.add(id);
                }
            }
            scroll = 0;
        }

        private static String name(Gfx g, String id) {
            return NAME_CACHE.computeIfAbsent(id, k -> g.itemName(ItemSpec.of(k)));
        }

        @Override
        boolean key(BookUi b, Gfx g, int key, int mods) {
            if (key == Keys.ESCAPE) {
                return false;
            }
            if ((key == Keys.ENTER || key == Keys.KP_ENTER) && !count.focused) {
                if (tagMode && !tag.text().trim().isEmpty()) {
                    b.closeModal();
                    choose.accept(new ItemSpec(tag.text().trim().replace("#", ""), amount(), true, null, false));
                    return true;
                }
                if (!tagMode && !filtered.isEmpty()) {
                    b.closeModal();
                    choose.accept(new ItemSpec(filtered.get(0), amount(), false, current.data(), current.matchData()));
                    return true;
                }
            }
            TextEditor ed = count.focused ? count : active();
            return ed.keyPressed(key, mods, g);
        }

        @Override
        boolean chr(char c) {
            TextEditor ed = count.focused ? count : active();
            return ed.charTyped(c);
        }
    }

    // ---------------------------------------------------------------

    /** Team members, invitations and actions. */
    public static final class TeamPanel extends Modal {
        public TeamPanel() {
            super("Team", 270, 230);
        }

        @Override
        void content(BookUi b, Ui ui, int x, int y, int w, int h) {
            Gfx g = ui.g;
            Theme th = ui.th;
            JsonObject team = b.st.team;
            boolean solo = Json.bool(team, "solo", true);
            boolean owner = Json.str(team, "owner", "").equals(b.st.self.toString());
            y = ui.paragraph(solo ? "Solo" : "Team: " + Json.str(team, "name", "?"), x, y, w, th.color("text"), Gfx.BOLD) + 3;
            for (JsonElement e : Json.arr(team, "members")) {
                JsonObject m = e.getAsJsonObject();
                String name = Json.str(m, "name", "?");
                boolean isOwner = Json.str(m, "uuid", "").equals(Json.str(team, "owner", ""));
                g.text(ui.fit(name + (isOwner && !solo ? " (owner)" : ""), w - 40, 0), x + 4, y + 2, th.color("text"), 0);
                if (owner && !solo && !isOwner) {
                    ui.button("Kick", x + w - 36, y - 1, 36, 13, true, () -> b.st.teamAction("kick", name));
                }
                y += 14;
            }
            y += 6;
            if (solo) {
                ui.button("Create team...", x, y, w, 16, true, () -> b.open(new Prompt("Team name", "", false, n -> b.st.teamAction("create", n))));
                y += 22;
            } else {
                if (owner) {
                    ui.button("Invite player...", x, y, w, 16, true, () -> b.open(new Prompt("Player name", "", false, n -> b.st.teamAction("invite", n))));
                    y += 20;
                    ui.button("Disband team", x, y, w, 16, true, () -> b.open(new Confirm("Disband the team?", "",
                            () -> b.st.teamAction("disband", ""))));
                    y += 20;
                }
                ui.button("Leave team", x, y, w, 16, true, () -> b.open(new Confirm("Leave the team?", "",
                        () -> b.st.teamAction("leave", ""))));
                y += 22;
            }
            List<JsonObject> invites = new ArrayList<>();
            for (JsonElement e : Json.arr(team, "invites")) {
                invites.add(e.getAsJsonObject());
            }
            if (!invites.isEmpty() && solo) {
                g.text("Invitations", x, y, th.color("accent"), Gfx.BOLD);
                y += 13;
                for (JsonObject inv : invites) {
                    String id = Json.str(inv, "id", "");
                    String name = Json.str(inv, "name", "?");
                    g.text(ui.fit(name, w - 50, 0), x + 4, y + 3, th.color("text"), 0);
                    ui.button("Join", x + w - 44, y, 44, 14, true, () -> b.st.teamAction("join", id));
                    y += 17;
                }
            }
        }
    }

    // ---------------------------------------------------------------

    public static final class ThemePick extends Modal {
        public ThemePick() {
            super("Themes", 250, 190);
        }

        @Override
        void content(BookUi b, Ui ui, int x, int y, int w, int h) {
            Gfx g = ui.g;
            Theme th = ui.th;
            for (Theme t : ThemeManager.all()) {
                boolean sel = t.id.equals(ThemeManager.selectedId());
                boolean over = ui.hover(x, y, w, 24);
                th.draw(g, sel ? "tab_selected" : (over ? "button_hover" : "tab"), x, y, w, 24);
                g.text(ui.fit(t.name, w - 10, 0), x + 5, y + 3, th.color(sel ? "title" : "text"), 0);
                g.text(ui.fit(t.author.isEmpty() ? t.id : "by " + t.author, w - 10, 0), x + 5, y + 13, th.color("text_muted"), 0);
                final String id = t.id;
                ui.zone(x, y, w, 24, bt -> {
                    ThemeManager.select(id);
                    b.st.saveSettings();
                }, null);
                y += 27;
            }
            y += 4;
            ui.toggle("HUD tracker", x, y, w, 15, b.st.hud, () -> {
                b.st.hud = !b.st.hud;
                b.st.saveSettings();
            });
            y += 19;
        }
    }

    // ---------------------------------------------------------------

    public static final class ChapterEdit extends Modal {
        private final String chapterId;
        private final TextEditor name = new TextEditor(false);
        private final TextEditor group = new TextEditor(false);

        public ChapterEdit(String chapterId) {
            super("Edit chapter", 250, 190);
            this.chapterId = chapterId;
        }

        private void send(BookUi b, java.util.function.Consumer<Chapter> change) {
            Chapter c = b.st.book.chapter(chapterId);
            if (c == null) {
                return;
            }
            Chapter copy = new Chapter(c.id);
            copy.title = c.title;
            copy.icon = c.icon;
            copy.group = c.group;
            change.accept(copy);
            b.st.edit(EditOps.putChapter(copy, null));
        }

        @Override
        void content(BookUi b, Ui ui, int x, int y, int w, int h) {
            Gfx g = ui.g;
            Theme th = ui.th;
            Chapter c = b.st.book.chapter(chapterId);
            if (c == null) {
                b.closeModal();
                return;
            }
            if (!name.focused && !name.text().equals(c.title)) {
                name.set(c.title);
            }
            if (!group.focused && !group.text().equals(c.group)) {
                group.set(c.group);
            }
            name.onCommit = v -> {
                if (!v.isBlank() && !v.equals(c.title)) {
                    send(b, copy -> copy.title = v.trim());
                }
            };
            group.onCommit = v -> {
                if (!v.equals(c.group)) {
                    send(b, copy -> copy.group = v.trim());
                }
            };
            g.text("Title", x, y, th.color("text_muted"), 0);
            th.draw(g, "slot", x, y + 10, w - 26, 16);
            name.draw(g, x, y + 10, w - 26, 16, th.color("text"), th.color("text_muted"), ui.now);
            ui.zone(x, y + 10, w - 26, 16, bt -> {
                group.blur();
                name.focus();
                name.click(g, ui.mx - x, 0);
            }, null);
            ui.slot(x + w - 22, y + 8, 20);
            g.item(c.icon, x + w - 20, y + 10, 16);
            ui.zone(x + w - 22, y + 8, 20, 20, bt -> b.open(new ItemPick(c.icon, false, spec -> send(b, copy -> copy.icon = spec.withCount(1)))), null);
            y += 32;
            g.text(ui.fit("Group", w, 0), x, y, th.color("text_muted"), 0);
            th.draw(g, "slot", x, y + 10, w, 16);
            group.draw(g, x, y + 10, w, 16, th.color("text"), th.color("text_muted"), ui.now);
            ui.zone(x, y + 10, w, 16, bt -> {
                name.blur();
                group.focus();
                group.click(g, ui.mx - x, 0);
            }, null);
            y += 34;
            int idx = b.st.book.chapters.indexOf(c);
            ui.button("Move up", x, y, w / 2 - 2, 16, idx > 0, () -> b.st.edit(EditOps.moveChapter(chapterId, idx - 1)));
            ui.button("Move down", x + w / 2 + 2, y, w / 2 - 2, 16, idx < b.st.book.chapters.size() - 1,
                    () -> b.st.edit(EditOps.moveChapter(chapterId, idx + 1)));
            y += 22;
            ui.button("Delete chapter", x, y, w, 16, true, () -> b.open(new Confirm("Delete \"" + c.title + "\"?",
                    "Its " + c.quests.size() + " quests go too. Undo brings them back.", () -> {
                        b.st.edit(EditOps.removeChapter(chapterId));
                        b.chapterId = "";
                    })));
        }

        @Override
        boolean key(BookUi b, Gfx g, int key, int mods) {
            if (key == Keys.ESCAPE) {
                name.blur();
                group.blur();
                return false;
            }
            TextEditor ed = group.focused ? group : name;
            if (!name.focused && !group.focused) {
                name.focus();
            }
            return ed.keyPressed(key, mods, g);
        }

        @Override
        boolean chr(char c) {
            return (group.focused ? group : name).charTyped(c);
        }
    }
}
