package com.piotrgala.marketsnapshot.client;

import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.CoinMarket;
import com.piotrgala.marketsnapshot.model.PricePoint;

import java.io.IOException;
import java.util.List;

public interface MarketDataClient {

    List<CoinMarket> fetchMarkets(List<Asset> assets) throws IOException, InterruptedException;

    List<PricePoint> fetchPriceHistory(Asset asset, int days) throws IOException, InterruptedException;
}
