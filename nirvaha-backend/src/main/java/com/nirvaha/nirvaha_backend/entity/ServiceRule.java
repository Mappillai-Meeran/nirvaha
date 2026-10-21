package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_rules")
public class ServiceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", nullable = false)
    private IncidentCategory serviceCategory;

    @Column(nullable = false, length = 150)
    private String jurisdiction;

    @Column(name = "service_description", length = 500)
    private String serviceDescription;

    @Column(name = "target_days", nullable = false)
    private Integer targetDays;

    @Column(name = "business_day_rule", length = 50)
    private String businessDayRule;

    @Column(name = "escalation_reference", length = 250)
    private String escalationReference;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private DataSource source;

    @Column(name = "is_official_sla", nullable = false)
    private Boolean isOfficialSla = false;

    @Column(name = "verification_status", length = 50, nullable = false)
    private String verificationStatus = "UNVERIFIED";

    @Column(name = "statutory_reference", length = 255)
    private String statutoryReference;

    @Column(name = "warning_threshold_days")
    private Integer warningThresholdDays = 3;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ServiceRule() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isOfficialSla == null) {
            this.isOfficialSla = false;
        }
        if (this.verificationStatus == null) {
            this.verificationStatus = "UNVERIFIED";
        }
        if (this.warningThresholdDays == null) {
            this.warningThresholdDays = 3;
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public IncidentCategory getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(IncidentCategory serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public Integer getTargetDays() {
        return targetDays;
    }

    public void setTargetDays(Integer targetDays) {
        this.targetDays = targetDays;
    }

    public String getBusinessDayRule() {
        return businessDayRule;
    }

    public void setBusinessDayRule(String businessDayRule) {
        this.businessDayRule = businessDayRule;
    }

    public String getEscalationReference() {
        return escalationReference;
    }

    public void setEscalationReference(String escalationReference) {
        this.escalationReference = escalationReference;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public DataSource getSource() {
        return source;
    }

    public void setSource(DataSource source) {
        this.source = source;
    }

    public Boolean getIsOfficialSla() {
        return isOfficialSla;
    }

    public void setIsOfficialSla(Boolean isOfficialSla) {
        this.isOfficialSla = isOfficialSla;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getStatutoryReference() {
        return statutoryReference;
    }

    public void setStatutoryReference(String statutoryReference) {
        this.statutoryReference = statutoryReference;
    }

    public Integer getWarningThresholdDays() {
        return warningThresholdDays != null ? warningThresholdDays : 3;
    }

    public void setWarningThresholdDays(Integer warningThresholdDays) {
        this.warningThresholdDays = warningThresholdDays;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
