package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_service_tracking")
public class IncidentServiceTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_rule_id", nullable = true)
    private ServiceRule serviceRule;

    @Column(name = "clock_started_at", nullable = false)
    private LocalDateTime clockStartedAt;

    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "elapsed_days", nullable = false)
    private Long elapsedDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "sla_status", nullable = false)
    private SlaStatus slaStatus;

    @Column(name = "last_evaluated_at", nullable = false)
    private LocalDateTime lastEvaluatedAt;

    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;

    @Column(name = "approaching_notified_at")
    private LocalDateTime approachingNotifiedAt;

    @Column(name = "overdue_notified_at")
    private LocalDateTime overdueNotifiedAt;

    public IncidentServiceTracking() {
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.lastEvaluatedAt = LocalDateTime.now();
        if (this.escalationLevel == null) {
            this.escalationLevel = 0;
        }
    }

    // Getters and Setters

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

    public ServiceRule getServiceRule() {
        return serviceRule;
    }

    public void setServiceRule(ServiceRule serviceRule) {
        this.serviceRule = serviceRule;
    }

    public LocalDateTime getClockStartedAt() {
        return clockStartedAt;
    }

    public void setClockStartedAt(LocalDateTime clockStartedAt) {
        this.clockStartedAt = clockStartedAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public Long getElapsedDays() {
        return elapsedDays;
    }

    public void setElapsedDays(Long elapsedDays) {
        this.elapsedDays = elapsedDays;
    }

    public SlaStatus getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(SlaStatus slaStatus) {
        this.slaStatus = slaStatus;
    }

    public LocalDateTime getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) {
        this.lastEvaluatedAt = lastEvaluatedAt;
    }

    public Integer getEscalationLevel() {
        return escalationLevel != null ? escalationLevel : 0;
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
}
