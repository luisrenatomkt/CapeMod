package com.capemod.server.repositories;

import com.capemod.server.models.Player;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class JdbcPlayerRepository implements PlayerRepository {
    private final Database db;

    public JdbcPlayerRepository(Database db) { this.db = db; }

    @Override
    public void insert(Player p) {
        update("INSERT INTO players(id, user_id, active_cape_id) VALUES(?,?,?)", p.id, p.userId, p.activeCapeId);
    }

    @Override
    public Player findById(String id) {
        return one("SELECT id, user_id, active_cape_id FROM players WHERE id = ?", id);
    }

    @Override
    public Player findByUserId(String userId) {
        return one("SELECT id, user_id, active_cape_id FROM players WHERE user_id = ?", userId);
    }

    @Override
    public void setActiveCape(String playerId, String capeIdOrNull) {
        update("UPDATE players SET active_cape_id = ? WHERE id = ?", capeIdOrNull, playerId);
    }

    @Override
    public void clearActiveCape(String capeId) {
        update("UPDATE players SET active_cape_id = NULL WHERE active_cape_id = ?", capeId);
    }

    private void update(String sql, String... params) {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setString(i + 1, params[i]); // setString(null) grava NULL
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    private Player one(String sql, String param) {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Player(rs.getString(1), rs.getString(2), rs.getString(3)) : null;
            }
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }
}
