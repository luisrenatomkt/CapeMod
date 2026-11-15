package com.capemod.server.controllers;

import com.capemod.server.security.RateLimiter;
import com.capemod.server.services.ApiException;
import com.capemod.server.services.AuthService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Roteador unico: valida caminho/metodo, aplica limites de requisicao e autenticacao. */
public final class Router implements HttpHandler {
    private static final Logger LOG = Logger.getLogger(Router.class.getName());
    private static final long WINDOW_MS = 60000L;

    private final AuthService authService;
    private final AuthController auth;
    private final CapeController capes;
    private final PlayerController players;
    private final TextureController textures;
    private final RateLimiter limiter;
    private final int authPerMinute, uploadPerMinute;

    public Router(AuthService authService, AuthController auth, CapeController capes, PlayerController players,
                  TextureController textures, RateLimiter limiter, int authPerMinute, int uploadPerMinute) {
        this.authService = authService; this.auth = auth; this.capes = capes; this.players = players;
        this.textures = textures; this.limiter = limiter;
        this.authPerMinute = authPerMinute; this.uploadPerMinute = uploadPerMinute;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            dispatch(ex);
        } catch (ApiException e) {
            Http.error(ex, e.status, e.getMessage());
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Erro interno", t); // detalhes so no log, nunca na resposta
            try { Http.error(ex, 500, "Erro interno do servidor."); } catch (Throwable ignored) { }
        } finally {
            ex.close();
        }
    }

    private void limit(String key, int max) {
        if (!limiter.allow(key, max, WINDOW_MS)) throw new ApiException(429, "Muitas requisições. Tente novamente em instantes.");
    }

    private static void method(HttpExchange ex, String expected) {
        if (!expected.equals(ex.getRequestMethod())) throw new ApiException(405, "Método não permitido.");
    }

    private void dispatch(HttpExchange ex) throws IOException {
        String ip = Http.clientIp(ex);
        limit("global:" + ip, 300);

        String raw = ex.getRequestURI().getRawPath();
        if (raw == null || raw.indexOf('%') >= 0 || raw.contains("..") || raw.contains("\\")) {
            throw new ApiException(400, "Caminho inválido.");
        }
        List<String> seg = new ArrayList<String>();
        for (String s : raw.split("/")) if (!s.isEmpty()) seg.add(s);
        if (seg.size() < 2 || !"api".equals(seg.get(0))) throw new ApiException(404, "Não encontrado.");
        String m = ex.getRequestMethod();
        String area = seg.get(1);

        if ("health".equals(area) && seg.size() == 2) {
            method(ex, "GET");
            Http.json(ex, 200, Json.obj("status", "ok"));
        } else if ("auth".equals(area) && seg.size() == 3) {
            method(ex, "POST");
            limit("auth:" + ip, authPerMinute);
            if ("register".equals(seg.get(2))) auth.register(ex);
            else if ("login".equals(seg.get(2))) auth.login(ex);
            else throw new ApiException(404, "Não encontrado.");
        } else if ("capes".equals(area) && seg.size() == 3) {
            String x = seg.get(2);
            if ("upload".equals(x)) {
                method(ex, "POST");
                limit("upload:" + ip, uploadPerMinute);
                capes.upload(ex, authService.authenticate(ex.getRequestHeaders().getFirst("Authorization")));
            } else if ("GET".equals(m)) {
                capes.list(ex, authService.authenticate(ex.getRequestHeaders().getFirst("Authorization")), x);
            } else if ("DELETE".equals(m)) {
                capes.delete(ex, authService.authenticate(ex.getRequestHeaders().getFirst("Authorization")), x);
            } else {
                throw new ApiException(405, "Método não permitido.");
            }
        } else if ("capes".equals(area) && seg.size() == 4 && "active".equals(seg.get(3))) {
            method(ex, "PUT");
            capes.setActive(ex, authService.authenticate(ex.getRequestHeaders().getFirst("Authorization")), seg.get(2));
        } else if ("players".equals(area) && seg.size() == 4 && "cape".equals(seg.get(3))) {
            method(ex, "GET");
            players.activeCape(ex, seg.get(2));
        } else if ("textures".equals(area) && seg.size() == 3) {
            method(ex, "GET");
            textures.get(ex, seg.get(2));
        } else {
            throw new ApiException(404, "Não encontrado.");
        }
    }
}
