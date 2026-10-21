package com.nirvaha.nirvaha_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirvaha.nirvaha_backend.dto.WeatherData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final int TIMEOUT_MS = 5000;
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration FRESHNESS_THRESHOLD = Duration.ofMinutes(30);

    private final String baseUrl;
    private final String archiveBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> weatherCache = new ConcurrentHashMap<>();
    private final Map<String, HistoricalCacheEntry> historicalCache = new ConcurrentHashMap<>();

    private volatile LocalDateTime lastSuccessfulFetchTime;
    private volatile LocalDateTime lastObservationTime;
    private volatile String lastError;
    private volatile String lastErrorCategory;

    @org.springframework.beans.factory.annotation.Autowired
    public WeatherService(
            @Value("${nirvaha.weather.open-meteo.base-url:https://api.open-meteo.com/v1/forecast}") String baseUrl,
            @Value("${nirvaha.weather.open-meteo.archive-url:https://archive-api.open-meteo.com/v1/archive}") String archiveBaseUrl) {
        this(baseUrl, archiveBaseUrl, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(TIMEOUT_MS))
                .build());
    }

    public WeatherService(String baseUrl, String archiveBaseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.archiveBaseUrl = archiveBaseUrl;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    public WeatherService(String baseUrl, HttpClient httpClient) {
        this(baseUrl, "https://archive-api.open-meteo.com/v1/archive", httpClient);
    }


    public WeatherData fetchWeatherData(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return WeatherData.fallback("Weather data unavailable (missing coordinates); risk calculated from civic history and spatial signals.", "INVALID_COORDINATES");
        }

        String cacheKey = String.format(Locale.ROOT, "%.2f,%.2f", latitude.doubleValue(), longitude.doubleValue());
        CacheEntry cached = weatherCache.get(cacheKey);
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) {
            return copyWithCachedFlag(cached.data, true);
        }

        try {
            String uriString = String.format(Locale.ROOT,
                    "%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,rain&hourly=rain&forecast_days=2&timezone=auto",
                    baseUrl, latitude.doubleValue(), longitude.doubleValue());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uriString))
                    .timeout(Duration.ofMillis(TIMEOUT_MS))
                    .header("User-Agent", "Nirvaha-Civic-Platform/1.0 (Puducherry UT)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                LocalDateTime retrievedAt = LocalDateTime.now();
                WeatherData data = parseOpenMeteoResponse(response.body(), latitude, longitude, retrievedAt);
                if (Boolean.TRUE.equals(data.getAvailable())) {
                    weatherCache.put(cacheKey, new CacheEntry(data, Instant.now().plus(CACHE_TTL)));
                    this.lastSuccessfulFetchTime = retrievedAt;
                    this.lastObservationTime = data.getObservedAt();
                    this.lastError = null;
                    this.lastErrorCategory = null;
                }
                return data;
            } else if (response.statusCode() == 429) {
                this.lastError = "HTTP 429: Rate limit exceeded";
                this.lastErrorCategory = "RATE_LIMITED";
                log.warn("Open-Meteo rate limit reached for {}, {}", latitude, longitude);
                return WeatherData.fallback("Weather provider rate limit exceeded; risk calculated from civic history and spatial signals.", "RATE_LIMITED");
            } else if (response.statusCode() >= 500) {
                this.lastError = "HTTP " + response.statusCode() + ": Provider service unavailable";
                this.lastErrorCategory = "PROVIDER_UNAVAILABLE";
                log.warn("Open-Meteo upstream 5xx error {}: {}", response.statusCode(), response.body());
                return WeatherData.fallback("Weather provider service unavailable (HTTP " + response.statusCode() + "); risk calculated from civic history and spatial signals.", "PROVIDER_UNAVAILABLE");
            } else {
                this.lastError = "HTTP " + response.statusCode();
                this.lastErrorCategory = "PROVIDER_ERROR";
                return WeatherData.fallback("Weather data unavailable (HTTP " + response.statusCode() + "); risk calculated from civic history and spatial signals.", "PROVIDER_ERROR");
            }
        } catch (HttpConnectTimeoutException e) {
            log.warn("Connection timeout contacting Open-Meteo for {}, {}: {}", latitude, longitude, e.getMessage());
            this.lastError = "Connection timeout: " + e.getMessage();
            this.lastErrorCategory = "TIMEOUT";
            return WeatherData.fallback("Weather provider connection timed out; risk calculated from civic history and spatial signals.", "TIMEOUT");
        } catch (ConnectException e) {
            log.warn("Connection refused contacting Open-Meteo for {}, {}: {}", latitude, longitude, e.getMessage());
            this.lastError = "Connection refused: " + e.getMessage();
            this.lastErrorCategory = "CONNECTION_REFUSED";
            return WeatherData.fallback("Unable to connect to weather provider; risk calculated from civic history and spatial signals.", "CONNECTION_REFUSED");
        } catch (Exception e) {
            if (e instanceof TimeoutException || (e.getCause() != null && e.getCause() instanceof TimeoutException)) {
                this.lastError = "Timeout: " + e.getMessage();
                this.lastErrorCategory = "TIMEOUT";
                return WeatherData.fallback("Weather provider request timed out; risk calculated from civic history and spatial signals.", "TIMEOUT");
            }
            log.warn("Failed to fetch weather observations from Open-Meteo for {}, {}: {}", latitude, longitude, e.getMessage());
            this.lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            this.lastErrorCategory = "UNAVAILABLE";
            return WeatherData.fallback("Weather data unavailable; risk calculated from civic history and spatial signals.", "UNAVAILABLE");
        }
    }

    private WeatherData parseOpenMeteoResponse(String jsonBody, BigDecimal lat, BigDecimal lon, LocalDateTime retrievedAt) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode current = root.path("current");

            if (current.isMissingNode()) {
                this.lastError = "Incomplete weather payload: current node missing";
                this.lastErrorCategory = "INCOMPLETE_DATA";
                return WeatherData.fallback("Incomplete weather payload; risk calculated from civic history.", "INCOMPLETE_DATA");
            }

            Double temp = current.hasNonNull("temperature_2m") ? current.get("temperature_2m").asDouble() : null;
            Double humidity = current.hasNonNull("relative_humidity_2m") ? current.get("relative_humidity_2m").asDouble() : null;
            Double currentRain = current.hasNonNull("rain") ? current.get("rain").asDouble() : null;

            if (temp == null || humidity == null) {
                this.lastError = "Incomplete weather payload: vital metrics missing";
                this.lastErrorCategory = "INCOMPLETE_DATA";
                return WeatherData.fallback("Missing vital meteorological metrics from provider.", "INCOMPLETE_DATA");
            }

            // Parse observation timestamp from provider
            LocalDateTime observedAt = retrievedAt;
            String timeStr = current.path("time").asText(null);
            if (timeStr != null && !timeStr.isBlank()) {
                try {
                    observedAt = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (DateTimeParseException ignored) {
                    observedAt = retrievedAt;
                }
            }

            // Find current hour index in hourly.time array so forecast does not include past rainfall
            int currentHourIndex = 0;
            JsonNode hourlyTime = root.path("hourly").path("time");
            if (hourlyTime.isArray() && timeStr != null) {
                String currentHourPrefix = timeStr.length() >= 13 ? timeStr.substring(0, 13) : timeStr;
                for (int i = 0; i < hourlyTime.size(); i++) {
                    String hTime = hourlyTime.get(i).asText("");
                    if (hTime.startsWith(currentHourPrefix)) {
                        currentHourIndex = i;
                        break;
                    }
                }
            }

            // Real 24h forward forecast sum starting strictly from currentHourIndex
            Double forecastRain = null;
            JsonNode hourlyRain = root.path("hourly").path("rain");
            if (hourlyRain.isArray() && hourlyRain.size() > 0) {
                int start = currentHourIndex;
                int end = Math.min(start + 24, hourlyRain.size());
                double sum = 0.0;
                boolean hasAnyRainReading = false;
                for (int i = start; i < end; i++) {
                    JsonNode val = hourlyRain.get(i);
                    if (val != null && !val.isNull()) {
                        sum += val.asDouble();
                        hasAnyRainReading = true;
                    }
                }
                if (hasAnyRainReading) {
                    forecastRain = Math.round(sum * 100.0) / 100.0;
                }
            }

            WeatherData data = new WeatherData();
            data.setTemperature(temp);
            data.setRecentRainfall(currentRain); // Remains null if missing from provider
            data.setForecastRainfall(forecastRain); // Remains null if missing from provider
            data.setHumidity(humidity);
            data.setAvailable(true);
            data.setSourceMessage(String.format(Locale.ROOT,
                    "Open-Meteo Live Observations (Puducherry %.4f, %.4f)", lat.doubleValue(), lon.doubleValue()));
            data.setObservedAt(observedAt);
            data.setRetrievedAt(retrievedAt);
            data.setAccumulationPeriod("15_MIN_INTERVAL");
            data.setIsCached(false);

            boolean isStale = (observedAt != null && Duration.between(observedAt, retrievedAt).abs().compareTo(FRESHNESS_THRESHOLD) > 0);
            data.setIsStale(isStale);
            data.setProvider("Open-Meteo");
            data.setTemperatureUnit("°C");
            data.setRainfallUnit("mm");
            data.setHumidityUnit("%");
            data.setErrorCategory(null);

            return data;
        } catch (Exception e) {
            log.error("Failed to parse Open-Meteo response JSON: {}", e.getMessage());
            this.lastError = "JSON parse error: " + e.getMessage();
            this.lastErrorCategory = "MALFORMED_PAYLOAD";
            return WeatherData.fallback("Weather data parsing error; risk calculated from civic history and spatial signals.", "MALFORMED_PAYLOAD");
        }
    }

    private WeatherData copyWithCachedFlag(WeatherData original, boolean cached) {
        WeatherData copy = new WeatherData();
        copy.setTemperature(original.getTemperature());
        copy.setRecentRainfall(original.getRecentRainfall());
        copy.setForecastRainfall(original.getForecastRainfall());
        copy.setHumidity(original.getHumidity());
        copy.setAvailable(original.getAvailable());
        copy.setSourceMessage(original.getSourceMessage());
        copy.setObservedAt(original.getObservedAt());
        copy.setRetrievedAt(original.getRetrievedAt());
        copy.setAccumulationPeriod(original.getAccumulationPeriod());
        copy.setIsCached(cached);

        boolean isStale = (original.getObservedAt() != null && Duration.between(original.getObservedAt(), LocalDateTime.now()).abs().compareTo(FRESHNESS_THRESHOLD) > 0)
                || (original.getRetrievedAt() != null && Duration.between(original.getRetrievedAt(), LocalDateTime.now()).abs().compareTo(FRESHNESS_THRESHOLD) > 0);
        copy.setIsStale(isStale);
        copy.setProvider(original.getProvider());
        copy.setTemperatureUnit(original.getTemperatureUnit());
        copy.setRainfallUnit(original.getRainfallUnit());
        copy.setHumidityUnit(original.getHumidityUnit());
        copy.setErrorCategory(original.getErrorCategory());
        return copy;
    }

    public LocalDateTime getLastSuccessfulFetchTime() {
        return lastSuccessfulFetchTime;
    }

    public LocalDateTime getLastObservationTime() {
        return lastObservationTime;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLastErrorCategory() {
        return lastErrorCategory;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void clearCache() {
        weatherCache.clear();
        historicalCache.clear();
    }

    public HistoricalPrecipitationData fetchHistoricalPrecipitation(BigDecimal latitude, BigDecimal longitude, String startDateStr, String endDateStr) {
        LocalDate start = null;
        LocalDate end = null;
        try {
            if (startDateStr != null && !startDateStr.isBlank()) {
                start = LocalDate.parse(startDateStr.trim());
            }
        } catch (Exception e) {
            log.warn("Invalid start date string format: {}", startDateStr);
        }
        try {
            if (endDateStr != null && !endDateStr.isBlank()) {
                end = LocalDate.parse(endDateStr.trim());
            }
        } catch (Exception e) {
            log.warn("Invalid end date string format: {}", endDateStr);
        }
        return fetchHistoricalPrecipitation(latitude, longitude, start, end);
    }

    public HistoricalPrecipitationData fetchHistoricalPrecipitation(BigDecimal latitude, BigDecimal longitude, LocalDate startDate, LocalDate endDate) {
        if (latitude == null || longitude == null) {
            latitude = new BigDecimal("11.9156");
            longitude = new BigDecimal("79.8296");
        }
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(7);
        }
        if (endDate == null) {
            endDate = LocalDate.now().minusDays(1);
        }
        if (endDate.isBefore(startDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        String cacheKey = String.format(Locale.ROOT, "%.4f:%.4f:%s:%s", latitude.doubleValue(), longitude.doubleValue(), startDate, endDate);
        HistoricalCacheEntry cached = historicalCache.get(cacheKey);
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) {
            return cached.data;
        }

        String url = String.format(Locale.ROOT,
                "%s?latitude=%.4f&longitude=%.4f&start_date=%s&end_date=%s&hourly=precipitation&timezone=Asia%%2FKolkata",
                archiveBaseUrl, latitude.doubleValue(), longitude.doubleValue(), startDate, endDate);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(TIMEOUT_MS))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                HistoricalPrecipitationData data = parseHistoricalResponse(response.body(), latitude, longitude, startDate, endDate);
                if (Boolean.TRUE.equals(data.getAvailable())) {
                    historicalCache.put(cacheKey, new HistoricalCacheEntry(data, Instant.now().plus(Duration.ofHours(6))));
                }
                return data;
            } else if (response.statusCode() == 429) {
                return HistoricalPrecipitationData.fallback("HTTP 429: Open-Meteo Archive API rate limit exceeded.", "RATE_LIMITED");
            } else if (response.statusCode() >= 500) {
                return HistoricalPrecipitationData.fallback("Upstream HTTP error " + response.statusCode(), "PROVIDER_ERROR");
            } else {
                return HistoricalPrecipitationData.fallback("Upstream HTTP error " + response.statusCode(), "UPSTREAM_ERROR");
            }
        } catch (HttpConnectTimeoutException e) {
            return HistoricalPrecipitationData.fallback("Connection timeout contacting Open-Meteo Archive: " + e.getMessage(), "CONNECTION_TIMEOUT");
        } catch (ConnectException e) {
            return HistoricalPrecipitationData.fallback("Connection refused to Open-Meteo Archive: " + e.getMessage(), "CONNECTION_REFUSED");
        } catch (Exception e) {
            return HistoricalPrecipitationData.fallback("Error retrieving historical weather telemetry: " + e.getMessage(), "UNAVAILABLE");
        }
    }

    private HistoricalPrecipitationData parseHistoricalResponse(String json, BigDecimal lat, BigDecimal lon, LocalDate start, LocalDate end) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode hourly = root.path("hourly");
            JsonNode times = hourly.path("time");
            JsonNode precips = hourly.path("precipitation");

            if (!times.isArray() || !precips.isArray()) {
                return HistoricalPrecipitationData.fallback("Malformed historical precipitation payload.", "MALFORMED_PAYLOAD");
            }

            HistoricalPrecipitationData data = new HistoricalPrecipitationData();
            data.setLatitude(lat);
            data.setLongitude(lon);
            data.setStartDate(start);
            data.setEndDate(end);
            data.setAvailable(true);
            data.setRetrievedAt(LocalDateTime.now());
            data.setSourceMessage(String.format(Locale.ROOT, "Open-Meteo Historical Weather Archive (ERA5) for Puducherry %.4f, %.4f", lat.doubleValue(), lon.doubleValue()));

            double totalMm = 0.0;
            Double maxHourlyMm = null;
            int availableHours = 0;
            int missingHours = 0;
            List<HistoricalPrecipitationData.HourlyPrecipitationPoint> points = new ArrayList<>();

            for (int i = 0; i < times.size() && i < precips.size(); i++) {
                String timeStr = times.get(i).asText();
                JsonNode pNode = precips.get(i);
                Double p = null;
                if (pNode != null && !pNode.isNull()) {
                    p = pNode.asDouble();
                    totalMm += p;
                    availableHours++;
                    if (maxHourlyMm == null || p > maxHourlyMm) {
                        maxHourlyMm = p;
                    }
                } else {
                    missingHours++;
                }
                points.add(new HistoricalPrecipitationData.HourlyPrecipitationPoint(timeStr, p));
            }

            data.setTotalHoursCount(points.size());
            data.setAvailableHoursCount(availableHours);
            data.setMissingHoursCount(missingHours);
            data.setIsCompleteCoverage(!points.isEmpty() && missingHours == 0);
            double covPct = points.isEmpty() ? 0.0 : BigDecimal.valueOf((availableHours * 100.0) / points.size()).setScale(1, RoundingMode.HALF_UP).doubleValue();
            data.setCoveragePercentage(covPct);

            if (availableHours > 0) {
                data.setTotalRainfallMm(BigDecimal.valueOf(totalMm).setScale(2, RoundingMode.HALF_UP).doubleValue());
                data.setMaxHourlyRainfallMm(BigDecimal.valueOf(maxHourlyMm != null ? maxHourlyMm : 0.0).setScale(2, RoundingMode.HALF_UP).doubleValue());
            } else {
                data.setTotalRainfallMm(null);
                data.setMaxHourlyRainfallMm(null);
            }
            data.setHourlyData(points);

            return data;
        } catch (Exception e) {
            return HistoricalPrecipitationData.fallback("Failed to parse historical precipitation JSON: " + e.getMessage(), "MALFORMED_PAYLOAD");
        }
    }

    public String getArchiveBaseUrl() {
        return archiveBaseUrl;
    }

    public void clearHistoricalCache() {
        historicalCache.clear();
    }

    private static class CacheEntry {
        final WeatherData data;
        final Instant expiresAt;

        CacheEntry(WeatherData data, Instant expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }
    }

    private static class HistoricalCacheEntry {
        final HistoricalPrecipitationData data;
        final Instant expiresAt;

        HistoricalCacheEntry(HistoricalPrecipitationData data, Instant expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }
    }
}

