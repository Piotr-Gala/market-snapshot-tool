package com.piotrgala.marketsnapshot;

import com.piotrgala.marketsnapshot.client.CoinGeckoClient;
import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.CoinMarket;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class MarketSnapshotApplication {

    private MarketSnapshotApplication() {
    }

    public static void main(String[] args) {
        CoinGeckoClient client = new CoinGeckoClient();

        try {
            List<CoinMarket> markets = client.fetchMarkets(Asset.defaultAssets());
            printMarkets(markets);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Request was interrupted: " + exception.getMessage());
        } catch (IOException | IllegalStateException exception) {
            System.err.println("Failed to fetch market data: " + exception.getMessage());
        }
    }

    private static void printMarkets(List<CoinMarket> markets) {
        System.out.println("market-snapshot-tool");
        System.out.println();

        for (CoinMarket market : markets) {
            System.out.printf(
                    Locale.US,
                    "%-4s %-10s price: %12s | 24h: %8s | 7d: %8s | 30d: %8s%n",
                    market.symbol().toUpperCase(Locale.US),
                    market.name(),
                    formatPrice(market.currentPrice()),
                    formatPercent(market.priceChangePercentage24h()),
                    formatPercent(market.priceChangePercentage7d()),
                    formatPercent(market.priceChangePercentage30d())
            );
        }
    }

    private static String formatPrice(Double value) {
        if (value == null) {
            return "n/a";
        }
        return String.format(Locale.US, "$%,.2f", value);
    }

    private static String formatPercent(Double value) {
        if (value == null) {
            return "n/a";
        }
        return String.format(Locale.US, "%,.2f%%", value);
    }
}
