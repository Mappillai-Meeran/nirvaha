package com.nirvaha.nirvaha_backend.dto;

import java.time.LocalDateTime;

public class WeatherData {

    private Double temperature;
    private Double recentRainfall; // in mm
    private Double forecastRainfall; // in mm (next 24h deterministic forecast)
    private Double humidity;
    private Boolean available;
    private String sourceMessage;
    private LocalDateTime observedAt;   // Instant when provider took meteorological reading
    private LocalDateTime retrievedAt;  // Instant when Nirvaha retrieved reading
    private String accumulationPeriod;  // e.g. "15_MIN_INTERVAL", "PAST_24_HOURS"
    private Boolean isCached = false;
    private Boolean isStale = false;
    private String provider = "Open-Meteo";
    private String temperatureUnit = "°C";
    private String rainfallUnit = "mm";
    private String humidityUnit = "%";
    private String errorCategory;

    public WeatherData() {
    }

    public WeatherData(Double temperature, Double recentRainfall, Double forecastRainfall, Double humidity,
                       Boolean available, String sourceMessage, LocalDateTime observedAt, LocalDateTime retrievedAt,
                       String accumulationPeriod, Boolean isCached) {
        this.temperature = temperature;
        this.recentRainfall = recentRainfall;
        this.forecastRainfall = forecastRainfall;
        this.humidity = humidity;
        this.available = available;
        this.sourceMessage = sourceMessage;
        this.observedAt = observedAt;
        this.retrievedAt = retrievedAt;
        this.accumulationPeriod = accumulationPeriod;
        this.isCached = isCached;
        this.isStale = false;
    }

    public WeatherData(Double temperature, Double recentRainfall, Double forecastRainfall, Double humidity,
                       Boolean available, String sourceMessage, LocalDateTime timestamp) {
        this(temperature, recentRainfall, forecastRainfall, humidity, available, sourceMessage, timestamp, timestamp, "15_MIN_INTERVAL", false);
    }

    public static WeatherData fallback(String reason) {
        return fallback(reason, "UNAVAILABLE");
    }

    public static WeatherData fallback(String reason, String errorCategory) {
        WeatherData data = new WeatherData(
                null,
                null,
                null,
                null,
                false,
                reason != null ? reason : "Weather data unavailable; risk calculated from civic history and spatial signals.",
                null,
                LocalDateTime.now(),
                "NONE",
                false
        );
        data.setIsStale(false);
        data.setErrorCategory(errorCategory);
        return data;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getRecentRainfall() {
        return recentRainfall;
    }

    public void setRecentRainfall(Double recentRainfall) {
        this.recentRainfall = recentRainfall;
    }

    public Double getForecastRainfall() {
        return forecastRainfall;
    }

    public void setForecastRainfall(Double forecastRainfall) {
        this.forecastRainfall = forecastRainfall;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getSourceMessage() {
        return sourceMessage;
    }

    public void setSourceMessage(String sourceMessage) {
        this.sourceMessage = sourceMessage;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDateTime observedAt) {
        this.observedAt = observedAt;
    }

    public LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public String getAccumulationPeriod() {
        return accumulationPeriod;
    }

    public void setAccumulationPeriod(String accumulationPeriod) {
        this.accumulationPeriod = accumulationPeriod;
    }

    public Boolean getIsCached() {
        return isCached;
    }

    public void setIsCached(Boolean cached) {
        isCached = cached;
    }

    public Boolean getIsStale() {
        return isStale;
    }

    public void setIsStale(Boolean stale) {
        isStale = stale;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTemperatureUnit() {
        return temperatureUnit;
    }

    public void setTemperatureUnit(String temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    public String getRainfallUnit() {
        return rainfallUnit;
    }

    public void setRainfallUnit(String rainfallUnit) {
        this.rainfallUnit = rainfallUnit;
    }

    public String getHumidityUnit() {
        return humidityUnit;
    }

    public void setHumidityUnit(String humidityUnit) {
        this.humidityUnit = humidityUnit;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }

    // Backward compatibility delegates
    public LocalDateTime getTimestamp() {
        return observedAt != null ? observedAt : retrievedAt;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.observedAt = timestamp;
        if (this.retrievedAt == null) {
            this.retrievedAt = timestamp;
        }
    }
}
