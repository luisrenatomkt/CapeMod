package com.capemod.server.models;

/** Identidade de jogador do CapeMod: identificador proprio (UUID) ligado a uma conta autenticada. */
public final class Player {
    public final String id;
    public final String userId;
    public final String activeCapeId; // pode ser null

    public Player(String id, String userId, String activeCapeId) {
        this.id = id; this.userId = userId; this.activeCapeId = activeCapeId;
    }
}
