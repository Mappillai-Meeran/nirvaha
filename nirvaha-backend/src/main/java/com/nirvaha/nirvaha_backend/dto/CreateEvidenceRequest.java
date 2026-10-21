package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.EvidenceType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateEvidenceRequest {

    @NotNull(message = "Incident ID is required")
    private Long incidentId;

    @NotNull(message = "Evidence type is required")
    private EvidenceType evidenceType;

    @NotBlank(message = "File URL is required")
    @Size(max = 1000, message = "File URL must not exceed 1000 characters")
    private String fileUrl;

    @Size(max = 255, message = "File name must not exceed 255 characters")
    private String fileName;

    @Size(max = 100, message = "MIME type must not exceed 100 characters")
    private String mimeType;

    private Long fileSize;

    private LocalDateTime capturedAt;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    private BigDecimal longitude;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    public CreateEvidenceRequest() {
    }

    public CreateEvidenceRequest(Long incidentId, EvidenceType evidenceType, String fileUrl, String fileName,
                                 String mimeType, Long fileSize, LocalDateTime capturedAt, BigDecimal latitude,
                                 BigDecimal longitude, String description) {
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
}
