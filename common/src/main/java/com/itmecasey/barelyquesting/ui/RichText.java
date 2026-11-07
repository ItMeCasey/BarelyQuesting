package com.itmecasey.barelyquesting.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Quest descriptions: a small markup that is easy to type in the editor and to read in a JSON file.
 * <pre>
 * **bold**  *italic*  __underline__  ~~strike~~  &amp;c red  &amp;r reset  [label](https://link)  \n new line
 * </pre>
 * Colors use the vanilla codes {@code &0} to {@code &f}. Parsing never fails: unmatched markers are shown as typed.
 */
public final class RichText {
    /** A piece of text with one style. {@code color} 0 means the default text color. */
    public static final class Run {
        public final String text;
        public final int color;
        public final int style;
        public final String url;

        Run(String text, int color, int style, String url) {
            this.text = text;
            this.color = color;
            this.style = style;
            this.url = url;
        }
    }

    private static final int[] PALETTE = {
        0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA, 0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
        0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF, 0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF};

    private RichText() {
    }

    /** Splits the markup into styled runs (one list, with {@code "\n"} runs marking line breaks). */
    public static List<Run> parse(String src) {
        List<Run> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int color = 0;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strike = false;
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (c == '\\' && i + 1 < n && src.charAt(i + 1) == 'n') {
                flush(out, cur, color, style(bold, italic, underline, strike), null);
                out.add(new Run("\n", 0, 0, null));
                i += 2;
            } else if (c == '\n') {
                flush(out, cur, color, style(bold, italic, underline, strike), null);
                out.add(new Run("\n", 0, 0, null));
                i++;
            } else if (src.startsWith("**", i) && src.indexOf("**", i + 2) > 0 || (bold && src.startsWith("**", i))) {
                flush(out, cur, color, style(bold, italic, underline, strike), null);
                bold = !bold;
                i += 2;
            } else if (src.startsWith("__", i) && (underline || src.indexOf("__", i + 2) > 0)) {
                flush(out, cur, color, style(bold, italic, underline, strike), null);
                underline = !underline;
                i += 2;
            } else if (src.startsWith("~~", i) && (strike || src.indexOf("~~", i + 2) > 0)) {
                flush(out, cur, color, style(bold, italic, underline, strike), null);
                strike = !strike;
                i += 2;
            } else if (c == '*' && (italic || src.indexOf('*', i + 1) > 0)) {
                flush(out, cur, color, style(bold, italic, underline, strike), null);
                italic = !italic;
                i++;
            } else if (c == '&' && i + 1 < n && Character.digit(src.charAt(i + 1), 16) >= 0) {
                flush(out, cur, color, style(bold, italic, underline, strike), null);
                color = PALETTE[Character.digit(src.charAt(i + 1), 16)];
                i += 2;
            } else if (c == '&' && i + 1 < n && src.charAt(i + 1) == 'r') {
                flush(out, cur, color, style(bold, italic, underline, strike), null);
                color = 0;
                bold = italic = underline = strike = false;
                i += 2;
            } else if (c == '[') {
                int close = src.indexOf("](", i);
                int end = close < 0 ? -1 : src.indexOf(')', close);
                if (close > i && end > close) {
                    flush(out, cur, color, style(bold, italic, underline, strike), null);
                    out.add(new Run(src.substring(i + 1, close), 0xFF6FA8FF, style(bold, italic, true, strike), src.substring(close + 2, end)));
                    i = end + 1;
                } else {
                    cur.append(c);
                    i++;
                }
            } else {
                cur.append(c);
                i++;
            }
        }
        flush(out, cur, color, style(bold, italic, underline, strike), null);
        return out;
    }

    private static int style(boolean bold, boolean italic, boolean underline, boolean strike) {
        return (bold ? Gfx.BOLD : 0) | (italic ? Gfx.ITALIC : 0) | (underline ? Gfx.UNDERLINE : 0) | (strike ? Gfx.STRIKE : 0);
    }

    private static void flush(List<Run> out, StringBuilder cur, int color, int style, String url) {
        if (cur.length() > 0) {
            out.add(new Run(cur.toString(), color, style, url));
            cur.setLength(0);
        }
    }

    /** Text with all markup removed (for tooltips and the HUD). */
    public static String plain(String src) {
        StringBuilder sb = new StringBuilder();
        for (Run r : parse(src)) {
            sb.append(r.text.equals("\n") ? " " : r.text);
        }
        return sb.toString();
    }

    /** A positioned piece of a laid-out line. */
    public static final class Piece {
        public final Run run;
        public final String text;
        public final int x;

        Piece(Run run, String text, int x) {
            this.run = run;
            this.text = text;
            this.x = x;
        }
    }

    /** Wraps runs to a width; each inner list is one line of pieces with x offsets. */
    public static List<List<Piece>> layout(Gfx g, List<Run> runs, int maxWidth) {
        List<List<Piece>> lines = new ArrayList<>();
        List<Piece> line = new ArrayList<>();
        int x = 0;
        for (Run run : runs) {
            if (run.text.equals("\n")) {
                lines.add(line);
                line = new ArrayList<>();
                x = 0;
                continue;
            }
            // Split into words, keeping the spaces with the word that follows them.
            String[] words = run.text.split("(?<= )");
            for (String word : words) {
                int w = g.textWidth(word, run.style);
                if (x > 0 && x + g.textWidth(word.stripTrailing(), run.style) > maxWidth) {
                    lines.add(line);
                    line = new ArrayList<>();
                    x = 0;
                    word = word.stripLeading();
                    w = g.textWidth(word, run.style);
                }
                while (w > maxWidth && word.length() > 1) {
                    // A single word wider than the box: break it by characters.
                    int cut = word.length();
                    while (cut > 1 && g.textWidth(word.substring(0, cut), run.style) > maxWidth - x) {
                        cut--;
                    }
                    line.add(new Piece(run, word.substring(0, cut), x));
                    lines.add(line);
                    line = new ArrayList<>();
                    x = 0;
                    word = word.substring(cut);
                    w = g.textWidth(word, run.style);
                }
                line.add(new Piece(run, word, x));
                x += w;
            }
        }
        lines.add(line);
        return lines;
    }

    /** Draws a laid-out text and returns its height; the url under the mouse is returned through {@code hover[0]}. */
    public static int draw(Gfx g, List<List<Piece>> lines, int x, int y, int defaultColor, int mouseX, int mouseY, String[] hover) {
        int lh = g.lineHeight() + 2;
        int cy = y;
        for (List<Piece> line : lines) {
            for (Piece p : line) {
                int color = p.run.color == 0 ? defaultColor : p.run.color;
                g.text(p.text, x + p.x, cy, color, p.run.style);
                if (hover != null && p.run.url != null && mouseY >= cy && mouseY < cy + lh && mouseX >= x + p.x
                        && mouseX < x + p.x + g.textWidth(p.text, p.run.style)) {
                    hover[0] = p.run.url;
                }
            }
            cy += lh;
        }
        return cy - y;
    }
}
