package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident_actions")
public class IncidentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = false)
    private User officer;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    @Column(length = 2000)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "demo_run_id", length = 100)
    private String demoRunId;

    public IncidentAction() {
    }

    public IncidentAction(Long id, Incident incident, User officer, ActionType actionType, String remarks, LocalDateTime createdAt) {
        this.id = id;
        this.incident = incident;
        this.officer = officer;
        this.actionType = actionType;
        this.remarks = remarks;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.incident != null) {
            this.demoRunId = this.incident.getDemoRunId();
        }
    }

    @PreUpdate
    protected void onUpdate() {
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

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
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

    public String getDemoRunId() {
        return demoRunId != null ? demoRunId : (incident != null ? incident.getDemoRunId() : null);
    }

    public void setDemoRunId(String demoRunId) {
        this.demoRunId = demoRunId;
    }
}
