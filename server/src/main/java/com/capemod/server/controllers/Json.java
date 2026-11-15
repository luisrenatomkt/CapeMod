package com.capemod.server.controllers;

import com.capemod.server.services.ApiException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** JSON minimo sem dependencias: escrita de objetos simples e leitura de objetos "planos". */
public final class Json {
    private Json() {}

    /** Trecho JSON ja pronto (inserido sem aspas). */
    public static final class Raw {
        final String text;
        public Raw(String text) { this.text = text; }
    }

    public static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /** obj("a", 1, "b", "x") -> {"a":1,"b":"x"} */
    public static String obj(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append(quote((String) kv[i])).append(':').append(value(kv[i + 1]));
        }
        return sb.append('}').toString();
    }

    public static String array(List<String> rawItems) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rawItems.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(rawItems.get(i));
        }
        return sb.append(']').toString();
    }

    private static String value(Object v) {
        if (v == null) return "null";
        if (v instanceof Raw) return ((Raw) v).text;
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return quote(v.toString());
    }

    // ---------------- leitura ----------------

    /** Le um objeto JSON plano: valores string, numero, boolean ou null. Rejeita aninhamento. */
    public static Map<String, Object> parseFlat(String s) {
        return new Parser(s).parse();
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) { this.s = s; }

        private ApiException bad() { return new ApiException(400, "JSON inválido."); }

        private void ws() { while (i < s.length() && " \t\r\n".indexOf(s.charAt(i)) >= 0) i++; }

        private void expect(char c) {
            ws();
            if (i >= s.length() || s.charAt(i) != c) throw bad();
            i++;
        }

        Map<String, Object> parse() {
            Map<String, Object> out = new HashMap<String, Object>();
            expect('{');
            ws();
            if (i < s.length() && s.charAt(i) == '}') { i++; return end(out); }
            while (true) {
                ws();
                String key = string();
                expect(':');
                if (out.containsKey(key)) throw bad();
                out.put(key, value());
                ws();
                if (i >= s.length()) throw bad();
                char c = s.charAt(i++);
                if (c == '}') return end(out);
                if (c != ',') throw bad();
            }
        }

        private Map<String, Object> end(Map<String, Object> m) {
            ws();
            if (i != s.length()) throw bad();
            return m;
        }

        private Object value() {
            ws();
            if (i >= s.length()) throw bad();
            char c = s.charAt(i);
            if (c == '"') return string();
            if (s.startsWith("null", i)) { i += 4; return null; }
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (c == '-' || (c >= '0' && c <= '9')) {
                int st = i;
                while (i < s.length() && "+-.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
                try { return Double.valueOf(s.substring(st, i)); } catch (NumberFormatException e) { throw bad(); }
            }
            throw bad(); // objetos/arrays aninhados nao sao aceitos
        }

        private String string() {
            ws();
            if (i >= s.length() || s.charAt(i) != '"') throw bad();
            i++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (i >= s.length()) throw bad();
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c < 0x20) throw bad();
                if (c != '\\') { sb.append(c); continue; }
                if (i >= s.length()) throw bad();
                char e = s.charAt(i++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length()) throw bad();
                        try { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); }
                        catch (NumberFormatException ex) { throw bad(); }
                        i += 4;
                        break;
                    default: throw bad();
                }
            }
        }
    }
}
