package com.capemod.server.repositories;

import com.capemod.server.models.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public final class JdbcUserRepository implements UserRepository {
    private final Database db;

    public JdbcUserRepository(Database db) { this.db = db; }

    @Override
    public boolean insert(User u) {
        String sql = "INSERT INTO users(id, username, username_lower, password_hash, created_at) VALUES(?,?,?,?,?)";
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.id);
            ps.setString(2, u.username);
            ps.setString(3, u.username.toLowerCase(Locale.ROOT));
            ps.setString(4, u.passwordHash);
            ps.setLong(5, u.createdAt);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (Database.isUniqueViolation(e)) return false;
            throw new RepositoryException(e);
        }
    }

    @Override
    public User findByUsername(String username) {
        return one("SELECT id, username, password_hash, created_at FROM users WHERE username_lower = ?",
                username.toLowerCase(Locale.ROOT));
    }

    @Override
    public User findById(String id) {
        return one("SELECT id, username, password_hash, created_at FROM users WHERE id = ?", id);
    }

    private User one(String sql, String param) {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new User(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4)) : null;
            }
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }
}
