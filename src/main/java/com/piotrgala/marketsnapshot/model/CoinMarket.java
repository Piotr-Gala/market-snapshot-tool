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
        @JsonProperty("market_cap_rank") Integer marketCapRank,
        @JsonProperty("price_change_percentage_24h") Double priceChangePercentage24h,
        @JsonProperty("price_change_percentage_7d_in_currency") Double priceChangePercentage7d,
        @JsonProperty("price_change_percentage_30d_in_currency") Double priceChangePercentage30d,
        @JsonProperty("last_updated") String lastUpdated
) {
}
