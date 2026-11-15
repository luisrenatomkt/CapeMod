package com.capemod.server;

import com.capemod.server.models.Cape;
import com.capemod.server.models.Player;
import com.capemod.server.models.User;
import com.capemod.server.repositories.CapeRepository;
import com.capemod.server.repositories.PlayerRepository;
import com.capemod.server.repositories.TokenRepository;
import com.capemod.server.repositories.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Implementacoes em memoria dos repositorios, so para testes de API sem banco. */
public final class InMemoryStores {
    private InMemoryStores() {}

    public static final class Users implements UserRepository {
        final Map<String, User> byId = new LinkedHashMap<String, User>();
        public synchronized boolean insert(User u) {
            for (User x : byId.values()) if (x.username.toLowerCase(Locale.ROOT).equals(u.username.toLowerCase(Locale.ROOT))) return false;
            byId.put(u.id, u);
            return true;
        }
        public synchronized User findByUsername(String n) {
            for (User x : byId.values()) if (x.username.equalsIgnoreCase(n)) return x;
            return null;
        }
        public synchronized User findById(String id) { return byId.get(id); }
    }

    public static final class Players implements PlayerRepository {
        final Map<String, Player> byId = new LinkedHashMap<String, Player>();
        public synchronized void insert(Player p) { byId.put(p.id, p); }
        public synchronized Player findById(String id) { return byId.get(id); }
        public synchronized Player findByUserId(String uid) {
            for (Player p : byId.values()) if (p.userId.equals(uid)) return p;
            return null;
        }
        public synchronized void setActiveCape(String pid, String cid) {
            Player p = byId.get(pid);
            if (p != null) byId.put(pid, new Player(p.id, p.userId, cid));
        }
        public synchronized void clearActiveCape(String cid) {
            for (Player p : new ArrayList<Player>(byId.values())) {
                if (cid.equals(p.activeCapeId)) byId.put(p.id, new Player(p.id, p.userId, null));
            }
        }
    }

    public static final class Capes implements CapeRepository {
        final Map<String, Cape> byId = new LinkedHashMap<String, Cape>();
        public synchronized void insert(Cape c) { byId.put(c.id, c); }
        public synchronized Cape findById(String id) { return byId.get(id); }
        public synchronized List<Cape> findByOwner(String o) {
            List<Cape> out = new ArrayList<Cape>();
            for (Cape c : byId.values()) if (c.ownerId.equals(o)) out.add(c);
            return out;
        }
        public synchronized int countByOwner(String o) { return findByOwner(o).size(); }
        public synchronized boolean delete(String id) { return byId.remove(id) != null; }
        public synchronized int countBySha(String sha) {
            int n = 0;
            for (Cape c : byId.values()) if (c.sha256.equals(sha)) n++;
            return n;
        }
    }

    public static final class Tokens implements TokenRepository {
        final Map<String, long[]> exp = new LinkedHashMap<String, long[]>();
        final Map<String, String> user = new LinkedHashMap<String, String>();
        public synchronized void insert(String h, String uid, long e) { exp.put(h, new long[]{e}); user.put(h, uid); }
        public synchronized String findUserId(String h, long now) {
            long[] e = exp.get(h);
            return e != null && e[0] > now ? user.get(h) : null;
        }
        public synchronized void deleteExpired(long now) {
            for (String h : new ArrayList<String>(exp.keySet())) if (exp.get(h)[0] <= now) { exp.remove(h); user.remove(h); }
        }
    }
}
