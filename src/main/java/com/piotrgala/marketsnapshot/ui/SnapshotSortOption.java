package com.piotrgala.marketsnapshot.ui;

import com.piotrgala.marketsnapshot.model.AssetSnapshot;

import java.util.Comparator;
import java.util.List;

public enum SnapshotSortOption {
    MARKET_CAP("Market cap", Comparator.comparing(
            AssetSnapshot::marketCap,
            Comparator.nullsLast(Comparator.reverseOrder())
    )),
    CHANGE_24H("24h change", Comparator.comparing(
            AssetSnapshot::change24h,
            Comparator.nullsLast(Comparator.reverseOrder())
    )),
    RETURN_7D("7d return", Comparator.comparingDouble(AssetSnapshot::return7d).reversed()),
    RETURN_30D("30d return", Comparator.comparingDouble(AssetSnapshot::return30d).reversed()),
    VOLATILITY("30d volatility", Comparator.comparingDouble(AssetSnapshot::realizedVolatility).reversed()),
    SYMBOL("Symbol", Comparator.comparing(AssetSnapshot::symbol, String.CASE_INSENSITIVE_ORDER));

    private final String label;
    private final Comparator<AssetSnapshot> comparator;

    SnapshotSortOption(String label, Comparator<AssetSnapshot> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public List<AssetSnapshot> sort(List<AssetSnapshot> snapshots) {
        return snapshots.stream()
                .sorted(comparator)
                .toList();
    }

    @Override
    public String toString() {
        return label;
    }
}
