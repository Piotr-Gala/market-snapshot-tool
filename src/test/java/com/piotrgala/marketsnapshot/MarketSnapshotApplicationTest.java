package com.piotrgala.marketsnapshot;

import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.service.SnapshotDataSource;
import com.piotrgala.marketsnapshot.service.SnapshotResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
