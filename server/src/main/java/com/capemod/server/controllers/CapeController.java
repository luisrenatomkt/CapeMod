package com.capemod.server.controllers;

import com.capemod.server.models.Cape;
import com.capemod.server.services.ApiException;
import com.capemod.server.services.CapeService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Endpoints autenticados de capas + textura publica. */
public final class CapeController {
    static final int MAX_UPLOAD_BODY = 1024 * 1024 + 16 * 1024; // PNG de 1 MB + cabecalhos multipart
    private final CapeService capes;

    public CapeController(CapeService capes) { this.capes = capes; }

    static String capeJson(Cape c, boolean active) {
        return Json.obj("id", c.id, "sha256", c.sha256, "width", c.width, "height", c.height,
                "createdAt", c.createdAt, "textureUrl", "/api/textures/" + c.sha256 + ".png", "active", active);
    }

    /** POST /api/capes/upload */
    public void upload(HttpExchange ex, String userId) throws IOException {
        byte[] body = Http.readBody(ex, MAX_UPLOAD_BODY);
        byte[] png = Multipart.extractFile(body, ex.getRequestHeaders().getFirst("Content-Type"));
        CapeService.Uploaded u = capes.upload(userId, png);
        List<String> w = new ArrayList<String>();
        for (String s : u.warnings) w.add(Json.quote(s));
        Http.json(ex, 201, Json.obj("capeId", u.cape.id, "sha256", u.cape.sha256, "width", u.cape.width,
                "height", u.cape.height, "textureUrl", "/api/textures/" + u.cape.sha256 + ".png",
                "active", u.active, "warnings", new Json.Raw(Json.array(w))));
    }

    /** GET /api/capes/{playerId} */
    public void list(HttpExchange ex, String userId, String playerId) throws IOException {
        List<Cape> list = capes.listForPlayer(userId, playerId);
        String active = capes.activeCapeId(userId, playerId);
        List<String> items = new ArrayList<String>();
        for (Cape c : list) items.add(capeJson(c, c.id.equals(active)));
        Http.json(ex, 200, Json.obj("playerId", playerId, "capes", new Json.Raw(Json.array(items))));
    }

    /** PUT /api/capes/{playerId}/active  corpo: {"capeId":"..."} ou {"capeId":null} para remover */
    public void setActive(HttpExchange ex, String userId, String playerId) throws IOException {
        Map<String, Object> b = Http.readJson(ex);
        if (!b.containsKey("capeId")) throw new ApiException(400, "Campo 'capeId' ausente.");
        Object v = b.get("capeId");
        if (v != null && !(v instanceof String)) throw new ApiException(400, "capeId inválido.");
        capes.setActive(userId, playerId, (String) v);
        Http.json(ex, 200, Json.obj("playerId", playerId, "activeCapeId", v));
    }

    /** DELETE /api/capes/{capeId} */
    public void delete(HttpExchange ex, String userId, String capeId) throws IOException {
        capes.delete(userId, capeId);
        Http.json(ex, 200, Json.obj("deleted", capeId));
    }
}
