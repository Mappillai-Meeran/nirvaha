package com.nirvaha.nirvaha_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateResolutionRequest {

    @NotNull(message = "Officer ID is required")
    private Long officerId;

    @NotNull(message = "Evidence ID is required")
    private Long evidenceId;

    @Size(max = 2000, message = "Remarks cannot exceed 2000 characters")
    private String remarks;

    public CreateResolutionRequest() {
    }

    public CreateResolutionRequest(Long officerId, Long evidenceId, String remarks) {
        this.officerId = officerId;
        this.evidenceId = evidenceId;
        this.remarks = remarks;
    }

    public Long getOfficerId() {
        return officerId;
    }

    public void setOfficerId(Long officerId) {
        this.officerId = officerId;
    }

    public Long getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(Long evidenceId) {
        this.evidenceId = evidenceId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
