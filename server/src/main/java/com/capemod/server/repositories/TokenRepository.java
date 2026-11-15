package com.capemod.server.repositories;

public interface TokenRepository {
    /** Guarda somente o HASH do token (o token em si nunca e armazenado). */
    void insert(String tokenHash, String userId, long expiresAtMs);
    /** Retorna o id do usuario se o token existe e nao expirou; senao null. */
    String findUserId(String tokenHash, long nowMs);
    void deleteExpired(long nowMs);
}
