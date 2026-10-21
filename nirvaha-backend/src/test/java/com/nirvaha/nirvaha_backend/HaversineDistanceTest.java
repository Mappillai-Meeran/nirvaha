package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.util.HaversineDistance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HaversineDistanceTest {

    @Test
    void shouldCalculateDistanceAccurately() {
        // Point 1: Puducherry Promenade Beach (11.9350, 79.8350)
        // Point 2: Nearby street (approx 150-200 meters away: 11.9360, 79.8360)
        BigDecimal lat1 = new BigDecimal("11.9350");
        BigDecimal lon1 = new BigDecimal("79.8350");
        BigDecimal lat2 = new BigDecimal("11.9360");
        BigDecimal lon2 = new BigDecimal("79.8360");

        double distance = HaversineDistance.calculateDistanceMeters(lat1, lon1, lat2, lon2);

        // Expected distance is approximately 155 meters
        assertThat(distance).isBetween(140.0, 170.0);
    }

    @Test
    void shouldReturnZeroForSamePoint() {
        BigDecimal lat = new BigDecimal("11.9416");
        BigDecimal lon = new BigDecimal("79.8083");

        double distance = HaversineDistance.calculateDistanceMeters(lat, lon, lat, lon);

        assertThat(distance).isEqualTo(0.0);
    }
}
