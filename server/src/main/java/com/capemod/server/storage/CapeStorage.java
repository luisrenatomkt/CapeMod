package com.capemod.server.storage;

import com.capemod.server.security.InputValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Armazenamento de texturas enderecado pelo conteudo: arquivo = {sha256}.png.
 * Nomes vem so de um hash hex validado (nunca de entrada do usuario): sem path traversal, com deduplicacao.
 */
public final class CapeStorage {
    private final Path root;

    public CapeStorage(Path root) throws IOException {
        this.root = root.toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    private Path resolve(String sha) {
        if (!InputValidator.isSha256(sha)) throw new IllegalArgumentException("hash invalido");
        Path p = root.resolve(sha + ".png").normalize();
        if (!p.getParent().equals(root)) throw new IllegalArgumentException("caminho invalido");
        return p;
    }

    public void write(String sha, byte[] data) throws IOException {
        Path target = resolve(sha);
        if (Files.exists(target)) return;
        Path tmp = Files.createTempFile(root, "up", ".tmp");
        try {
            Files.write(tmp, data);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Retorna null se nao existir. */
    public byte[] read(String sha) throws IOException {
        Path p = resolve(sha);
        return Files.exists(p) ? Files.readAllBytes(p) : null;
    }

    public void delete(String sha) throws IOException { Files.deleteIfExists(resolve(sha)); }
}
