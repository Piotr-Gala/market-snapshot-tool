package com.piotrgala.marketsnapshot.exception;

import java.io.IOException;

public final class InvalidMarketDataException extends IOException {

    public InvalidMarketDataException(String message) {
        super(message);
    }

    public InvalidMarketDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
