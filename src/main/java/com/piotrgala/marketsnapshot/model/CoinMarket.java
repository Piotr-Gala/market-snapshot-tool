package com.piotrgala.marketsnapshot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinMarket(
        @JsonProperty("id") String id,
        @JsonProperty("symbol") String symbol,
        @JsonProperty("name") String name,
        @JsonProperty("current_price") Double currentPrice,
        @JsonProperty("market_cap") Double marketCap,
        @JsonProperty("price_change_percentage_24h") Double priceChangePercentage24h
) {
}
