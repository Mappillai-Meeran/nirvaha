package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.DuplicateDetectionResponse;
import com.nirvaha.nirvaha_backend.dto.PriorityFactors;
import com.nirvaha.nirvaha_backend.dto.PriorityScoreResponse;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.PriorityLevel;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.util.HaversineDistance;
import com.nirvaha.nirvaha_backend.entity.IncidentSource;
import com.nirvaha.nirvaha_backend.security.IncidentAccessValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PriorityScoringService {

    public static final double WEIGHT_SEVERITY = 0.40;
    public static final double WEIGHT_REPORT_VOLUME = 0.30;
    public static final double WEIGHT_LOCATION_IMPACT = 0.20;
    public static final double WEIGHT_RAINFALL = 0.10;

    public static final double NEARBY_DISTANCE_THRESHOLD_METERS = 500.0;

    private final IncidentRepository incidentRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final IncidentAccessValidator incidentAccessValidator;

    public PriorityScoringService(IncidentRepository incidentRepository,
                                  DuplicateDetectionService duplicateDetectionService,
                                  IncidentAccessValidator incidentAccessValidator) {
        this.incidentRepository = incidentRepository;
        this.duplicateDetectionService = duplicateDetectionService;
        this.incidentAccessValidator = incidentAccessValidator;
    }

    @Transactional(readOnly = true)
    public PriorityScoreResponse calculatePriority(Long incidentId) {
        return calculatePriority(incidentId, null);
    }

    @Transactional(readOnly = true)
    public PriorityScoreResponse calculatePriority(Long incidentId, Double rainfallMm) {
        Incident targetIncident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));

        incidentAccessValidator.verifyIncidentAccess(targetIncident);

        List<String> reasons = new ArrayList<>();

        // 1. Severity Factor
        double severityFactor = calculateSeverityFactor(targetIncident.getSeverity(), reasons);

        // 2. Report Volume Factor (reusing DuplicateDetectionService)
        double reportVolumeFactor = calculateReportVolumeFactor(targetIncident.getId(), reasons);

        // 3. Location / Local Impact Factor (considering same-category incidents within 500m)
        double locationImpactFactor = calculateLocationImpactFactor(targetIncident, reasons);

        // 4. Environmental / Rainfall Factor
        double rainfallFactor = calculateRainfallFactor(rainfallMm, reasons);

        // Calculate weighted score (0.0 to 1.0 -> 0.0 to 100.0)
        double rawWeightedScore = (severityFactor * WEIGHT_SEVERITY)
                + (reportVolumeFactor * WEIGHT_REPORT_VOLUME)
                + (locationImpactFactor * WEIGHT_LOCATION_IMPACT)
                + (rainfallFactor * WEIGHT_RAINFALL);

        double priorityScore = BigDecimal.valueOf(rawWeightedScore * 100.0)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        // Determine Priority Level
        PriorityLevel priorityLevel = determinePriorityLevel(priorityScore);

        PriorityFactors factors = new PriorityFactors(
                roundFactor(severityFactor),
                roundFactor(reportVolumeFactor),
                roundFactor(locationImpactFactor),
                roundFactor(rainfallFactor)
        );

        return new PriorityScoreResponse(incidentId, priorityScore, priorityLevel, factors, reasons);
    }

    private double calculateSeverityFactor(IncidentSeverity severity, List<String> reasons) {
        if (severity == null) {
            reasons.add("Incident severity unavailable; defaulting to LOW severity");
            return 0.25;
        }

        switch (severity) {
            case CRITICAL:
                reasons.add("Incident severity is CRITICAL");
                return 1.00;
            case HIGH:
                reasons.add("Incident severity is HIGH");
                return 0.75;
            case MEDIUM:
                reasons.add("Incident severity is MEDIUM");
                return 0.50;
            case LOW:
            default:
                reasons.add("Incident severity is LOW");
                return 0.25;
        }
    }

    private double calculateReportVolumeFactor(Long incidentId, List<String> reasons) {
        DuplicateDetectionResponse duplicateResponse = duplicateDetectionService.findDuplicatesForIncident(incidentId);
        int duplicateMatchesCount = (duplicateResponse != null && duplicateResponse.getMatches() != null)
                ? duplicateResponse.getMatches().size()
                : 0;

        int totalReports = 1 + duplicateMatchesCount;

        if (totalReports == 1) {
            reasons.add("Single report filed for this incident");
            return 0.00;
        } else if (totalReports == 2) {
            reasons.add("2 reports indicate the same civic issue");
            return 0.50;
        } else {
            reasons.add("Multiple reports (" + totalReports + ") indicate the same civic issue");
            return 1.00;
        }
    }

    private double calculateLocationImpactFactor(Incident targetIncident, List<String> reasons) {
        List<Incident> sameCategoryIncidents = incidentRepository.findByCategory(targetIncident.getCategory());

        long nearbySameCategoryCount = sameCategoryIncidents.stream()
                .filter(candidate -> candidate.getId() != null && !candidate.getId().equals(targetIncident.getId()))
                .filter(candidate -> candidate.getLatitude() != null && candidate.getLongitude() != null)
                .filter(candidate -> targetIncident.getIntakeSource() == IncidentSource.SIMULATED || candidate.getIntakeSource() != IncidentSource.SIMULATED)
                .filter(candidate -> {
                    double distance = HaversineDistance.calculateDistanceMeters(
                            targetIncident.getLatitude(),
                            targetIncident.getLongitude(),
                            candidate.getLatitude(),
                            candidate.getLongitude()
                    );
                    return distance <= NEARBY_DISTANCE_THRESHOLD_METERS;
                })
                .count();

        if (nearbySameCategoryCount == 0) {
            reasons.add("No nearby related incidents reported");
            return 0.00;
        } else if (nearbySameCategoryCount <= 2) {
            reasons.add("Nearby related incidents increase local impact");
            return 0.50;
        } else {
            reasons.add("Dense cluster of nearby related incidents (" + nearbySameCategoryCount + ") significantly increases local impact");
            return 1.00;
        }
    }

    private double calculateRainfallFactor(Double rainfallMm, List<String> reasons) {
        if (rainfallMm == null) {
            reasons.add("Rainfall data unavailable; no environmental contribution applied");
            return 0.00;
        } else if (rainfallMm <= 0.0) {
            reasons.add("No rainfall detected (0.0 mm); normal weather conditions");
            return 0.00;
        } else if (rainfallMm <= 10.0) {
            reasons.add(String.format("Light rainfall detected (%.1f mm)", rainfallMm));
            return 0.25;
        } else if (rainfallMm <= 35.0) {
            reasons.add(String.format("Moderate rainfall detected (%.1f mm)", rainfallMm));
            return 0.50;
        } else if (rainfallMm <= 75.0) {
            reasons.add(String.format("Heavy rainfall detected (%.1f mm)", rainfallMm));
            return 0.75;
        } else {
            reasons.add(String.format("Extreme rainfall detected (%.1f mm)", rainfallMm));
            return 1.00;
        }
    }

    private PriorityLevel determinePriorityLevel(double score) {
        if (score < 25.0) {
            return PriorityLevel.LOW;
        } else if (score < 50.0) {
            return PriorityLevel.MEDIUM;
        } else if (score < 75.0) {
            return PriorityLevel.HIGH;
        } else {
            return PriorityLevel.CRITICAL;
        }
    }

    private double roundFactor(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
