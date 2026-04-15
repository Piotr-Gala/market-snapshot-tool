package com.piotrgala.marketsnapshot;

import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.service.MarketSnapshotService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class MarketSnapshotApplication {

    private MarketSnapshotApplication() {
    }

    public static void main(String[] args) {
        MarketSnapshotService marketSnapshotService = new MarketSnapshotService();

        try {
            List<AssetSnapshot> snapshots = marketSnapshotService.getSnapshot(Asset.defaultAssets());
            printSnapshot(snapshots);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Request was interrupted: " + exception.getMessage());
        } catch (IOException | IllegalStateException exception) {
            System.err.println("Failed to fetch market data: " + exception.getMessage());
        }
    }

    private static void printSnapshot(List<AssetSnapshot> snapshots) {
        System.out.println("market-snapshot-tool");
        System.out.println();

        for (AssetSnapshot snapshot : snapshots) {
            System.out.printf(
                    Locale.US,
                    "%-4s %-10s price: %12s | 24h: %8s | 7d: %8s | 30d: %8s | 30d vol (ann.): %8s%n",
                    snapshot.symbol(),
                    snapshot.name(),
                    formatPrice(snapshot.currentPrice()),
                    formatPercent(snapshot.change24h()),
                    formatPercent(snapshot.return7d()),
                    formatPercent(snapshot.return30d()),
                    formatPercent(snapshot.realizedVolatility())
            );
        }
    }

    private static String formatPrice(double value) {
        return String.format(Locale.US, "$%,.2f", value);
    }

    private static String formatPercent(Double value) {
        if (value == null) {
            return "n/a";
        }
        return String.format(Locale.US, "%,.2f%%", value);
    }
}
