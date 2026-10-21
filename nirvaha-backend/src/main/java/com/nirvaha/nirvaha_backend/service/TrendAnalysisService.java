package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.TrendDirection;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrendAnalysisService {

    public static final double INCREASING_THRESHOLD_PCT = 25.0;
    public static final double DECREASING_THRESHOLD_PCT = -25.0;
    public static final int MIN_OBSERVATIONS_FOR_TREND = 2;

    public TrendDirection evaluateTrend(int currentCount, int previousCount) {
        int total = currentCount + previousCount;
        if (total < MIN_OBSERVATIONS_FOR_TREND) {
            return TrendDirection.INSUFFICIENT_DATA;
        }

        double changePct = calculateChangePercentage(currentCount, previousCount);

        if (changePct >= INCREASING_THRESHOLD_PCT) {
            return TrendDirection.INCREASING;
        } else if (changePct <= DECREASING_THRESHOLD_PCT) {
            return TrendDirection.DECREASING;
        } else {
            return TrendDirection.STABLE;
        }
    }

    public double calculateChangePercentage(int currentCount, int previousCount) {
        if (previousCount == 0) {
            return currentCount > 0 ? (currentCount * 100.0) : 0.0;
        }
        return ((currentCount - previousCount) / (double) previousCount) * 100.0;
    }
}
