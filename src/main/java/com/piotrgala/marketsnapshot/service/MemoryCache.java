package com.piotrgala.marketsnapshot.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MemoryCache<K, V> {

    private final Map<K, CacheEntry<V>> entries = new ConcurrentHashMap<>();

    CacheEntry<V> get(K key) {
        return entries.get(key);
    }

    CacheEntry<V> getFresh(K key, Instant now, Duration ttl) {
        CacheEntry<V> entry = entries.get(key);
        if (entry == null || !entry.isFreshAt(now, ttl)) {
            return null;
        }
        return entry;
    }

    void put(K key, V value, Instant storedAt) {
        entries.put(key, new CacheEntry<>(value, storedAt));
    }

    record CacheEntry<V>(V value, Instant storedAt) {

        boolean isFreshAt(Instant now, Duration ttl) {
            return !storedAt.plus(ttl).isBefore(now);
        }
    }
}
