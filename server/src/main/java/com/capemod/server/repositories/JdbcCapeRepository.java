package com.capemod.server.repositories;

import com.capemod.server.models.Cape;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcCapeRepository implements CapeRepository {
    private static final String COLS = "id, owner_id, sha256, width, height, created_at";
    private final Database db;

    public JdbcCapeRepository(Database db) { this.db = db; }

    @Override
    public void insert(Cape c) {
        String sql = "INSERT INTO capes(" + COLS + ") VALUES(?,?,?,?,?,?)";
        try (Connection con = db.open(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.id);
            ps.setString(2, c.ownerId);
            ps.setString(3, c.sha256);
            ps.setInt(4, c.width);
            ps.setInt(5, c.height);
            ps.setLong(6, c.createdAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public Cape findById(String id) {
        List<Cape> l = query("SELECT " + COLS + " FROM capes WHERE id = ?", id);
        return l.isEmpty() ? null : l.get(0);
    }

    @Override
    public List<Cape> findByOwner(String ownerId) {
        return query("SELECT " + COLS + " FROM capes WHERE owner_id = ? ORDER BY created_at, id", ownerId);
    }

    @Override
    public int countByOwner(String ownerId) { return count("SELECT COUNT(*) FROM capes WHERE owner_id = ?", ownerId); }

    @Override
    public int countBySha(String sha) { return count("SELECT COUNT(*) FROM capes WHERE sha256 = ?", sha); }

    @Override
    public boolean delete(String id) {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement("DELETE FROM capes WHERE id = ?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    private int count(String sql, String param) {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    private List<Cape> query(String sql, String param) {
        try (Connection c = db.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                List<Cape> out = new ArrayList<Cape>();
                while (rs.next()) {
                    out.add(new Cape(rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getInt(4), rs.getInt(5), rs.getLong(6)));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }
}
