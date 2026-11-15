package com.capemod.server.controllers;

import com.capemod.server.services.ApiException;
import com.sun.net.httpserver.HttpExchange;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Utilitarios HTTP compartilhados pelos controllers. */
public final class Http {
    private Http() {}

    public static void json(HttpExchange ex, int status, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, b.length);
        OutputStream os = ex.getResponseBody();
        os.write(b);
        os.close();
    }

    public static void error(HttpExchange ex, int status, String message) throws IOException {
        json(ex, status, Json.obj("error", message));
    }

    /** Le o corpo respeitando um limite (413 se maior). */
    public static byte[] readBody(HttpExchange ex, int max) throws IOException {
        String cl = ex.getRequestHeaders().getFirst("Content-Length");
        if (cl != null) {
            try {
                if (Long.parseLong(cl.trim()) > max) throw new ApiException(413, "Corpo da requisição grande demais.");
            } catch (NumberFormatException e) {
                throw new ApiException(400, "Content-Length inválido.");
            }
        }
        InputStream in = ex.getRequestBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            if (out.size() > max) throw new ApiException(413, "Corpo da requisição grande demais.");
        }
        return out.toByteArray();
    }

    public static Map<String, Object> readJson(HttpExchange ex) throws IOException {
        byte[] body = readBody(ex, 4096);
        return Json.parseFlat(new String(body, StandardCharsets.UTF_8));
    }

    /** Campo string obrigatorio; 400 se ausente ou de outro tipo. */
    public static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (!(v instanceof String)) throw new ApiException(400, "Campo '" + key + "' ausente ou inválido.");
        return (String) v;
    }

    public static String clientIp(HttpExchange ex) {
        return ex.getRemoteAddress().getAddress().getHostAddress();
    }
}
