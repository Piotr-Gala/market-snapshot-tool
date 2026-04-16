package com.piotrgala.marketsnapshot.service;

import com.piotrgala.marketsnapshot.client.CoinGeckoClient;
import com.piotrgala.marketsnapshot.client.CoinGeckoRequestException;
import com.piotrgala.marketsnapshot.client.MarketDataClient;
import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.model.CoinMarket;
import com.piotrgala.marketsnapshot.model.PricePoint;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MarketSnapshotService {

    private static final int HISTORY_DAYS = 30;
    private static final int MAX_ATTEMPTS = 3;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 2_200L;
    private static final Duration MARKETS_CACHE_TTL = Duration.ofSeconds(45);
    private static final Duration HISTORY_CACHE_TTL = Duration.ofMinutes(15);

    private final MarketDataClient marketDataClient;
    private final StatisticsCalculator statisticsCalculator;
    private final Clock clock;
    private final MemoryCache<String, List<CoinMarket>> marketsCache;
    private final MemoryCache<String, List<PricePoint>> historyCache;
    private long lastRequestTimestampMillis;

    public MarketSnapshotService() {
        this(new CoinGeckoClient(), new StatisticsCalculator(), Clock.systemDefaultZone());
    }

    MarketSnapshotService(MarketDataClient marketDataClient, StatisticsCalculator statisticsCalculator, Clock clock) {
        this.marketDataClient = marketDataClient;
        this.statisticsCalculator = statisticsCalculator;
        this.clock = clock;
        this.marketsCache = new MemoryCache<>();
        this.historyCache = new MemoryCache<>();
    }

    public List<AssetSnapshot> getSnapshot(List<Asset> assets) throws IOException, InterruptedException {
        return getSnapshotResult(assets).snapshots();
    }

    public SnapshotResult getSnapshotResult(List<Asset> assets) throws IOException, InterruptedException {
        if (assets.isEmpty()) {
            return new SnapshotResult(List.of(), SnapshotDataSource.LIVE, Instant.now(clock), List.of());
        }

        CachedFetch<List<CoinMarket>> marketsResult = fetchMarketsWithCache(assets);
        Map<String, CoinMarket> marketsById = indexMarketsById(marketsResult.value());
        List<AssetSnapshot> snapshots = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        SnapshotDataSource dataSource = marketsResult.dataSource();
        Instant dataAsOf = marketsResult.dataAsOf();

        for (Asset asset : assets) {
            CoinMarket market = marketsById.get(asset.coinGeckoId());
            if (market == null) {
                warnings.add("Skipped " + asset.symbol().toUpperCase() + ": market data unavailable.");
                continue;
            }

            try {
                CachedFetch<List<PricePoint>> historyResult = fetchHistoryWithCache(asset, HISTORY_DAYS);
                List<PricePoint> priceHistory = historyResult.value();
                List<PricePoint> dailyPrices = statisticsCalculator.sampleDailyPrices(priceHistory, HISTORY_DAYS);
                dataSource = dataSource.combine(historyResult.dataSource());
                dataAsOf = earliestInstant(dataAsOf, historyResult.dataAsOf());

                snapshots.add(new AssetSnapshot(
                        market.symbol().toUpperCase(),
                        market.name(),
                        requireValue(market.currentPrice(), "current price", asset),
                        market.marketCap(),
                        market.priceChangePercentage24h(),
                        statisticsCalculator.calculateReturn(dailyPrices, 7),
                        statisticsCalculator.calculateReturn(dailyPrices, 30),
                        statisticsCalculator.calculateAnnualizedVolatility(dailyPrices)
                ));
            } catch (IOException | IllegalStateException exception) {
                warnings.add("Skipped " + asset.symbol().toUpperCase() + ": " + exception.getMessage());
            }
        }

        return new SnapshotResult(snapshots, dataSource, dataAsOf, warnings);
    }

    private double requireValue(Double value, String fieldName, Asset asset) {
        if (value == null) {
            throw new IllegalStateException("CoinGecko response is missing " + fieldName + " for " + asset.symbol());
        }
        return value;
    }

    private <T> T executeCoinGeckoCall(CoinGeckoCall<T> call) throws IOException, InterruptedException {
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

    private CachedFetch<List<CoinMarket>> fetchMarketsWithCache(List<Asset> assets)
            throws IOException, InterruptedException {
        String cacheKey = Asset.joinCoinGeckoIds(assets);
        Instant now = Instant.now(clock);

        MemoryCache.CacheEntry<List<CoinMarket>> freshEntry = marketsCache.getFresh(cacheKey, now, MARKETS_CACHE_TTL);
        if (freshEntry != null) {
            return new CachedFetch<>(freshEntry.value(), SnapshotDataSource.CACHED, freshEntry.storedAt());
        }

        try {
            List<CoinMarket> markets = List.copyOf(executeCoinGeckoCall(() -> marketDataClient.fetchMarkets(assets)));
            Instant fetchedAt = Instant.now(clock);
            marketsCache.put(cacheKey, markets, fetchedAt);
            return new CachedFetch<>(markets, SnapshotDataSource.LIVE, fetchedAt);
        } catch (IOException | CoinGeckoRequestException exception) {
            MemoryCache.CacheEntry<List<CoinMarket>> staleEntry = marketsCache.get(cacheKey);
            if (staleEntry != null) {
                return new CachedFetch<>(staleEntry.value(), SnapshotDataSource.CACHED, staleEntry.storedAt());
            }
            throw exception;
        }
    }

    private CachedFetch<List<PricePoint>> fetchHistoryWithCache(Asset asset, int days)
            throws IOException, InterruptedException {
        String cacheKey = asset.coinGeckoId() + ":" + days;
        Instant now = Instant.now(clock);

        MemoryCache.CacheEntry<List<PricePoint>> freshEntry = historyCache.getFresh(cacheKey, now, HISTORY_CACHE_TTL);
        if (freshEntry != null) {
            return new CachedFetch<>(freshEntry.value(), SnapshotDataSource.CACHED, freshEntry.storedAt());
        }

        try {
            List<PricePoint> history = List.copyOf(executeCoinGeckoCall(
                    () -> marketDataClient.fetchPriceHistory(asset, days)
            ));
            Instant fetchedAt = Instant.now(clock);
            historyCache.put(cacheKey, history, fetchedAt);
            return new CachedFetch<>(history, SnapshotDataSource.LIVE, fetchedAt);
        } catch (IOException | CoinGeckoRequestException exception) {
            MemoryCache.CacheEntry<List<PricePoint>> staleEntry = historyCache.get(cacheKey);
            if (staleEntry != null) {
                return new CachedFetch<>(staleEntry.value(), SnapshotDataSource.CACHED, staleEntry.storedAt());
            }
            throw exception;
        }
    }

    private Instant earliestInstant(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    private Map<String, CoinMarket> indexMarketsById(List<CoinMarket> markets) {
        Map<String, CoinMarket> marketsById = new LinkedHashMap<>();
        for (CoinMarket market : markets) {
            marketsById.put(market.id(), market);
        }
        return marketsById;
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
    private interface CoinGeckoCall<T> {
        T execute() throws IOException, InterruptedException;
    }

    private record CachedFetch<T>(T value, SnapshotDataSource dataSource, Instant dataAsOf) {
    }
}
