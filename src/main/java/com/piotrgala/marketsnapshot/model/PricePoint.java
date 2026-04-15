package com.piotrgala.marketsnapshot.model;

public record PricePoint(
        long timestampMillis,
        double price
) {
}
