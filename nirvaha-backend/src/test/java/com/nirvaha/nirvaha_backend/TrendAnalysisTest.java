package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.TrendDirection;
import com.nirvaha.nirvaha_backend.service.TrendAnalysisService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrendAnalysisTest {

    private final TrendAnalysisService trendService = new TrendAnalysisService();

    @Test
    void testTrendDirectionClassification() {
        // Surge >= +25%
        assertThat(trendService.evaluateTrend(10, 5)).isEqualTo(TrendDirection.INCREASING);

        // Decline <= -25%
        assertThat(trendService.evaluateTrend(5, 10)).isEqualTo(TrendDirection.DECREASING);

        // Stable change
        assertThat(trendService.evaluateTrend(10, 9)).isEqualTo(TrendDirection.STABLE);

        // Insufficient data (< 2 total observations)
        assertThat(trendService.evaluateTrend(1, 0)).isEqualTo(TrendDirection.INSUFFICIENT_DATA);
    }
}
