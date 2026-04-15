package com.piotrgala.marketsnapshot.view;

import com.piotrgala.marketsnapshot.model.AssetSnapshot;

import java.util.Comparator;
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
                    formatPrice(snapshot.currentPrice()),
                    formatPercent(snapshot.change24h()),
                    formatPercent(snapshot.return7d()),
                    formatPercent(snapshot.return30d()),
                    formatPercent(snapshot.realizedVolatility()),
                    formatMarketCap(snapshot.marketCap())
            );
        }

        System.out.println();
        System.out.println("Tracked assets by market cap");
        System.out.println();

        List<AssetSnapshot> rankedSnapshots = snapshots.stream()
                .sorted(Comparator.comparing(AssetSnapshot::marketCap,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        for (int i = 0; i < rankedSnapshots.size(); i++) {
            AssetSnapshot snapshot = rankedSnapshots.get(i);
            System.out.printf(
                    Locale.US,
                    "%d. %-4s %-10s | mc: %10s%n",
                    i + 1,
                    snapshot.symbol(),
                    snapshot.name(),
                    formatMarketCap(snapshot.marketCap())
            );
        }
    }

    private String formatPrice(double value) {
        return String.format(Locale.US, "$%,.2f", value);
    }

    private String formatPercent(Double value) {
        if (value == null) {
            return "n/a";
        }
        return String.format(Locale.US, "%,.2f%%", value);
    }

    private String formatMarketCap(Double value) {
        if (value == null) {
            return "n/a";
        }
        double absoluteValue = Math.abs(value);
        if (absoluteValue >= 1_000_000_000_000.0) {
            return String.format(Locale.US, "$%.2fT", value / 1_000_000_000_000.0);
        }
        if (absoluteValue >= 1_000_000_000.0) {
            return String.format(Locale.US, "$%.2fB", value / 1_000_000_000.0);
        }
        if (absoluteValue >= 1_000_000.0) {
            return String.format(Locale.US, "$%.2fM", value / 1_000_000.0);
        }
        return String.format(Locale.US, "$%,.2f", value);
    }

}
