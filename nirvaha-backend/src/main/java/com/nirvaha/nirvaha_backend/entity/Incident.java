package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 500)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_area_id")
    private AdministrativeArea assignedArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_method")
    private AssignmentMethod assignmentMethod;

    @Column(name = "assignment_confidence", precision = 5, scale = 4)
    private BigDecimal assignmentConfidence;

    @Column(name = "assignment_timestamp")
    private LocalDateTime assignmentTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_tag", nullable = false)
    private DataClassification classificationTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_source", length = 50)
    private IncidentSource intakeSource;

    @Column(name = "external_record_id", length = 100)
    private String externalRecordId;

    @Column(name = "external_source_identifier", length = 100)
    private String externalSourceIdentifier;

    @Column(name = "retrieval_timestamp")
    private LocalDateTime retrievalTimestamp;

    @Column(name = "source_timestamp")
    private LocalDateTime sourceTimestamp;

    @Column(name = "ingestion_outcome", length = 50)
    private String ingestionOutcome;

    @Column(name = "demo_run_id", length = 100)
    private String demoRunId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_source_id")
    private DataSource dataSource;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<IncidentServiceTracking> serviceTrackings = new java.util.ArrayList<>();

    @Column(name = "ward_assignment_notes", length = 500)
    private String wardAssignmentNotes;

    @Column(name = "original_description", length = 4000)
    private String originalDescription;

    @Column(name = "original_language", length = 10)
    private String originalLanguage;

    @Column(name = "normalized_description", length = 4000)
    private String normalizedDescription;

    @Column(name = "normalization_status", length = 50)
    private String normalizationStatus;

    @Column(name = "language_confidence")
    private Double languageConfidence;

    @Column(name = "language_provider", length = 100)
    private String languageProvider;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Incident() {
    }

    public Incident(Long id, User reporter, String title, String description, IncidentCategory category,
                    IncidentSeverity severity, IncidentStatus status, BigDecimal latitude, BigDecimal longitude,
                    String address, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.reporter = reporter;
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

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.status == null) {
            this.status = IncidentStatus.REPORTED;
        }
        if (this.classificationTag == null) {
            this.classificationTag = DataClassification.CITIZEN;
        }
        if (this.classificationTag == DataClassification.SIMULATED) {
            this.intakeSource = IncidentSource.SIMULATED;
        }
        if (this.intakeSource == null) {
            if (this.classificationTag == DataClassification.OFFICIAL
                    && this.externalSourceIdentifier != null && !this.externalSourceIdentifier.isBlank()
                    && this.externalRecordId != null && !this.externalRecordId.isBlank()) {
                this.intakeSource = IncidentSource.AUTHORIZED_GOVERNMENT_SOURCE;
            } else {
                this.intakeSource = IncidentSource.RESIDENT_SUBMITTED;
            }
        }
        // Provenance verification: Government-origin records cannot be claimed without verified integration credentials
        if (this.intakeSource == IncidentSource.AUTHORIZED_GOVERNMENT_SOURCE) {
            if (this.externalSourceIdentifier == null || this.externalSourceIdentifier.isBlank()
                    || this.externalRecordId == null || this.externalRecordId.isBlank()) {
                this.intakeSource = IncidentSource.RESIDENT_SUBMITTED;
                if (this.classificationTag == DataClassification.OFFICIAL) {
                    this.classificationTag = DataClassification.CITIZEN;
                }
            }
        }
        if (this.sourceTimestamp == null) {
            this.sourceTimestamp = now;
        }
        if (this.retrievalTimestamp == null) {
            this.retrievalTimestamp = now;
        }
        if (this.ingestionOutcome == null) {
            this.ingestionOutcome = "SUCCESS";
        }
        if (this.assignmentMethod == null) {
            this.assignmentMethod = AssignmentMethod.UNASSIGNED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
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

    public AdministrativeArea getAssignedArea() {
        return assignedArea;
    }

    public void setAssignedArea(AdministrativeArea assignedArea) {
        this.assignedArea = assignedArea;
    }

    public AssignmentMethod getAssignmentMethod() {
        return assignmentMethod;
    }

    public void setAssignmentMethod(AssignmentMethod assignmentMethod) {
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

    public DataClassification getClassificationTag() {
        return classificationTag;
    }

    public void setClassificationTag(DataClassification classificationTag) {
        this.classificationTag = classificationTag;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getWardAssignmentNotes() {
        return wardAssignmentNotes;
    }

    public void setWardAssignmentNotes(String wardAssignmentNotes) {
        this.wardAssignmentNotes = wardAssignmentNotes;
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

    public String getDemoRunId() {
        return demoRunId;
    }

    public void setDemoRunId(String demoRunId) {
        this.demoRunId = demoRunId;
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
}
