package com.piotrgala.marketsnapshot.client;

import java.io.IOException;

public final class CoinGeckoRequestException extends IOException {

    private final int statusCode;

    public CoinGeckoRequestException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
