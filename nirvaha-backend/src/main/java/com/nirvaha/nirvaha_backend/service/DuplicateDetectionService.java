package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.DuplicateDetectionResponse;
import com.nirvaha.nirvaha_backend.dto.DuplicateMatch;
import com.nirvaha.nirvaha_backend.dto.DuplicateMatchReason;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentSource;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.security.IncidentAccessValidator;
import com.nirvaha.nirvaha_backend.util.HaversineDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DuplicateDetectionService {

    // Configurable thresholds for deterministic duplicate scoring
    public static final double MAX_LOCATION_THRESHOLD_METERS = 500.0;
    public static final long MAX_TIME_THRESHOLD_MINUTES = 1440; // 24 hours

    public static final double WEIGHT_CATEGORY_MATCH = 0.40;
    public static final double WEIGHT_LOCATION_MATCH = 0.40;
    public static final double WEIGHT_TIME_MATCH = 0.20;

    private final IncidentRepository incidentRepository;
    private final IncidentAccessValidator incidentAccessValidator;

    public DuplicateDetectionService(IncidentRepository incidentRepository,
                                     IncidentAccessValidator incidentAccessValidator) {
        this.incidentRepository = incidentRepository;
        this.incidentAccessValidator = incidentAccessValidator;
    }

    @Transactional(readOnly = true)
    public DuplicateDetectionResponse findDuplicatesForIncident(Long incidentId) {
        Incident targetIncident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));

        incidentAccessValidator.verifyIncidentAccess(targetIncident);

        List<Incident> candidates = incidentRepository.findByCategory(targetIncident.getCategory());
        List<DuplicateMatch> matches = new ArrayList<>();

        for (Incident candidate : candidates) {
            // Rule 1: Do not compare an incident with itself
            if (candidate.getId().equals(targetIncident.getId())) {
                continue;
            }

            // Real-data isolation: official incidents must never match against simulated records
            if (targetIncident.getIntakeSource() != IncidentSource.SIMULATED &&
                    candidate.getIntakeSource() == IncidentSource.SIMULATED) {
                continue;
            }

            // Distance calculation
            double distanceMeters = HaversineDistance.calculateDistanceMeters(
                    targetIncident.getLatitude(),
                    targetIncident.getLongitude(),
                    candidate.getLatitude(),
                    candidate.getLongitude()
            );

            // Rule 2: Location must be within distance threshold to be considered a physical duplicate
            if (distanceMeters > MAX_LOCATION_THRESHOLD_METERS) {
                continue;
            }

            List<DuplicateMatchReason> reasons = new ArrayList<>();
            double score = 0.0;

            // Category match check (+0.40)
            reasons.add(DuplicateMatchReason.SAME_CATEGORY);
            score += WEIGHT_CATEGORY_MATCH;

            // Location match check (+0.40)
            reasons.add(DuplicateMatchReason.LOCATION_WITHIN_THRESHOLD);
            score += WEIGHT_LOCATION_MATCH;

            // Time difference calculation (+0.20 if within time window)
            long timeDiffMinutes = calculateTimeDifferenceMinutes(targetIncident.getCreatedAt(), candidate.getCreatedAt());
            if (timeDiffMinutes <= MAX_TIME_THRESHOLD_MINUTES) {
                score += WEIGHT_TIME_MATCH;
                reasons.add(DuplicateMatchReason.TIME_WITHIN_THRESHOLD);
            }

            double roundedScore = BigDecimal.valueOf(score)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            double roundedDistance = BigDecimal.valueOf(distanceMeters)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();

            matches.add(new DuplicateMatch(
                    candidate.getId(),
                    roundedScore,
                    roundedDistance,
                    timeDiffMinutes,
                    reasons
            ));
        }

        // Sort results: highest similarity score first, ties broken by closest distance
        matches.sort(Comparator
                .comparing(DuplicateMatch::getDuplicateSimilarityScore).reversed()
                .thenComparing(DuplicateMatch::getDistanceMeters));

        return new DuplicateDetectionResponse(incidentId, matches);
    }

    private long calculateTimeDifferenceMinutes(LocalDateTime t1, LocalDateTime t2) {
        if (t1 == null || t2 == null) {
            return Long.MAX_VALUE;
        }
        return Math.abs(Duration.between(t1, t2).toMinutes());
    }
}
