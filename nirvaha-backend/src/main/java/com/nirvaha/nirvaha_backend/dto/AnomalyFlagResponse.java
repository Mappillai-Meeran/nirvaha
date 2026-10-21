package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.AnomalyFlag;
import com.nirvaha.nirvaha_backend.entity.AnomalySeverity;
import com.nirvaha.nirvaha_backend.entity.AnomalyStatus;
import com.nirvaha.nirvaha_backend.entity.AnomalyType;

import java.time.LocalDateTime;

public class AnomalyFlagResponse {

    private Long id;
    private Long incidentId;
    private AnomalyType anomalyType;
    private AnomalySeverity severity;
    private String description;
    private String relatedIncidentIds;
    private AnomalyStatus status;
    private LocalDateTime detectedAt;

    public AnomalyFlagResponse() {
    }

    public AnomalyFlagResponse(Long id, Long incidentId, AnomalyType anomalyType, AnomalySeverity severity,
                               String description, String relatedIncidentIds, AnomalyStatus status, LocalDateTime detectedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.anomalyType = anomalyType;
        this.severity = severity;
        this.description = description;
        this.relatedIncidentIds = relatedIncidentIds;
        this.status = status;
        this.detectedAt = detectedAt;
    }

    public static AnomalyFlagResponse fromEntity(AnomalyFlag flag) {
        if (flag == null) {
            return null;
        }
        return new AnomalyFlagResponse(
                flag.getId(),
                flag.getIncident() != null ? flag.getIncident().getId() : null,
                flag.getAnomalyType(),
                flag.getSeverity(),
                flag.getDescription(),
                flag.getRelatedIncidentIds(),
                flag.getStatus(),
                flag.getDetectedAt()
        );
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
