package com.capemod.server.security;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Limitador de janela fixa por chave (ex.: "login:IP"). */
public final class RateLimiter {
    private static final class Bucket { long windowStart; int count; }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<String, Bucket>();
    private final LongSupplier clock;

    public RateLimiter(LongSupplier clock) { this.clock = clock; }

    public boolean allow(String key, int limit, long windowMs) {
        long now = clock.getAsLong();
        if (buckets.size() > 20000) cleanup(now, windowMs);
        Bucket b = buckets.get(key);
        if (b == null) {
            Bucket nb = new Bucket();
            nb.windowStart = now;
            b = buckets.putIfAbsent(key, nb);
            if (b == null) b = nb;
        }
        synchronized (b) {
            if (now - b.windowStart >= windowMs) { b.windowStart = now; b.count = 0; }
            if (b.count >= limit) return false;
            b.count++;
            return true;
        }
    }

    private void cleanup(long now, long windowMs) {
        Iterator<Map.Entry<String, Bucket>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            Bucket b = it.next().getValue();
            synchronized (b) { if (now - b.windowStart > windowMs * 2) it.remove(); }
        }
    }
}
