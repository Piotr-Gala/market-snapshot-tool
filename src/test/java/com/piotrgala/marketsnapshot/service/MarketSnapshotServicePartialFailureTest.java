package com.piotrgala.marketsnapshot.service;

import com.piotrgala.marketsnapshot.client.CoinGeckoRequestException;
import com.piotrgala.marketsnapshot.client.MarketDataClient;
import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.CoinMarket;
import com.piotrgala.marketsnapshot.model.PricePoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketSnapshotServicePartialFailureTest {

    @Test
    void shouldReturnSuccessfulAssetsAndWarningsWhenOneAssetFails() throws IOException, InterruptedException {
        MarketDataClient marketDataClient = new FakeMarketDataClient(
                List.of(
                        new CoinMarket("bitcoin", "btc", "Bitcoin", 100_000.0, 2_000_000_000_000.0, 1.5),
                        new CoinMarket("ethereum", "eth", "Ethereum", 5_000.0, 600_000_000_000.0, 2.0)
                ),
                Map.of(
                        Asset.BTC, history(100.0, 101.0, 102.0, 103.0, 104.0, 105.0, 106.0, 107.0,
                                108.0, 109.0, 110.0, 111.0, 112.0, 113.0, 114.0, 115.0,
                                116.0, 117.0, 118.0, 119.0, 120.0, 121.0, 122.0, 123.0,
                                124.0, 125.0, 126.0, 127.0, 128.0, 129.0, 130.0)
                ),
                Map.of(Asset.ETH, new CoinGeckoRequestException("CoinGecko history request failed with status code 500", 500))
        );

        MarketSnapshotService service = new MarketSnapshotService(
                marketDataClient,
                new StatisticsCalculator(),
                Clock.fixed(Instant.parse("2026-04-16T18:00:00Z"), ZoneOffset.UTC)
        );

        SnapshotResult result = service.getSnapshotResult(List.of(Asset.BTC, Asset.ETH));

        assertEquals(1, result.snapshots().size());
        assertEquals("BTC", result.snapshots().get(0).symbol());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("Skipped ETH"));
    }

    private static List<PricePoint> history(double... prices) {
        java.util.ArrayList<PricePoint> points = new java.util.ArrayList<>();
        for (int i = 0; i < prices.length; i++) {
            points.add(new PricePoint((long) i * 86_400_000L, prices[i]));
        }
        return points;
    }

    private record FakeMarketDataClient(
            List<CoinMarket> markets,
            Map<Asset, List<PricePoint>> histories,
            Map<Asset, RuntimeException> historyFailures
    ) implements MarketDataClient {

        @Override
        public List<CoinMarket> fetchMarkets(List<Asset> assets) {
            return markets;
        }

        @Override
        public List<PricePoint> fetchPriceHistory(Asset asset, int days) {
            RuntimeException failure = historyFailures.get(asset);
            if (failure != null) {
                throw failure;
            }
            return histories.get(asset);
        }
    }
}
