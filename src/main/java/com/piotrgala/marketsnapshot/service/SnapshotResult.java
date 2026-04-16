package com.piotrgala.marketsnapshot.service;

import com.piotrgala.marketsnapshot.model.AssetSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record SnapshotResult(
        List<AssetSnapshot> snapshots,
        SnapshotDataSource dataSource,
        Instant dataAsOf,
        List<String> warnings
) {
    public SnapshotResult {
        snapshots = List.copyOf(snapshots);
        dataSource = Objects.requireNonNull(dataSource);
        dataAsOf = Objects.requireNonNull(dataAsOf);
        warnings = List.copyOf(warnings);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
