package com.capemod.server.configuration;

import java.util.Map;

/** Configuracao 100% por variaveis de ambiente: nenhum segredo no codigo-fonte. */
public final class ServerConfig {
    public final int port;
    public final String dbPath;
    public final String storageDir;
    public final String keystorePath;      // PKCS12; vazio = sem TLS proprio
    public final String keystorePassword;
    public final boolean devHttp;          // HTTP sem TLS, somente em 127.0.0.1
    public final long tokenTtlMs;
    public final int pbkdf2Iterations;
    public final int maxCapesPerUser;
    public final int authPerMinute;
    public final int uploadPerMinute;

    private ServerConfig(Map<String, String> e) {
        port = intOf(e, "CAPEMOD_PORT", 8443, 1, 65535);
        dbPath = str(e, "CAPEMOD_DB_PATH", "data/capemod.db");
        storageDir = str(e, "CAPEMOD_STORAGE_DIR", "data/capes");
        keystorePath = str(e, "CAPEMOD_KEYSTORE", "");
        keystorePassword = str(e, "CAPEMOD_KEYSTORE_PASSWORD", "");
        devHttp = "true".equalsIgnoreCase(str(e, "CAPEMOD_DEV_HTTP", "false"));
        tokenTtlMs = intOf(e, "CAPEMOD_TOKEN_TTL_HOURS", 24, 1, 24 * 90) * 3600_000L;
        pbkdf2Iterations = intOf(e, "CAPEMOD_PBKDF2_ITERATIONS", 120000, 1000, 2000000);
        maxCapesPerUser = intOf(e, "CAPEMOD_MAX_CAPES_PER_USER", 20, 1, 1000);
        authPerMinute = intOf(e, "CAPEMOD_AUTH_PER_MINUTE", 10, 1, 10000);
        uploadPerMinute = intOf(e, "CAPEMOD_UPLOAD_PER_MINUTE", 10, 1, 10000);
    }

    public static ServerConfig fromEnv(Map<String, String> env) { return new ServerConfig(env); }

    private static String str(Map<String, String> e, String k, String d) {
        String v = e.get(k);
        return v == null ? d : v;
    }

    private static int intOf(Map<String, String> e, String k, int d, int min, int max) {
        String v = e.get(k);
        if (v == null || v.trim().isEmpty()) return d;
        try {
            int n = Integer.parseInt(v.trim());
            if (n < min || n > max) throw new IllegalArgumentException(k + " fora do intervalo " + min + ".." + max);
            return n;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(k + " deve ser um numero inteiro");
        }
    }
}
