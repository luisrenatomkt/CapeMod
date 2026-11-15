package com.capemod.server.controllers;

import com.capemod.server.services.AuthService;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.Map;

/** POST /api/auth/register e POST /api/auth/login */
public final class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    public void register(HttpExchange ex) throws IOException {
        Map<String, Object> b = Http.readJson(ex);
        AuthService.Registered r = auth.register(Http.str(b, "username"), Http.str(b, "password"));
        Http.json(ex, 201, Json.obj("userId", r.userId, "playerId", r.playerId));
    }

    public void login(HttpExchange ex) throws IOException {
        Map<String, Object> b = Http.readJson(ex);
        AuthService.LoginResult r = auth.login(Http.str(b, "username"), Http.str(b, "password"));
        Http.json(ex, 200, Json.obj("token", r.token, "expiresAt", r.expiresAt, "playerId", r.playerId));
    }
}
