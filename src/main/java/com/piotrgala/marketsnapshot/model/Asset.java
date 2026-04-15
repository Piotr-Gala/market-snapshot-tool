package com.piotrgala.marketsnapshot.model;

import java.util.List;
import java.util.stream.Collectors;

public enum Asset {
    BTC("bitcoin", "btc"),
    ETH("ethereum", "eth"),
    SOL("solana", "sol"),
    XRP("ripple", "xrp"),
    BNB("binancecoin", "bnb");

    private final String coinGeckoId;
    private final String symbol;

    Asset(String coinGeckoId, String symbol) {
        this.coinGeckoId = coinGeckoId;
        this.symbol = symbol;
    }

    public String coinGeckoId() {
        return coinGeckoId;
    }

    public String symbol() {
        return symbol;
    }

    public static List<Asset> defaultAssets() {
        return List.of(BTC, ETH, SOL);
    }

    public static String joinCoinGeckoIds(List<Asset> assets) {
        return assets.stream()
                .map(Asset::coinGeckoId)
                .collect(Collectors.joining(","));
    }
}
