package com.capemod.server.models;

public final class User {
    public final String id;
    public final String username;
    public final String passwordHash;
    public final long createdAt;

    public User(String id, String username, String passwordHash, long createdAt) {
        this.id = id; this.username = username; this.passwordHash = passwordHash; this.createdAt = createdAt;
    }
}
