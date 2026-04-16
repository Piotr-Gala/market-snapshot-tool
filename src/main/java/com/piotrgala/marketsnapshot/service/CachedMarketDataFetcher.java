package com.piotrgala.marketsnapshot.service;

import com.piotrgala.marketsnapshot.client.CoinGeckoRequestException;
import com.piotrgala.marketsnapshot.client.MarketDataClient;
import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.CoinMarket;
import com.piotrgala.marketsnapshot.model.PricePoint;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public final class CachedMarketDataFetcher implements SnapshotDataFetcher {

    private static final int MAX_ATTEMPTS = 3;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 2_200L;
    private static final Duration MARKETS_CACHE_TTL = Duration.ofSeconds(45);
    private static final Duration HISTORY_CACHE_TTL = Duration.ofMinutes(15);

    private final MarketDataClient marketDataClient;
    private final Clock clock;
    private final MemoryCache<String, List<CoinMarket>> marketsCache;
    private final MemoryCache<String, List<PricePoint>> historyCache;
    private long lastRequestTimestampMillis;

    public CachedMarketDataFetcher(MarketDataClient marketDataClient, Clock clock) {
        this.marketDataClient = marketDataClient;
        this.clock = clock;
        this.marketsCache = new MemoryCache<>();
        this.historyCache = new MemoryCache<>();
    }

    @Override
    public FetchResult<List<CoinMarket>> fetchMarkets(List<Asset> assets) throws IOException, InterruptedException {
        String cacheKey = Asset.joinCoinGeckoIds(assets);
        Instant now = Instant.now(clock);

        MemoryCache.CacheEntry<List<CoinMarket>> freshEntry = marketsCache.getFresh(cacheKey, now, MARKETS_CACHE_TTL);
        if (freshEntry != null) {
            return new FetchResult<>(freshEntry.value(), SnapshotDataSource.CACHED, freshEntry.storedAt());
        }

        try {
            List<CoinMarket> markets = List.copyOf(executeRequest(() -> marketDataClient.fetchMarkets(assets)));
            Instant fetchedAt = Instant.now(clock);
            marketsCache.put(cacheKey, markets, fetchedAt);
            return new FetchResult<>(markets, SnapshotDataSource.LIVE, fetchedAt);
        } catch (IOException | CoinGeckoRequestException exception) {
            MemoryCache.CacheEntry<List<CoinMarket>> staleEntry = marketsCache.get(cacheKey);
            if (staleEntry != null) {
                return new FetchResult<>(staleEntry.value(), SnapshotDataSource.CACHED, staleEntry.storedAt());
            }
            throw exception;
        }
    }

    @Override
    public FetchResult<List<PricePoint>> fetchPriceHistory(Asset asset, int days)
            throws IOException, InterruptedException {
        String cacheKey = asset.coinGeckoId() + ":" + days;
        Instant now = Instant.now(clock);

        MemoryCache.CacheEntry<List<PricePoint>> freshEntry = historyCache.getFresh(cacheKey, now, HISTORY_CACHE_TTL);
        if (freshEntry != null) {
            return new FetchResult<>(freshEntry.value(), SnapshotDataSource.CACHED, freshEntry.storedAt());
        }

        try {
            List<PricePoint> history = List.copyOf(executeRequest(() -> marketDataClient.fetchPriceHistory(asset, days)));
            Instant fetchedAt = Instant.now(clock);
            historyCache.put(cacheKey, history, fetchedAt);
            return new FetchResult<>(history, SnapshotDataSource.LIVE, fetchedAt);
        } catch (IOException | CoinGeckoRequestException exception) {
            MemoryCache.CacheEntry<List<PricePoint>> staleEntry = historyCache.get(cacheKey);
            if (staleEntry != null) {
                return new FetchResult<>(staleEntry.value(), SnapshotDataSource.CACHED, staleEntry.storedAt());
            }
            throw exception;
        }
    }

    private <T> T executeRequest(RequestCall<T> call) throws IOException, InterruptedException {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            waitForNextRequestWindow();

            try {
                return call.execute();
            } catch (CoinGeckoRequestException exception) {
                if (exception.statusCode() != 429 || attempt == MAX_ATTEMPTS) {
                    throw exception;
                }

                Thread.sleep(2L * attempt * 1000L);
            }
        }

        throw new IllegalStateException("CoinGecko request failed after retries");
    }

    private synchronized void waitForNextRequestWindow() throws InterruptedException {
        long now = System.currentTimeMillis();
        long waitMillis = (lastRequestTimestampMillis + MIN_REQUEST_INTERVAL_MILLIS) - now;

        if (waitMillis > 0) {
            Thread.sleep(waitMillis);
        }

        lastRequestTimestampMillis = System.currentTimeMillis();
    }

    @FunctionalInterface
    private interface RequestCall<T> {
        T execute() throws IOException, InterruptedException;
    }
}
