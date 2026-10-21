package com.nirvaha.nirvaha_backend.dto;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentAction;
import com.nirvaha.nirvaha_backend.entity.IntelligenceSignal;

import java.util.List;

public class ClosedLoopChainDTO {

    private IncidentResponse incident;
    private List<EvidenceResponse> evidence;
    private PriorityFactors priorityFactors;
    private WardRiskResponse wardRisk;
    private List<IntelligenceSignal> intelligenceSignals;
    private ActionRecommendationDTO actionRecommendation;
    private List<OfficerDecisionResponse> officerDecisions;
    private List<IncidentActionResponse> workflowHistory;
    private ResolutionResponse resolution;
    private List<LearningSignalDTO> learningSignals;

    public ClosedLoopChainDTO() {
    }

    public ClosedLoopChainDTO(IncidentResponse incident, List<EvidenceResponse> evidence, PriorityFactors priorityFactors,
                              WardRiskResponse wardRisk, List<IntelligenceSignal> intelligenceSignals,
                              ActionRecommendationDTO actionRecommendation, List<OfficerDecisionResponse> officerDecisions,
                              List<IncidentActionResponse> workflowHistory, ResolutionResponse resolution,
                              List<LearningSignalDTO> learningSignals) {
        this.incident = incident;
        this.evidence = evidence;
        this.priorityFactors = priorityFactors;
        this.wardRisk = wardRisk;
        this.intelligenceSignals = intelligenceSignals;
        this.actionRecommendation = actionRecommendation;
        this.officerDecisions = officerDecisions;
        this.workflowHistory = workflowHistory;
        this.resolution = resolution;
        this.learningSignals = learningSignals;
    }

    public IncidentResponse getIncident() {
        return incident;
    }

    public void setIncident(IncidentResponse incident) {
        this.incident = incident;
    }

    public List<EvidenceResponse> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<EvidenceResponse> evidence) {
        this.evidence = evidence;
    }

    public PriorityFactors getPriorityFactors() {
        return priorityFactors;
    }

    public void setPriorityFactors(PriorityFactors priorityFactors) {
        this.priorityFactors = priorityFactors;
    }

    public WardRiskResponse getWardRisk() {
        return wardRisk;
    }

    public void setWardRisk(WardRiskResponse wardRisk) {
        this.wardRisk = wardRisk;
    }

    public List<IntelligenceSignal> getIntelligenceSignals() {
        return intelligenceSignals;
    }

    public void setIntelligenceSignals(List<IntelligenceSignal> intelligenceSignals) {
        this.intelligenceSignals = intelligenceSignals;
    }

    public ActionRecommendationDTO getActionRecommendation() {
        return actionRecommendation;
    }

    public void setActionRecommendation(ActionRecommendationDTO actionRecommendation) {
        this.actionRecommendation = actionRecommendation;
    }

    public List<OfficerDecisionResponse> getOfficerDecisions() {
        return officerDecisions;
    }

    public void setOfficerDecisions(List<OfficerDecisionResponse> officerDecisions) {
        this.officerDecisions = officerDecisions;
    }

    public List<IncidentActionResponse> getWorkflowHistory() {
        return workflowHistory;
    }

    public void setWorkflowHistory(List<IncidentActionResponse> workflowHistory) {
        this.workflowHistory = workflowHistory;
    }

    public ResolutionResponse getResolution() {
        return resolution;
    }

    public void setResolution(ResolutionResponse resolution) {
        this.resolution = resolution;
    }

    public List<LearningSignalDTO> getLearningSignals() {
        return learningSignals;
    }

    public void setLearningSignals(List<LearningSignalDTO> learningSignals) {
        this.learningSignals = learningSignals;
    }
}
