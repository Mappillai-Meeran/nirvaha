package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_flags")
public class AnomalyFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false)
    private AnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalySeverity severity;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "related_incident_ids", length = 500)
    private String relatedIncidentIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyStatus status;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    public AnomalyFlag() {
    }

    public AnomalyFlag(Long id, Incident incident, AnomalyType anomalyType, AnomalySeverity severity,
                       String description, String relatedIncidentIds, AnomalyStatus status, LocalDateTime detectedAt) {
        this.id = id;
        this.incident = incident;
        this.anomalyType = anomalyType;
        this.severity = severity;
        this.description = description;
        this.relatedIncidentIds = relatedIncidentIds;
        this.status = status;
        this.detectedAt = detectedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.detectedAt == null) {
            this.detectedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = AnomalyStatus.OPEN;
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

    public AnomalyType getAnomalyType() {
        return anomalyType;
    }

    public void setAnomalyType(AnomalyType anomalyType) {
        this.anomalyType = anomalyType;
    }

    public AnomalySeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AnomalySeverity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRelatedIncidentIds() {
        return relatedIncidentIds;
    }

    public void setRelatedIncidentIds(String relatedIncidentIds) {
        this.relatedIncidentIds = relatedIncidentIds;
    }

    public AnomalyStatus getStatus() {
        return status;
    }

    public void setStatus(AnomalyStatus status) {
        this.status = status;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }
}
