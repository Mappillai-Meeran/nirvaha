package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.WeatherData;
import com.nirvaha.nirvaha_backend.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private WeatherService weatherService;

    private static final BigDecimal PUDUCHERRY_LAT = new BigDecimal("11.9156");
    private static final BigDecimal PUDUCHERRY_LON = new BigDecimal("79.8296");

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService("https://api.open-meteo.com/v1/forecast", mockHttpClient);
    }

    @Test
    @DisplayName("1. Returns fallback when coordinates are null without invoking network")
    void shouldReturnFallbackWhenCoordinatesAreNull() throws IOException, InterruptedException {
        WeatherData result = weatherService.fetchWeatherData(null, null);

        assertNotNull(result);
        assertFalse(result.getAvailable());
        assertNull(result.getTemperature());
        assertNull(result.getHumidity());
        assertNull(result.getRecentRainfall());
        assertNull(result.getForecastRainfall());
        assertEquals("INVALID_COORDINATES", result.getErrorCategory());
        assertTrue(result.getSourceMessage().contains("missing coordinates"));

        verifyNoInteractions(mockHttpClient);
    }

    @Test
    @DisplayName("2. Parses fresh Open-Meteo observation and forward forecast correctly")
    void shouldParseFreshObservationAndForecastCorrectly() throws Exception {
        LocalDateTime freshTime = LocalDateTime.now().minusMinutes(5).withSecond(0).withNano(0);
        String timeStr = freshTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        String jsonPayload = String.format(java.util.Locale.ROOT, """
        {
          "latitude": 11.9156,
          "longitude": 79.8296,
          "current": {
            "time": "%s",
            "interval": 900,
            "temperature_2m": 31.4,
            "relative_humidity_2m": 76,
            "rain": 2.5
          },
          "hourly": {
            "time": ["%s", "%s", "%s"],
            "rain": [0.5, 3.2, 1.8]
          }
        }
        """, timeStr, timeStr, timeStr, timeStr);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        WeatherData data = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);

        assertNotNull(data);
        assertTrue(data.getAvailable());
        assertEquals(31.4, data.getTemperature());
        assertEquals(76.0, data.getHumidity());
        assertEquals(2.5, data.getRecentRainfall());
        assertEquals(5.5, data.getForecastRainfall()); // 0.5 + 3.2 + 1.8
        assertEquals("Open-Meteo", data.getProvider());
        assertEquals("°C", data.getTemperatureUnit());
        assertEquals("mm", data.getRainfallUnit());
        assertEquals("%", data.getHumidityUnit());
        assertFalse(data.getIsCached());
        assertFalse(data.getIsStale());
        assertNotNull(data.getObservedAt());
        assertNotNull(data.getRetrievedAt());
        assertEquals(freshTime, data.getObservedAt());
    }

    @Test
    @DisplayName("3. Preserves distinct observation and retrieval timestamps on cached responses")
    void shouldPreserveTimestampsAcrossCacheHits() throws Exception {
        String jsonPayload = """
        {
          "current": {
            "time": "2026-10-15T11:45",
            "temperature_2m": 29.8,
            "relative_humidity_2m": 80,
            "rain": 0.0
          },
          "hourly": {
            "time": ["2026-10-15T11:00"],
            "rain": [0.0]
          }
        }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // First call: Fresh fetch
        WeatherData firstFetch = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);
        assertFalse(firstFetch.getIsCached());
        LocalDateTime initialRetrievalTime = firstFetch.getRetrievedAt();
        LocalDateTime initialObservedTime = firstFetch.getObservedAt();

        // Second call: Cached hit within TTL
        WeatherData cachedFetch = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);
        assertTrue(cachedFetch.getIsCached());
        assertEquals(initialObservedTime, cachedFetch.getObservedAt());
        assertEquals(initialRetrievalTime, cachedFetch.getRetrievedAt());
        assertEquals(firstFetch.getTemperature(), cachedFetch.getTemperature());

        // Verify HTTP client was called exactly once
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    @DisplayName("4. Missing rainfall measurements remain NULL rather than silently becoming 0.0")
    void missingMeasurementsShouldRemainNullNotZero() throws Exception {
        String jsonPayload = """
        {
          "current": {
            "time": "2026-10-15T12:00",
            "temperature_2m": 30.0,
            "relative_humidity_2m": 72
          },
          "hourly": {
            "time": []
          }
        }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        WeatherData data = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);

        assertTrue(data.getAvailable());
        assertEquals(30.0, data.getTemperature());
        assertEquals(72.0, data.getHumidity());
        // CRITICAL CHECK: Missing measurements remain null!
        assertNull(data.getRecentRainfall(), "Missing current rain must remain null, never silently zero");
        assertNull(data.getForecastRainfall(), "Missing hourly forecast must remain null, never silently zero");
    }

    @Test
    @DisplayName("5. HTTP 429 Rate Limit produces controlled RATE_LIMITED failure state")
    void shouldHandleRateLimitingGracefully() throws Exception {
        when(mockResponse.statusCode()).thenReturn(429);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        WeatherData data = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);

        assertFalse(data.getAvailable());
        assertEquals("RATE_LIMITED", data.getErrorCategory());
        assertEquals("RATE_LIMITED", weatherService.getLastErrorCategory());
        assertTrue(data.getSourceMessage().contains("rate limit exceeded"));
        assertNull(data.getTemperature());
        assertNull(data.getRecentRainfall());
    }

    @Test
    @DisplayName("6. HTTP 503 Provider Outage produces controlled PROVIDER_UNAVAILABLE failure state")
    void shouldHandleProviderOutageGracefully() throws Exception {
        when(mockResponse.statusCode()).thenReturn(503);
        when(mockResponse.body()).thenReturn("Service Unavailable");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        WeatherData data = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);

        assertFalse(data.getAvailable());
        assertEquals("PROVIDER_UNAVAILABLE", data.getErrorCategory());
        assertEquals("PROVIDER_UNAVAILABLE", weatherService.getLastErrorCategory());
        assertTrue(data.getSourceMessage().contains("service unavailable"));
    }

    @Test
    @DisplayName("7. Network Connect Timeout produces controlled TIMEOUT failure state")
    void shouldHandleTimeoutGracefully() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpConnectTimeoutException("Connection timed out after 3000ms"));

        WeatherData data = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);

        assertFalse(data.getAvailable());
        assertEquals("TIMEOUT", data.getErrorCategory());
        assertEquals("TIMEOUT", weatherService.getLastErrorCategory());
        assertTrue(data.getSourceMessage().contains("timed out"));
    }

    @Test
    @DisplayName("8. Connection Refused produces controlled CONNECTION_REFUSED failure state")
    void shouldHandleConnectionRefusedGracefully() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new ConnectException("Connection refused to api.open-meteo.com"));

        WeatherData data = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);

        assertFalse(data.getAvailable());
        assertEquals("CONNECTION_REFUSED", data.getErrorCategory());
        assertEquals("CONNECTION_REFUSED", weatherService.getLastErrorCategory());
        assertTrue(data.getSourceMessage().contains("Unable to connect"));
    }

    @Test
    @DisplayName("9. Malformed JSON payload produces controlled MALFORMED_PAYLOAD failure state")
    void shouldHandleMalformedJsonGracefully() throws Exception {
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("<html><body>502 Bad Gateway Nginx</body></html>");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        WeatherData data = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);

        assertFalse(data.getAvailable());
        assertEquals("MALFORMED_PAYLOAD", data.getErrorCategory());
        assertEquals("MALFORMED_PAYLOAD", weatherService.getLastErrorCategory());
    }

    @Test
    @DisplayName("10. Missing vital parameters produces controlled INCOMPLETE_DATA failure state")
    void shouldHandleMissingVitalMetricsGracefully() throws Exception {
        // Missing temperature_2m and relative_humidity_2m
        String incompleteJson = "{\"current\":{\"time\":\"2026-10-15T12:00\",\"rain\":0.0}}";

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(incompleteJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        WeatherData data = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);

        assertFalse(data.getAvailable());
        assertEquals("INCOMPLETE_DATA", data.getErrorCategory());
        assertEquals("INCOMPLETE_DATA", weatherService.getLastErrorCategory());
        assertTrue(data.getSourceMessage().contains("Missing vital meteorological metrics"));
    }

    @Test
    @DisplayName("11. Stale observations older than 30 minutes are explicitly flagged isStale=true")
    void shouldIdentifyStaleObservationCorrectly() throws Exception {
        LocalDateTime staleTime = LocalDateTime.now().minusHours(5).withSecond(0).withNano(0);
        String timeStr = staleTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        // Observation from 5 hours ago
        String staleJson = String.format(java.util.Locale.ROOT, """
        {
          "current": {
            "time": "%s",
            "temperature_2m": 27.5,
            "relative_humidity_2m": 85,
            "rain": 0.0
          },
          "hourly": {
            "time": ["%s"],
            "rain": [0.0]
          }
        }
        """, timeStr, timeStr);

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(staleJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        WeatherData data = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);

        assertTrue(data.getAvailable());
        assertTrue(data.getIsStale(), "Observation from 5 hours ago must be explicitly flagged as stale");
    }

    @Test
    @DisplayName("12. Parses historical ERA5 precipitation response correctly")
    void shouldParseHistoricalPrecipitationCorrectly() throws Exception {
        String historicalJson = """
        {
          "latitude": 11.9156,
          "longitude": 79.8296,
          "hourly": {
            "time": ["2024-11-20T00:00", "2024-11-20T01:00", "2024-11-20T02:00"],
            "precipitation": [0.0, 12.4, 6.2]
          }
        }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(historicalJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData data =
                weatherService.fetchHistoricalPrecipitation(PUDUCHERRY_LAT, PUDUCHERRY_LON, "2024-11-20", "2024-11-20");

        assertNotNull(data);
        assertTrue(data.getAvailable());
        assertEquals(3, data.getHourlyData().size());
        assertEquals(18.6, data.getTotalPrecipitationMm());
        assertEquals(12.4, data.getMaxHourlyRateMm());
        assertEquals("Open-Meteo Historical Weather Archive", data.getProvider());
        assertEquals("Creative Commons Attribution 4.0 International (CC BY 4.0)", data.getLicense());
        assertEquals("OFFICIAL_EXTERNAL_DATA", data.getProvenanceClass());
        assertFalse(data.getIsCached());
    }

    @Test
    @DisplayName("13. Handles provider failure gracefully for historical precipitation")
    void shouldHandleHistoricalPrecipitationFailureGracefully() throws Exception {
        when(mockResponse.statusCode()).thenReturn(500);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData data =
                weatherService.fetchHistoricalPrecipitation(PUDUCHERRY_LAT, PUDUCHERRY_LON, "2024-11-20", "2024-11-21");

        assertNotNull(data);
        assertFalse(data.getAvailable());
        assertEquals("PROVIDER_ERROR", data.getErrorCategory());
        assertTrue(data.getHourlyData().isEmpty());
    }

    @Test
    @DisplayName("14. Preserves null precipitation values as unknown and reports partial coverage")
    void shouldPreserveNullPrecipitationAndDistinguishFromZero() throws Exception {
        String jsonPayload = """
        {
          "latitude": 11.9156,
          "longitude": 79.8296,
          "hourly": {
            "time": ["2024-11-20T00:00", "2024-11-20T01:00", "2024-11-20T02:00", "2024-11-20T03:00"],
            "precipitation": [0.0, null, 5.2, null]
          }
        }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData data =
                weatherService.fetchHistoricalPrecipitation(PUDUCHERRY_LAT, PUDUCHERRY_LON, "2024-11-20", "2024-11-20");

        assertNotNull(data);
        assertTrue(data.getAvailable());
        assertEquals(4, data.getHourlyData().size());
        assertEquals(0.0, data.getHourlyData().get(0).getPrecipitationMm(), "Actual zero precipitation must be preserved as 0.0");
        assertNull(data.getHourlyData().get(1).getPrecipitationMm(), "Missing hour must remain null, never converted to 0.0");
        assertEquals(5.2, data.getHourlyData().get(2).getPrecipitationMm());
        assertNull(data.getHourlyData().get(3).getPrecipitationMm(), "Unassimilated hour must remain null");

        assertEquals(5.2, data.getTotalPrecipitationMm(), "Cumulative total must sum only available hours");
        assertEquals(5.2, data.getMaxHourlyRateMm());
        assertEquals(4, data.getTotalHoursCount());
        assertEquals(2, data.getAvailableHoursCount());
        assertEquals(2, data.getMissingHoursCount());
        assertFalse(data.getIsCompleteCoverage());
        assertEquals(50.0, data.getCoveragePercentage());
    }

    @Test
    @DisplayName("15. Distinguishes fully dry period (0.0mm) with 100% complete coverage")
    void shouldHandleActualZeroRainfallAcrossAllHours() throws Exception {
        String jsonPayload = """
        {
          "latitude": 11.9156,
          "longitude": 79.8296,
          "hourly": {
            "time": ["2024-11-20T00:00", "2024-11-20T01:00", "2024-11-20T02:00"],
            "precipitation": [0.0, 0.0, 0.0]
          }
        }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData data =
                weatherService.fetchHistoricalPrecipitation(PUDUCHERRY_LAT, PUDUCHERRY_LON, "2024-11-20", "2024-11-20");

        assertNotNull(data);
        assertTrue(data.getAvailable());
        assertEquals(0.0, data.getTotalPrecipitationMm());
        assertEquals(0.0, data.getMaxHourlyRateMm());
        assertEquals(3, data.getAvailableHoursCount());
        assertEquals(0, data.getMissingHoursCount());
        assertTrue(data.getIsCompleteCoverage());
        assertEquals(100.0, data.getCoveragePercentage());
    }

    @Test
    @DisplayName("16. Leaves cumulative total as null when all hours are missing/unassimilated")
    void shouldLeaveCumulativeTotalAsNullWhenAllHoursAreMissing() throws Exception {
        String jsonPayload = """
        {
          "latitude": 11.9156,
          "longitude": 79.8296,
          "hourly": {
            "time": ["2024-11-20T00:00", "2024-11-20T01:00"],
            "precipitation": [null, null]
          }
        }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData data =
                weatherService.fetchHistoricalPrecipitation(PUDUCHERRY_LAT, PUDUCHERRY_LON, "2024-11-20", "2024-11-20");

        assertNotNull(data);
        assertTrue(data.getAvailable());
        assertNull(data.getTotalPrecipitationMm(), "Cumulative total must remain null when zero hours are available");
        assertNull(data.getMaxHourlyRateMm());
        assertEquals(0, data.getAvailableHoursCount());
        assertEquals(2, data.getMissingHoursCount());
        assertFalse(data.getIsCompleteCoverage());
        assertEquals(0.0, data.getCoveragePercentage());
    }

    @Test
    @DisplayName("17. Handles malformed historical response payload gracefully")
    void shouldHandleMalformedHistoricalResponsePayload() throws Exception {
        String malformedJson = """
        {
          "latitude": 11.9156,
          "longitude": 79.8296,
          "hourly": {
            "time": "not_an_array",
            "precipitation": 12.5
          }
        }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(malformedJson);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData data =
                weatherService.fetchHistoricalPrecipitation(PUDUCHERRY_LAT, PUDUCHERRY_LON, "2024-11-20", "2024-11-20");

        assertNotNull(data);
        assertFalse(data.getAvailable());
        assertEquals("MALFORMED_PAYLOAD", data.getErrorCategory());
    }

    @Test
    @DisplayName("18. Handles empty hourly data arrays safely without false complete coverage")
    void shouldHandleEmptyHourlyDataArraysSafely() throws Exception {
        String jsonPayload = """
        {
          "latitude": 11.9156,
          "longitude": 79.8296,
          "hourly": {
            "time": [],
            "precipitation": []
          }
        }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData data =
                weatherService.fetchHistoricalPrecipitation(PUDUCHERRY_LAT, PUDUCHERRY_LON, "2024-11-20", "2024-11-20");

        assertNotNull(data);
        assertTrue(data.getAvailable());
        assertTrue(data.getHourlyData().isEmpty());
        assertEquals(0, data.getTotalHoursCount());
        assertEquals(0, data.getAvailableHoursCount());
        assertEquals(0, data.getMissingHoursCount());
        assertFalse(data.getIsCompleteCoverage(), "Empty interval data must not be marked as complete coverage");
        assertEquals(0.0, data.getCoveragePercentage());
        assertNull(data.getTotalPrecipitationMm());
        assertNull(data.getMaxHourlyRateMm());
    }

    @Test
    @DisplayName("19. Validates complete period with all non-null positive observations")
    void shouldHandleCompletePeriodWithAllValidObservations() throws Exception {
        String jsonPayload = """
        {
          "latitude": 11.9156,
          "longitude": 79.8296,
          "hourly": {
            "time": ["2024-11-20T00:00", "2024-11-20T01:00", "2024-11-20T02:00"],
            "precipitation": [2.4, 6.8, 1.2]
          }
        }
        """;

        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonPayload);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData data =
                weatherService.fetchHistoricalPrecipitation(PUDUCHERRY_LAT, PUDUCHERRY_LON, "2024-11-20", "2024-11-20");

        assertNotNull(data);
        assertTrue(data.getAvailable());
        assertEquals(3, data.getHourlyData().size());
        assertEquals(3, data.getTotalHoursCount());
        assertEquals(3, data.getAvailableHoursCount());
        assertEquals(0, data.getMissingHoursCount());
        assertTrue(data.getIsCompleteCoverage());
        assertEquals(100.0, data.getCoveragePercentage());
        assertEquals(10.4, data.getTotalPrecipitationMm());
        assertEquals(6.8, data.getMaxHourlyRateMm());
    }
}
