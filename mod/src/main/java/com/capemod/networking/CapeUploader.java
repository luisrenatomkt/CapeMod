package com.capemod.networking;

import com.capemod.configuration.CapeConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Envio de capa por HTTPS. Roda SEMPRE em thread propria (nunca na thread do Minecraft).
 * Contrato (a API da ETAPA 4 deve seguir): POST {apiBaseUrl}/api/capes/upload,
 * multipart/form-data, campo "file" (image/png), cabecalho "Authorization: Bearer <token>".
 * Sucesso: HTTP 200/201 com JSON. Callbacks sao chamados na thread do uploader.
 */
public final class CapeUploader {
    public interface Listener {
        void onProgress(long sent, long total);
        void onSuccess(String responseBody);
        void onError(String message);
    }

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 20000;
    private static final int CHUNK = 8192;
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "CapeMod-Uploader");
            t.setDaemon(true);
            return t;
        }
    });

    private CapeUploader() {}

    /** Retorna o motivo pelo qual nao e possivel enviar agora, ou null se estiver tudo pronto. */
    public static String preflight() {
        String base = CapeConfig.getApiBaseUrl().trim();
        if (base.isEmpty()) return "Servidor não configurado (apiBaseUrl em config/capemod.cfg).";
        URL u;
        try {
            u = new URL(base);
        } catch (MalformedURLException e) {
            return "URL do servidor inválida.";
        }
        boolean https = "https".equals(u.getProtocol());
        boolean devLoopback = "http".equals(u.getProtocol())
                && CapeConfig.isAllowInsecureLocalhost() && isLoopback(u.getHost());
        if (!https && !devLoopback) return "A URL do servidor precisa usar HTTPS.";
        if (CapeConfig.getAuthToken().isEmpty()) return "Sem sessão iniciada (o login chega na ETAPA 5).";
        return null;
    }

    private static boolean isLoopback(String host) {
        return "localhost".equals(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
    }

    public static void upload(final byte[] png, final Listener l) {
        EXEC.submit(new Runnable() {
            public void run() {
                try {
                    doUpload(png, l);
                } catch (IOException ex) {
                    l.onError("Erro de conexão: " + clean(ex.getMessage()));
                } catch (RuntimeException ex) {
                    l.onError("Falha inesperada no envio: " + clean(String.valueOf(ex)));
                }
            }
        });
    }

    private static void doUpload(byte[] png, Listener l) throws IOException {
        String pre = preflight();
        if (pre != null) { l.onError(pre); return; }

        String base = CapeConfig.getApiBaseUrl().trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        HttpURLConnection c = (HttpURLConnection) new URL(base + "/api/capes/upload").openConnection();
        try {
            String boundary = "----CapeMod" + UUID.randomUUID().toString().replace("-", "");
            byte[] head = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; "
                    + "filename=\"cape.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            long total = (long) head.length + png.length + tail.length;

            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setInstanceFollowRedirects(false); // evita downgrade/redirecionamento para outro host
            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
            c.setReadTimeout(READ_TIMEOUT_MS);
            c.setFixedLengthStreamingMode((int) total);
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            c.setRequestProperty("Authorization", "Bearer " + CapeConfig.getAuthToken());
            c.setRequestProperty("Accept", "application/json");

            OutputStream out = c.getOutputStream();
            try {
                out.write(head);
                long sent = head.length;
                l.onProgress(sent, total);
                for (int off = 0; off < png.length; off += CHUNK) {
                    int n = Math.min(CHUNK, png.length - off);
                    out.write(png, off, n);
                    sent += n;
                    l.onProgress(sent, total);
                }
                out.write(tail);
                out.flush();
                l.onProgress(total, total);
            } finally {
                out.close();
            }

            int code = c.getResponseCode();
            String body = readLimited(code >= 400 ? c.getErrorStream() : c.getInputStream());
            if (code == 200 || code == 201) l.onSuccess(body);
            else l.onError(describe(code, body));
        } finally {
            c.disconnect();
        }
    }

    private static String describe(int code, String body) {
        switch (code) {
            case 400: case 415: case 422: return "O servidor recusou a capa: " + body;
            case 401: case 403: return "Não autorizado. Entre novamente na sua conta (ETAPA 5).";
            case 413: return "Arquivo grande demais para o servidor.";
            case 429: return "Muitas requisições. Tente de novo em instantes.";
            default:
                if (code >= 300 && code < 400) return "O servidor tentou redirecionar (bloqueado por segurança).";
                if (code >= 500) return "Erro no servidor (HTTP " + code + ").";
                return "Resposta inesperada do servidor (HTTP " + code + ").";
        }
    }

    private static String readLimited(InputStream in) throws IOException {
        if (in == null) return "";
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = in.read(buf)) > 0 && bo.size() < 2048) bo.write(buf, 0, n);
            return clean(new String(bo.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            in.close();
        }
    }

    private static String clean(String s) {
        if (s == null) return "";
        s = s.replaceAll("[\\p{Cntrl}]", " ").trim();
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
