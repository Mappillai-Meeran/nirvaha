package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;

import com.nirvaha.nirvaha_backend.entity.IncidentSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class IncidentResponse {

    private Long id;
    private Long reporterId;
    private String reporterName;
    private String title;
    private String description;
    private IncidentCategory category;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private Long assignedAreaId;
    private String assignedAreaName;
    private String assignmentMethod;
    private BigDecimal assignmentConfidence;
    private LocalDateTime assignmentTimestamp;
    private String wardAssignmentNotes;
    private String classificationTag;
    private String dataSourceName;
    private String weatherContext;
    private String serviceRuleDescription;
    private String slaStatus;
    private LocalDateTime dueAt;
    private Boolean isOfficialSla;
    private String slaVerificationStatus;
    private Integer targetDays;
    private Integer warningThresholdDays;
    private Long remainingDays;
    private Integer escalationLevel;
    private LocalDateTime approachingNotifiedAt;
    private LocalDateTime overdueNotifiedAt;
    private String originalDescription;
    private String originalLanguage;
    private String normalizedDescription;
    private String normalizationStatus;
    private Double languageConfidence;
    private String languageProvider;
    private IncidentSource intakeSource;
    private String externalRecordId;
    private String externalSourceIdentifier;
    private LocalDateTime retrievalTimestamp;
    private LocalDateTime sourceTimestamp;
    private String ingestionOutcome;
    private String nearestRoadName;
    private Double nearestRoadDistanceMeters;
    private Double nearestRoadConfidence;
    private String roadContextExplanation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IncidentResponse() {
    }

    public IncidentResponse(Long id, Long reporterId, String reporterName, String title, String description,
                            IncidentCategory category, IncidentSeverity severity, IncidentStatus status,
                            BigDecimal latitude, BigDecimal longitude, String address,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static IncidentResponse fromEntity(Incident incident) {
        Long reporterId = incident.getReporter() != null ? incident.getReporter().getId() : null;
        String reporterName = incident.getReporter() != null ? incident.getReporter().getName() : null;

        IncidentResponse response = new IncidentResponse(
                incident.getId(),
                reporterId,
                reporterName,
                incident.getTitle(),
                incident.getDescription(),
                incident.getCategory(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getLatitude(),
                incident.getLongitude(),
                incident.getAddress(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );

        if (incident.getAssignedArea() != null) {
            response.setAssignedAreaId(incident.getAssignedArea().getId());
            response.setAssignedAreaName(incident.getAssignedArea().getName());
        }
        if (incident.getAssignmentMethod() != null) {
            response.setAssignmentMethod(incident.getAssignmentMethod().name());
        }
        response.setAssignmentConfidence(incident.getAssignmentConfidence());
        response.setAssignmentTimestamp(incident.getAssignmentTimestamp());
        response.setWardAssignmentNotes(incident.getWardAssignmentNotes());
        if (incident.getClassificationTag() != null) {
            response.setClassificationTag(incident.getClassificationTag().name());
        } else {
            response.setClassificationTag("CITIZEN");
        }
        if (incident.getDataSource() != null) {
            response.setDataSourceName(incident.getDataSource().getName());
        } else {
            response.setDataSourceName("Nirvaha Citizen Evidence Network");
        }

        response.setOriginalDescription(incident.getOriginalDescription());
        response.setOriginalLanguage(incident.getOriginalLanguage());
        response.setNormalizedDescription(incident.getNormalizedDescription());
        response.setNormalizationStatus(incident.getNormalizationStatus());
        response.setLanguageConfidence(incident.getLanguageConfidence());
        response.setLanguageProvider(incident.getLanguageProvider());

        response.setIntakeSource(incident.getIntakeSource());
        response.setExternalRecordId(incident.getExternalRecordId());
        response.setExternalSourceIdentifier(incident.getExternalSourceIdentifier());
        response.setRetrievalTimestamp(incident.getRetrievalTimestamp());
        response.setSourceTimestamp(incident.getSourceTimestamp());
        response.setIngestionOutcome(incident.getIngestionOutcome());

        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
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

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
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

    public Long getAssignedAreaId() {
        return assignedAreaId;
    }

    public void setAssignedAreaId(Long assignedAreaId) {
        this.assignedAreaId = assignedAreaId;
    }

    public String getAssignedAreaName() {
        return assignedAreaName;
    }

    public void setAssignedAreaName(String assignedAreaName) {
        this.assignedAreaName = assignedAreaName;
    }

    public String getAssignmentMethod() {
        return assignmentMethod;
    }

    public void setAssignmentMethod(String assignmentMethod) {
        this.assignmentMethod = assignmentMethod;
    }

    public BigDecimal getAssignmentConfidence() {
        return assignmentConfidence;
    }

    public void setAssignmentConfidence(BigDecimal assignmentConfidence) {
        this.assignmentConfidence = assignmentConfidence;
    }

    public LocalDateTime getAssignmentTimestamp() {
        return assignmentTimestamp;
    }

    public void setAssignmentTimestamp(LocalDateTime assignmentTimestamp) {
        this.assignmentTimestamp = assignmentTimestamp;
    }

    public String getWardAssignmentNotes() {
        return wardAssignmentNotes;
    }

    public void setWardAssignmentNotes(String wardAssignmentNotes) {
        this.wardAssignmentNotes = wardAssignmentNotes;
    }

    public String getClassificationTag() {
        return classificationTag;
    }

    public void setClassificationTag(String classificationTag) {
        this.classificationTag = classificationTag;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getWeatherContext() {
        return weatherContext;
    }

    public void setWeatherContext(String weatherContext) {
        this.weatherContext = weatherContext;
    }

    public String getServiceRuleDescription() {
        return serviceRuleDescription;
    }

    public void setServiceRuleDescription(String serviceRuleDescription) {
        this.serviceRuleDescription = serviceRuleDescription;
    }

    public String getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public Boolean getIsOfficialSla() {
        return isOfficialSla;
    }

    public void setIsOfficialSla(Boolean isOfficialSla) {
        this.isOfficialSla = isOfficialSla;
    }

    public String getSlaVerificationStatus() {
        return slaVerificationStatus;
    }

    public void setSlaVerificationStatus(String slaVerificationStatus) {
        this.slaVerificationStatus = slaVerificationStatus;
    }

    public Integer getTargetDays() {
        return targetDays;
    }

    public void setTargetDays(Integer targetDays) {
        this.targetDays = targetDays;
    }

    public Integer getWarningThresholdDays() {
        return warningThresholdDays;
    }

    public void setWarningThresholdDays(Integer warningThresholdDays) {
        this.warningThresholdDays = warningThresholdDays;
    }

    public Long getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(Long remainingDays) {
        this.remainingDays = remainingDays;
    }

    public Integer getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(Integer escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public LocalDateTime getApproachingNotifiedAt() {
        return approachingNotifiedAt;
    }

    public void setApproachingNotifiedAt(LocalDateTime approachingNotifiedAt) {
        this.approachingNotifiedAt = approachingNotifiedAt;
    }

    public LocalDateTime getOverdueNotifiedAt() {
        return overdueNotifiedAt;
    }

    public void setOverdueNotifiedAt(LocalDateTime overdueNotifiedAt) {
        this.overdueNotifiedAt = overdueNotifiedAt;
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

    public String getOriginalDescription() {
        return originalDescription;
    }

    public void setOriginalDescription(String originalDescription) {
        this.originalDescription = originalDescription;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public String getNormalizedDescription() {
        return normalizedDescription;
    }

    public void setNormalizedDescription(String normalizedDescription) {
        this.normalizedDescription = normalizedDescription;
    }

    public String getNormalizationStatus() {
        return normalizationStatus;
    }

    public void setNormalizationStatus(String normalizationStatus) {
        this.normalizationStatus = normalizationStatus;
    }

    public Double getLanguageConfidence() {
        return languageConfidence;
    }

    public void setLanguageConfidence(Double languageConfidence) {
        this.languageConfidence = languageConfidence;
    }

    public String getLanguageProvider() {
        return languageProvider;
    }

    public void setLanguageProvider(String languageProvider) {
        this.languageProvider = languageProvider;
    }

    public IncidentSource getIntakeSource() {
        return intakeSource;
    }

    public void setIntakeSource(IncidentSource intakeSource) {
        this.intakeSource = intakeSource;
    }

    public String getExternalRecordId() {
        return externalRecordId;
    }

    public void setExternalRecordId(String externalRecordId) {
        this.externalRecordId = externalRecordId;
    }

    public String getExternalSourceIdentifier() {
        return externalSourceIdentifier;
    }

    public void setExternalSourceIdentifier(String externalSourceIdentifier) {
        this.externalSourceIdentifier = externalSourceIdentifier;
    }

    public LocalDateTime getRetrievalTimestamp() {
        return retrievalTimestamp;
    }

    public void setRetrievalTimestamp(LocalDateTime retrievalTimestamp) {
        this.retrievalTimestamp = retrievalTimestamp;
    }

    public LocalDateTime getSourceTimestamp() {
        return sourceTimestamp;
    }

    public void setSourceTimestamp(LocalDateTime sourceTimestamp) {
        this.sourceTimestamp = sourceTimestamp;
    }

    public String getIngestionOutcome() {
        return ingestionOutcome;
    }

    public void setIngestionOutcome(String ingestionOutcome) {
        this.ingestionOutcome = ingestionOutcome;
    }

    public String getNearestRoadName() {
        return nearestRoadName;
    }

    public void setNearestRoadName(String nearestRoadName) {
        this.nearestRoadName = nearestRoadName;
    }

    public Double getNearestRoadDistanceMeters() {
        return nearestRoadDistanceMeters;
    }

    public void setNearestRoadDistanceMeters(Double nearestRoadDistanceMeters) {
        this.nearestRoadDistanceMeters = nearestRoadDistanceMeters;
    }

    public Double getNearestRoadConfidence() {
        return nearestRoadConfidence;
    }

    public void setNearestRoadConfidence(Double nearestRoadConfidence) {
        this.nearestRoadConfidence = nearestRoadConfidence;
    }

    public String getRoadContextExplanation() {
        return roadContextExplanation;
    }

    public void setRoadContextExplanation(String roadContextExplanation) {
        this.roadContextExplanation = roadContextExplanation;
    }
}
