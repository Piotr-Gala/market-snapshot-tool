package com.piotrgala.marketsnapshot.client;

public final class CoinGeckoRequestException extends IllegalStateException {

    private final int statusCode;

    public CoinGeckoRequestException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
