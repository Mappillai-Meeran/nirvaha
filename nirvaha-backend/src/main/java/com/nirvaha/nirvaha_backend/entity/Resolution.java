package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resolutions")
public class Resolution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private User officer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolution_evidence_id", nullable = false)
    private Evidence resolutionEvidence;

    @Column(name = "resolution_similarity_score", nullable = false)
    private Double resolutionSimilarityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;

    @Column(length = 2000)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "demo_run_id", length = 100)
    private String demoRunId;

    public Resolution() {
    }

    public Resolution(Long id, Incident incident, User officer, Evidence resolutionEvidence,
                      Double resolutionSimilarityScore, VerificationStatus verificationStatus,
                      String remarks, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.incident = incident;
        this.officer = officer;
        this.resolutionEvidence = resolutionEvidence;
        this.resolutionSimilarityScore = resolutionSimilarityScore;
        this.verificationStatus = verificationStatus;
        this.remarks = remarks;
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
        if (this.incident != null) {
            this.demoRunId = this.incident.getDemoRunId();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.incident != null) {
            this.demoRunId = this.incident.getDemoRunId();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public User getOfficer() {
        return officer;
    }

    public void setOfficer(User officer) {
        this.officer = officer;
    }

    public Evidence getResolutionEvidence() {
        return resolutionEvidence;
    }

    public void setResolutionEvidence(Evidence resolutionEvidence) {
        this.resolutionEvidence = resolutionEvidence;
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

    public String getDemoRunId() {
        return demoRunId != null ? demoRunId : (incident != null ? incident.getDemoRunId() : null);
    }

    public void setDemoRunId(String demoRunId) {
        this.demoRunId = demoRunId;
    }
}
