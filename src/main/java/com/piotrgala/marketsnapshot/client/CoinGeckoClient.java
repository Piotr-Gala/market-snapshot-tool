package com.piotrgala.marketsnapshot.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CoinGeckoClient {

    private static final String BASE_URL = "https://api.coingecko.com/api/v3";
    private static final int MAX_ATTEMPTS = 3;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 2_200L;
    private static final String USER_AGENT = "market-snapshot-tool/0.1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String demoApiKey;
    private long lastRequestTimestampMillis;

    public CoinGeckoClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.demoApiKey = System.getenv("COINGECKO_DEMO_API_KEY");
    }

    public List<CoinMarket> fetchMarkets(List<Asset> assets) throws IOException, InterruptedException {
        if (assets.isEmpty()) {
            return List.of();
        }

        HttpRequest request = baseRequest(buildMarketsUri(assets)).GET().build();

        HttpResponse<String> response = sendWithRetry(request);

        if (response.statusCode() != 200) {
            throw new IllegalStateException(buildFailureMessage("CoinGecko request failed", response.statusCode()));
        }

        List<CoinMarket> markets = objectMapper.readValue(response.body(), new TypeReference<>() {
        });

        return sortByRequestedAssets(markets, assets);
    }

    public List<PricePoint> fetchPriceHistory(Asset asset, int days) throws IOException, InterruptedException {
        HttpRequest request = baseRequest(buildMarketChartUri(asset, days)).GET().build();

        HttpResponse<String> response = sendWithRetry(request);

        if (response.statusCode() != 200) {
            throw new IllegalStateException(buildFailureMessage("CoinGecko history request failed", response.statusCode()));
        }

        MarketChartResponse marketChart = objectMapper.readValue(response.body(), MarketChartResponse.class);
        if (marketChart.prices() == null || marketChart.prices().isEmpty()) {
            throw new IllegalStateException("CoinGecko history response is missing prices for " + asset.symbol());
        }

        return marketChart.prices().stream()
                .map(this::toPricePoint)
                .collect(Collectors.toList());
    }

    private URI buildMarketsUri(List<Asset> assets) {
        String ids = Asset.joinCoinGeckoIds(assets);
        String uri = BASE_URL
                + "/coins/markets"
                + "?vs_currency=usd"
                + "&ids=" + ids
                + "&price_change_percentage=7d,30d"
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

    private List<CoinMarket> sortByRequestedAssets(List<CoinMarket> markets, List<Asset> assets) {
        Map<String, CoinMarket> marketsById = new LinkedHashMap<>();
        for (CoinMarket market : markets) {
            marketsById.put(market.id(), market);
        }

        return assets.stream()
                .map(asset -> requireMarket(marketsById, asset))
                .toList();
    }

    private CoinMarket requireMarket(Map<String, CoinMarket> marketsById, Asset asset) {
        CoinMarket market = marketsById.get(asset.coinGeckoId());
        if (market == null) {
            throw new IllegalStateException("CoinGecko response is missing data for " + asset.symbol());
        }
        return market;
    }

    private PricePoint toPricePoint(List<Number> rawPricePoint) {
        if (rawPricePoint.size() < 2) {
            throw new IllegalStateException("CoinGecko price point is malformed");
        }

        long timestampMillis = rawPricePoint.get(0).longValue();
        double price = rawPricePoint.get(1).doubleValue();
        return new PricePoint(timestampMillis, price);
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("user-agent", USER_AGENT);

        if (demoApiKey != null && !demoApiKey.isBlank()) {
            builder.header("x-cg-demo-api-key", demoApiKey);
        }

        return builder;
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            throttleRequests();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 429 || attempt == MAX_ATTEMPTS) {
                return response;
            }

            Thread.sleep(resolveRetryDelayMillis(response, attempt));
        }

        return response;
    }

    private synchronized void throttleRequests() throws InterruptedException {
        long now = System.currentTimeMillis();
        long waitMillis = (lastRequestTimestampMillis + MIN_REQUEST_INTERVAL_MILLIS) - now;

        if (waitMillis > 0) {
            Thread.sleep(waitMillis);
        }

        lastRequestTimestampMillis = System.currentTimeMillis();
    }

    private long resolveRetryDelayMillis(HttpResponse<String> response, int attempt) {
        return response.headers()
                .firstValue("Retry-After")
                .map(this::parseRetryAfterSeconds)
                .orElse(2L * attempt * 1000L);
    }

    private String buildFailureMessage(String prefix, int statusCode) {
        if (statusCode == 429) {
            return prefix
                    + " with status code 429. CoinGecko public API rate limit was hit. "
                    + "Wait a bit and retry, or set COINGECKO_DEMO_API_KEY for more reliable runs.";
        }
        return prefix + " with status code " + statusCode;
    }

    private long parseRetryAfterSeconds(String value) {
        try {
            return Long.parseLong(value) * 1000L;
        } catch (NumberFormatException exception) {
            return 2000L;
        }
    }
}
