package com.capemod.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.capemod.server.controllers.AuthController;
import com.capemod.server.controllers.CapeController;
import com.capemod.server.controllers.Json;
import com.capemod.server.controllers.PlayerController;
import com.capemod.server.controllers.Router;
import com.capemod.server.controllers.TextureController;
import com.capemod.server.security.PasswordHasher;
import com.capemod.server.security.RateLimiter;
import com.capemod.server.services.AuthService;
import com.capemod.server.services.CapeService;
import com.capemod.server.storage.CapeStorage;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import javax.imageio.ImageIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Sobe o servidor real (HTTP local, repositorios em memoria) e chama a API por HTTP. */
public class ApiIntegrationTest {
    private HttpServer server;
    private String base;
    private Path storageDir;
    private final AtomicLong now = new AtomicLong(1_000_000L);

    private static final class Resp {
        int status; String body; byte[] raw;
        Map<String, Object> json() { return Json.parseFlat(body); }
    }

    @Before
    public void start() throws Exception {
        LongSupplier clock = new LongSupplier() { public long getAsLong() { return now.get(); } };
        InMemoryStores.Users users = new InMemoryStores.Users();
        InMemoryStores.Players players = new InMemoryStores.Players();
        InMemoryStores.Capes capes = new InMemoryStores.Capes();
        InMemoryStores.Tokens tokens = new InMemoryStores.Tokens();
        storageDir = Files.createTempDirectory("capemod-test");
        CapeStorage storage = new CapeStorage(storageDir);
        AuthService auth = new AuthService(users, players, tokens, new PasswordHasher(1000), 3600_000L, clock);
        CapeService cs = new CapeService(capes, players, storage, 2, clock);
        Router router = new Router(auth, new AuthController(auth), new CapeController(cs),
                new PlayerController(cs), new TextureController(cs), new RateLimiter(clock), 100, 100);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", router);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void stop() { server.stop(0); }

    // ---------- helpers ----------
    private Resp call(String method, String path, String token, String contentType, byte[] body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(base + path).openConnection();
        c.setRequestMethod(method);
        if (token != null) c.setRequestProperty("Authorization", "Bearer " + token);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", contentType);
            OutputStream os = c.getOutputStream();
            os.write(body);
            os.close();
        }
        Resp r = new Resp();
        r.status = c.getResponseCode();
        InputStream in = r.status >= 400 ? c.getErrorStream() : c.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        if (in != null) { byte[] b = new byte[4096]; int n; while ((n = in.read(b)) > 0) bo.write(b, 0, n); in.close(); }
        r.raw = bo.toByteArray();
        r.body = new String(r.raw, StandardCharsets.UTF_8);
        return r;
    }

    private Resp json(String method, String path, String token, String body) throws IOException {
        return call(method, path, token, "application/json", body == null ? null : body.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] png(int w, int h, int argb) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setRGB(x, y, argb);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private Resp upload(String token, byte[] file) throws IOException {
        String b = "XBOUNDARY123";
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(("--" + b + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"cape.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        o.write(file);
        o.write(("\r\n--" + b + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return call("POST", "/api/capes/upload", token, "multipart/form-data; boundary=" + b, o.toByteArray());
    }

    private String[] registerAndLogin(String user) throws IOException {
        Resp r = json("POST", "/api/auth/register", null, "{\"username\":\"" + user + "\",\"password\":\"senha-forte-123\"}");
        assertEquals(r.body, 201, r.status);
        String playerId = (String) r.json().get("playerId");
        Resp l = json("POST", "/api/auth/login", null, "{\"username\":\"" + user + "\",\"password\":\"senha-forte-123\"}");
        assertEquals(l.body, 200, l.status);
        return new String[]{(String) l.json().get("token"), playerId};
    }

    // ---------- testes ----------
    @Test public void healthWorks() throws Exception {
        assertEquals(200, json("GET", "/api/health", null, null).status);
    }

    @Test public void registerValidationAndDuplicates() throws Exception {
        assertEquals(400, json("POST", "/api/auth/register", null, "{\"username\":\"a\",\"password\":\"senha-forte-123\"}").status);
        assertEquals(400, json("POST", "/api/auth/register", null, "{\"username\":\"usuario\",\"password\":\"curta\"}").status);
        assertEquals(400, json("POST", "/api/auth/register", null, "isto nao e json").status);
        assertEquals(201, json("POST", "/api/auth/register", null, "{\"username\":\"Alice\",\"password\":\"senha-forte-123\"}").status);
        assertEquals(409, json("POST", "/api/auth/register", null, "{\"username\":\"alice\",\"password\":\"outra-senha-123\"}").status);
    }

    @Test public void loginRejectsWrongPasswordAndUnknownUser() throws Exception {
        registerAndLogin("alice");
        assertEquals(401, json("POST", "/api/auth/login", null, "{\"username\":\"alice\",\"password\":\"errada-errada\"}").status);
        assertEquals(401, json("POST", "/api/auth/login", null, "{\"username\":\"ninguem\",\"password\":\"senha-forte-123\"}").status);
    }

    @Test public void passwordIsNeverStoredInPlainText() {
        String h = new PasswordHasher(1000).hash("minha-senha-secreta");
        assertFalse(h.contains("minha-senha-secreta"));
        assertTrue(new PasswordHasher(1000).verify("minha-senha-secreta", h));
        assertFalse(new PasswordHasher(1000).verify("outra", h));
    }

    @Test public void uploadRequiresAuthentication() throws Exception {
        assertEquals(401, upload(null, png(64, 32, 0xFFAA2222)).status);
        assertEquals(401, upload("token-falso", png(64, 32, 0xFFAA2222)).status);
    }

    @Test public void fullFlowUploadQueryTextureAndDelete() throws Exception {
        String[] a = registerAndLogin("alice");
        byte[] file = png(64, 32, 0xFFAA2222);
        Resp up = upload(a[0], file);
        assertEquals(up.body, 201, up.status);
        assertTrue(up.body.contains("\"active\":true")); // primeira capa vira ativa
        String capeId = extract(up.body, "capeId");
        String sha = extract(up.body, "sha256");

        // consulta publica (sem token) da capa ativa
        Resp pub = json("GET", "/api/players/" + a[1] + "/cape", null, null);
        assertEquals(pub.body, 200, pub.status);
        assertTrue(pub.body.contains(sha));

        // textura publica, PNG valido e cache imutavel
        Resp tex = call("GET", "/api/textures/" + sha + ".png", null, null, null);
        assertEquals(200, tex.status);
        assertEquals(sha, com.capemod.server.security.TokenService.sha256Hex(tex.raw));
        assertTrue(com.capemod.server.security.PngValidator.validate(tex.raw).ok);

        // lista do dono
        Resp list = json("GET", "/api/capes/" + a[1], a[0], null);
        assertEquals(list.body, 200, list.status);
        assertTrue(list.body.contains(capeId));

        // remover capa ativa e consultar de novo
        assertEquals(200, json("PUT", "/api/capes/" + a[1] + "/active", a[0], "{\"capeId\":null}").status);
        assertEquals(404, json("GET", "/api/players/" + a[1] + "/cape", null, null).status);
        assertEquals(200, json("PUT", "/api/capes/" + a[1] + "/active", a[0], "{\"capeId\":\"" + capeId + "\"}").status);
        assertEquals(200, json("GET", "/api/players/" + a[1] + "/cape", null, null).status);

        // apagar: some da consulta e a textura (sem referencias) e removida
        assertEquals(200, json("DELETE", "/api/capes/" + capeId, a[0], null).status);
        assertEquals(404, json("GET", "/api/players/" + a[1] + "/cape", null, null).status);
        assertEquals(404, call("GET", "/api/textures/" + sha + ".png", null, null, null).status);
    }

    @Test public void otherUserCannotTouchSomeoneElsesCapes() throws Exception {
        String[] a = registerAndLogin("alice");
        String[] b = registerAndLogin("bruno");
        String capeId = extract(upload(a[0], png(64, 32, 0xFFAA2222)).body, "capeId");
        assertEquals(404, json("PUT", "/api/capes/" + a[1] + "/active", b[0], "{\"capeId\":\"" + capeId + "\"}").status);
        assertEquals(404, json("GET", "/api/capes/" + a[1], b[0], null).status);
        assertEquals(404, json("DELETE", "/api/capes/" + capeId, b[0], null).status);
        // e nao pode ativar a capa de outro no proprio jogador
        assertEquals(404, json("PUT", "/api/capes/" + b[1] + "/active", b[0], "{\"capeId\":\"" + capeId + "\"}").status);
        // a capa de Alice continua la
        assertEquals(200, json("GET", "/api/players/" + a[1] + "/cape", null, null).status);
    }

    @Test public void rejectsMaliciousOrInvalidUploads() throws Exception {
        String[] a = registerAndLogin("alice");
        assertEquals(400, upload(a[0], "nao sou um png, sou texto comprido o bastante".getBytes()).status);
        assertEquals(400, upload(a[0], png(64, 64, 0xFFAA2222)).status);
        assertEquals(400, upload(a[0], png(64, 32, 0x00000000)).status);
        assertEquals(415, call("POST", "/api/capes/upload", a[0], "application/json", "{}".getBytes()).status);
        // Corpo acima do limite: o servidor responde 413 sem ler tudo; o cliente pode ver 413 ou conexao encerrada.
        try {
            assertEquals(413, call("POST", "/api/capes/upload", a[0], "multipart/form-data; boundary=X", new byte[1024 * 1024 + 20000]).status);
        } catch (java.net.SocketException expected) {
            // aceitavel: o servidor fechou a conexao ao recusar o corpo grande
        }
    }

    @Test public void enforcesCapeLimitPerUser() throws Exception {
        String[] a = registerAndLogin("alice");
        assertEquals(201, upload(a[0], png(64, 32, 0xFFAA2222)).status);
        assertEquals(201, upload(a[0], png(64, 32, 0xFF22AA22)).status);
        assertEquals(409, upload(a[0], png(64, 32, 0xFF2222AA)).status); // limite de teste = 2
    }

    @Test public void tokensExpire() throws Exception {
        String[] a = registerAndLogin("alice");
        assertEquals(200, json("GET", "/api/capes/" + a[1], a[0], null).status);
        now.addAndGet(3600_000L + 1);
        assertEquals(401, json("GET", "/api/capes/" + a[1], a[0], null).status);
    }

    @Test public void blocksPathTraversalAndBadIds() throws Exception {
        assertEquals(400, json("GET", "/api/textures/..%2F..%2Fetc%2Fpasswd", null, null).status);
        assertEquals(400, json("GET", "/api/textures/../x.png", null, null).status);
        assertEquals(400, json("GET", "/api/textures/nao-e-hash.png", null, null).status);
        assertEquals(400, json("GET", "/api/players/nao-e-uuid/cape", null, null).status);
        assertEquals(404, json("GET", "/api/nada", null, null).status);
        assertEquals(405, json("GET", "/api/auth/login", null, null).status);
    }

    @Test public void rateLimitsLoginAttempts() throws Exception {
        // Router do teste usa limite alto; aqui valida o limitador isoladamente com relogio controlado.
        RateLimiter rl = new RateLimiter(new LongSupplier() { public long getAsLong() { return now.get(); } });
        for (int i = 0; i < 3; i++) assertTrue(rl.allow("k", 3, 60000));
        assertFalse(rl.allow("k", 3, 60000));
        now.addAndGet(60001);
        assertTrue(rl.allow("k", 3, 60000));
    }

    private static String extract(String json, String key) {
        int i = json.indexOf("\"" + key + "\":\"");
        assertTrue(json, i >= 0);
        int s = i + key.length() + 4;
        return json.substring(s, json.indexOf('"', s));
    }
}
