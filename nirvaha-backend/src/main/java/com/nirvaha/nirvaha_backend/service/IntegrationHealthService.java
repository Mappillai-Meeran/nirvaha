package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.IntegrationHealthDTO;
import com.nirvaha.nirvaha_backend.dto.IntegrationHealthDTO.IntegrationDetailDTO;
import com.nirvaha.nirvaha_backend.dto.WeatherData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntegrationHealthService {

    // Default reference coordinate: Puducherry City Center (Boulevard area)
    private static final BigDecimal PUDUCHERRY_LAT = new BigDecimal("11.9156");
    private static final BigDecimal PUDUCHERRY_LON = new BigDecimal("79.8296");
    private static final Duration FRESHNESS_THRESHOLD = Duration.ofMinutes(30);

    private final WeatherService weatherService;
    private final ImdWeatherAdapter imdWeatherAdapter;

    public IntegrationHealthService(WeatherService weatherService, ImdWeatherAdapter imdWeatherAdapter) {
        this.weatherService = weatherService;
        this.imdWeatherAdapter = imdWeatherAdapter;
    }

    public IntegrationHealthDTO evaluateSystemIntegrations() {
        List<IntegrationDetailDTO> details = new ArrayList<>();

        // 1. Open-Meteo Live Weather & Forecast
        WeatherData weather = weatherService.fetchWeatherData(PUDUCHERRY_LAT, PUDUCHERRY_LON);
        boolean weatherOk = Boolean.TRUE.equals(weather.getAvailable());
        LocalDateTime weatherObsTime = weather.getObservedAt() != null ? weather.getObservedAt() : weather.getTimestamp();
        LocalDateTime lastRetrieval = weatherService.getLastSuccessfulFetchTime();

        String weatherStatus;
        if (weatherOk) {
            boolean isStale = Boolean.TRUE.equals(weather.getIsStale()) ||
                    (lastRetrieval != null && Duration.between(lastRetrieval, LocalDateTime.now()).abs().compareTo(FRESHNESS_THRESHOLD) > 0) ||
                    (weatherObsTime != null && Duration.between(weatherObsTime, LocalDateTime.now()).abs().compareTo(FRESHNESS_THRESHOLD) > 0);
            if (isStale) {
                weatherStatus = "STALE";
            } else {
                weatherStatus = "CONNECTED";
            }
        } else {
            weatherStatus = "UNAVAILABLE";
        }

        String weatherSummary;
        if (weatherOk) {
            String tempStr = weather.getTemperature() != null ? String.format("%.1f°C", weather.getTemperature()) : "temp: unknown";
            String humStr = weather.getHumidity() != null ? String.format("%.0f%% humidity", weather.getHumidity()) : "humidity: unknown";
            String rainStr = weather.getRecentRainfall() != null ? String.format("%.2f mm current rain (15m)", weather.getRecentRainfall()) : "current rain: unknown";
            String forecastStr = weather.getForecastRainfall() != null ? String.format("%.2f mm 24h forecast rain", weather.getForecastRainfall()) : "forecast: unknown";
            String cacheTag = Boolean.TRUE.equals(weather.getIsCached()) ? " [Cached]" : " [Live]";
            String staleTag = "STALE".equals(weatherStatus) ? " [STALE]" : "";
            weatherSummary = String.format("%s, %s, %s, %s%s%s", tempStr, humStr, rainStr, forecastStr, cacheTag, staleTag);
        } else {
            weatherSummary = weather.getSourceMessage() != null ? weather.getSourceMessage() : "Weather unavailable";
        }

        String weatherError = null;
        if (!weatherOk) {
            if (weatherService.getLastErrorCategory() != null) {
                weatherError = weatherService.getLastErrorCategory();
            } else if (weather.getErrorCategory() != null) {
                weatherError = weather.getErrorCategory();
            } else if (weatherService.getLastError() != null) {
                weatherError = weatherService.getLastError();
            } else {
                weatherError = "CONNECTION_FAILED";
            }
        }

        details.add(new IntegrationDetailDTO(
                "Open-Meteo Real-Time Weather & Forecast API",
                "METEOROLOGICAL_OBSERVATION",
                weatherStatus,
                sanitize(weatherService.getBaseUrl()),
                "PUBLIC_REST_NO_AUTH",
                weatherObsTime,
                lastRetrieval,
                sanitize(weatherSummary),
                sanitize(weatherError)
        ));

        // 2. India Meteorological Department (IMD) API
        String imdStatus = imdWeatherAdapter.getImdStatus();
        String imdSummary;
        String imdError;
        if ("CONNECTED".equals(imdStatus)) {
            imdSummary = "Configured and verified with official IMD meteorological gateway.";
            imdError = null;
        } else if ("PENDING_AUTHORIZATION".equals(imdStatus)) {
            imdSummary = "Official IMD credentials configured; awaiting verification and endpoint authorization handshake.";
            imdError = "AWAITING_GATEWAY_AUTHORIZATION";
        } else {
            imdSummary = "Official IMD access requires registration at api.imd.gov.in with issued X-API-KEY and authenticated JWT bearer token.";
            imdError = "PENDING_CREDENTIALS";
        }

        details.add(new IntegrationDetailDTO(
                "India Meteorological Department (IMD) API",
                "OFFICIAL_GOVERNMENT_WEATHER",
                imdStatus,
                sanitize(imdWeatherAdapter.getEndpoint()),
                "X_API_KEY_AND_JWT_BEARER",
                null,
                null,
                sanitize(imdSummary),
                sanitize(imdError)
        ));

        // 3. Pondicherry Municipality Grievance Portal (pdymun.in)
        details.add(new IntegrationDetailDTO(
                "Pondicherry Municipality Grievance Portal (pdymun.in)",
                "MUNICIPAL_GRIEVANCE_INTAKE",
                "NOT_CONFIGURED",
                "https://pdymun.in/grievance-redressal.php",
                "CITIZEN_CHARTER_MANUAL",
                null,
                null,
                "Municipal citizen charter standards referenced offline. No public programmatic REST API; complaints routed through LGRAMS and municipal grievance desk.",
                "NO_PUBLIC_REST_API"
        ));

        // 4. Puducherry LG's Petition Redressal System (lgredressal.py.gov.in)
        details.add(new IntegrationDetailDTO(
                "Puducherry LG's Petition Redressal System (lgredressal.py.gov.in)",
                "APEX_GRIEVANCE_REDRESSAL",
                "PENDING_AUTHORIZATION",
                "https://lgredressal.py.gov.in/pgrs/",
                "RESTRICTED_INTERNAL_ASPX",
                null,
                null,
                "State petition monitoring system. Access restricted to authorized administrative personnel; programmatic intake requires official NIC MoU and verified credentials.",
                "NIC_MOU_REQUIRED"
        ));

        // 5. Puducherry Public Works Department (pwd.py.gov.in)
        details.add(new IntegrationDetailDTO(
                "Puducherry Public Works Department (pwd.py.gov.in)",
                "CIVIC_INFRASTRUCTURE_POLICY",
                "UNAVAILABLE",
                "https://pwd.py.gov.in/citizens-charter",
                "PUBLIC_CHARTER_MANUAL",
                null,
                null,
                "Official PWD portal SSL/TLS certificate expired; direct HTTP retrieval failed. Statutory citizen charter standards (15-day pothole SLA, 3-day drainage SLA) verified from official published documentation.",
                "PORTAL_TLS_EXPIRED"
        ));


        // Overall system status: CONNECTED only if live weather is functional and not stale
        String systemStatus = "CONNECTED".equals(weatherStatus) ? "CONNECTED" : "DEGRADED";

        return new IntegrationHealthDTO(systemStatus, LocalDateTime.now(), details);
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("(?i)(api[_-]?key|token|password|bearer|secret|client[_-]?secret)=[^&\\s]+", "$1=[REDACTED]");
    }
}
