package com.capemod.server.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class JdbcTokenRepository implements TokenRepository {
    private final Database db;

    public JdbcTokenRepository(Database db) { this.db = db; }

    @Override
    public void insert(String tokenHash, String userId, long expiresAtMs) {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement("INSERT INTO tokens(token_hash, user_id, expires_at) VALUES(?,?,?)")) {
            ps.setString(1, tokenHash);
            ps.setString(2, userId);
            ps.setLong(3, expiresAtMs);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public String findUserId(String tokenHash, long nowMs) {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement("SELECT user_id FROM tokens WHERE token_hash = ? AND expires_at > ?")) {
            ps.setString(1, tokenHash);
            ps.setLong(2, nowMs);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void deleteExpired(long nowMs) {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement("DELETE FROM tokens WHERE expires_at <= ?")) {
            ps.setLong(1, nowMs);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }
}
