package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.AdministrativeAreaRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.IntelligenceSignalRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CivicIntelligenceOrchestrator {

    public static final String MODEL_VERSION = "nirvaha-intelligence-v1";

    private final IncidentRepository incidentRepository;
    private final AdministrativeAreaRepository areaRepository;
    private final IntelligenceSignalRepository signalRepository;

    private final IntelligenceEligibilityService eligibilityService;
    private final TemporalAggregationService temporalService;
    private final TrendAnalysisService trendService;
    private final HotspotDetectionService hotspotService;
    private final EmergingIssueService emergingService;
    private final RecurringProblemService recurringService;
    private final UnresolvedLoadService unresolvedService;
    private final ResolutionPerformanceService resolutionService;
    private final ImdWeatherAdapter weatherAdapter;

    public CivicIntelligenceOrchestrator(IncidentRepository incidentRepository,
                                         AdministrativeAreaRepository areaRepository,
                                         IntelligenceSignalRepository signalRepository,
                                         IntelligenceEligibilityService eligibilityService,
                                         TemporalAggregationService temporalService,
                                         TrendAnalysisService trendService,
                                         HotspotDetectionService hotspotService,
                                         EmergingIssueService emergingService,
                                         RecurringProblemService recurringService,
                                         UnresolvedLoadService unresolvedService,
                                         ResolutionPerformanceService resolutionService,
                                         ImdWeatherAdapter weatherAdapter) {
        this.incidentRepository = incidentRepository;
        this.areaRepository = areaRepository;
        this.signalRepository = signalRepository;
        this.eligibilityService = eligibilityService;
        this.temporalService = temporalService;
        this.trendService = trendService;
        this.hotspotService = hotspotService;
        this.emergingService = emergingService;
        this.recurringService = recurringService;
        this.unresolvedService = unresolvedService;
        this.resolutionService = resolutionService;
        this.weatherAdapter = weatherAdapter;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runInitialIntelligenceEvaluation() {
        evaluateAndPersistSignals();
    }

    @Transactional
    public List<IntelligenceSignal> evaluateAndPersistSignals() {
        List<Incident> allIncidents = incidentRepository.findAll();
        List<Incident> eligibleIncidents = eligibilityService.filterEligibleIncidents(allIncidents);
        List<Incident> eligibleGeospatial = eligibilityService.filterEligibleGeospatialIncidents(allIncidents);

        LocalDateTime now = LocalDateTime.now();
        List<IntelligenceSignal> generatedSignals = new ArrayList<>();

        // 1. Hotspot Signals (Spatial concentration within 500m)
        List<HotspotDetectionService.SpatialCluster> clusters = hotspotService.detectHotspots(eligibleGeospatial);
        for (HotspotDetectionService.SpatialCluster cluster : clusters) {
            IntelligenceSignal sig = new IntelligenceSignal();
            sig.setSignalType(SignalType.HOTSPOT);
            sig.setCategory(cluster.category);
            sig.setCentroidLat(cluster.centroidLat);
            sig.setCentroidLng(cluster.centroidLng);
            sig.setRadiusMeters(cluster.radiusMeters);
            sig.setTimeWindow(TimeWindow.D7);
            sig.setWindowStart(temporalService.calculateWindowStart(TimeWindow.D7, now));
            sig.setWindowEnd(now);
            sig.setIncidentCount(cluster.incidents.size());
            sig.setUnresolvedCount(cluster.unresolvedCount);
            sig.setConfidence(1.0);

            sig.setExplanationWhat("Category Hotspot Detected: Concentrated " + cluster.category.name().toLowerCase() + " activity.");
            sig.setExplanationWhere(String.format("Lat %.4f, Lng %.4f (Within %.0fm radius)", cluster.centroidLat, cluster.centroidLng, cluster.radiusMeters));
            sig.setExplanationWhen("Observed in recent 7-day reporting window.");
            sig.setExplanationWhy(String.format("%d incident reports recorded within approximately 500m density cluster.", cluster.incidents.size()));
            sig.setExplanationEvidence(String.format("%d total incidents, %d unresolved.", cluster.incidents.size(), cluster.unresolvedCount));
            sig.setExplanationSource("Nirvaha Citizen Evidence Network");
            sig.setModelVersion(MODEL_VERSION);

            generatedSignals.add(signalRepository.save(sig));
        }

        // 2. Emerging Issue Signals (Surging activity >= 50%)
        LocalDateTime d7Start = temporalService.calculateWindowStart(TimeWindow.D7, now);
        LocalDateTime d7PrevStart = temporalService.calculatePreviousWindowStart(TimeWindow.D7, now);

        List<Incident> cur7d = temporalService.filterIncidentsInWindow(eligibleIncidents, d7Start, now);
        List<Incident> prev7d = temporalService.filterIncidentsInWindow(eligibleIncidents, d7PrevStart, d7Start);

        List<EmergingIssueService.EmergingCandidate> emergingCandidates = emergingService.detectEmergingIssues(cur7d, prev7d, trendService);
        for (EmergingIssueService.EmergingCandidate cand : emergingCandidates) {
            IntelligenceSignal sig = new IntelligenceSignal();
            sig.setSignalType(SignalType.EMERGING_ISSUE);
            sig.setCategory(cand.category);
            sig.setTimeWindow(TimeWindow.D7);
            sig.setWindowStart(d7Start);
            sig.setWindowEnd(now);
            sig.setTrendDirection(TrendDirection.INCREASING);
            sig.setChangePercentage(cand.changePercentage);
            sig.setIncidentCount(cand.currentCount);
            sig.setPreviousPeriodCount(cand.previousCount);
            sig.setUnresolvedCount((int) cand.incidents.stream().filter(i -> i.getStatus() != IncidentStatus.RESOLVED).count());
            sig.setConfidence(0.95);

            sig.setExplanationWhat("Emerging Civic Issue: Rapid surge in " + cand.category.name().toLowerCase() + " reports.");
            sig.setExplanationWhere("Puducherry Municipal / Ward Jurisdictions");
            sig.setExplanationWhen("Last 7 Days vs Previous 7 Days");
            sig.setExplanationWhy(String.format("%d reports in current period vs %d in previous period (+%.1f%% surge).", cand.currentCount, cand.previousCount, cand.changePercentage));
            sig.setExplanationEvidence(String.format("Data support: %d reports recorded.", cand.currentCount));
            sig.setExplanationSource("Nirvaha Citizen Evidence Network + IMD Meteorological Observations");
            sig.setModelVersion(MODEL_VERSION);

            generatedSignals.add(signalRepository.save(sig));
        }

        // 3. Recurring Problem Signals (Repeated activity over multiple periods)
        LocalDateTime d30Start = temporalService.calculateWindowStart(TimeWindow.D30, now);
        List<Incident> last30d = temporalService.filterIncidentsInWindow(eligibleIncidents, d30Start, now);
        List<RecurringProblemService.RecurringCandidate> recurringCandidates = recurringService.detectRecurringProblems(last30d);

        for (RecurringProblemService.RecurringCandidate rec : recurringCandidates) {
            IntelligenceSignal sig = new IntelligenceSignal();
            sig.setSignalType(SignalType.RECURRING_PROBLEM);
            sig.setCategory(rec.category);
            sig.setTimeWindow(TimeWindow.D30);
            sig.setWindowStart(d30Start);
            sig.setWindowEnd(now);
            sig.setIncidentCount(rec.totalIncidents);
            sig.setUnresolvedCount((int) rec.incidents.stream().filter(i -> i.getStatus() != IncidentStatus.RESOLVED).count());
            sig.setConfidence(0.90);

            sig.setExplanationWhat("Recurring Civic Problem Identified");
            sig.setExplanationWhere("Puducherry Ward Jurisdiction");
            sig.setExplanationWhen("Last 30 Days (Multi-Period Analysis)");
            sig.setExplanationWhy(String.format("%s incidents were observed repeatedly across %d separate 7-day intervals during the last 30 days.", rec.category.name().toLowerCase(), rec.activePeriodsCount));
            sig.setExplanationEvidence(String.format("%d total incidents recorded across multiple intervals.", rec.totalIncidents));
            sig.setExplanationSource("Nirvaha Citizen Evidence Network");
            sig.setModelVersion(MODEL_VERSION);

            generatedSignals.add(signalRepository.save(sig));
        }

        return generatedSignals;
    }

    public List<IntelligenceSignal> getActiveSignals(SignalType type, IncidentCategory category, TimeWindow window) {
        return signalRepository.findFilteredSignals("ACTIVE", type, category, window);
    }
}
