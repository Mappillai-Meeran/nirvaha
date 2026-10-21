package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.IntegrationHealthDTO;
import com.nirvaha.nirvaha_backend.dto.WeatherData;
import com.nirvaha.nirvaha_backend.service.ImdWeatherAdapter;
import com.nirvaha.nirvaha_backend.service.IntegrationHealthService;
import com.nirvaha.nirvaha_backend.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationHealthServiceTest {

    @Mock
    private WeatherService weatherService;

    @Mock
    private ImdWeatherAdapter imdWeatherAdapter;

    private IntegrationHealthService healthService;

    @BeforeEach
    void setUp() {
        healthService = new IntegrationHealthService(weatherService, imdWeatherAdapter);
    }

    @Test
    @DisplayName("Should report CONNECTED when live weather is fresh and functional")
    void shouldReportConnectedWhenLiveWeatherIsAvailable() {
        WeatherData mockWeather = new WeatherData(
                28.5, 0.0, 1.2, 78.0, true,
                "Open-Meteo Live Observations", LocalDateTime.now()
        );
        when(weatherService.fetchWeatherData(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(mockWeather);
        when(weatherService.getBaseUrl()).thenReturn("https://api.open-meteo.com/v1/forecast");
        when(weatherService.getLastSuccessfulFetchTime()).thenReturn(LocalDateTime.now());
        when(imdWeatherAdapter.getImdStatus()).thenReturn("NOT_CONFIGURED");
        when(imdWeatherAdapter.getEndpoint()).thenReturn("https://api.imd.gov.in/api/v1/cityforecast");

        IntegrationHealthDTO result = healthService.evaluateSystemIntegrations();

        assertNotNull(result);
        assertEquals("CONNECTED", result.getSystemStatus());
        assertNotNull(result.getEvaluatedAt());
        assertEquals(5, result.getIntegrations().size());

        // Check Open-Meteo
        IntegrationHealthDTO.IntegrationDetailDTO openMeteo = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("Open-Meteo"))
                .findFirst().orElseThrow();
        assertEquals("CONNECTED", openMeteo.getStatus());
        assertNull(openMeteo.getErrorCategory());
        assertTrue(openMeteo.getObservationSummary().contains("28.5°C"));

        // Check IMD honest unconfigured status
        IntegrationHealthDTO.IntegrationDetailDTO imd = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("IMD"))
                .findFirst().orElseThrow();
        assertEquals("NOT_CONFIGURED", imd.getStatus());
        assertEquals("PENDING_CREDENTIALS", imd.getErrorCategory());

        // Check Municipal Charter source
        IntegrationHealthDTO.IntegrationDetailDTO mun = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("Pondicherry Municipality"))
                .findFirst().orElseThrow();
        assertEquals("NOT_CONFIGURED", mun.getStatus());
        assertEquals("NO_PUBLIC_REST_API", mun.getErrorCategory());

        // Check LG Redressal
        IntegrationHealthDTO.IntegrationDetailDTO lg = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("LG's Petition Redressal"))
                .findFirst().orElseThrow();
        assertEquals("PENDING_AUTHORIZATION", lg.getStatus());
        assertEquals("NIC_MOU_REQUIRED", lg.getErrorCategory());

        // Check PWD
        IntegrationHealthDTO.IntegrationDetailDTO pwd = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("Public Works Department"))
                .findFirst().orElseThrow();
        assertEquals("UNAVAILABLE", pwd.getStatus());
        assertEquals("PORTAL_TLS_EXPIRED", pwd.getErrorCategory());
    }

    @Test
    @DisplayName("Should report DEGRADED and UNAVAILABLE when weather endpoint fails")
    void shouldReportDegradedWhenWeatherIsUnavailable() {
        WeatherData fallbackWeather = WeatherData.fallback("Service unreachable", "CONNECTION_REFUSED");
        when(weatherService.fetchWeatherData(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(fallbackWeather);
        when(weatherService.getBaseUrl()).thenReturn("https://api.open-meteo.com/v1/forecast");
        when(weatherService.getLastErrorCategory()).thenReturn("CONNECTION_REFUSED");
        when(imdWeatherAdapter.getImdStatus()).thenReturn("NOT_CONFIGURED");
        when(imdWeatherAdapter.getEndpoint()).thenReturn("https://api.imd.gov.in/api/v1/cityforecast");

        IntegrationHealthDTO result = healthService.evaluateSystemIntegrations();

        assertNotNull(result);
        assertEquals("DEGRADED", result.getSystemStatus());

        IntegrationHealthDTO.IntegrationDetailDTO openMeteo = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("Open-Meteo"))
                .findFirst().orElseThrow();
        assertEquals("UNAVAILABLE", openMeteo.getStatus());
        assertEquals("CONNECTION_REFUSED", openMeteo.getErrorCategory());
    }

    @Test
    @DisplayName("Should report STALE when observations exceed the 30-minute freshness threshold")
    void shouldReportStaleWhenObservationExceedsFreshnessWindow() {
        WeatherData mockWeather = new WeatherData(
                28.5, 0.0, 1.2, 78.0, true,
                "Open-Meteo Live Observations", LocalDateTime.now().minusMinutes(45)
        );
        mockWeather.setIsStale(true);
        when(weatherService.fetchWeatherData(any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(mockWeather);
        when(weatherService.getBaseUrl()).thenReturn("https://api.open-meteo.com/v1/forecast");
        when(weatherService.getLastSuccessfulFetchTime()).thenReturn(LocalDateTime.now().minusMinutes(45));
        when(imdWeatherAdapter.getImdStatus()).thenReturn("NOT_CONFIGURED");
        when(imdWeatherAdapter.getEndpoint()).thenReturn("https://api.imd.gov.in/api/v1/cityforecast");

        IntegrationHealthDTO result = healthService.evaluateSystemIntegrations();

        assertNotNull(result);
        assertEquals("DEGRADED", result.getSystemStatus());

        IntegrationHealthDTO.IntegrationDetailDTO openMeteo = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("Open-Meteo"))
                .findFirst().orElseThrow();
        assertEquals("STALE", openMeteo.getStatus());
        assertTrue(openMeteo.getObservationSummary().contains("[STALE]"));
    }

    @Test
    @DisplayName("Should report PENDING_AUTHORIZATION for IMD when credentials exist but lack gateway verification")
    void shouldReportPendingAuthorizationForImdWhenConfiguredWithoutActiveSession() {
        WeatherData mockWeather = new WeatherData(
                29.0, 0.0, 0.0, 70.0, true,
                "Open-Meteo Live", LocalDateTime.now()
        );
        when(weatherService.fetchWeatherData(any(), any())).thenReturn(mockWeather);
        when(weatherService.getBaseUrl()).thenReturn("https://api.open-meteo.com/v1/forecast");
        when(weatherService.getLastSuccessfulFetchTime()).thenReturn(LocalDateTime.now());
        when(imdWeatherAdapter.getImdStatus()).thenReturn("PENDING_AUTHORIZATION");
        when(imdWeatherAdapter.getEndpoint()).thenReturn("https://api.imd.gov.in/api/v1/cityforecast");

        IntegrationHealthDTO result = healthService.evaluateSystemIntegrations();

        IntegrationHealthDTO.IntegrationDetailDTO imd = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("IMD"))
                .findFirst().orElseThrow();
        assertEquals("PENDING_AUTHORIZATION", imd.getStatus());
        assertEquals("AWAITING_GATEWAY_AUTHORIZATION", imd.getErrorCategory());
    }

    @Test
    @DisplayName("Should strictly sanitize endpoints containing sensitive tokens or API keys")
    void shouldSanitizeEndpointsContainingTokensOrKeys() {
        WeatherData mockWeather = new WeatherData(
                28.0, null, null, 75.0, true,
                "Open-Meteo", LocalDateTime.now()
        );
        when(weatherService.fetchWeatherData(any(), any())).thenReturn(mockWeather);
        when(weatherService.getBaseUrl()).thenReturn("https://api.open-meteo.com/v1/forecast?api_key=SECRET12345&client_secret=TOP_SECRET");
        when(weatherService.getLastSuccessfulFetchTime()).thenReturn(LocalDateTime.now());
        when(imdWeatherAdapter.getImdStatus()).thenReturn("NOT_CONFIGURED");
        when(imdWeatherAdapter.getEndpoint()).thenReturn("https://api.imd.gov.in/api/v1/cityforecast?token=MY_BEARER_TOKEN");

        IntegrationHealthDTO result = healthService.evaluateSystemIntegrations();

        IntegrationHealthDTO.IntegrationDetailDTO openMeteo = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("Open-Meteo"))
                .findFirst().orElseThrow();
        assertFalse(openMeteo.getEndpoint().contains("SECRET12345"));
        assertTrue(openMeteo.getEndpoint().contains("api_key=[REDACTED]"));
        assertTrue(openMeteo.getEndpoint().contains("client_secret=[REDACTED]"));

        IntegrationHealthDTO.IntegrationDetailDTO imd = result.getIntegrations().stream()
                .filter(i -> i.getName().contains("IMD"))
                .findFirst().orElseThrow();
        assertFalse(imd.getEndpoint().contains("MY_BEARER_TOKEN"));
        assertTrue(imd.getEndpoint().contains("token=[REDACTED]"));
    }
}
