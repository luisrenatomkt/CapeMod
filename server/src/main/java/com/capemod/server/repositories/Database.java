package com.capemod.server.repositories;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Acesso JDBC (SQLite por padrao). Uma conexao curta por operacao. */
public final class Database {
    private static final String[] SCHEMA = {
        "CREATE TABLE IF NOT EXISTS users ("
            + "id TEXT PRIMARY KEY, username TEXT NOT NULL, username_lower TEXT NOT NULL UNIQUE, "
            + "password_hash TEXT NOT NULL, created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS players ("
            + "id TEXT PRIMARY KEY, user_id TEXT NOT NULL UNIQUE REFERENCES users(id), active_cape_id TEXT)",
        "CREATE TABLE IF NOT EXISTS capes ("
            + "id TEXT PRIMARY KEY, owner_id TEXT NOT NULL REFERENCES users(id), sha256 TEXT NOT NULL, "
            + "width INTEGER NOT NULL, height INTEGER NOT NULL, created_at INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS idx_capes_owner ON capes(owner_id)",
        "CREATE INDEX IF NOT EXISTS idx_capes_sha ON capes(sha256)",
        "CREATE TABLE IF NOT EXISTS tokens ("
            + "token_hash TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id), expires_at INTEGER NOT NULL)"
    };

    private final String url;

    public Database(String path) {
        File parent = new File(path).getAbsoluteFile().getParentFile();
        if (parent != null) parent.mkdirs();
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Driver SQLite ausente no classpath.", e);
        }
        this.url = "jdbc:sqlite:" + path;
        init();
    }

    public Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("PRAGMA busy_timeout=5000");
        }
        return c;
    }

    private void init() {
        try (Connection c = open(); Statement s = c.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            for (String sql : SCHEMA) s.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao criar o esquema do banco.", e);
        }
    }

    static boolean isUniqueViolation(SQLException e) {
        String m = e.getMessage();
        return m != null && (m.contains("UNIQUE") || m.contains("PRIMARY KEY"));
    }
}
