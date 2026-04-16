package com.piotrgala.marketsnapshot;

import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.service.SnapshotDataSource;
import com.piotrgala.marketsnapshot.service.SnapshotResult;
import com.piotrgala.marketsnapshot.view.ConsoleRenderer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketSnapshotApplicationTest {

    private static final Instant DATA_AS_OF = Instant.parse("2026-04-17T00:00:00Z");

    @Test
    void shouldReturnSuccessExitCodeWhenAtLeastOneSnapshotIsPresent() {
        SnapshotResult result = new SnapshotResult(
                List.of(new AssetSnapshot("BTC", "Bitcoin", 100_000.0, 2_000_000_000_000.0, 1.5, 7.0, 12.0, 45.0)),
                SnapshotDataSource.LIVE,
                DATA_AS_OF,
                List.of("Skipped ETH: market data unavailable.")
        );

        assertEquals(0, MarketSnapshotApplication.resolveCliExitCode(result));
    }

    @Test
    void shouldReturnFailureExitCodeWhenNoSnapshotsWereLoaded() {
        SnapshotResult result = new SnapshotResult(
                List.of(),
                SnapshotDataSource.LIVE,
                DATA_AS_OF,
                List.of("Skipped BTC: market data unavailable.")
        );

        assertEquals(1, MarketSnapshotApplication.resolveCliExitCode(result));
    }

    @Test
    void shouldNotRenderEmptySnapshotInCli() {
        SnapshotResult result = new SnapshotResult(
                List.of(),
                SnapshotDataSource.LIVE,
                DATA_AS_OF,
                List.of("Skipped BTC: market data unavailable.")
        );
        ConsoleCapture consoleCapture = captureConsole(() ->
                assertEquals(1, MarketSnapshotApplication.renderCliResult(result, new ConsoleRenderer()))
        );

        assertFalse(consoleCapture.stdout().contains("Snapshot"));
        assertTrue(consoleCapture.stderr().contains("Warning: Skipped BTC: market data unavailable."));
        assertTrue(consoleCapture.stderr().contains("Failed to fetch market data: no assets loaded."));
    }

    @Test
    void shouldRenderSnapshotWhenCliHasData() {
        SnapshotResult result = new SnapshotResult(
                List.of(new AssetSnapshot("BTC", "Bitcoin", 100_000.0, 2_000_000_000_000.0, 1.5, 7.0, 12.0, 45.0)),
                SnapshotDataSource.LIVE,
                DATA_AS_OF,
                List.of()
        );
        ConsoleCapture consoleCapture = captureConsole(() ->
                assertEquals(0, MarketSnapshotApplication.renderCliResult(result, new ConsoleRenderer()))
        );

        assertTrue(consoleCapture.stdout().contains("Snapshot"));
        assertTrue(consoleCapture.stdout().contains("BTC"));
        assertEquals("", consoleCapture.stderr());
    }

    private ConsoleCapture captureConsole(Runnable action) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(stdout));
            System.setErr(new PrintStream(stderr));
            action.run();
            return new ConsoleCapture(stdout.toString(), stderr.toString());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private record ConsoleCapture(String stdout, String stderr) {
    }
}
