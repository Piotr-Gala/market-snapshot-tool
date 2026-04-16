package com.piotrgala.marketsnapshot.export;

import com.piotrgala.marketsnapshot.model.AssetSnapshot;
import com.piotrgala.marketsnapshot.service.SnapshotDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SnapshotCsvExporter {

    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public void export(Path targetPath, List<AssetSnapshot> snapshots, SnapshotDataSource dataSource, Instant dataAsOf)
            throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(",",
                "symbol",
                "name",
                "current_price_usd",
                "change_24h_pct",
                "return_7d_pct",
                "return_30d_pct",
                "volatility_30d_ann_pct",
                "market_cap_usd",
                "data_source",
                "data_as_of_utc"
        ));

        String dataAsOfValue = EXPORT_TIMESTAMP_FORMATTER.withZone(ZoneOffset.UTC).format(dataAsOf);
        for (AssetSnapshot snapshot : snapshots) {
            lines.add(String.join(",",
                    csv(snapshot.symbol()),
                    csv(snapshot.name()),
                    number(snapshot.currentPrice()),
                    nullableNumber(snapshot.change24h()),
                    number(snapshot.return7d()),
                    number(snapshot.return30d()),
                    number(snapshot.realizedVolatility()),
                    nullableNumber(snapshot.marketCap()),
                    csv(dataSource.label()),
                    csv(dataAsOfValue)
            ));
        }

        Files.write(targetPath, lines);
    }

    private String number(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private String nullableNumber(Double value) {
        if (value == null) {
            return "";
        }
        return String.format(Locale.US, "%.6f", value);
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
