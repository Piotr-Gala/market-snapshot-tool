package com.piotrgala.marketsnapshot.service;

import com.piotrgala.marketsnapshot.client.CoinGeckoClient;
import com.piotrgala.marketsnapshot.client.CoinGeckoRequestException;
import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.model.CoinMarket;
import com.piotrgala.marketsnapshot.model.PricePoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MarketSnapshotService {

    private static final int HISTORY_DAYS = 30;
    private static final int MAX_ATTEMPTS = 3;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 2_200L;

    private final CoinGeckoClient coinGeckoClient;
    private final StatisticsCalculator statisticsCalculator;
    private long lastRequestTimestampMillis;

    public MarketSnapshotService() {
        this.coinGeckoClient = new CoinGeckoClient();
        this.statisticsCalculator = new StatisticsCalculator();
    }

    public List<AssetSnapshot> getSnapshot(List<Asset> assets) throws IOException, InterruptedException {
        List<CoinMarket> markets = executeCoinGeckoCall(() -> coinGeckoClient.fetchMarkets(assets));
        List<AssetSnapshot> snapshots = new ArrayList<>();

        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            CoinMarket market = markets.get(i);

            List<PricePoint> priceHistory = executeCoinGeckoCall(
                    () -> coinGeckoClient.fetchPriceHistory(asset, HISTORY_DAYS)
            );
            List<PricePoint> dailyPrices = statisticsCalculator.sampleDailyPrices(priceHistory, HISTORY_DAYS);

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
        }

        return snapshots;
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
}
