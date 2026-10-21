package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.OfficerDecisionType;
import com.nirvaha.nirvaha_backend.entity.RecommendedActionType;

import java.time.LocalDateTime;

public class OfficerDecisionResponse {

    private Long id;
    private Long incidentId;
    private String recommendationId;
    private RecommendedActionType recommendedAction;
    private OfficerDecisionType decision;
    private String reason;
    private Long decidedById;
    private String decidedByName;
    private LocalDateTime decidedAt;

    public OfficerDecisionResponse() {
    }

    public OfficerDecisionResponse(Long id, Long incidentId, String recommendationId,
                                   RecommendedActionType recommendedAction, OfficerDecisionType decision,
                                   String reason, Long decidedById, String decidedByName, LocalDateTime decidedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.recommendationId = recommendationId;
        this.recommendedAction = recommendedAction;
        this.decision = decision;
        this.reason = reason;
        this.decidedById = decidedById;
        this.decidedByName = decidedByName;
        this.decidedAt = decidedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getDecidedById() {
        return decidedById;
    }

    public void setDecidedById(Long decidedById) {
        this.decidedById = decidedById;
    }

    public String getDecidedByName() {
        return decidedByName;
    }

    public void setDecidedByName(String decidedByName) {
        this.decidedByName = decidedByName;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
