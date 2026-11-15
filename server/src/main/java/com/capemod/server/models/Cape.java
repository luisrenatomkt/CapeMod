package com.capemod.server.models;

public final class Cape {
    public final String id;
    public final String ownerId;
    public final String sha256;
    public final int width;
    public final int height;
    public final long createdAt;

    public Cape(String id, String ownerId, String sha256, int width, int height, long createdAt) {
        this.id = id; this.ownerId = ownerId; this.sha256 = sha256;
        this.width = width; this.height = height; this.createdAt = createdAt;
    }
}
