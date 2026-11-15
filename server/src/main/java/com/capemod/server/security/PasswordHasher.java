package com.capemod.server.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2-HMAC-SHA256 com sal aleatorio. Formato: pbkdf2_sha256$iteracoes$salBase64$hashBase64. */
public final class PasswordHasher {
    private static final String PREFIX = "pbkdf2_sha256";
    private static final int SALT_BYTES = 16, KEY_BITS = 256, MAX_ITERATIONS = 2000000;
    private final SecureRandom random = new SecureRandom();
    private final int iterations;

    public PasswordHasher(int iterations) { this.iterations = iterations; }

    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] dk = derive(password, salt, iterations);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIX + "$" + iterations + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(dk);
    }

    public boolean verify(String password, String stored) {
        try {
            String[] p = stored.split("\\$");
            if (p.length != 4 || !PREFIX.equals(p[0])) return false;
            int it = Integer.parseInt(p[1]);
            if (it < 1 || it > MAX_ITERATIONS) return false;
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] expected = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, it)); // comparacao em tempo constante
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 indisponivel", e);
        } finally {
            spec.clearPassword();
        }
    }

    static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }
}
