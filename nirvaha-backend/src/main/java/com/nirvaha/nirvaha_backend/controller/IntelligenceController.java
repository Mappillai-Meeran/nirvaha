package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    private final CivicIntelligenceOrchestrator orchestrator;
    private final IncidentRepository incidentRepository;
    private final IntelligenceEligibilityService eligibilityService;
    private final TemporalAggregationService temporalService;
    private final TrendAnalysisService trendService;
    private final HotspotDetectionService hotspotService;
    private final EmergingIssueService emergingService;
    private final UnresolvedLoadService unresolvedService;
    private final ResolutionPerformanceService resolutionService;

    public IntelligenceController(CivicIntelligenceOrchestrator orchestrator,
                                  IncidentRepository incidentRepository,
                                  IntelligenceEligibilityService eligibilityService,
                                  TemporalAggregationService temporalService,
                                  TrendAnalysisService trendService,
                                  HotspotDetectionService hotspotService,
                                  EmergingIssueService emergingService,
                                  UnresolvedLoadService unresolvedService,
                                  ResolutionPerformanceService resolutionService) {
        this.orchestrator = orchestrator;
        this.incidentRepository = incidentRepository;
        this.eligibilityService = eligibilityService;
        this.temporalService = temporalService;
        this.trendService = trendService;
        this.hotspotService = hotspotService;
        this.emergingService = emergingService;
        this.unresolvedService = unresolvedService;
        this.resolutionService = resolutionService;
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getIntelligenceOverview() {
        List<Incident> all = incidentRepository.findAll();
        List<Incident> eligible = eligibilityService.filterEligibleIncidents(all);
        List<Incident> eligibleGeospatial = eligibilityService.filterEligibleGeospatialIncidents(all);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime d7Start = temporalService.calculateWindowStart(TimeWindow.D7, now);
        LocalDateTime d7PrevStart = temporalService.calculatePreviousWindowStart(TimeWindow.D7, now);

        List<Incident> cur7d = temporalService.filterIncidentsInWindow(eligible, d7Start, now);
        List<Incident> prev7d = temporalService.filterIncidentsInWindow(eligible, d7PrevStart, d7Start);

        List<HotspotDetectionService.SpatialCluster> hotspots = hotspotService.detectHotspots(eligibleGeospatial);
        List<EmergingIssueService.EmergingCandidate> emerging = emergingService.detectEmergingIssues(cur7d, prev7d, trendService);
        UnresolvedLoadService.UnresolvedLoadSummary unresolvedSummary = unresolvedService.calculateUnresolvedLoad(eligible);
        ResolutionPerformanceService.SystemResolutionMetrics systemMetrics = resolutionService.calculateSystemMetrics(eligible);
        List<IntelligenceSignal> activeSignals = orchestrator.getActiveSignals(null, null, null);

        Map<String, Object> response = new HashMap<>();
        response.put("activeSignalsCount", activeSignals.size());
        response.put("hotspotsCount", hotspots.size());
        response.put("emergingIssuesCount", emerging.size());
        response.put("unresolvedLoadSummary", unresolvedSummary);
        response.put("systemResolutionMetrics", systemMetrics);
        response.put("modelVersion", CivicIntelligenceOrchestrator.MODEL_VERSION);
        response.put("evaluatedAt", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/signals")
    public ResponseEntity<List<IntelligenceSignal>> getSignals(
            @RequestParam(required = false) SignalType type,
            @RequestParam(required = false) IncidentCategory category,
            @RequestParam(required = false) TimeWindow window) {
        return ResponseEntity.ok(orchestrator.getActiveSignals(type, category, window));
    }

    @GetMapping("/hotspots")
    public ResponseEntity<List<HotspotDetectionService.SpatialCluster>> getHotspots() {
        List<Incident> eligibleGeospatial = eligibilityService.filterEligibleGeospatialIncidents(incidentRepository.findAll());
        return ResponseEntity.ok(hotspotService.detectHotspots(eligibleGeospatial));
    }

    @GetMapping("/emerging-issues")
    public ResponseEntity<List<EmergingIssueService.EmergingCandidate>> getEmergingIssues() {
        List<Incident> eligible = eligibilityService.filterEligibleIncidents(incidentRepository.findAll());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime d7Start = temporalService.calculateWindowStart(TimeWindow.D7, now);
        LocalDateTime d7PrevStart = temporalService.calculatePreviousWindowStart(TimeWindow.D7, now);

        List<Incident> cur7d = temporalService.filterIncidentsInWindow(eligible, d7Start, now);
        List<Incident> prev7d = temporalService.filterIncidentsInWindow(eligible, d7PrevStart, d7Start);

        return ResponseEntity.ok(emergingService.detectEmergingIssues(cur7d, prev7d, trendService));
    }

    @GetMapping("/unresolved-load")
    public ResponseEntity<UnresolvedLoadService.UnresolvedLoadSummary> getUnresolvedLoad() {
        List<Incident> eligible = eligibilityService.filterEligibleIncidents(incidentRepository.findAll());
        return ResponseEntity.ok(unresolvedService.calculateUnresolvedLoad(eligible));
    }

    @GetMapping("/resolution-performance")
    public ResponseEntity<ResolutionPerformanceService.SystemResolutionMetrics> getResolutionPerformance() {
        List<Incident> eligible = eligibilityService.filterEligibleIncidents(incidentRepository.findAll());
        return ResponseEntity.ok(resolutionService.calculateSystemMetrics(eligible));
    }
}
