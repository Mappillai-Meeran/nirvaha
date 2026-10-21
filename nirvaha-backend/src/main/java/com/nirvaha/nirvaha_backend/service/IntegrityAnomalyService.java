package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.AnomalyFlagResponse;
import com.nirvaha.nirvaha_backend.dto.IncidentAnomaliesResponse;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.util.HaversineDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IntegrityAnomalyService {

    public static final int BULK_CLOSURE_THRESHOLD = 5;
    public static final long BULK_CLOSURE_WINDOW_MINUTES = 60;

    public static final double REPEATED_REPORTING_DISTANCE_METERS = 500.0;
    public static final long REPEATED_REPORTING_WINDOW_MINUTES = 1440; // 24 hours
    public static final int REPEATED_REPORTING_MIN_COUNT = 3;

    private final IncidentRepository incidentRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final ResolutionRepository resolutionRepository;
    private final IncidentActionRepository incidentActionRepository;
    private final UserRepository userRepository;

    public IntegrityAnomalyService(IncidentRepository incidentRepository,
                                  AnomalyFlagRepository anomalyFlagRepository,
                                  ResolutionRepository resolutionRepository,
                                  IncidentActionRepository incidentActionRepository,
                                  UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.resolutionRepository = resolutionRepository;
        this.incidentActionRepository = incidentActionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public IncidentAnomaliesResponse detectAnomalies(Long incidentId) {
        Incident targetIncident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));

        List<AnomalyFlag> detectedFlags = new ArrayList<>();

        // Rule 1: Resolution Evidence Reuse
        detectResolutionEvidenceReuse(targetIncident).ifPresent(detectedFlags::add);

        // Rule 2: Suspicious Bulk Closures
        detectBulkClosures(targetIncident).ifPresent(detectedFlags::add);

        // Rule 3: Coordinated / Repeated Reporting
        detectRepeatedReporting(targetIncident).ifPresent(detectedFlags::add);

        // Deduplication & Persistence logic
        List<AnomalyFlag> persistedFlags = new ArrayList<>();
        for (AnomalyFlag newFlag : detectedFlags) {
            Optional<AnomalyFlag> existingOpt = anomalyFlagRepository
                    .findByIncidentIdAndAnomalyType(targetIncident.getId(), newFlag.getAnomalyType());

            if (existingOpt.isPresent()) {
                AnomalyFlag existing = existingOpt.get();
                existing.setDescription(newFlag.getDescription());
                existing.setSeverity(newFlag.getSeverity());
                existing.setRelatedIncidentIds(newFlag.getRelatedIncidentIds());
                persistedFlags.add(anomalyFlagRepository.save(existing));
            } else {
                persistedFlags.add(anomalyFlagRepository.save(newFlag));
            }
        }

        // Retrieve all current flags for the incident (including any previously detected ones)
        List<AnomalyFlagResponse> anomalyResponses = anomalyFlagRepository.findByIncidentId(incidentId)
                .stream()
                .map(AnomalyFlagResponse::fromEntity)
                .collect(Collectors.toList());

        return new IncidentAnomaliesResponse(incidentId, anomalyResponses);
    }

    @Transactional(readOnly = true)
    public IncidentAnomaliesResponse getAnomaliesForIncident(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident with ID " + incidentId + " not found");
        }

        List<AnomalyFlagResponse> anomalies = anomalyFlagRepository.findByIncidentId(incidentId)
                .stream()
                .map(AnomalyFlagResponse::fromEntity)
                .collect(Collectors.toList());

        return new IncidentAnomaliesResponse(incidentId, anomalies);
    }

    @Transactional(readOnly = true)
    public List<AnomalyFlagResponse> getAnomaliesForOfficer(Long officerId) {
        if (!userRepository.existsById(officerId)) {
            throw new ResourceNotFoundException("User with ID " + officerId + " not found");
        }

        List<IncidentAction> officerResolvedActions = incidentActionRepository
                .findByOfficerIdAndActionTypeOrderByCreatedAtDesc(officerId, ActionType.RESOLVED);

        Set<Long> incidentIds = officerResolvedActions.stream()
                .map(action -> action.getIncident().getId())
                .collect(Collectors.toSet());

        List<AnomalyFlag> officerAnomalies = new ArrayList<>();
        for (Long incId : incidentIds) {
            officerAnomalies.addAll(anomalyFlagRepository.findByIncidentId(incId));
        }

        return officerAnomalies.stream()
                .map(AnomalyFlagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AnomalyFlagResponse> getOpenAnomalies() {
        return anomalyFlagRepository.findByStatus(AnomalyStatus.OPEN).stream()
                .map(AnomalyFlagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private Optional<AnomalyFlag> detectResolutionEvidenceReuse(Incident targetIncident) {
        List<Resolution> targetResolutions = resolutionRepository.findByIncidentId(targetIncident.getId());
        if (targetResolutions.isEmpty()) {
            return Optional.empty();
        }

        Resolution targetResolution = targetResolutions.get(targetResolutions.size() - 1);
        Evidence targetEvidence = targetResolution.getResolutionEvidence();

        if (targetEvidence == null || targetEvidence.getFileUrl() == null || targetEvidence.getFileUrl().isBlank()) {
            return Optional.empty();
        }

        String targetUrl = targetEvidence.getFileUrl();
        List<Resolution> allResolutions = resolutionRepository.findAll();

        Set<Long> relatedIncidentIds = new TreeSet<>();
        for (Resolution res : allResolutions) {
            if (res.getIncident() == null || res.getIncident().getId().equals(targetIncident.getId())) {
                continue;
            }

            Evidence otherEvidence = res.getResolutionEvidence();
            if (otherEvidence != null && targetUrl.equalsIgnoreCase(otherEvidence.getFileUrl())) {
                relatedIncidentIds.add(res.getIncident().getId());
            }
        }

        if (relatedIncidentIds.isEmpty()) {
            return Optional.empty();
        }

        String relatedIdsStr = relatedIncidentIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        String description = "The same resolution evidence reference is associated with multiple incidents. Review recommended.";

        AnomalyFlag flag = new AnomalyFlag();
        flag.setIncident(targetIncident);
        flag.setAnomalyType(AnomalyType.RESOLUTION_EVIDENCE_REUSE);
        flag.setSeverity(AnomalySeverity.HIGH);
        flag.setDescription(description);
        flag.setRelatedIncidentIds(relatedIdsStr);
        flag.setStatus(AnomalyStatus.OPEN);

        return Optional.of(flag);
    }

    private Optional<AnomalyFlag> detectBulkClosures(Incident targetIncident) {
        List<IncidentAction> actions = incidentActionRepository.findByIncidentIdOrderByCreatedAtDesc(targetIncident.getId());

        IncidentAction resolveAction = actions.stream()
                .filter(a -> a.getActionType() == ActionType.RESOLVED)
                .findFirst()
                .orElse(null);

        if (resolveAction == null) {
            // Also check resolutions table if action record wasn't present directly
            Optional<Resolution> resOpt = resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(targetIncident.getId());
            if (resOpt.isEmpty()) {
                return Optional.empty();
            }
            User officer = resOpt.get().getOfficer();
            LocalDateTime targetTime = resOpt.get().getCreatedAt();
            return evaluateOfficerBulkClosure(targetIncident, officer, targetTime);
        }

        User officer = resolveAction.getOfficer();
        LocalDateTime targetTime = resolveAction.getCreatedAt();
        return evaluateOfficerBulkClosure(targetIncident, officer, targetTime);
    }

    private Optional<AnomalyFlag> evaluateOfficerBulkClosure(Incident targetIncident, User officer, LocalDateTime targetTime) {
        if (officer == null || targetTime == null) {
            return Optional.empty();
        }

        List<IncidentAction> officerResolvedActions = incidentActionRepository
                .findByOfficerIdAndActionTypeOrderByCreatedAtDesc(officer.getId(), ActionType.RESOLVED);

        // Find resolutions within rolling 60-minute window around target resolution time
        List<IncidentAction> actionsInWindow = officerResolvedActions.stream()
                .filter(a -> Math.abs(Duration.between(a.getCreatedAt(), targetTime).toMinutes()) <= BULK_CLOSURE_WINDOW_MINUTES)
                .collect(Collectors.toList());

        Set<Long> resolvedIncidentIdsInWindow = actionsInWindow.stream()
                .map(a -> a.getIncident().getId())
                .collect(Collectors.toCollection(TreeSet::new));

        // Also check resolutions table for officers who submitted resolution evidence
        List<Resolution> allResolutions = resolutionRepository.findAll();
        for (Resolution res : allResolutions) {
            if (res.getOfficer() != null && res.getOfficer().getId().equals(officer.getId()) && res.getCreatedAt() != null) {
                if (Math.abs(Duration.between(res.getCreatedAt(), targetTime).toMinutes()) <= BULK_CLOSURE_WINDOW_MINUTES) {
                    resolvedIncidentIdsInWindow.add(res.getIncident().getId());
                }
            }
        }

        int count = resolvedIncidentIdsInWindow.size();
        if (count < BULK_CLOSURE_THRESHOLD) {
            return Optional.empty();
        }

        Set<Long> relatedIds = new TreeSet<>(resolvedIncidentIdsInWindow);
        relatedIds.remove(targetIncident.getId());

        String relatedIdsStr = relatedIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        String description = String.format("%d incidents were marked resolved by the same officer within %d minutes. Review recommended.",
                count, BULK_CLOSURE_WINDOW_MINUTES);

        AnomalyFlag flag = new AnomalyFlag();
        flag.setIncident(targetIncident);
        flag.setAnomalyType(AnomalyType.BULK_CLOSURE);
        flag.setSeverity(AnomalySeverity.MEDIUM);
        flag.setDescription(description);
        flag.setRelatedIncidentIds(relatedIdsStr);
        flag.setStatus(AnomalyStatus.OPEN);

        return Optional.of(flag);
    }

    private Optional<AnomalyFlag> detectRepeatedReporting(Incident targetIncident) {
        if (targetIncident.getCategory() == null || targetIncident.getLatitude() == null || targetIncident.getLongitude() == null) {
            return Optional.empty();
        }

        List<Incident> candidates = incidentRepository.findByCategory(targetIncident.getCategory());
        Set<Long> matchingIncidentIds = new TreeSet<>();

        for (Incident candidate : candidates) {
            if (candidate.getLatitude() == null || candidate.getLongitude() == null || candidate.getCreatedAt() == null) {
                continue;
            }

            double distanceMeters = HaversineDistance.calculateDistanceMeters(
                    targetIncident.getLatitude(),
                    targetIncident.getLongitude(),
                    candidate.getLatitude(),
                    candidate.getLongitude()
            );

            long timeDiffMinutes = Math.abs(Duration.between(targetIncident.getCreatedAt(), candidate.getCreatedAt()).toMinutes());

            if (distanceMeters <= REPEATED_REPORTING_DISTANCE_METERS && timeDiffMinutes <= REPEATED_REPORTING_WINDOW_MINUTES) {
                matchingIncidentIds.add(candidate.getId());
            }
        }

        int totalMatchingCount = matchingIncidentIds.size();
        if (totalMatchingCount < REPEATED_REPORTING_MIN_COUNT) {
            return Optional.empty();
        }

        Set<Long> relatedIds = new TreeSet<>(matchingIncidentIds);
        relatedIds.remove(targetIncident.getId());

        String relatedIdsStr = relatedIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));

        String categoryName = targetIncident.getCategory().name().toLowerCase();
        String description = String.format("%d similar %s reports were submitted within %d metres and 24 hours. Review recommended.",
                totalMatchingCount, categoryName, (int) REPEATED_REPORTING_DISTANCE_METERS);

        AnomalyFlag flag = new AnomalyFlag();
        flag.setIncident(targetIncident);
        flag.setAnomalyType(AnomalyType.REPEATED_REPORTING);
        flag.setSeverity(AnomalySeverity.LOW);
        flag.setDescription(description);
        flag.setRelatedIncidentIds(relatedIdsStr);
        flag.setStatus(AnomalyStatus.OPEN);

        return Optional.of(flag);
    }
}
