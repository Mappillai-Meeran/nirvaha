package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentCategory category;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "historical_score", nullable = false)
    private Double historicalScore;

    @Column(name = "recent_activity_score", nullable = false)
    private Double recentActivityScore;

    @Column(name = "recurrence_score", nullable = false)
    private Double recurrenceScore;

    @Column(name = "unresolved_load_score", nullable = false)
    private Double unresolvedLoadScore;

    @Column(name = "spatial_concentration_score", nullable = false)
    private Double spatialConcentrationScore;

    @Column(name = "weather_impact_score", nullable = false)
    private Double weatherImpactScore;

    @Column(name = "weather_context", length = 500)
    private String weatherContext;

    @Column(nullable = false, length = 2000)
    private String explanation;

    @Column(name = "recommended_action", length = 1000)
    private String recommendedAction;

    @Column(name = "assessment_time", nullable = false)
    private LocalDateTime assessmentTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RiskAssessment() {
    }

    public RiskAssessment(Long id, Ward ward, IncidentCategory category, Double riskScore, RiskLevel riskLevel,
                          Double historicalScore, Double recentActivityScore, Double recurrenceScore,
                          Double unresolvedLoadScore, Double spatialConcentrationScore, Double weatherImpactScore,
                          String weatherContext, String explanation, String recommendedAction,
                          LocalDateTime assessmentTime, LocalDateTime createdAt) {
        this.id = id;
        this.ward = ward;
        this.category = category;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.historicalScore = historicalScore;
        this.recentActivityScore = recentActivityScore;
        this.recurrenceScore = recurrenceScore;
        this.unresolvedLoadScore = unresolvedLoadScore;
        this.spatialConcentrationScore = spatialConcentrationScore;
        this.weatherImpactScore = weatherImpactScore;
        this.weatherContext = weatherContext;
        this.explanation = explanation;
        this.recommendedAction = recommendedAction;
        this.assessmentTime = assessmentTime;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.assessmentTime == null) {
            this.assessmentTime = now;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ward getWard() {
        return ward;
    }

    public void setWard(Ward ward) {
        this.ward = ward;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Double getHistoricalScore() {
        return historicalScore;
    }

    public void setHistoricalScore(Double historicalScore) {
        this.historicalScore = historicalScore;
    }

    public Double getRecentActivityScore() {
        return recentActivityScore;
    }

    public void setRecentActivityScore(Double recentActivityScore) {
        this.recentActivityScore = recentActivityScore;
    }

    public Double getRecurrenceScore() {
        return recurrenceScore;
    }

    public void setRecurrenceScore(Double recurrenceScore) {
        this.recurrenceScore = recurrenceScore;
    }

    public Double getUnresolvedLoadScore() {
        return unresolvedLoadScore;
    }

    public void setUnresolvedLoadScore(Double unresolvedLoadScore) {
        this.unresolvedLoadScore = unresolvedLoadScore;
    }

    public Double getSpatialConcentrationScore() {
        return spatialConcentrationScore;
    }

    public void setSpatialConcentrationScore(Double spatialConcentrationScore) {
        this.spatialConcentrationScore = spatialConcentrationScore;
    }

    public Double getWeatherImpactScore() {
        return weatherImpactScore;
    }

    public void setWeatherImpactScore(Double weatherImpactScore) {
        this.weatherImpactScore = weatherImpactScore;
    }

    public String getWeatherContext() {
        return weatherContext;
    }

    public void setWeatherContext(String weatherContext) {
        this.weatherContext = weatherContext;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public LocalDateTime getAssessmentTime() {
        return assessmentTime;
    }

    public void setAssessmentTime(LocalDateTime assessmentTime) {
        this.assessmentTime = assessmentTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
