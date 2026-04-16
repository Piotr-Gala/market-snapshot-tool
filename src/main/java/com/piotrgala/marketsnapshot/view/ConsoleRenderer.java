package com.piotrgala.marketsnapshot.view;

import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.ui.SnapshotSortOption;

import java.util.List;
import java.util.Locale;

public final class ConsoleRenderer {

    public void render(List<AssetSnapshot> snapshots) {
        System.out.println("market-snapshot-tool");
        System.out.println();
        System.out.println("Snapshot");
        System.out.println();

        for (AssetSnapshot snapshot : snapshots) {
            System.out.printf(
                    Locale.US,
                    "%-4s %-10s price: %12s | 24h: %8s | 7d: %8s | 30d: %8s | 30d vol (ann.): %8s | mc: %10s%n",
                    snapshot.symbol(),
                    snapshot.name(),
                    SnapshotFormatter.formatPrice(snapshot.currentPrice()),
                    SnapshotFormatter.formatPercent(snapshot.change24h()),
                    SnapshotFormatter.formatPercent(snapshot.return7d()),
                    SnapshotFormatter.formatPercent(snapshot.return30d()),
                    SnapshotFormatter.formatPercent(snapshot.realizedVolatility()),
                    SnapshotFormatter.formatMarketCap(snapshot.marketCap())
            );
        }

        System.out.println();
        System.out.println("Tracked assets by market cap");
        System.out.println();

        List<AssetSnapshot> rankedSnapshots = SnapshotSortOption.MARKET_CAP.sort(snapshots);

        for (int i = 0; i < rankedSnapshots.size(); i++) {
            AssetSnapshot snapshot = rankedSnapshots.get(i);
            System.out.printf(
                    Locale.US,
                    "%d. %-4s %-10s | mc: %10s%n",
                    i + 1,
                    snapshot.symbol(),
                    snapshot.name(),
                    SnapshotFormatter.formatMarketCap(snapshot.marketCap())
            );
        }
    }
}
