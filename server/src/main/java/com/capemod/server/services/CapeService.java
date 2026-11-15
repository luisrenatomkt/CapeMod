package com.capemod.server.services;

import com.capemod.server.models.Cape;
import com.capemod.server.models.Player;
import com.capemod.server.repositories.CapeRepository;
import com.capemod.server.repositories.PlayerRepository;
import com.capemod.server.security.InputValidator;
import com.capemod.server.security.PngValidator;
import com.capemod.server.security.TokenService;
import com.capemod.server.storage.CapeStorage;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class CapeService {
    public static final class Uploaded {
        public final Cape cape;
        public final boolean active;
        public final List<String> warnings;
        Uploaded(Cape c, boolean a, List<String> w) { cape = c; active = a; warnings = w; }
    }

    private final CapeRepository capes;
    private final PlayerRepository players;
    private final CapeStorage storage;
    private final int maxCapesPerUser;
    private final LongSupplier clock;

    public CapeService(CapeRepository capes, PlayerRepository players, CapeStorage storage,
                       int maxCapesPerUser, LongSupplier clock) {
        this.capes = capes; this.players = players; this.storage = storage;
        this.maxCapesPerUser = maxCapesPerUser; this.clock = clock;
    }

    public Uploaded upload(String userId, byte[] rawPng) {
        PngValidator.Result r = PngValidator.validate(rawPng);
        if (!r.ok) throw new ApiException(400, r.error);
        Player player = players.findByUserId(userId);
        if (player == null) throw new ApiException(404, "Jogador não encontrado.");
        if (capes.countByOwner(userId) >= maxCapesPerUser) {
            throw new ApiException(409, "Limite de " + maxCapesPerUser + " capas atingido. Remova alguma antes.");
        }
        try {
            byte[] clean = PngValidator.encode(r.image); // sempre armazena a versao regravada, nunca o upload bruto
            String sha = TokenService.sha256Hex(clean);
            storage.write(sha, clean);
            Cape cape = new Cape(UUID.randomUUID().toString(), userId, sha, r.width, r.height, clock.getAsLong());
            capes.insert(cape);
            boolean makeActive = player.activeCapeId == null;
            if (makeActive) players.setActiveCape(player.id, cape.id);
            return new Uploaded(cape, makeActive, r.warnings);
        } catch (IOException e) {
            throw new ApiException(500, "Falha ao armazenar a capa.");
        }
    }

    /** Lista as capas do proprio jogador. Nao revela a existencia de jogadores de outras contas (404). */
    public List<Cape> listForPlayer(String userId, String playerId) {
        ownedPlayer(userId, playerId);
        return capes.findByOwner(userId);
    }

    public String activeCapeId(String userId, String playerId) {
        return ownedPlayer(userId, playerId).activeCapeId;
    }

    public void setActive(String userId, String playerId, String capeIdOrNull) {
        Player p = ownedPlayer(userId, playerId);
        if (capeIdOrNull != null) {
            if (!InputValidator.isUuid(capeIdOrNull)) throw new ApiException(400, "capeId inválido.");
            Cape c = capes.findById(capeIdOrNull);
            if (c == null || !c.ownerId.equals(userId)) throw new ApiException(404, "Capa não encontrada.");
        }
        players.setActiveCape(p.id, capeIdOrNull);
    }

    public void delete(String userId, String capeId) {
        if (!InputValidator.isUuid(capeId)) throw new ApiException(400, "capeId inválido.");
        Cape c = capes.findById(capeId);
        if (c == null || !c.ownerId.equals(userId)) throw new ApiException(404, "Capa não encontrada.");
        players.clearActiveCape(c.id);
        capes.delete(c.id);
        if (capes.countBySha(c.sha256) == 0) {
            try {
                storage.delete(c.sha256);
            } catch (IOException ignored) {
                // arquivo orfao nao afeta a corretude; pode ser limpo depois
            }
        }
    }

    /** Consulta publica: capa ativa de um jogador (usada pelo mod ao ver outros jogadores). */
    public Cape publicActiveCape(String playerId) {
        if (!InputValidator.isUuid(playerId)) throw new ApiException(400, "playerId inválido.");
        Player p = players.findById(playerId);
        if (p == null || p.activeCapeId == null) throw new ApiException(404, "Este jogador não tem capa.");
        Cape c = capes.findById(p.activeCapeId);
        if (c == null) throw new ApiException(404, "Este jogador não tem capa.");
        return c;
    }

    private Player ownedPlayer(String userId, String playerId) {
        if (!InputValidator.isUuid(playerId)) throw new ApiException(400, "playerId inválido.");
        Player p = players.findById(playerId);
        if (p == null || !p.userId.equals(userId)) throw new ApiException(404, "Jogador não encontrado.");
        return p;
    }

    public byte[] texture(String sha) {
        if (!InputValidator.isSha256(sha)) throw new ApiException(400, "Textura inválida.");
        try {
            byte[] d = storage.read(sha);
            if (d == null) throw new ApiException(404, "Textura não encontrada.");
            return d;
        } catch (IOException e) {
            throw new ApiException(500, "Falha ao ler a textura.");
        }
    }
}
