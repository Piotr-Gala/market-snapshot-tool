package com.piotrgala.marketsnapshot;

import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.service.MarketSnapshotService;
import com.piotrgala.marketsnapshot.service.SnapshotResult;
import com.piotrgala.marketsnapshot.ui.MarketSnapshotFrame;
import com.piotrgala.marketsnapshot.view.ConsoleRenderer;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;
import java.util.Arrays;

public final class MarketSnapshotApplication {

    private MarketSnapshotApplication() {
    }

    public static void main(String[] args) {
        if (containsArg(args, "--help")) {
            printUsage();
            return;
        }

        if (containsArg(args, "--ui")) {
            launchUi();
            return;
        }

        System.exit(runCli());
    }

    private static int runCli() {
        MarketSnapshotService marketSnapshotService = new MarketSnapshotService();
        ConsoleRenderer consoleRenderer = new ConsoleRenderer();

        try {
            SnapshotResult result = marketSnapshotService.getSnapshotResult(Asset.defaultAssets());
            consoleRenderer.render(result.snapshots());
            for (String warning : result.warnings()) {
                System.err.println("Warning: " + warning);
            }
            return resolveCliExitCode(result);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Request was interrupted: " + exception.getMessage());
            return 1;
        } catch (IOException | IllegalStateException exception) {
            System.err.println("Failed to fetch market data: " + exception.getMessage());
            return 1;
        }
    }

    static int resolveCliExitCode(SnapshotResult result) {
        if (result.snapshots().isEmpty()) {
            return 1;
        }
        return 0;
    }

    private static boolean containsArg(String[] args, String expectedArg) {
        return Arrays.stream(args)
                .anyMatch(expectedArg::equalsIgnoreCase);
    }

    private static void launchUi() {
        configureLookAndFeel();
        SwingUtilities.invokeLater(() -> new MarketSnapshotFrame().setVisible(true));
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Falling back to the default look and feel is fine for this mini UI.
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  mvn exec:java");
        System.out.println("  mvn exec:java -Dexec.args=\"--ui\"");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --ui    Launch the desktop UI instead of the terminal snapshot");
        System.out.println("  --help  Show this help message");
    }
}
