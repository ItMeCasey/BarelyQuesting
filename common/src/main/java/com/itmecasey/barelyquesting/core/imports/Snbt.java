package com.itmecasey.barelyquesting.core.imports;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A small reader for SNBT, the text format FTB Quests uses: {@code {key: value, ...}}, {@code [a, b]}, quoted strings, numbers with a
 * type suffix ({@code 1b}, {@code 2L}, {@code 3.5d}), {@code true}/{@code false}, and typed arrays ({@code [I; 1, 2]}). Commas and newlines
 * both separate entries. The result is plain JSON; number suffixes are dropped.
 */
public final class Snbt {
    private final String s;
    private int i;

    private Snbt(String s) {
        this.s = s;
    }

    public static JsonElement parse(String text) {
        Snbt p = new Snbt(text);
        p.ws();
        JsonElement e = p.value();
        p.ws();
        return e;
    }

    private IllegalArgumentException error(String what) {
        int line = 1;
        for (int k = 0; k < Math.min(i, s.length()); k++) {
            if (s.charAt(k) == '\n') {
                line++;
            }
        }
        return new IllegalArgumentException("SNBT: " + what + " at line " + line);
    }

    private void ws() {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == ',') {
                i++;
            } else if (c == '#' ) {
                while (i < s.length() && s.charAt(i) != '\n') {
                    i++;
                }
            } else {
                break;
            }
        }
    }

    private JsonElement value() {
        if (i >= s.length()) {
            throw error("unexpected end");
        }
        char c = s.charAt(i);
        if (c == '{') {
            return compound();
        }
        if (c == '[') {
            return list();
        }
        if (c == '"' || c == '\'') {
            return new JsonPrimitive(quoted());
        }
        return scalar();
    }

    private JsonObject compound() {
        JsonObject o = new JsonObject();
        i++; // {
        ws();
        while (i < s.length() && s.charAt(i) != '}') {
            String key;
            char c = s.charAt(i);
            if (c == '"' || c == '\'') {
                key = quoted();
            } else {
                int start = i;
                while (i < s.length() && s.charAt(i) != ':' && !Character.isWhitespace(s.charAt(i))) {
                    i++;
                }
                key = s.substring(start, i);
            }
            ws();
            if (i >= s.length() || s.charAt(i) != ':') {
                throw error("expected ':' after " + key);
            }
            i++;
            ws();
            o.add(key, value());
            ws();
        }
        if (i >= s.length()) {
            throw error("missing '}'");
        }
        i++; // }
        return o;
    }

    private JsonArray list() {
        JsonArray a = new JsonArray();
        i++; // [
        ws();
        // typed array prefix: [I; ...] [B; ...] [L; ...]
        if (i + 1 < s.length() && "IBL".indexOf(s.charAt(i)) >= 0 && s.charAt(i + 1) == ';') {
            i += 2;
            ws();
        }
        while (i < s.length() && s.charAt(i) != ']') {
            a.add(value());
            ws();
        }
        if (i >= s.length()) {
            throw error("missing ']'");
        }
        i++; // ]
        return a;
    }

    private String quoted() {
        char q = s.charAt(i++);
        StringBuilder sb = new StringBuilder();
        while (i < s.length() && s.charAt(i) != q) {
            char c = s.charAt(i++);
            if (c == '\\' && i < s.length()) {
                char n = s.charAt(i++);
                switch (n) {
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (i + 4 <= s.length()) {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        break;
                    default:
                        sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        if (i >= s.length()) {
            throw error("unterminated string");
        }
        i++;
        return sb.toString();
    }

    private JsonElement scalar() {
        int start = i;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || c == ',' || c == '}' || c == ']' || c == ':') {
                break;
            }
            i++;
        }
        String w = s.substring(start, i);
        if (w.isEmpty()) {
            throw error("unexpected character '" + s.charAt(i) + "'");
        }
        if (w.equals("true")) {
            return new JsonPrimitive(true);
        }
        if (w.equals("false")) {
            return new JsonPrimitive(false);
        }
        if (w.equals("null")) {
            return JsonNull.INSTANCE;
        }
        String num = w;
        char last = Character.toLowerCase(w.charAt(w.length() - 1));
        if (w.length() > 1 && "bslfd".indexOf(last) >= 0) {
            num = w.substring(0, w.length() - 1);
        }
        try {
            if (num.contains(".") || num.contains("e") || num.contains("E")) {
                return new JsonPrimitive(Double.parseDouble(num));
            }
            return new JsonPrimitive(Long.parseLong(num));
        } catch (NumberFormatException e) {
            return new JsonPrimitive(w);
        }
    }
}
