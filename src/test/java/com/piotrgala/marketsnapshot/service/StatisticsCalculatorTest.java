package com.piotrgala.marketsnapshot.service;

import com.piotrgala.marketsnapshot.model.PricePoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsCalculatorTest {

    private final StatisticsCalculator calculator = new StatisticsCalculator();

    @Test
    void shouldCalculateReturnFromSampledDailyPrices() {
        List<PricePoint> dailyPrices = List.of(
                new PricePoint(1L, 100.0),
                new PricePoint(2L, 110.0),
                new PricePoint(3L, 121.0)
        );

        double return2d = calculator.calculateReturn(dailyPrices, 2);

        assertEquals(21.0, return2d, 0.0001);
    }

    @Test
    void shouldCalculateZeroVolatilityForConstantLogReturns() {
        List<PricePoint> dailyPrices = List.of(
                new PricePoint(1L, 100.0),
                new PricePoint(2L, 110.0),
                new PricePoint(3L, 121.0),
                new PricePoint(4L, 133.1)
        );

        double realizedVolatility = calculator.calculateAnnualizedVolatility(dailyPrices);

        assertEquals(0.0, realizedVolatility, 0.0001);
    }
}
