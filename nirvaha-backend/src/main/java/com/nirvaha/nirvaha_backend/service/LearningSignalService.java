package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.LearningSignalDTO;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentServiceTrackingRepository;
import com.nirvaha.nirvaha_backend.repository.ResolutionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LearningSignalService {

    public static final String LEARNING_MODEL_VERSION = "nirvaha-learning-v1";

    private final IncidentRepository incidentRepository;
    private final ResolutionRepository resolutionRepository;
    private final IncidentServiceTrackingRepository trackingRepository;
    private final IntelligenceEligibilityService eligibilityService;

    public LearningSignalService(IncidentRepository incidentRepository,
                                ResolutionRepository resolutionRepository,
                                IncidentServiceTrackingRepository trackingRepository,
                                IntelligenceEligibilityService eligibilityService) {
        this.incidentRepository = incidentRepository;
        this.resolutionRepository = resolutionRepository;
        this.trackingRepository = trackingRepository;
        this.eligibilityService = eligibilityService;
    }

    public List<LearningSignalDTO> evaluateLearningSignals() {
        List<Incident> allIncidents = incidentRepository.findAll();
        List<Incident> eligibleIncidents = eligibilityService.filterEligibleIncidents(allIncidents);
        LocalDateTime now = LocalDateTime.now();

        List<LearningSignalDTO> signals = new ArrayList<>();

        if (eligibleIncidents.isEmpty()) {
            return signals;
        }

        // 1. Elevated Reopening Rate Learning Signal
        List<Resolution> resolutions = resolutionRepository.findAll();
        List<Resolution> rejectedResolutions = resolutions.stream()
                .filter(r -> r.getVerificationStatus() == VerificationStatus.FAILED &&
                             eligibilityService.isEligibleForIntelligence(r.getIncident()))
                .collect(Collectors.toList());

        if (!rejectedResolutions.isEmpty()) {
            Map<IncidentCategory, List<Resolution>> byCategory = rejectedResolutions.stream()
                    .collect(Collectors.groupingBy(r -> r.getIncident().getCategory()));

            for (Map.Entry<IncidentCategory, List<Resolution>> entry : byCategory.entrySet()) {
                IncidentCategory cat = entry.getKey();
                List<Resolution> list = entry.getValue();
                if (list.size() >= 1) {
                    LearningSignalDTO sig = new LearningSignalDTO();
                    sig.setId("learn-reopen-" + cat.name().toLowerCase());
                    sig.setSignalType("ELEVATED_REOPEN_FREQUENCY");
                    sig.setCategory(cat);
                    sig.setAreaName("Municipal Wide");
                    sig.setObservation(String.format("Elevated resolution verification rejection rate observed for %s category (%d failed verifications).", cat.name().toLowerCase(), list.size()));
                    sig.setRecommendedStrategy("Review resolution evidence protocols and field repair verification standards for " + cat.name() + " incidents.");
                    sig.setSampleCount(list.size());
                    sig.setModelVersion(LEARNING_MODEL_VERSION);
                    sig.setGeneratedAt(now);
                    signals.add(sig);
                }
            }
        }

        // 2. SLA Service Standard Pressure Learning Signal
        List<IncidentServiceTracking> trackings = trackingRepository.findAll();
        List<IncidentServiceTracking> breachedTrackings = trackings.stream()
                .filter(t -> (t.getSlaStatus() == SlaStatus.OVERDUE || t.getSlaStatus() == SlaStatus.APPROACHING_DEADLINE) &&
                             eligibilityService.isEligibleForIntelligence(t.getIncident()))
                .collect(Collectors.toList());

        if (!breachedTrackings.isEmpty()) {
            Map<IncidentCategory, List<IncidentServiceTracking>> byCategory = breachedTrackings.stream()
                    .collect(Collectors.groupingBy(t -> t.getIncident().getCategory()));

            for (Map.Entry<IncidentCategory, List<IncidentServiceTracking>> entry : byCategory.entrySet()) {
                IncidentCategory cat = entry.getKey();
                List<IncidentServiceTracking> list = entry.getValue();
                if (list.size() >= 1) {
                    LearningSignalDTO sig = new LearningSignalDTO();
                    sig.setId("learn-sla-" + cat.name().toLowerCase());
                    sig.setSignalType("SLA_SERVICE_PRESSURE");
                    sig.setCategory(cat);
                    sig.setAreaName("Municipal Wide");
                    sig.setObservation(String.format("%s category frequently approaches or breaches designated SLA service standards (%d SLA pressure incidents).", cat.name().toLowerCase(), list.size()));
                    sig.setRecommendedStrategy("Evaluate resource allocation and dispatch capacity for " + cat.name() + " maintenance teams.");
                    sig.setSampleCount(list.size());
                    sig.setModelVersion(LEARNING_MODEL_VERSION);
                    sig.setGeneratedAt(now);
                    signals.add(sig);
                }
            }
        }

        // 3. Recurring Infrastructure Vulnerability Learning Signal
        Map<IncidentCategory, List<Incident>> byCatAll = eligibleIncidents.stream()
                .collect(Collectors.groupingBy(Incident::getCategory));

        for (Map.Entry<IncidentCategory, List<Incident>> entry : byCatAll.entrySet()) {
            IncidentCategory cat = entry.getKey();
            List<Incident> catList = entry.getValue();
            if (catList.size() >= 4) {
                LearningSignalDTO sig = new LearningSignalDTO();
                sig.setId("learn-vuln-" + cat.name().toLowerCase());
                sig.setSignalType("RECURRING_INFRASTRUCTURE_VULNERABILITY");
                sig.setCategory(cat);
                sig.setAreaName("High-Density Corridors");
                sig.setObservation(String.format("Persistent recurrence pattern detected for %s infrastructure across multiple reporting intervals (%d total reports).", cat.name().toLowerCase(), catList.size()));
                sig.setRecommendedStrategy("Initiate preventative structural overhaul rather than repeated reactive patch repairs.");
                sig.setSampleCount(catList.size());
                sig.setModelVersion(LEARNING_MODEL_VERSION);
                sig.setGeneratedAt(now);
                signals.add(sig);
            }
        }

        return signals;
    }
}
