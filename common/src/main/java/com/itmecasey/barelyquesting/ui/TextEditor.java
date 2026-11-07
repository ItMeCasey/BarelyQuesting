package com.itmecasey.barelyquesting.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * A text box for single or multi line text with a caret, optional selection of everything, clipboard and basic navigation. Keeps no
 * layout of its own except what is needed to place the caret from a click; the owner draws it with {@link #draw}.
 */
public final class TextEditor {
    private final StringBuilder text = new StringBuilder();
    private final boolean multiline;
    private int caret;
    private boolean selectAll;
    private int scrollLine;
    public boolean focused;
    public String placeholder = "";
    public int maxLength = 4000;
    public boolean numeric;
    /** Called with the new text after every change. */
    public java.util.function.Consumer<String> onChange;
    /** Called when Enter commits (single line) or focus is lost. */
    public java.util.function.Consumer<String> onCommit;

    public TextEditor(boolean multiline) {
        this.multiline = multiline;
    }

    public String text() {
        return text.toString();
    }

    public void set(String s) {
        text.setLength(0);
        text.append(s == null ? "" : s);
        caret = text.length();
        selectAll = false;
    }

    public boolean isMultiline() {
        return multiline;
    }

    public void focus() {
        focused = true;
    }

    public void blur() {
        if (focused) {
            focused = false;
            selectAll = false;
            if (onCommit != null) {
                onCommit.accept(text());
            }
        }
    }

    private void changed() {
        if (onChange != null) {
            onChange.accept(text());
        }
    }

    public void insert(String s) {
        if (numeric) {
            StringBuilder f = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (Character.isDigit(c) || c == '-' || c == '.') {
                    f.append(c);
                }
            }
            s = f.toString();
        }
        if (!multiline) {
            s = s.replace("\n", " ").replace("\r", "");
        }
        if (s.isEmpty()) {
            return;
        }
        if (selectAll) {
            text.setLength(0);
            caret = 0;
            selectAll = false;
        }
        if (text.length() + s.length() > maxLength) {
            s = s.substring(0, Math.max(0, maxLength - text.length()));
        }
        text.insert(caret, s);
        caret += s.length();
        changed();
    }

    public boolean charTyped(char c) {
        if (!focused || c < 32 || c == 127) {
            return false;
        }
        insert(String.valueOf(c));
        return true;
    }

    /** Returns true if the key was used. */
    public boolean keyPressed(int key, int mods, Gfx g) {
        if (!focused) {
            return false;
        }
        boolean ctrl = (mods & Keys.MOD_CTRL) != 0;
        if (ctrl && key == Keys.A) {
            selectAll = true;
            caret = text.length();
            return true;
        }
        if (ctrl && key == Keys.C) {
            g.setClipboard(selectAll ? text() : text());
            return true;
        }
        if (ctrl && key == Keys.X) {
            g.setClipboard(text());
            text.setLength(0);
            caret = 0;
            selectAll = false;
            changed();
            return true;
        }
        if (ctrl && key == Keys.V) {
            insert(g.clipboard());
            return true;
        }
        switch (key) {
            case Keys.BACKSPACE:
                if (selectAll) {
                    text.setLength(0);
                    caret = 0;
                    selectAll = false;
                    changed();
                } else if (caret > 0) {
                    text.deleteCharAt(caret - 1);
                    caret--;
                    changed();
                }
                return true;
            case Keys.DELETE:
                if (selectAll) {
                    text.setLength(0);
                    caret = 0;
                    selectAll = false;
                    changed();
                } else if (caret < text.length()) {
                    text.deleteCharAt(caret);
                    changed();
                }
                return true;
            case Keys.LEFT:
                selectAll = false;
                caret = Math.max(0, caret - 1);
                return true;
            case Keys.RIGHT:
                selectAll = false;
                caret = Math.min(text.length(), caret + 1);
                return true;
            case Keys.HOME:
                selectAll = false;
                caret = multiline ? lineStart(caret) : 0;
                return true;
            case Keys.END:
                selectAll = false;
                caret = multiline ? lineEnd(caret) : text.length();
                return true;
            case Keys.UP:
            case Keys.DOWN:
                if (multiline) {
                    moveVertical(key == Keys.UP ? -1 : 1);
                }
                return multiline;
            case Keys.ENTER:
            case Keys.KP_ENTER:
                if (multiline) {
                    insert("\n");
                } else {
                    blur();
                }
                return true;
            case Keys.ESCAPE:
                blur();
                return true;
            case Keys.TAB:
                return false;
            default:
                return true; // swallow other keys while typing so shortcuts do not fire
        }
    }

    private int lineStart(int pos) {
        int i = text.lastIndexOf("\n", Math.max(0, pos - 1));
        return pos == 0 ? 0 : i < 0 ? 0 : (i == pos - 1 && pos > 0 && text.charAt(pos - 1) == '\n' ? pos : i + 1);
    }

    private int lineEnd(int pos) {
        int i = text.indexOf("\n", pos);
        return i < 0 ? text.length() : i;
    }

    private void moveVertical(int dir) {
        int start = lineStart(caret);
        int col = caret - start;
        if (dir < 0) {
            if (start == 0) {
                caret = 0;
                return;
            }
            int prevEnd = start - 1;
            int prevStart = lineStart(prevEnd);
            caret = Math.min(prevStart + col, prevEnd);
        } else {
            int end = lineEnd(caret);
            if (end >= text.length()) {
                caret = text.length();
                return;
            }
            int nextStart = end + 1;
            int nextEnd = lineEnd(nextStart);
            caret = Math.min(nextStart + col, nextEnd);
        }
    }

    /** Draws the box contents inside the given area (the owner draws the frame) and keeps the caret visible. */
    public void draw(Gfx g, int x, int y, int w, int h, int color, int mutedColor, long millis) {
        g.clipPush(x, y, w, h);
        int lh = g.lineHeight() + 1;
        String all = text();
        if (all.isEmpty() && !focused) {
            g.text(placeholder, x + 3, y + 3, mutedColor, 0);
        }
        if (!multiline) {
            int cx = g.textWidth(all.substring(0, Math.min(caret, all.length())), 0);
            int shift = Math.max(0, cx - (w - 8));
            if (selectAll && focused) {
                g.fill(x + 2, y + 2, Math.min(w - 4, g.textWidth(all, 0) + 2), lh, 0x803A6FD8);
            }
            g.text(all, x + 3 - shift, y + 3, color, 0);
            if (focused && (millis / 500) % 2 == 0) {
                g.fill(x + 3 + cx - shift, y + 2, 1, lh, color);
            }
        } else {
            String[] lines = all.split("\n", -1);
            int caretLine = 0;
            int pos = 0;
            int caretCol = 0;
            for (int i = 0; i < lines.length; i++) {
                if (caret >= pos && caret <= pos + lines[i].length()) {
                    caretLine = i;
                    caretCol = caret - pos;
                    break;
                }
                pos += lines[i].length() + 1;
            }
            int visible = Math.max(1, (h - 4) / lh);
            if (caretLine < scrollLine) {
                scrollLine = caretLine;
            }
            if (caretLine >= scrollLine + visible) {
                scrollLine = caretLine - visible + 1;
            }
            for (int i = 0; i < visible && scrollLine + i < lines.length; i++) {
                String line = lines[scrollLine + i];
                if (selectAll && focused) {
                    g.fill(x + 2, y + 2 + i * lh, Math.min(w - 4, g.textWidth(line, 0) + 2), lh, 0x803A6FD8);
                }
                g.text(line, x + 3, y + 3 + i * lh, color, 0);
            }
            if (focused && (millis / 500) % 2 == 0 && caretLine >= scrollLine && caretLine < scrollLine + visible) {
                int cx = g.textWidth(lines[caretLine].substring(0, Math.min(caretCol, lines[caretLine].length())), 0);
                g.fill(x + 3 + cx, y + 2 + (caretLine - scrollLine) * lh, 1, lh, color);
            }
        }
        g.clipPop();
    }

    /** Puts the caret where a click landed (single line: by x; multi line: by x and y). */
    public void click(Gfx g, int relX, int relY) {
        selectAll = false;
        String all = text();
        if (!multiline) {
            int best = all.length();
            for (int i = 0; i <= all.length(); i++) {
                if (g.textWidth(all.substring(0, i), 0) + 3 >= relX) {
                    best = i;
                    break;
                }
            }
            caret = best;
            return;
        }
        int lh = g.lineHeight() + 1;
        int line = Math.max(0, (relY - 3) / lh + scrollLine);
        String[] lines = all.split("\n", -1);
        line = Math.min(line, lines.length - 1);
        int pos = 0;
        for (int i = 0; i < line; i++) {
            pos += lines[i].length() + 1;
        }
        String l = lines[line];
        int col = l.length();
        for (int i = 0; i <= l.length(); i++) {
            if (g.textWidth(l.substring(0, i), 0) + 3 >= relX) {
                col = i;
                break;
            }
        }
        caret = pos + col;
    }

    public void scroll(int lines) {
        if (multiline) {
            scrollLine = Math.max(0, scrollLine + lines);
        }
    }

    /** All lines of text, for tests. */
    public List<String> lines() {
        List<String> out = new ArrayList<>();
        for (String s : text().split("\n", -1)) {
            out.add(s);
        }
        return out;
    }
}
