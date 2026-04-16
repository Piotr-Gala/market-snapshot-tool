package com.piotrgala.marketsnapshot.service;

import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.CoinMarket;
import com.piotrgala.marketsnapshot.model.PricePoint;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public interface SnapshotDataFetcher {

    FetchResult<List<CoinMarket>> fetchMarkets(List<Asset> assets) throws IOException, InterruptedException;

    FetchResult<List<PricePoint>> fetchPriceHistory(Asset asset, int days) throws IOException, InterruptedException;

    record FetchResult<T>(T value, SnapshotDataSource dataSource, Instant dataAsOf) {
        public FetchResult {
            value = Objects.requireNonNull(value);
            dataSource = Objects.requireNonNull(dataSource);
            dataAsOf = Objects.requireNonNull(dataAsOf);
        }
    }
}
