package com.capemod.server;

import com.capemod.server.configuration.ServerConfig;
import com.capemod.server.configuration.TlsSupport;
import com.capemod.server.controllers.AuthController;
import com.capemod.server.controllers.CapeController;
import com.capemod.server.controllers.PlayerController;
import com.capemod.server.controllers.Router;
import com.capemod.server.controllers.TextureController;
import com.capemod.server.repositories.Database;
import com.capemod.server.repositories.JdbcCapeRepository;
import com.capemod.server.repositories.JdbcPlayerRepository;
import com.capemod.server.repositories.JdbcTokenRepository;
import com.capemod.server.repositories.JdbcUserRepository;
import com.capemod.server.security.PasswordHasher;
import com.capemod.server.security.RateLimiter;
import com.capemod.server.services.AuthService;
import com.capemod.server.services.CapeService;
import com.capemod.server.storage.CapeStorage;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.LongSupplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public final class ServerMain {
    public static void main(String[] args) throws Exception {
        ServerConfig cfg = ServerConfig.fromEnv(System.getenv());
        boolean tls = !cfg.keystorePath.isEmpty();
        if (!tls && !cfg.devHttp) {
            System.err.println("ERRO: configure CAPEMOD_KEYSTORE (PKCS12) e CAPEMOD_KEYSTORE_PASSWORD para HTTPS,\n"
                    + "ou use CAPEMOD_DEV_HTTP=true (somente desenvolvimento, escuta apenas em 127.0.0.1).\n"
                    + "Atras de um proxy reverso com TLS, rode com CAPEMOD_DEV_HTTP=true e deixe o proxy na frente.");
            System.exit(2);
        }

        LongSupplier clock = new LongSupplier() {
            public long getAsLong() { return System.currentTimeMillis(); }
        };
        Database db = new Database(cfg.dbPath);
        JdbcUserRepository users = new JdbcUserRepository(db);
        JdbcPlayerRepository players = new JdbcPlayerRepository(db);
        JdbcCapeRepository capes = new JdbcCapeRepository(db);
        JdbcTokenRepository tokens = new JdbcTokenRepository(db);
        CapeStorage storage = new CapeStorage(Paths.get(cfg.storageDir));

        AuthService authService = new AuthService(users, players, tokens,
                new PasswordHasher(cfg.pbkdf2Iterations), cfg.tokenTtlMs, clock);
        CapeService capeService = new CapeService(capes, players, storage, cfg.maxCapesPerUser, clock);
        Router router = new Router(authService, new AuthController(authService), new CapeController(capeService),
                new PlayerController(capeService), new TextureController(capeService),
                new RateLimiter(clock), cfg.authPerMinute, cfg.uploadPerMinute);

        HttpServer server;
        if (tls) {
            final SSLContext ctx = TlsSupport.create(cfg.keystorePath, cfg.keystorePassword);
            HttpsServer https = HttpsServer.create(new InetSocketAddress(cfg.port), 0);
            https.setHttpsConfigurator(new HttpsConfigurator(ctx) {
                @Override
                public void configure(HttpsParameters params) {
                    SSLParameters p = ctx.getDefaultSSLParameters();
                    List<String> protos = new ArrayList<String>();
                    List<String> supported = Arrays.asList(ctx.getSupportedSSLParameters().getProtocols());
                    if (supported.contains("TLSv1.3")) protos.add("TLSv1.3");
                    protos.add("TLSv1.2");
                    p.setProtocols(protos.toArray(new String[0]));
                    params.setSSLParameters(p);
                }
            });
            server = https;
        } else {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), cfg.port), 0);
        }
        server.createContext("/", router);
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.start();
        System.out.println("CapeMod server em " + (tls ? "https" : "http (DEV, 127.0.0.1)") + "://*:" + cfg.port);
    }
}
