package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.Evidence;
import com.nirvaha.nirvaha_backend.entity.EvidenceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EvidenceResponse {

    private Long id;
    private Long incidentId;
    private EvidenceType evidenceType;
    private String fileUrl;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private LocalDateTime capturedAt;
    private BigDecimal latitude;
    private String contentHash;
    private BigDecimal longitude;
    private String description;
    private LocalDateTime createdAt;

    public EvidenceResponse() {
    }

    public EvidenceResponse(Long id, Long incidentId, EvidenceType evidenceType, String fileUrl, String fileName,
                            String mimeType, Long fileSize, LocalDateTime capturedAt, BigDecimal latitude,
                            BigDecimal longitude, String description, LocalDateTime createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.evidenceType = evidenceType;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.capturedAt = capturedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static EvidenceResponse fromEntity(Evidence evidence) {
        Long incidentId = evidence.getIncident() != null ? evidence.getIncident().getId() : null;
        EvidenceResponse response = new EvidenceResponse(
                evidence.getId(),
                incidentId,
                evidence.getEvidenceType(),
                evidence.getFileUrl(),
                evidence.getFileName(),
                evidence.getMimeType(),
                evidence.getFileSize(),
                evidence.getCapturedAt(),
                evidence.getLatitude(),
                evidence.getLongitude(),
                evidence.getDescription(),
                evidence.getCreatedAt()
        );
        response.setContentHash(evidence.getContentHash());
        return response;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
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

    public EvidenceType getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(EvidenceType evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
