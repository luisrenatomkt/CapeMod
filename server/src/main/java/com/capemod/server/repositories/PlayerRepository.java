package com.capemod.server.repositories;

import com.capemod.server.models.Player;

public interface PlayerRepository {
    void insert(Player player);
    Player findById(String playerId);
    Player findByUserId(String userId);
    void setActiveCape(String playerId, String capeIdOrNull);
    /** Limpa a capa ativa de qualquer jogador que use esta capa (usado ao remover a capa). */
    void clearActiveCape(String capeId);
}
