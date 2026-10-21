package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.entity.PriorityLevel;
import com.nirvaha.nirvaha_backend.entity.RecommendedActionType;
import com.nirvaha.nirvaha_backend.entity.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;

public class ActionRecommendationDTO {

    private String id;
    private Long incidentId;
    private String incidentTitle;
    private IncidentCategory category;
    private String areaName;
    private PriorityLevel priorityLevel;
    private RiskLevel riskLevel;
    private RecommendedActionType recommendedAction;
    private String reason;
    private String suggestedDepartment;
    private String suggestedNextStep;
    private List<String> supportingSignals;
    private Long ageHours;
    private String slaState;
    private Integer evidenceCount;
    private IncidentStatus currentStatus;
    private String modelVersion;
    private LocalDateTime generatedAt;

    public ActionRecommendationDTO() {
    }

    public ActionRecommendationDTO(String id, Long incidentId, String incidentTitle, IncidentCategory category,
                                 String areaName, PriorityLevel priorityLevel, RiskLevel riskLevel,
                                 RecommendedActionType recommendedAction, String reason, String suggestedDepartment,
                                 String suggestedNextStep, List<String> supportingSignals, Long ageHours,
                                 String slaState, Integer evidenceCount, IncidentStatus currentStatus,
                                 String modelVersion, LocalDateTime generatedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.incidentTitle = incidentTitle;
        this.category = category;
        this.areaName = areaName;
        this.priorityLevel = priorityLevel;
        this.riskLevel = riskLevel;
        this.recommendedAction = recommendedAction;
        this.reason = reason;
        this.suggestedDepartment = suggestedDepartment;
        this.suggestedNextStep = suggestedNextStep;
        this.supportingSignals = supportingSignals;
        this.ageHours = ageHours;
        this.slaState = slaState;
        this.evidenceCount = evidenceCount;
        this.currentStatus = currentStatus;
        this.modelVersion = modelVersion;
        this.generatedAt = generatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public String getIncidentTitle() {
        return incidentTitle;
    }

    public void setIncidentTitle(String incidentTitle) {
        this.incidentTitle = incidentTitle;
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

    public PriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(PriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public RecommendedActionType getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(RecommendedActionType recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestedDepartment() {
        return suggestedDepartment;
    }

    public void setSuggestedDepartment(String suggestedDepartment) {
        this.suggestedDepartment = suggestedDepartment;
    }

    public String getSuggestedNextStep() {
        return suggestedNextStep;
    }

    public void setSuggestedNextStep(String suggestedNextStep) {
        this.suggestedNextStep = suggestedNextStep;
    }

    public List<String> getSupportingSignals() {
        return supportingSignals;
    }

    public void setSupportingSignals(List<String> supportingSignals) {
        this.supportingSignals = supportingSignals;
    }

    public Long getAgeHours() {
        return ageHours;
    }

    public void setAgeHours(Long ageHours) {
        this.ageHours = ageHours;
    }

    public String getSlaState() {
        return slaState;
    }

    public void setSlaState(String slaState) {
        this.slaState = slaState;
    }

    public Integer getEvidenceCount() {
        return evidenceCount;
    }

    public void setEvidenceCount(Integer evidenceCount) {
        this.evidenceCount = evidenceCount;
    }

    public IncidentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(IncidentStatus currentStatus) {
        this.currentStatus = currentStatus;
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
