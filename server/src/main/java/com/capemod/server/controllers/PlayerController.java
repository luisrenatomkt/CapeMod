package com.capemod.server.controllers;

import com.capemod.server.models.Cape;
import com.capemod.server.services.CapeService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/** GET /api/players/{playerId}/cape  (publico: usado pelo mod ao ver outros jogadores) */
public final class PlayerController {
    private final CapeService capes;

    public PlayerController(CapeService capes) { this.capes = capes; }

    public void activeCape(HttpExchange ex, String playerId) throws IOException {
        Cape c = capes.publicActiveCape(playerId);
        Http.json(ex, 200, Json.obj("playerId", playerId, "capeId", c.id, "sha256", c.sha256,
                "width", c.width, "height", c.height, "textureUrl", "/api/textures/" + c.sha256 + ".png"));
    }
}
