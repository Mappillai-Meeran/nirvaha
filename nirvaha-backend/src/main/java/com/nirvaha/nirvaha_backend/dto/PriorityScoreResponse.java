package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.PriorityLevel;

import java.util.List;

public class PriorityScoreResponse {

    private Long incidentId;
    private double priorityScore;
    private PriorityLevel priorityLevel;
    private PriorityFactors factors;
    private List<String> reasons;

    public PriorityScoreResponse() {
    }

    public PriorityScoreResponse(Long incidentId, double priorityScore, PriorityLevel priorityLevel, PriorityFactors factors, List<String> reasons) {
        this.incidentId = incidentId;
        this.priorityScore = priorityScore;
        this.priorityLevel = priorityLevel;
        this.factors = factors;
        this.reasons = reasons;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public double getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(double priorityScore) {
        this.priorityScore = priorityScore;
    }

    public PriorityLevel getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(PriorityLevel priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public PriorityFactors getFactors() {
        return factors;
    }

    public void setFactors(PriorityFactors factors) {
        this.factors = factors;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}
