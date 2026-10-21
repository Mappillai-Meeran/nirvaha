package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CreateIncidentRequest {

    @NotNull(message = "Reporter ID is required")
    private Long reporterId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private IncidentCategory category;

    @NotNull(message = "Severity is required")
    private IncidentSeverity severity;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    private BigDecimal longitude;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String originalLanguage;

    private IncidentStatus status;

    public CreateIncidentRequest() {
    }

    public CreateIncidentRequest(Long reporterId, String title, String description, IncidentCategory category,
                                 IncidentSeverity severity, BigDecimal latitude, BigDecimal longitude, String address) {
        this.reporterId = reporterId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public CreateIncidentRequest(Long reporterId, String title, String description, IncidentCategory category,
                                 IncidentSeverity severity, BigDecimal latitude, BigDecimal longitude, String address, IncidentStatus status) {
        this(reporterId, title, description, category, severity, latitude, longitude, address);
        this.status = status;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }
}
