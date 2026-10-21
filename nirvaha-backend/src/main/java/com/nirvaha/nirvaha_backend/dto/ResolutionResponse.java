package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.Resolution;
import com.nirvaha.nirvaha_backend.entity.VerificationStatus;

import java.time.LocalDateTime;

public class ResolutionResponse {

    private Long id;
    private Long incidentId;
    private Long officerId;
    private String officerName;
    private Long resolutionEvidenceId;
    private String resolutionEvidenceUrl;
    private Double resolutionSimilarityScore;
    private VerificationStatus verificationStatus;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ResolutionResponse() {
    }

    public ResolutionResponse(Long id, Long incidentId, Long officerId, String officerName,
                              Long resolutionEvidenceId, String resolutionEvidenceUrl,
                              Double resolutionSimilarityScore, VerificationStatus verificationStatus,
                              String remarks, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.officerId = officerId;
        this.officerName = officerName;
        this.resolutionEvidenceId = resolutionEvidenceId;
        this.resolutionEvidenceUrl = resolutionEvidenceUrl;
        this.resolutionSimilarityScore = resolutionSimilarityScore;
        this.verificationStatus = verificationStatus;
        this.remarks = remarks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ResolutionResponse fromEntity(Resolution resolution) {
        return new ResolutionResponse(
                resolution.getId(),
                resolution.getIncident().getId(),
                resolution.getOfficer().getId(),
                resolution.getOfficer().getName(),
                resolution.getResolutionEvidence().getId(),
                resolution.getResolutionEvidence().getFileUrl(),
                resolution.getResolutionSimilarityScore(),
                resolution.getVerificationStatus(),
                resolution.getRemarks(),
                resolution.getCreatedAt(),
                resolution.getUpdatedAt()
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

    public Long getResolutionEvidenceId() {
        return resolutionEvidenceId;
    }

    public void setResolutionEvidenceId(Long resolutionEvidenceId) {
        this.resolutionEvidenceId = resolutionEvidenceId;
    }

    public String getResolutionEvidenceUrl() {
        return resolutionEvidenceUrl;
    }

    public void setResolutionEvidenceUrl(String resolutionEvidenceUrl) {
        this.resolutionEvidenceUrl = resolutionEvidenceUrl;
    }

    public Double getResolutionSimilarityScore() {
        return resolutionSimilarityScore;
    }

    public void setResolutionSimilarityScore(Double resolutionSimilarityScore) {
        this.resolutionSimilarityScore = resolutionSimilarityScore;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
