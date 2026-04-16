package com.piotrgala.marketsnapshot.export;

import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.service.SnapshotDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnapshotCsvExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExportSnapshotRowsToCsv() throws IOException {
        SnapshotCsvExporter exporter = new SnapshotCsvExporter();
        Path output = tempDir.resolve("snapshot.csv");

        exporter.export(
                output,
                List.of(new AssetSnapshot("BTC", "Bitcoin", 100_000.0, 2_000_000_000_000.0, 1.5, 7.0, 12.0, 45.0)),
                SnapshotDataSource.CACHED,
                Instant.parse("2026-04-16T18:00:00Z")
        );

        String csv = Files.readString(output);

        assertTrue(csv.contains("symbol,name,current_price_usd"));
        assertTrue(csv.contains("\"BTC\",\"Bitcoin\",100000.000000"));
        assertTrue(csv.contains("\"cached\""));
        assertTrue(csv.contains("\"2026-04-16T18:00:00Z\""));
    }
}
