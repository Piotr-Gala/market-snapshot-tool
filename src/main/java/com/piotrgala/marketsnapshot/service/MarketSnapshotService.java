package com.piotrgala.marketsnapshot.service;

import com.piotrgala.marketsnapshot.client.CoinGeckoClient;
import com.piotrgala.marketsnapshot.exception.InvalidMarketDataException;
import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.model.CoinMarket;
import com.piotrgala.marketsnapshot.model.PricePoint;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MarketSnapshotService {

    private static final int HISTORY_DAYS = 30;

    private final SnapshotDataFetcher snapshotDataFetcher;
    private final StatisticsCalculator statisticsCalculator;
    private final Clock clock;

    public MarketSnapshotService() {
        this(Clock.systemDefaultZone());
    }

    private MarketSnapshotService(Clock clock) {
        this(new CachedMarketDataFetcher(new CoinGeckoClient(), clock), new StatisticsCalculator(), clock);
    }

    MarketSnapshotService(SnapshotDataFetcher snapshotDataFetcher, StatisticsCalculator statisticsCalculator, Clock clock) {
        this.snapshotDataFetcher = snapshotDataFetcher;
        this.statisticsCalculator = statisticsCalculator;
        this.clock = clock;
    }

    public List<AssetSnapshot> getSnapshot(List<Asset> assets) throws IOException, InterruptedException {
        return getSnapshotResult(assets).snapshots();
    }

    public SnapshotResult getSnapshotResult(List<Asset> assets) throws IOException, InterruptedException {
        if (assets.isEmpty()) {
            return new SnapshotResult(List.of(), SnapshotDataSource.LIVE, Instant.now(clock), List.of());
        }

        SnapshotDataFetcher.FetchResult<List<CoinMarket>> marketsResult = snapshotDataFetcher.fetchMarkets(assets);
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
                SnapshotDataFetcher.FetchResult<List<PricePoint>> historyResult =
                        snapshotDataFetcher.fetchPriceHistory(asset, HISTORY_DAYS);
                dataSource = dataSource.combine(historyResult.dataSource());
                dataAsOf = earliestInstant(dataAsOf, historyResult.dataAsOf());
                snapshots.add(toAssetSnapshot(asset, market, historyResult.value()));
            } catch (IOException exception) {
                warnings.add("Skipped " + asset.symbol().toUpperCase() + ": " + exception.getMessage());
            }
        }

        return new SnapshotResult(snapshots, dataSource, dataAsOf, warnings);
    }

    private AssetSnapshot toAssetSnapshot(Asset asset, CoinMarket market, List<PricePoint> priceHistory)
            throws InvalidMarketDataException {
        List<PricePoint> dailyPrices = sampleDailyPrices(priceHistory);

        return new AssetSnapshot(
                market.symbol().toUpperCase(),
                market.name(),
                requireValue(market.currentPrice(), "current price", asset),
                market.marketCap(),
                market.priceChangePercentage24h(),
                calculateReturn(dailyPrices, 7),
                calculateReturn(dailyPrices, 30),
                calculateAnnualizedVolatility(dailyPrices)
        );
    }

    private List<PricePoint> sampleDailyPrices(List<PricePoint> priceHistory) throws InvalidMarketDataException {
        try {
            return statisticsCalculator.sampleDailyPrices(priceHistory, HISTORY_DAYS);
        } catch (IllegalArgumentException exception) {
            throw new InvalidMarketDataException(exception.getMessage(), exception);
        }
    }

    private double calculateReturn(List<PricePoint> dailyPrices, int days) throws InvalidMarketDataException {
        try {
            return statisticsCalculator.calculateReturn(dailyPrices, days);
        } catch (IllegalArgumentException exception) {
            throw new InvalidMarketDataException(exception.getMessage(), exception);
        }
    }

    private double calculateAnnualizedVolatility(List<PricePoint> dailyPrices) throws InvalidMarketDataException {
        try {
            return statisticsCalculator.calculateAnnualizedVolatility(dailyPrices);
        } catch (IllegalArgumentException exception) {
            throw new InvalidMarketDataException(exception.getMessage(), exception);
        }
    }

    private double requireValue(Double value, String fieldName, Asset asset) throws InvalidMarketDataException {
        if (value == null) {
            throw new InvalidMarketDataException(
                    "CoinGecko response is missing " + fieldName + " for " + asset.symbol()
            );
        }
        return value;
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
}
