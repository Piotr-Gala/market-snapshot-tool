package com.piotrgala.marketsnapshot.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.piotrgala.marketsnapshot.model.Asset;
import com.piotrgala.marketsnapshot.model.CoinMarket;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CoinGeckoClient {

    private static final String BASE_URL = "https://api.coingecko.com/api/v3";

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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(buildMarketsUri(assets))
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("CoinGecko request failed with status code " + response.statusCode());
        }

        List<CoinMarket> markets = objectMapper.readValue(response.body(), new TypeReference<>() {
        });

        return sortByRequestedAssets(markets, assets);
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
}
