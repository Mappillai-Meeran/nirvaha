package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.util.HaversineDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RiskIntelligenceService implements RiskPredictionService {

    // Configurable Signal Weights (sum = 1.0)
    public static final double WEIGHT_HISTORICAL = 0.20;
    public static final double WEIGHT_RECURRENCE = 0.20;
    public static final double WEIGHT_RECENT_ACTIVITY = 0.20;
    public static final double WEIGHT_UNRESOLVED_LOAD = 0.20;
    public static final double WEIGHT_SPATIAL_CONCENTRATION = 0.10;
    public static final double WEIGHT_WEATHER_IMPACT = 0.10;

    // Configurable Parameters & Decay Constant
    public static final double DECAY_LAMBDA = 0.1; // e^(-lambda * days)
    public static final double MAX_SPATIAL_DISTANCE_METERS = 500.0;
    public static final double WARD_RADIUS_KM = 3.0; // Spatial proximity threshold to associate incident with ward center

    private final WardRepository wardRepository;
    private final IncidentRepository incidentRepository;
    private final ResolutionRepository resolutionRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final WeatherService weatherService;

    public RiskIntelligenceService(WardRepository wardRepository,
                                   IncidentRepository incidentRepository,
                                   ResolutionRepository resolutionRepository,
                                   RiskAssessmentRepository riskAssessmentRepository,
                                   WeatherService weatherService) {
        this.wardRepository = wardRepository;
        this.incidentRepository = incidentRepository;
        this.resolutionRepository = resolutionRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.weatherService = weatherService;
    }

    @Override
    @Transactional
    public WardRiskResponse predictWardRisk(Long wardId, IncidentCategory category) {
        return evaluateWardRisk(wardId, category);
    }

    @Transactional
    public WardRiskResponse evaluateWardRisk(Long wardId, IncidentCategory categoryFilter) {
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ward with ID " + wardId + " not found"));

        List<Incident> allWardIncidents = findIncidentsForWard(ward);
        
        // Target category for category-aware risk calculation
        IncidentCategory evalCategory = categoryFilter != null ? categoryFilter : determineTopCategoryForWard(allWardIncidents);
        List<Incident> categoryIncidents = allWardIncidents.stream()
                .filter(inc -> inc.getCategory() == evalCategory)
                .collect(Collectors.toList());

        // Fetch Weather Information via WeatherService abstraction
        WeatherData weather = weatherService.fetchWeatherData(ward.getLatitude(), ward.getLongitude());

        // 1. Historical Activity Signal (20%)
        double historicalScore = calculateHistoricalScore(categoryIncidents);

        // 2. Temporal Recurrence Signal (20%)
        double recurrenceScore = calculateRecurrenceScore(categoryIncidents);

        // 3. Recent Activity + Time Decay Signal (20%)
        double recentActivityScore = calculateRecentActivityScore(categoryIncidents);

        // 4. Unresolved Incident Load Signal (20%)
        double unresolvedLoadScore = calculateUnresolvedLoadScore(categoryIncidents);

        // 5. Spatial Concentration Signal (10%)
        double spatialConcentrationScore = calculateSpatialConcentrationScore(categoryIncidents);

        // 6. Environmental / Weather Impact Signal (10%)
        double categoryWeatherRelevance = getCategoryWeatherRelevance(evalCategory);
        double rawWeatherScore = calculateRawWeatherScore(weather);
        double weatherImpactScore = weather.getAvailable() ? (rawWeatherScore * categoryWeatherRelevance) : 0.0;

        // Score Normalization & Weight Allocation
        double finalRiskScore;
        if (weather.getAvailable()) {
            finalRiskScore = (historicalScore * WEIGHT_HISTORICAL)
                    + (recurrenceScore * WEIGHT_RECURRENCE)
                    + (recentActivityScore * WEIGHT_RECENT_ACTIVITY)
                    + (unresolvedLoadScore * WEIGHT_UNRESOLVED_LOAD)
                    + (spatialConcentrationScore * WEIGHT_SPATIAL_CONCENTRATION)
                    + (weatherImpactScore * WEIGHT_WEATHER_IMPACT);
        } else {
            // Weather fail-safe: redistribute weather weight budget (0.10) proportionally to civic factors
            double civicSum = (historicalScore * WEIGHT_HISTORICAL)
                    + (recurrenceScore * WEIGHT_RECURRENCE)
                    + (recentActivityScore * WEIGHT_RECENT_ACTIVITY)
                    + (unresolvedLoadScore * WEIGHT_UNRESOLVED_LOAD)
                    + (spatialConcentrationScore * WEIGHT_SPATIAL_CONCENTRATION);
            double civicWeightSum = WEIGHT_HISTORICAL + WEIGHT_RECURRENCE + WEIGHT_RECENT_ACTIVITY + WEIGHT_UNRESOLVED_LOAD + WEIGHT_SPATIAL_CONCENTRATION;
            finalRiskScore = civicSum / civicWeightSum;
        }

        finalRiskScore = Math.max(0.0, Math.min(1.0, finalRiskScore));
        finalRiskScore = BigDecimal.valueOf(finalRiskScore).setScale(2, RoundingMode.HALF_UP).doubleValue();

        int riskScore100 = (int) Math.round(finalRiskScore * 100);
        RiskLevel riskLevel = determineRiskLevel(finalRiskScore);

        // Factor Breakdown Construction
        List<RiskFactorContribution> factors = buildFactorContributions(
                historicalScore, recurrenceScore, recentActivityScore,
                unresolvedLoadScore, spatialConcentrationScore, weatherImpactScore,
                weather
        );

        String explanation = buildExplanation(ward.getName(), evalCategory, riskLevel, factors, weather, categoryIncidents.size());
        String recommendedAction = buildRecommendedAction(evalCategory, riskLevel, weather);

        // Persistence for auditability & trend tracking
        RiskAssessment assessment = new RiskAssessment();
        assessment.setWard(ward);
        assessment.setCategory(evalCategory);
        assessment.setRiskScore(finalRiskScore);
        assessment.setRiskLevel(riskLevel);
        assessment.setHistoricalScore(historicalScore);
        assessment.setRecentActivityScore(recentActivityScore);
        assessment.setRecurrenceScore(recurrenceScore);
        assessment.setUnresolvedLoadScore(unresolvedLoadScore);
        assessment.setSpatialConcentrationScore(spatialConcentrationScore);
        assessment.setWeatherImpactScore(weatherImpactScore);
        assessment.setWeatherContext(weather.getSourceMessage());
        assessment.setExplanation(explanation);
        assessment.setRecommendedAction(recommendedAction);
        assessment.setAssessmentTime(LocalDateTime.now());
        riskAssessmentRepository.save(assessment);

        int activeCount = (int) categoryIncidents.stream().filter(i -> i.getStatus() != IncidentStatus.RESOLVED).count();
        int unresolvedCount = (int) categoryIncidents.stream().filter(this::isIncidentUnresolved).count();

        return new WardRiskResponse(
                ward.getId(),
                ward.getWardNumber(),
                ward.getName(),
                finalRiskScore,
                riskScore100,
                riskLevel,
                evalCategory,
                activeCount,
                unresolvedCount,
                factors,
                explanation,
                recommendedAction,
                weather.getSourceMessage()
        );
    }

    @Transactional
    public List<WardRiskResponse> getAllWardRisks() {
        List<Ward> wards = wardRepository.findAll();
        List<WardRiskResponse> responses = new ArrayList<>();
        for (Ward ward : wards) {
            responses.add(evaluateWardRisk(ward.getId(), null));
        }
        return responses;
    }

    public RiskLevel determineRiskLevel(double score) {
        if (score >= 0.75) {
            return RiskLevel.CRITICAL;
        } else if (score >= 0.50) {
            return RiskLevel.HIGH;
        } else if (score >= 0.25) {
            return RiskLevel.MODERATE;
        } else {
            return RiskLevel.LOW;
        }
    }

    private double calculateHistoricalScore(List<Incident> incidents) {
        LocalDateTime now = LocalDateTime.now();
        long count7Days = incidents.stream()
                .filter(i -> i.getCreatedAt() != null && Math.abs(Duration.between(i.getCreatedAt(), now).toDays()) <= 7)
                .count();
        long count30Days = incidents.stream()
                .filter(i -> i.getCreatedAt() != null && Math.abs(Duration.between(i.getCreatedAt(), now).toDays()) <= 30)
                .count();

        double score7 = Math.min(1.0, count7Days / 5.0);
        double score30 = Math.min(1.0, count30Days / 15.0);
        return (score7 * 0.60) + (score30 * 0.40);
    }

    private double calculateRecurrenceScore(List<Incident> incidents) {
        if (incidents.isEmpty()) return 0.0;
        Set<String> yearMonths = incidents.stream()
                .filter(i -> i.getCreatedAt() != null)
                .map(i -> i.getCreatedAt().getYear() + "-" + i.getCreatedAt().getMonthValue())
                .collect(Collectors.toSet());

        return Math.min(1.0, yearMonths.size() / 4.0);
    }

    private double calculateRecentActivityScore(List<Incident> incidents) {
        if (incidents.isEmpty()) return 0.0;
        LocalDateTime now = LocalDateTime.now();
        double totalWeightedActivity = 0.0;

        for (Incident inc : incidents) {
            if (inc.getCreatedAt() == null) continue;
            double ageInDays = Math.max(0.0, Duration.between(inc.getCreatedAt(), now).toHours() / 24.0);
            double decayWeight = Math.exp(-DECAY_LAMBDA * ageInDays);
            totalWeightedActivity += decayWeight;
        }

        return Math.min(1.0, totalWeightedActivity / 3.0);
    }

    private double calculateUnresolvedLoadScore(List<Incident> incidents) {
        if (incidents.isEmpty()) return 0.0;
        long unresolvedCount = incidents.stream()
                .filter(this::isIncidentUnresolved)
                .count();

        return Math.min(1.0, unresolvedCount / 4.0);
    }

    private boolean isIncidentUnresolved(Incident incident) {
        if (incident.getStatus() == IncidentStatus.REPORTED ||
                incident.getStatus() == IncidentStatus.VERIFIED ||
                incident.getStatus() == IncidentStatus.IN_PROGRESS) {
            return true;
        }
        // If status is RESOLVED, check Phase 7 Resolution Verification status
        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            Optional<Resolution> resOpt = resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(incident.getId());
            if (resOpt.isPresent()) {
                VerificationStatus status = resOpt.get().getVerificationStatus();
                if (status == VerificationStatus.PENDING || status == VerificationStatus.MANUAL_REVIEW || status == VerificationStatus.FAILED) {
                    return true; // Pending/failed verification counts as unresolved operational burden
                }
            }
        }
        return false;
    }

    private double calculateSpatialConcentrationScore(List<Incident> incidents) {
        if (incidents.size() < 2) return 0.0;

        int maxClusterSize = 1;
        for (int i = 0; i < incidents.size(); i++) {
            Incident target = incidents.get(i);
            if (target.getLatitude() == null || target.getLongitude() == null) continue;
            int clusterSize = 1;

            for (int j = 0; j < incidents.size(); j++) {
                if (i == j) continue;
                Incident cand = incidents.get(j);
                if (cand.getLatitude() == null || cand.getLongitude() == null) continue;

                double distMeters = HaversineDistance.calculateDistanceMeters(
                        target.getLatitude(), target.getLongitude(),
                        cand.getLatitude(), cand.getLongitude()
                );
                if (distMeters <= MAX_SPATIAL_DISTANCE_METERS) {
                    clusterSize++;
                }
            }
            if (clusterSize > maxClusterSize) {
                maxClusterSize = clusterSize;
            }
        }

        return Math.min(1.0, (maxClusterSize - 1) / 3.0);
    }

    private double calculateRawWeatherScore(WeatherData weather) {
        if (!weather.getAvailable()) return 0.0;
        double recent = weather.getRecentRainfall() != null ? weather.getRecentRainfall() : 0.0;
        double forecast = weather.getForecastRainfall() != null ? weather.getForecastRainfall() : 0.0;
        double rainTotal = recent + forecast;
        double score = Math.min(1.0, rainTotal / 40.0);
        if (Boolean.TRUE.equals(weather.getIsStale())) {
            score *= 0.50; // Discount stale observation to prevent false escalation
        }
        return score;
    }

    private double getCategoryWeatherRelevance(IncidentCategory category) {
        if (category == null) return 0.2;
        switch (category) {
            case DRAINAGE:
                return 1.0;
            case POTHOLE:
                return 0.5;
            case WATER_SUPPLY:
                return 0.3;
            case GARBAGE:
                return 0.1;
            case STREETLIGHT:
            case OTHER:
            default:
                return 0.0;
        }
    }

    private List<Incident> findIncidentsForWard(Ward ward) {
        List<Incident> allIncidents = incidentRepository.findAll();
        List<Incident> wardIncidents = new ArrayList<>();

        for (Incident inc : allIncidents) {
            // SIMULATED DATA ISOLATION: Exclude test/stress-test records from production risk intelligence
            if (inc.getClassificationTag() == com.nirvaha.nirvaha_backend.entity.DataClassification.SIMULATED
                    || inc.getIntakeSource() == com.nirvaha.nirvaha_backend.entity.IncidentSource.SIMULATED) {
                continue;
            }
            if (inc.getLatitude() == null || inc.getLongitude() == null) continue;
            double distKm = HaversineDistance.calculateDistanceMeters(
                    ward.getLatitude(), ward.getLongitude(),
                    inc.getLatitude(), inc.getLongitude()
            ) / 1000.0;

            if (distKm <= WARD_RADIUS_KM) {
                wardIncidents.add(inc);
            }
        }
        return wardIncidents;
    }

    private IncidentCategory determineTopCategoryForWard(List<Incident> wardIncidents) {
        if (wardIncidents.isEmpty()) return IncidentCategory.DRAINAGE;
        Map<IncidentCategory, Long> categoryCounts = wardIncidents.stream()
                .collect(Collectors.groupingBy(Incident::getCategory, Collectors.counting()));
        return categoryCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(IncidentCategory.DRAINAGE);
    }

    private List<RiskFactorContribution> buildFactorContributions(double historical, double recurrence, double recent,
                                                                   double unresolved, double spatial, double weatherScore,
                                                                   WeatherData weather) {
        List<RiskFactorContribution> list = new ArrayList<>();

        double weightWeatherEff = weather.getAvailable() ? WEIGHT_WEATHER_IMPACT : 0.0;
        double totalWeight = WEIGHT_HISTORICAL + WEIGHT_RECURRENCE + WEIGHT_RECENT_ACTIVITY + WEIGHT_UNRESOLVED_LOAD + WEIGHT_SPATIAL_CONCENTRATION + weightWeatherEff;

        list.add(new RiskFactorContribution("Historical Activity", roundVal(historical), roundVal((historical * WEIGHT_HISTORICAL) / totalWeight), "Short and medium term volume trend"));
        list.add(new RiskFactorContribution("Temporal Recurrence", roundVal(recurrence), roundVal((recurrence * WEIGHT_RECURRENCE) / totalWeight), "Multi-month recurring issue pattern"));
        list.add(new RiskFactorContribution("Recent Activity / Time Decay", roundVal(recent), roundVal((recent * WEIGHT_RECENT_ACTIVITY) / totalWeight), "Time-decay weighted recent activity"));
        list.add(new RiskFactorContribution("Unresolved Load", roundVal(unresolved), roundVal((unresolved * WEIGHT_UNRESOLVED_LOAD) / totalWeight), "Unresolved & unverified operational load"));
        list.add(new RiskFactorContribution("Spatial Concentration", roundVal(spatial), roundVal((spatial * WEIGHT_SPATIAL_CONCENTRATION) / totalWeight), "Clustering density within 500m"));

        if (weather.getAvailable()) {
            String note = weather.getSourceMessage();
            if (Boolean.TRUE.equals(weather.getIsStale())) {
                note += " [STALE OBSERVATION - 50% DISCOUNT APPLIED]";
            }
            list.add(new RiskFactorContribution("Environmental Impact", roundVal(weatherScore), roundVal((weatherScore * WEIGHT_WEATHER_IMPACT) / totalWeight), note));
        } else {
            list.add(new RiskFactorContribution("Environmental Impact", 0.0, 0.0, weather.getSourceMessage()));
        }

        return list;
    }

    private Double roundVal(double val) {
        return BigDecimal.valueOf(val).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private String buildExplanation(String wardName, IncidentCategory category, RiskLevel level,
                                     List<RiskFactorContribution> factors, WeatherData weather, int totalIncidents) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Risk for %s in %s is %s. ", category.name().toLowerCase().replace('_', ' '), wardName, level.name()));

        if (totalIncidents == 0) {
            sb.append("No active or historical incident reports registered in this ward.");
        } else {
            sb.append(String.format("Driven by %d historical reports, unresolved load, and spatial clustering. ", totalIncidents));
        }

        double recent = weather.getRecentRainfall() != null ? weather.getRecentRainfall() : 0.0;
        double forecast = weather.getForecastRainfall() != null ? weather.getForecastRainfall() : 0.0;
        double totalRain = recent + forecast;

        if (!weather.getAvailable()) {
            sb.append("Weather data unavailable; risk calculated from civic history and spatial signals.");
        } else if (Boolean.TRUE.equals(weather.getIsStale())) {
            sb.append(String.format("Weather telemetry is stale (last observed %s); risk factor discounted.",
                    weather.getObservedAt() != null ? weather.getObservedAt().toString() : "previously"));
        } else if (totalRain > 0) {
            sb.append(String.format("Recent/forecast rainfall (%.1fmm) adds environmental risk context.", totalRain));
        }

        return sb.toString();
    }

    private String buildRecommendedAction(IncidentCategory category, RiskLevel level, WeatherData weather) {
        if (level == RiskLevel.LOW) {
            return "Routine monitoring. Standard sanitation and infrastructure maintenance schedule.";
        }

        switch (category) {
            case DRAINAGE:
                return (level == RiskLevel.CRITICAL || level == RiskLevel.HIGH)
                        ? "Inspect drainage hotspots and verify pumping/de-silting readiness before forecast rainfall."
                        : "Clear storm drains and schedule field inspection of drainage hotspots.";
            case POTHOLE:
                return "Prioritize field inspection and asphalt repair of concentrated pothole locations.";
            case GARBAGE:
                return "Increase sanitation collection and inspection frequency in the affected hotspot.";
            case STREETLIGHT:
                return "Prioritize field inspection and restoration of affected streetlight locations.";
            case WATER_SUPPLY:
                return "Deploy municipal water maintenance team to inspect supply pipeline pressure.";
            default:
                return "Schedule field inspection and proactive maintenance in the affected ward area.";
        }
    }
}
