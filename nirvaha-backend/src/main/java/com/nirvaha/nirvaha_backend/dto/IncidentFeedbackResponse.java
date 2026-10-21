package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.FeedbackType;
import com.nirvaha.nirvaha_backend.entity.IncidentFeedback;

import java.time.LocalDateTime;

public class IncidentFeedbackResponse {

    private Long id;
    private Long incidentId;
    private Long residentId;
    private String residentName;
    private Long resolutionId;
    private FeedbackType feedbackType;
    private String explanation;
    private String status;
    private Boolean reviewed;
    private Long reviewedByOfficerId;
    private String reviewedByOfficerName;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
    private LocalDateTime createdAt;
    private String demoRunId;

    public IncidentFeedbackResponse() {
    }

    public static IncidentFeedbackResponse fromEntity(IncidentFeedback entity) {
        if (entity == null) {
            return null;
        }
        IncidentFeedbackResponse dto = new IncidentFeedbackResponse();
        dto.setId(entity.getId());
        dto.setIncidentId(entity.getIncident() != null ? entity.getIncident().getId() : null);
        if (entity.getResident() != null) {
            dto.setResidentId(entity.getResident().getId());
            dto.setResidentName(entity.getResident().getName());
        }
        dto.setResolutionId(entity.getResolution() != null ? entity.getResolution().getId() : null);
        dto.setFeedbackType(entity.getFeedbackType());
        dto.setExplanation(entity.getExplanation());
        dto.setStatus(entity.getStatus());
        dto.setReviewed(entity.getReviewed());
        if (entity.getReviewedBy() != null) {
            dto.setReviewedByOfficerId(entity.getReviewedBy().getId());
            dto.setReviewedByOfficerName(entity.getReviewedBy().getName());
        }
        dto.setReviewedAt(entity.getReviewedAt());
        dto.setReviewNotes(entity.getReviewNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setDemoRunId(entity.getDemoRunId());
        return dto;
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

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

    public String getResidentName() {
        return residentName;
    }

    public void setResidentName(String residentName) {
        this.residentName = residentName;
    }

    public Long getResolutionId() {
        return resolutionId;
    }

    public void setResolutionId(Long resolutionId) {
        this.resolutionId = resolutionId;
    }

    public FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(FeedbackType feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getReviewed() {
        return reviewed;
    }

    public void setReviewed(Boolean reviewed) {
        this.reviewed = reviewed;
    }

    public Long getReviewedByOfficerId() {
        return reviewedByOfficerId;
    }

    public void setReviewedByOfficerId(Long reviewedByOfficerId) {
        this.reviewedByOfficerId = reviewedByOfficerId;
    }

    public String getReviewedByOfficerName() {
        return reviewedByOfficerName;
    }

    public void setReviewedByOfficerName(String reviewedByOfficerName) {
        this.reviewedByOfficerName = reviewedByOfficerName;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDemoRunId() {
        return demoRunId;
    }

    public void setDemoRunId(String demoRunId) {
        this.demoRunId = demoRunId;
    }
}
