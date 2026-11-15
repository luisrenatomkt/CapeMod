package com.capemod.server.services;

import com.capemod.server.models.Player;
import com.capemod.server.models.User;
import com.capemod.server.repositories.PlayerRepository;
import com.capemod.server.repositories.TokenRepository;
import com.capemod.server.repositories.UserRepository;
import com.capemod.server.security.InputValidator;
import com.capemod.server.security.PasswordHasher;
import com.capemod.server.security.TokenService;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class AuthService {
    public static final class Registered {
        public final String userId, playerId;
        Registered(String u, String p) { userId = u; playerId = p; }
    }

    public static final class LoginResult {
        public final String token, playerId;
        public final long expiresAt;
        LoginResult(String t, String p, long e) { token = t; playerId = p; expiresAt = e; }
    }

    private final UserRepository users;
    private final PlayerRepository players;
    private final TokenRepository tokens;
    private final PasswordHasher hasher;
    private final long ttlMs;
    private final LongSupplier clock;
    private final String dummyHash;

    public AuthService(UserRepository users, PlayerRepository players, TokenRepository tokens,
                       PasswordHasher hasher, long ttlMs, LongSupplier clock) {
        this.users = users; this.players = players; this.tokens = tokens;
        this.hasher = hasher; this.ttlMs = ttlMs; this.clock = clock;
        this.dummyHash = hasher.hash("capemod-dummy-password"); // evita revelar se o usuario existe pelo tempo de resposta
    }

    public Registered register(String username, String password) {
        if (!InputValidator.isUsername(username)) {
            throw new ApiException(400, "Usuário inválido: use 3 a 16 letras, números ou _.");
        }
        if (!InputValidator.isPassword(password)) {
            throw new ApiException(400, "Senha inválida: use de 8 a 128 caracteres.");
        }
        User u = new User(UUID.randomUUID().toString(), username, hasher.hash(password), clock.getAsLong());
        if (!users.insert(u)) throw new ApiException(409, "Nome de usuário já está em uso.");
        Player p = new Player(UUID.randomUUID().toString(), u.id, null);
        players.insert(p);
        return new Registered(u.id, p.id);
    }

    public LoginResult login(String username, String password) {
        User u = InputValidator.isUsername(username) ? users.findByUsername(username) : null;
        boolean passwordOk = hasher.verify(password == null ? "" : password, u != null ? u.passwordHash : dummyHash);
        if (u == null || !passwordOk) throw new ApiException(401, "Usuário ou senha incorretos.");
        long now = clock.getAsLong();
        tokens.deleteExpired(now);
        String token = TokenService.newToken();
        long expires = now + ttlMs;
        tokens.insert(TokenService.hash(token), u.id, expires);
        Player p = players.findByUserId(u.id);
        return new LoginResult(token, p == null ? null : p.id, expires);
    }

    /** Valida o cabecalho "Authorization: Bearer ..." e retorna o id do usuario. */
    public String authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ApiException(401, "Autenticação necessária.");
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty() || token.length() > 256) throw new ApiException(401, "Token inválido.");
        String userId = tokens.findUserId(TokenService.hash(token), clock.getAsLong());
        if (userId == null) throw new ApiException(401, "Token inválido ou expirado.");
        return userId;
    }
}
