package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.IncidentCategory;

import java.time.LocalDateTime;

public class LearningSignalDTO {

    private String id;
    private String signalType; // ELEVATED_REOPEN_FREQUENCY, RECURRING_INFRASTRUCTURE_VULNERABILITY, SLA_SERVICE_PRESSURE
    private IncidentCategory category;
    private String areaName;
    private String observation;
    private String recommendedStrategy;
    private Integer sampleCount;
    private String modelVersion;
    private LocalDateTime generatedAt;

    public LearningSignalDTO() {
    }

    public LearningSignalDTO(String id, String signalType, IncidentCategory category, String areaName,
                             String observation, String recommendedStrategy, Integer sampleCount,
                             String modelVersion, LocalDateTime generatedAt) {
        this.id = id;
        this.signalType = signalType;
        this.category = category;
        this.areaName = areaName;
        this.observation = observation;
        this.recommendedStrategy = recommendedStrategy;
        this.sampleCount = sampleCount;
        this.modelVersion = modelVersion;
        this.generatedAt = generatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getRecommendedStrategy() {
        return recommendedStrategy;
    }

    public void setRecommendedStrategy(String recommendedStrategy) {
        this.recommendedStrategy = recommendedStrategy;
    }

    public Integer getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(Integer sampleCount) {
        this.sampleCount = sampleCount;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
