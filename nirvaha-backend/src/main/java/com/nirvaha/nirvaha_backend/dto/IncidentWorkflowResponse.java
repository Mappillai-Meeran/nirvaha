package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.IncidentStatus;

import java.util.List;

public class IncidentWorkflowResponse {

    private Long incidentId;
    private IncidentStatus currentStatus;
    private Long assignedOfficerId;
    private String assignedOfficerName;
    private List<IncidentActionResponse> actions;

    public IncidentWorkflowResponse() {
    }

    public IncidentWorkflowResponse(Long incidentId, IncidentStatus currentStatus, List<IncidentActionResponse> actions) {
        this.incidentId = incidentId;
        this.currentStatus = currentStatus;
        this.actions = actions;
    }

    public IncidentWorkflowResponse(Long incidentId, IncidentStatus currentStatus, Long assignedOfficerId, String assignedOfficerName, List<IncidentActionResponse> actions) {
        this.incidentId = incidentId;
        this.currentStatus = currentStatus;
        this.assignedOfficerId = assignedOfficerId;
        this.assignedOfficerName = assignedOfficerName;
        this.actions = actions;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public IncidentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(IncidentStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Long getAssignedOfficerId() {
        return assignedOfficerId;
    }

    public void setAssignedOfficerId(Long assignedOfficerId) {
        this.assignedOfficerId = assignedOfficerId;
    }

    public String getAssignedOfficerName() {
        return assignedOfficerName;
    }

    public void setAssignedOfficerName(String assignedOfficerName) {
        this.assignedOfficerName = assignedOfficerName;
    }

    public List<IncidentActionResponse> getActions() {
        return actions;
    }

    public void setActions(List<IncidentActionResponse> actions) {
        this.actions = actions;
    }
}
