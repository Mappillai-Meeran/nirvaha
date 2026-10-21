package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.OfficerDecisionType;
import com.nirvaha.nirvaha_backend.entity.RecommendedActionType;
import jakarta.validation.constraints.NotNull;

public class OfficerDecisionRequest {

    @NotNull(message = "Incident ID is required")
    private Long incidentId;

    private String recommendationId;

    @NotNull(message = "Recommended action type is required")
    private RecommendedActionType recommendedAction;

    @NotNull(message = "Officer decision type is required")
    private OfficerDecisionType decision;

    private String reason;

    public OfficerDecisionRequest() {
    }

    public OfficerDecisionRequest(Long incidentId, String recommendationId, RecommendedActionType recommendedAction, OfficerDecisionType decision, String reason) {
        this.incidentId = incidentId;
        this.recommendationId = recommendationId;
        this.recommendedAction = recommendedAction;
        this.decision = decision;
        this.reason = reason;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public RecommendedActionType getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(RecommendedActionType recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public OfficerDecisionType getDecision() {
        return decision;
    }

    public void setDecision(OfficerDecisionType decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
