package com.piotrgala.marketsnapshot.model;

public record AssetSnapshot(
        String symbol,
        String name,
        double currentPrice,
        Double marketCap,
        Integer marketCapRank,
        Double change24h,
        double return7d,
        double return30d,
        double realizedVolatility
) {
}
