package com.piotrgala.marketsnapshot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketChartResponse(
        @JsonProperty("prices") List<List<Number>> prices
) {
}
