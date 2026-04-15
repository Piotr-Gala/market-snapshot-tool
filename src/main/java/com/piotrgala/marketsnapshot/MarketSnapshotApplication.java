package com.piotrgala.marketsnapshot;

import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.service.MarketSnapshotService;
import com.piotrgala.marketsnapshot.view.ConsoleRenderer;

import java.io.IOException;

public final class MarketSnapshotApplication {

    private MarketSnapshotApplication() {
    }

    public static void main(String[] args) {
        System.exit(run());
    }

    private static int run() {
        MarketSnapshotService marketSnapshotService = new MarketSnapshotService();
        ConsoleRenderer consoleRenderer = new ConsoleRenderer();

        try {
            consoleRenderer.render(marketSnapshotService.getSnapshot(Asset.defaultAssets()));
            return 0;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Request was interrupted: " + exception.getMessage());
            return 1;
        } catch (IOException | IllegalStateException exception) {
            System.err.println("Failed to fetch market data: " + exception.getMessage());
            return 1;
        }
    }
}
