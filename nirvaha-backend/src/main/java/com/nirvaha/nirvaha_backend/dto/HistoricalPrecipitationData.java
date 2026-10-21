package com.nirvaha.nirvaha_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HistoricalPrecipitationData {

    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double totalRainfallMm;
    private Double maxHourlyRainfallMm;
    private Boolean available;
    private String provider = "Open-Meteo Historical Weather Archive";
    private String license = "Creative Commons Attribution 4.0 International (CC BY 4.0)";
    private LocalDateTime retrievedAt;
    private String sourceMessage;
    private String errorCategory;
    private String provenanceClass = "OFFICIAL_EXTERNAL_DATA";
    private Boolean isCached = false;
    private Integer totalHoursCount;
    private Integer availableHoursCount;
    private Integer missingHoursCount;
    private Double coveragePercentage;
    private Boolean isCompleteCoverage;
    private List<HourlyPrecipitationPoint> hourlyData = new ArrayList<>();

    public HistoricalPrecipitationData() {
    }

    public static HistoricalPrecipitationData fallback(String reason, String errorCategory) {
        HistoricalPrecipitationData data = new HistoricalPrecipitationData();
        data.setAvailable(false);
        data.setSourceMessage(reason != null ? reason : "Historical precipitation telemetry unavailable.");
        data.setErrorCategory(errorCategory != null ? errorCategory : "UPSTREAM_UNAVAILABLE");
        data.setRetrievedAt(LocalDateTime.now());
        data.setTotalRainfallMm(null);
        data.setMaxHourlyRainfallMm(null);
        return data;
    }

    public static class HourlyPrecipitationPoint {
        private String time;
        private Double precipitationMm;

        public HourlyPrecipitationPoint() {}

        public HourlyPrecipitationPoint(String time, Double precipitationMm) {
            this.time = time;
            this.precipitationMm = precipitationMm;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public Double getPrecipitationMm() {
            return precipitationMm;
        }

        public void setPrecipitationMm(Double precipitationMm) {
            this.precipitationMm = precipitationMm;
        }
    }

    // Getters and Setters

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Double getTotalRainfallMm() {
        return totalRainfallMm;
    }

    public void setTotalRainfallMm(Double totalRainfallMm) {
        this.totalRainfallMm = totalRainfallMm;
    }

    public Double getMaxHourlyRainfallMm() {
        return maxHourlyRainfallMm;
    }

    public void setMaxHourlyRainfallMm(Double maxHourlyRainfallMm) {
        this.maxHourlyRainfallMm = maxHourlyRainfallMm;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public String getSourceMessage() {
        return sourceMessage;
    }

    public void setSourceMessage(String sourceMessage) {
        this.sourceMessage = sourceMessage;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }

    public List<HourlyPrecipitationPoint> getHourlyData() {
        return hourlyData;
    }

    public void setHourlyData(List<HourlyPrecipitationPoint> hourlyData) {
        this.hourlyData = hourlyData;
    }

    public String getProvenanceClass() {
        return provenanceClass;
    }

    public void setProvenanceClass(String provenanceClass) {
        this.provenanceClass = provenanceClass;
    }

    public Boolean getIsCached() {
        return isCached;
    }

    public void setIsCached(Boolean isCached) {
        this.isCached = isCached;
    }

    public Double getTotalPrecipitationMm() {
        return totalRainfallMm;
    }

    public Double getMaxHourlyRateMm() {
        return maxHourlyRainfallMm;
    }

    public Integer getTotalHoursCount() {
        return totalHoursCount;
    }

    public void setTotalHoursCount(Integer totalHoursCount) {
        this.totalHoursCount = totalHoursCount;
    }

    public Integer getAvailableHoursCount() {
        return availableHoursCount;
    }

    public void setAvailableHoursCount(Integer availableHoursCount) {
        this.availableHoursCount = availableHoursCount;
    }

    public Integer getMissingHoursCount() {
        return missingHoursCount;
    }

    public void setMissingHoursCount(Integer missingHoursCount) {
        this.missingHoursCount = missingHoursCount;
    }

    public Double getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(Double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public Boolean getIsCompleteCoverage() {
        return isCompleteCoverage;
    }

    public void setIsCompleteCoverage(Boolean isCompleteCoverage) {
        this.isCompleteCoverage = isCompleteCoverage;
    }
}
