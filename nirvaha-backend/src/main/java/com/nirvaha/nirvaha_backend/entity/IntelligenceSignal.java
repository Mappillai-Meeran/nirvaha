package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intelligence_signals")
public class IntelligenceSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false)
    private SignalType signalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrative_area_id")
    private AdministrativeArea assignedArea;

    @Column(name = "area_name", length = 150)
    private String areaName;

    @Column(name = "centroid_lat")
    private Double centroidLat;

    @Column(name = "centroid_lng")
    private Double centroidLng;

    @Column(name = "radius_meters")
    private Double radiusMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_window", nullable = false)
    private TimeWindow timeWindow;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "trend_direction")
    private TrendDirection trendDirection;

    @Column(name = "change_percentage")
    private Double changePercentage;

    @Column(name = "incident_count", nullable = false)
    private Integer incidentCount;

    @Column(name = "previous_period_count")
    private Integer previousPeriodCount;

    @Column(name = "unresolved_count", nullable = false)
    private Integer unresolvedCount;

    @Column(name = "overdue_count")
    private Integer overdueCount;

    @Column(name = "priority_summary")
    private Double prioritySummary;

    @Column(name = "risk_summary")
    private Double riskSummary;

    @Column(name = "confidence", nullable = false)
    private Double confidence;

    @Column(name = "explanation_what", length = 500)
    private String explanationWhat;

    @Column(name = "explanation_where", length = 500)
    private String explanationWhere;

    @Column(name = "explanation_when", length = 500)
    private String explanationWhen;

    @Column(name = "explanation_why", length = 1000)
    private String explanationWhy;

    @Column(name = "explanation_evidence", length = 1000)
    private String explanationEvidence;

    @Column(name = "explanation_source", length = 500)
    private String explanationSource;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(length = 50, nullable = false)
    private String status;

    public IntelligenceSignal() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
        if (this.modelVersion == null) {
            this.modelVersion = "nirvaha-intelligence-v1";
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        if (this.confidence == null) {
            this.confidence = 1.0;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SignalType getSignalType() {
        return signalType;
    }

    public void setSignalType(SignalType signalType) {
        this.signalType = signalType;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public AdministrativeArea getAssignedArea() {
        return assignedArea;
    }

    public void setAssignedArea(AdministrativeArea assignedArea) {
        this.assignedArea = assignedArea;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public Double getCentroidLat() {
        return centroidLat;
    }

    public void setCentroidLat(Double centroidLat) {
        this.centroidLat = centroidLat;
    }

    public Double getCentroidLng() {
        return centroidLng;
    }

    public void setCentroidLng(Double centroidLng) {
        this.centroidLng = centroidLng;
    }

    public Double getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(Double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public TimeWindow getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(TimeWindow timeWindow) {
        this.timeWindow = timeWindow;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(LocalDateTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public TrendDirection getTrendDirection() {
        return trendDirection;
    }

    public void setTrendDirection(TrendDirection trendDirection) {
        this.trendDirection = trendDirection;
    }

    public Double getChangePercentage() {
        return changePercentage;
    }

    public void setChangePercentage(Double changePercentage) {
        this.changePercentage = changePercentage;
    }

    public Integer getIncidentCount() {
        return incidentCount;
    }

    public void setIncidentCount(Integer incidentCount) {
        this.incidentCount = incidentCount;
    }

    public Integer getPreviousPeriodCount() {
        return previousPeriodCount;
    }

    public void setPreviousPeriodCount(Integer previousPeriodCount) {
        this.previousPeriodCount = previousPeriodCount;
    }

    public Integer getUnresolvedCount() {
        return unresolvedCount;
    }

    public void setUnresolvedCount(Integer unresolvedCount) {
        this.unresolvedCount = unresolvedCount;
    }

    public Integer getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(Integer overdueCount) {
        this.overdueCount = overdueCount;
    }

    public Double getPrioritySummary() {
        return prioritySummary;
    }

    public void setPrioritySummary(Double prioritySummary) {
        this.prioritySummary = prioritySummary;
    }

    public Double getRiskSummary() {
        return riskSummary;
    }

    public void setRiskSummary(Double riskSummary) {
        this.riskSummary = riskSummary;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getExplanationWhat() {
        return explanationWhat;
    }

    public void setExplanationWhat(String explanationWhat) {
        this.explanationWhat = explanationWhat;
    }

    public String getExplanationWhere() {
        return explanationWhere;
    }

    public void setExplanationWhere(String explanationWhere) {
        this.explanationWhere = explanationWhere;
    }

    public String getExplanationWhen() {
        return explanationWhen;
    }

    public void setExplanationWhen(String explanationWhen) {
        this.explanationWhen = explanationWhen;
    }

    public String getExplanationWhy() {
        return explanationWhy;
    }

    public void setExplanationWhy(String explanationWhy) {
        this.explanationWhy = explanationWhy;
    }

    public String getExplanationEvidence() {
        return explanationEvidence;
    }

    public void setExplanationEvidence(String explanationEvidence) {
        this.explanationEvidence = explanationEvidence;
    }

    public String getExplanationSource() {
        return explanationSource;
    }

    public void setExplanationSource(String explanationSource) {
        this.explanationSource = explanationSource;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
