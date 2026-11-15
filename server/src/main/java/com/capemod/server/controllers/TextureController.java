package com.capemod.server.controllers;

import com.capemod.server.services.ApiException;
import com.capemod.server.services.CapeService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/** GET /api/textures/{sha256}.png  (publico; conteudo imutavel, cache longo) */
public final class TextureController {
    private final CapeService capes;

    public TextureController(CapeService capes) { this.capes = capes; }

    public void get(HttpExchange ex, String file) throws IOException {
        if (!file.endsWith(".png")) throw new ApiException(404, "Não encontrado.");
        String sha = file.substring(0, file.length() - 4);
        byte[] data = capes.texture(sha);
        String etag = "\"" + sha + "\"";
        ex.getResponseHeaders().set("ETag", etag);
        ex.getResponseHeaders().set("Cache-Control", "public, max-age=31536000, immutable");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        if (etag.equals(ex.getRequestHeaders().getFirst("If-None-Match"))) {
            ex.sendResponseHeaders(304, -1);
            return;
        }
        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.sendResponseHeaders(200, data.length);
        OutputStream os = ex.getResponseBody();
        os.write(data);
        os.close();
    }
}
