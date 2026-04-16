package com.piotrgala.marketsnapshot.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MemoryCacheTest {

    @Test
    void shouldReturnFreshEntryWithinTtl() {
        MemoryCache<String, String> cache = new MemoryCache<>();
        Instant storedAt = Instant.parse("2026-04-16T12:00:00Z");

        cache.put("markets", "btc,eth,sol", storedAt);

        MemoryCache.CacheEntry<String> entry = cache.getFresh(
                "markets",
                Instant.parse("2026-04-16T12:00:30Z"),
                Duration.ofSeconds(45)
        );

        assertNotNull(entry);
        assertEquals("btc,eth,sol", entry.value());
        assertEquals(storedAt, entry.storedAt());
    }

    @Test
    void shouldReturnNullWhenEntryExpired() {
        MemoryCache<String, String> cache = new MemoryCache<>();
        cache.put("markets", "btc,eth,sol", Instant.parse("2026-04-16T12:00:00Z"));

        MemoryCache.CacheEntry<String> entry = cache.getFresh(
                "markets",
                Instant.parse("2026-04-16T12:00:46Z"),
                Duration.ofSeconds(45)
        );

        assertNull(entry);
    }

    @Test
    void shouldKeepExpiredEntryAvailableForFallback() {
        MemoryCache<String, String> cache = new MemoryCache<>();
        Instant storedAt = Instant.parse("2026-04-16T12:00:00Z");
        cache.put("markets", "btc,eth,sol", storedAt);

        MemoryCache.CacheEntry<String> staleEntry = cache.get("markets");

        assertNotNull(staleEntry);
        assertEquals("btc,eth,sol", staleEntry.value());
        assertEquals(storedAt, staleEntry.storedAt());
    }
}
