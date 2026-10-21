package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.ActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateIncidentActionRequest {

    @NotNull(message = "Officer ID is required")
    private Long officerId;

    @NotNull(message = "Action type is required")
    private ActionType actionType;

    @Size(max = 2000, message = "Remarks cannot exceed 2000 characters")
    private String remarks;

    public CreateIncidentActionRequest() {
    }

    public CreateIncidentActionRequest(Long officerId, ActionType actionType, String remarks) {
        this.officerId = officerId;
        this.actionType = actionType;
        this.remarks = remarks;
    }

    public Long getOfficerId() {
        return officerId;
    }

    public void setOfficerId(Long officerId) {
        this.officerId = officerId;
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
}
