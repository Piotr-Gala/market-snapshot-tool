package com.piotrgala.marketsnapshot.service;

import com.piotrgala.marketsnapshot.model.PricePoint;

import java.util.ArrayList;
import java.util.List;

public final class StatisticsCalculator {

    private static final long ONE_DAY_MILLIS = 24L * 60L * 60L * 1000L;
    private static final long COVERAGE_TOLERANCE_MILLIS = 12L * 60L * 60L * 1000L;
    private static final double CRYPTO_TRADING_DAYS = 365.0;

    public List<PricePoint> sampleDailyPrices(List<PricePoint> intradayPrices, int lookbackDays) {
        if (intradayPrices.isEmpty()) {
            throw new IllegalArgumentException("Price history cannot be empty");
        }

        validateHistoryCoverage(intradayPrices, lookbackDays);

        List<PricePoint> sampledPrices = new ArrayList<>();
        long latestTimestamp = intradayPrices.get(intradayPrices.size() - 1).timestampMillis();
        int index = 0;

        for (int daysBack = lookbackDays; daysBack >= 0; daysBack--) {
            long targetTimestamp = latestTimestamp - (daysBack * ONE_DAY_MILLIS);

            while (index + 1 < intradayPrices.size()
                    && intradayPrices.get(index + 1).timestampMillis() <= targetTimestamp) {
                index++;
            }

            sampledPrices.add(intradayPrices.get(index));
        }

        return sampledPrices;
    }

    private void validateHistoryCoverage(List<PricePoint> intradayPrices, int lookbackDays) {
        long earliestTimestamp = intradayPrices.get(0).timestampMillis();
        long latestTimestamp = intradayPrices.get(intradayPrices.size() - 1).timestampMillis();
        long requiredStartTimestamp = latestTimestamp - (lookbackDays * ONE_DAY_MILLIS);

        if (earliestTimestamp > requiredStartTimestamp + COVERAGE_TOLERANCE_MILLIS) {
            throw new IllegalArgumentException(
                    "Price history does not cover the full " + lookbackDays + " day window"
            );
        }
    }

    public double calculateReturn(List<PricePoint> dailyPrices, int days) {
        if (dailyPrices.size() <= days) {
            throw new IllegalArgumentException("Not enough daily prices to calculate " + days + "d return");
        }

        double startPrice = dailyPrices.get(dailyPrices.size() - 1 - days).price();
        double endPrice = dailyPrices.get(dailyPrices.size() - 1).price();

        return ((endPrice / startPrice) - 1.0) * 100.0;
    }

    public double calculateAnnualizedVolatility(List<PricePoint> dailyPrices) {
        if (dailyPrices.size() < 3) {
            throw new IllegalArgumentException("At least 3 daily prices are required to calculate volatility");
        }

        List<Double> logReturns = new ArrayList<>();
        for (int i = 1; i < dailyPrices.size(); i++) {
            double previousPrice = dailyPrices.get(i - 1).price();
            double currentPrice = dailyPrices.get(i).price();
            logReturns.add(Math.log(currentPrice / previousPrice));
        }

        double mean = logReturns.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElseThrow();

        double squaredDiffSum = 0.0;
        for (double logReturn : logReturns) {
            double diff = logReturn - mean;
            squaredDiffSum += diff * diff;
        }

        double variance = squaredDiffSum / (logReturns.size() - 1);
        double dailyVolatility = Math.sqrt(variance);

        return dailyVolatility * Math.sqrt(CRYPTO_TRADING_DAYS) * 100.0;
    }
}
