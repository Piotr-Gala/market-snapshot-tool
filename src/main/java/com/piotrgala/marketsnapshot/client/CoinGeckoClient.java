package com.piotrgala.marketsnapshot.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrgala.marketsnapshot.exception.InvalidMarketDataException;
import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.CoinMarket;
import com.piotrgala.marketsnapshot.model.MarketChartResponse;
import com.piotrgala.marketsnapshot.model.PricePoint;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CoinGeckoClient implements MarketDataClient {

    private static final String BASE_URL = "https://api.coingecko.com/api/v3";
    private static final String USER_AGENT = "market-snapshot-tool/0.1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CoinGeckoClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<CoinMarket> fetchMarkets(List<Asset> assets) throws IOException, InterruptedException {
        if (assets.isEmpty()) {
            return List.of();
        }

        HttpRequest request = baseRequest(buildMarketsUri(assets)).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new CoinGeckoRequestException(
                    "CoinGecko request failed with status code " + response.statusCode(),
                    response.statusCode()
            );
        }

        return objectMapper.readValue(response.body(), new TypeReference<>() {
        });
    }

    public List<PricePoint> fetchPriceHistory(Asset asset, int days) throws IOException, InterruptedException {
        HttpRequest request = baseRequest(buildMarketChartUri(asset, days)).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new CoinGeckoRequestException(
                    "CoinGecko history request failed with status code " + response.statusCode(),
                    response.statusCode()
            );
        }

        MarketChartResponse marketChart = objectMapper.readValue(response.body(), MarketChartResponse.class);
        if (marketChart.prices() == null || marketChart.prices().isEmpty()) {
            throw new InvalidMarketDataException("CoinGecko history response is missing prices for " + asset.symbol());
        }

        List<PricePoint> pricePoints = new ArrayList<>();
        for (List<Number> rawPricePoint : marketChart.prices()) {
            pricePoints.add(toPricePoint(rawPricePoint));
        }
        return pricePoints;
    }

    private URI buildMarketsUri(List<Asset> assets) {
        String ids = Asset.joinCoinGeckoIds(assets);
        String uri = BASE_URL
                + "/coins/markets"
                + "?vs_currency=usd"
                + "&ids=" + ids
                + "&sparkline=false"
                + "&precision=full";

        return URI.create(uri);
    }

    private URI buildMarketChartUri(Asset asset, int days) {
        String uri = BASE_URL
                + "/coins/" + asset.coinGeckoId() + "/market_chart"
                + "?vs_currency=usd"
                + "&days=" + days;

        return URI.create(uri);
    }

    private PricePoint toPricePoint(List<Number> rawPricePoint) throws InvalidMarketDataException {
        if (rawPricePoint.size() < 2) {
            throw new InvalidMarketDataException("CoinGecko price point is malformed");
        }

        long timestampMillis = rawPricePoint.get(0).longValue();
        double price = rawPricePoint.get(1).doubleValue();
        return new PricePoint(timestampMillis, price);
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("user-agent", USER_AGENT);
    }
}
