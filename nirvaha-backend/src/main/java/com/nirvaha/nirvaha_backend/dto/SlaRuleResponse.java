package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.ServiceRule;

public class SlaRuleResponse {

    private Long id;
    private String department;
    private String serviceCategory;
    private String jurisdiction;
    private String serviceDescription;
    private Integer targetDays;
    private Integer warningThresholdDays;
    private String businessDayRule;
    private String escalationReference;
    private Boolean isOfficialSla;
    private String verificationStatus;
    private String statutoryReference;
    private String notes;

    public SlaRuleResponse() {
    }

    public static SlaRuleResponse fromEntity(ServiceRule rule) {
        if (rule == null) {
            return null;
        }

        SlaRuleResponse resp = new SlaRuleResponse();
        resp.setId(rule.getId());
        resp.setDepartment(rule.getDepartment());
        resp.setServiceCategory(rule.getServiceCategory() != null ? rule.getServiceCategory().name() : null);
        resp.setJurisdiction(rule.getJurisdiction());
        resp.setServiceDescription(rule.getServiceDescription());
        resp.setTargetDays(rule.getTargetDays());
        resp.setWarningThresholdDays(rule.getWarningThresholdDays());
        resp.setBusinessDayRule(rule.getBusinessDayRule());
        resp.setEscalationReference(rule.getEscalationReference());
        resp.setIsOfficialSla(rule.getIsOfficialSla());
        resp.setVerificationStatus(rule.getVerificationStatus());
        resp.setStatutoryReference(rule.getStatutoryReference());
        resp.setNotes(rule.getNotes());
        return resp;
    }

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

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
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

    public Integer getWarningThresholdDays() {
        return warningThresholdDays;
    }

    public void setWarningThresholdDays(Integer warningThresholdDays) {
        this.warningThresholdDays = warningThresholdDays;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
