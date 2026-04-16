package com.piotrgala.marketsnapshot.ui;

import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SnapshotSortOptionTest {

    @Test
    void shouldSortByMarketCapDescendingWithNullsLast() {
        AssetSnapshot btc = snapshot("BTC", "Bitcoin", 1_000_000_000_000.0, 4.0, 8.0, 16.0, 42.0);
        AssetSnapshot eth = snapshot("ETH", "Ethereum", 400_000_000_000.0, 3.0, 6.0, 12.0, 48.0);
        AssetSnapshot xrp = snapshot("XRP", "XRP", null, 1.0, 2.0, 3.0, 55.0);

        List<AssetSnapshot> sorted = SnapshotSortOption.MARKET_CAP.sort(List.of(xrp, eth, btc));

        assertEquals(List.of(btc, eth, xrp), sorted);
    }

    @Test
    void shouldSortBySevenDayReturnDescending() {
        AssetSnapshot btc = snapshot("BTC", "Bitcoin", 1_000_000_000_000.0, 4.0, 8.0, 16.0, 42.0);
        AssetSnapshot eth = snapshot("ETH", "Ethereum", 400_000_000_000.0, 3.0, 12.0, 10.0, 48.0);
        AssetSnapshot sol = snapshot("SOL", "Solana", 60_000_000_000.0, 5.0, -2.0, 18.0, 70.0);

        List<AssetSnapshot> sorted = SnapshotSortOption.RETURN_7D.sort(List.of(btc, sol, eth));

        assertEquals(List.of(eth, btc, sol), sorted);
    }

    private AssetSnapshot snapshot(
            String symbol,
            String name,
            Double marketCap,
            Double change24h,
            double return7d,
            double return30d,
            double realizedVolatility
    ) {
        return new AssetSnapshot(
                symbol,
                name,
                100.0,
                marketCap,
                change24h,
                return7d,
                return30d,
                realizedVolatility
        );
    }
}
