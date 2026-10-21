package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.ActionType;

import java.time.LocalDateTime;

public class IncidentActionResponse {

    private Long id;
    private Long incidentId;
    private Long officerId;
    private String officerName;
    private ActionType actionType;
    private String remarks;
    private LocalDateTime createdAt;

    public IncidentActionResponse() {
    }

    public IncidentActionResponse(Long id, Long incidentId, Long officerId, String officerName, ActionType actionType, String remarks, LocalDateTime createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.officerId = officerId;
        this.officerName = officerName;
        this.actionType = actionType;
        this.remarks = remarks;
        this.createdAt = createdAt;
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

    public Long getOfficerId() {
        return officerId;
    }

    public void setOfficerId(Long officerId) {
        this.officerId = officerId;
    }

    public String getOfficerName() {
        return officerName;
    }

    public void setOfficerName(String officerName) {
        this.officerName = officerName;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static IncidentActionResponse fromEntity(com.nirvaha.nirvaha_backend.entity.IncidentAction action) {
        if (action == null) return null;
        return new IncidentActionResponse(
                action.getId(),
                action.getIncident() != null ? action.getIncident().getId() : null,
                action.getOfficer() != null ? action.getOfficer().getId() : null,
                action.getOfficer() != null ? action.getOfficer().getName() : null,
                action.getActionType(),
                action.getRemarks(),
                action.getCreatedAt()
        );
    }
}
