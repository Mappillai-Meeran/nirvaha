package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "officer_decisions")
public class OfficerDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "recommendation_id")
    private String recommendationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", nullable = false)
    private RecommendedActionType recommendedAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "officer_decision", nullable = false)
    private OfficerDecisionType officerDecision;

    @Column(name = "decision_reason", length = 2000)
    private String decisionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by", nullable = false)
    private User decidedBy;

    @Column(name = "decided_at", nullable = false, updatable = false)
    private LocalDateTime decidedAt;

    public OfficerDecision() {
    }

    public OfficerDecision(Long id, Incident incident, String recommendationId,
                           RecommendedActionType recommendedAction, OfficerDecisionType officerDecision,
                           String decisionReason, User decidedBy, LocalDateTime decidedAt) {
        this.id = id;
        this.incident = incident;
        this.recommendationId = recommendationId;
        this.recommendedAction = recommendedAction;
        this.officerDecision = officerDecision;
        this.decisionReason = decisionReason;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.decidedAt == null) {
            this.decidedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
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

    public OfficerDecisionType getOfficerDecision() {
        return officerDecision;
    }

    public void setOfficerDecision(OfficerDecisionType officerDecision) {
        this.officerDecision = officerDecision;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(User decidedBy) {
        this.decidedBy = decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
