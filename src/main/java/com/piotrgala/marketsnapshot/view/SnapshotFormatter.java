package com.piotrgala.marketsnapshot.view;

import java.util.Locale;

public final class SnapshotFormatter {

    private SnapshotFormatter() {
    }

    public static String formatPrice(double value) {
        return String.format(Locale.US, "$%,.2f", value);
    }

    public static String formatPercent(Double value) {
        if (value == null) {
            return "n/a";
        }
        return String.format(Locale.US, "%,.2f%%", value);
    }

    public static String formatMarketCap(Double value) {
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
