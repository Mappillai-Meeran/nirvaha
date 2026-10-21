package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentServiceTracking;
import com.nirvaha.nirvaha_backend.entity.ServiceRule;

import java.time.Duration;
import java.time.LocalDateTime;

public class SlaTrackingResponse {

    private Long id;
    private Long incidentId;
    private String incidentTitle;
    private String incidentCategory;
    private String incidentStatus;
    private String wardName;
    private Long serviceRuleId;
    private String serviceDescription;
    private String department;
    private Boolean isOfficialSla;
    private String verificationStatus;
    private String statutoryReference;
    private Integer targetDays;
    private Integer warningThresholdDays;
    private LocalDateTime clockStartedAt;
    private LocalDateTime dueAt;
    private Long elapsedDays;
    private Long remainingDays;
    private String slaStatus;
    private Integer escalationLevel;
    private LocalDateTime approachingNotifiedAt;
    private LocalDateTime overdueNotifiedAt;
    private LocalDateTime lastEvaluatedAt;

    public SlaTrackingResponse() {
    }

    public static SlaTrackingResponse fromEntity(IncidentServiceTracking tracking) {
        if (tracking == null) {
            return null;
        }

        SlaTrackingResponse resp = new SlaTrackingResponse();
        resp.setId(tracking.getId());
        resp.setClockStartedAt(tracking.getClockStartedAt());
        resp.setDueAt(tracking.getDueAt());
        resp.setElapsedDays(tracking.getElapsedDays());
        resp.setSlaStatus(tracking.getSlaStatus() != null ? tracking.getSlaStatus().name() : "NOT_APPLICABLE");
        resp.setEscalationLevel(tracking.getEscalationLevel() != null ? tracking.getEscalationLevel() : 0);
        resp.setApproachingNotifiedAt(tracking.getApproachingNotifiedAt());
        resp.setOverdueNotifiedAt(tracking.getOverdueNotifiedAt());
        resp.setLastEvaluatedAt(tracking.getLastEvaluatedAt());

        if (tracking.getDueAt() != null) {
            long rem = Duration.between(LocalDateTime.now(), tracking.getDueAt()).toDays();
            resp.setRemainingDays(rem);
        }

        Incident inc = tracking.getIncident();
        if (inc != null) {
            resp.setIncidentId(inc.getId());
            resp.setIncidentTitle(inc.getTitle());
            resp.setIncidentCategory(inc.getCategory() != null ? inc.getCategory().name() : null);
            resp.setIncidentStatus(inc.getStatus() != null ? inc.getStatus().name() : null);
            resp.setWardName(inc.getAssignedArea() != null ? inc.getAssignedArea().getName() : null);
        }

        ServiceRule rule = tracking.getServiceRule();
        if (rule != null) {
            resp.setServiceRuleId(rule.getId());
            resp.setServiceDescription(rule.getServiceDescription());
            resp.setDepartment(rule.getDepartment());
            resp.setIsOfficialSla(rule.getIsOfficialSla());
            resp.setVerificationStatus(rule.getVerificationStatus());
            resp.setStatutoryReference(rule.getStatutoryReference());
            resp.setTargetDays(rule.getTargetDays());
            resp.setWarningThresholdDays(rule.getWarningThresholdDays());
        }

        return resp;
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

    public String getIncidentTitle() {
        return incidentTitle;
    }

    public void setIncidentTitle(String incidentTitle) {
        this.incidentTitle = incidentTitle;
    }

    public String getIncidentCategory() {
        return incidentCategory;
    }

    public void setIncidentCategory(String incidentCategory) {
        this.incidentCategory = incidentCategory;
    }

    public String getIncidentStatus() {
        return incidentStatus;
    }

    public void setIncidentStatus(String incidentStatus) {
        this.incidentStatus = incidentStatus;
    }

    public String getWardName() {
        return wardName;
    }

    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    public Long getServiceRuleId() {
        return serviceRuleId;
    }

    public void setServiceRuleId(Long serviceRuleId) {
        this.serviceRuleId = serviceRuleId;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Boolean getIsOfficialSla() {
        return isOfficialSla;
    }

    public void setIsOfficialSla(Boolean officialSla) {
        isOfficialSla = officialSla;
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

    public Long getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(Long remainingDays) {
        this.remainingDays = remainingDays;
    }

    public String getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
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

    public LocalDateTime getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) {
        this.lastEvaluatedAt = lastEvaluatedAt;
    }
}
