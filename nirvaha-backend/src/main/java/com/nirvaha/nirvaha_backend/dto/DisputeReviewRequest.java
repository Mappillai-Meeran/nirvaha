package com.nirvaha.nirvaha_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DisputeReviewRequest {

    private String disposition;

    @NotBlank(message = "Review notes are required")
    @Size(max = 2000, message = "Review notes must not exceed 2000 characters")
    private String reviewNotes;

    public DisputeReviewRequest() {
    }

    public DisputeReviewRequest(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public DisputeReviewRequest(String disposition, String reviewNotes) {
        this.disposition = disposition;
        this.reviewNotes = reviewNotes;
    }

    public String getDisposition() {
        return disposition;
    }

    public void setDisposition(String disposition) {
        this.disposition = disposition;
    }

    public String getStatus() {
        return disposition;
    }

    public void setStatus(String status) {
        this.disposition = status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
}
