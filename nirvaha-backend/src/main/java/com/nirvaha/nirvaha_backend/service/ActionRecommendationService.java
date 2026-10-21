package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.ActionRecommendationDTO;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ActionRecommendationService {

    public static final String ACTION_MODEL_VERSION = "nirvaha-action-v1";

    private final IntelligenceEligibilityService eligibilityService;
    private final EvidenceRepository evidenceRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final ResolutionRepository resolutionRepository;
    private final IncidentServiceTrackingRepository serviceTrackingRepository;

    public ActionRecommendationService(IntelligenceEligibilityService eligibilityService,
                                       EvidenceRepository evidenceRepository,
                                       AnomalyFlagRepository anomalyFlagRepository,
                                       ResolutionRepository resolutionRepository,
                                       IncidentServiceTrackingRepository serviceTrackingRepository) {
        this.eligibilityService = eligibilityService;
        this.evidenceRepository = evidenceRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.resolutionRepository = resolutionRepository;
        this.serviceTrackingRepository = serviceTrackingRepository;
    }

    public ActionRecommendationDTO generateRecommendation(Incident incident, PriorityLevel priorityLevel, RiskLevel riskLevel) {
        if (incident == null || !eligibilityService.isEligibleForIntelligence(incident)) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = incident.getCreatedAt() != null ? incident.getCreatedAt() : now;
        long ageHours = Math.max(0, Duration.between(created, now).toHours());

        List<Evidence> evidenceList = evidenceRepository.findByIncidentId(incident.getId());
        int evidenceCount = evidenceList != null ? evidenceList.size() : 0;

        List<AnomalyFlag> anomalies = anomalyFlagRepository.findByIncidentId(incident.getId()).stream()
                .filter(a -> a.getStatus() == AnomalyStatus.OPEN)
                .collect(Collectors.toList());
        boolean hasOpenAnomalies = !anomalies.isEmpty();

        List<Resolution> resolutions = resolutionRepository.findByIncidentId(incident.getId());
        boolean failedVerification = resolutions.stream()
                .anyMatch(r -> r.getVerificationStatus() == VerificationStatus.FAILED);

        Optional<IncidentServiceTracking> trackingOpt = serviceTrackingRepository.findByIncidentId(incident.getId());
        String slaState = trackingOpt.map(t -> t.getSlaStatus().name()).orElse("WITHIN_STANDARD");

        RecommendedActionType recommendedAction;
        String reason;
        String suggestedDept = mapCategoryToDepartment(incident.getCategory());
        String suggestedNextStep;
        List<String> supportingSignals = new ArrayList<>();

        if (failedVerification) {
            recommendedAction = RecommendedActionType.REOPEN_CASE;
            reason = "Resolution evidence failed verification. Similarity or visual proof insufficient.";
            suggestedNextStep = "Reopen incident and request field officer re-inspection with verified photographic proof.";
            supportingSignals.add("Resolution Verification Failed");
        } else if (hasOpenAnomalies) {
            recommendedAction = RecommendedActionType.REVIEW_ANOMALY;
            reason = "Integrity anomaly flag detected on this incident (" + anomalies.get(0).getAnomalyType() + ").";
            suggestedNextStep = "Review anomaly details and conduct audit before proceeding with workflow.";
            supportingSignals.add("Open Integrity Anomaly: " + anomalies.get(0).getAnomalyType());
        } else if (priorityLevel == PriorityLevel.CRITICAL || riskLevel == RiskLevel.CRITICAL || riskLevel == RiskLevel.HIGH) {
            recommendedAction = RecommendedActionType.FIELD_INSPECTION;
            reason = "High civic risk concentration or critical priority level in " + (incident.getAssignedArea() != null ? incident.getAssignedArea().getName() : "municipal jurisdiction") + ".";
            suggestedNextStep = "Deploy urgent field team to inspect site and initiate immediate containment.";
            supportingSignals.add("Priority Level: " + (priorityLevel != null ? priorityLevel.name() : "HIGH"));
            supportingSignals.add("Area Risk Level: " + (riskLevel != null ? riskLevel.name() : "HIGH"));
        } else if ("OVERDUE".equals(slaState) || "APPROACHING_DEADLINE".equals(slaState)) {
            recommendedAction = RecommendedActionType.START_REPAIR;
            reason = "Incident SLA status is " + slaState + " (" + ageHours + " hours open).";
            suggestedNextStep = "Accelerate repair dispatch and update SLA progress status.";
            supportingSignals.add("SLA Status: " + slaState);
        } else if (evidenceCount == 0) {
            recommendedAction = RecommendedActionType.REQUEST_MORE_EVIDENCE;
            reason = "No photographic evidence attached to incident report.";
            suggestedNextStep = "Request citizen or field team to upload verified geo-tagged photo evidence.";
            supportingSignals.add("Zero Evidence Attached");
        } else if (incident.getStatus() == IncidentStatus.REPORTED) {
            recommendedAction = RecommendedActionType.ASSIGN_DEPARTMENT;
            reason = "New incident report requiring departmental assignment.";
            suggestedNextStep = "Assign incident to designated " + suggestedDept + " field officer.";
            supportingSignals.add("Unassigned Incident Status");
        } else if (incident.getStatus() == IncidentStatus.RESOLVED && !resolutions.isEmpty()) {
            recommendedAction = RecommendedActionType.VERIFY_RESOLUTION;
            reason = "Resolution submitted by officer; awaiting administrative verification.";
            suggestedNextStep = "Review resolution evidence and confirm verification status.";
            supportingSignals.add("Pending Resolution Verification");
        } else {
            recommendedAction = RecommendedActionType.MONITOR;
            reason = "Incident currently progressing under standard operational procedures.";
            suggestedNextStep = "Monitor workflow status and await updates from field crew.";
            supportingSignals.add("Standard Operational Tracking");
        }

        String areaName = incident.getAssignedArea() != null ? incident.getAssignedArea().getName() : "Unassigned Area";

        ActionRecommendationDTO dto = new ActionRecommendationDTO();
        dto.setId("rec-" + incident.getId() + "-" + System.currentTimeMillis() % 10000);
        dto.setIncidentId(incident.getId());
        dto.setIncidentTitle(incident.getTitle());
        dto.setCategory(incident.getCategory());
        dto.setAreaName(areaName);
        dto.setPriorityLevel(priorityLevel != null ? priorityLevel : PriorityLevel.MEDIUM);
        dto.setRiskLevel(riskLevel != null ? riskLevel : RiskLevel.MODERATE);
        dto.setRecommendedAction(recommendedAction);
        dto.setReason(reason);
        dto.setSuggestedDepartment(suggestedDept);
        dto.setSuggestedNextStep(suggestedNextStep);
        dto.setSupportingSignals(supportingSignals);
        dto.setAgeHours(ageHours);
        dto.setSlaState(slaState);
        dto.setEvidenceCount(evidenceCount);
        dto.setCurrentStatus(incident.getStatus());
        dto.setModelVersion(ACTION_MODEL_VERSION);
        dto.setGeneratedAt(now);

        return dto;
    }

    public List<ActionRecommendationDTO> generateRecommendations(List<Incident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return List.of();
        }

        return incidents.stream()
                .filter(eligibilityService::isEligibleForIntelligence)
                .map(i -> generateRecommendation(i, PriorityLevel.HIGH, RiskLevel.MODERATE))
                .filter(rec -> rec != null)
                .collect(Collectors.toList());
    }

    private String mapCategoryToDepartment(IncidentCategory category) {
        if (category == null) return "MUNICIPAL OPERATIONS";
        switch (category) {
            case DRAINAGE:
            case POTHOLE:
                return "PUBLIC WORKS DEPARTMENT (PWD)";
            case GARBAGE:
                return "SANITATION & SOLID WASTE MANAGEMENT";
            case STREETLIGHT:
                return "ELECTRICAL & INFRASTRUCTURE";
            case WATER_SUPPLY:
                return "WATER SUPPLY & SEWERAGE BOARD";
            default:
                return "CIVIC SERVICES DEPARTMENT";
        }
    }
}
