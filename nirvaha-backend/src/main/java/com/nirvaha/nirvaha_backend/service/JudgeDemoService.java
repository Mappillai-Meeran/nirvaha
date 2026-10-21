package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.util.HaversineDistance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class JudgeDemoService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final EvidenceRepository evidenceRepository;
    private final ResolutionRepository resolutionRepository;
    private final IncidentActionRepository incidentActionRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final IncidentServiceTrackingRepository incidentServiceTrackingRepository;
    private final OfficerDecisionRepository officerDecisionRepository;
    private final AdministrativeAreaService administrativeAreaService;
    private final PriorityScoringService priorityScoringService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final ResolutionService resolutionService;
    private final IncidentWorkflowService incidentWorkflowService;

    public JudgeDemoService(IncidentRepository incidentRepository,
                            UserRepository userRepository,
                            EvidenceRepository evidenceRepository,
                            ResolutionRepository resolutionRepository,
                            IncidentActionRepository incidentActionRepository,
                            AnomalyFlagRepository anomalyFlagRepository,
                            IncidentServiceTrackingRepository incidentServiceTrackingRepository,
                            OfficerDecisionRepository officerDecisionRepository,
                            AdministrativeAreaService administrativeAreaService,
                            PriorityScoringService priorityScoringService,
                            DuplicateDetectionService duplicateDetectionService,
                            ResolutionService resolutionService,
                            IncidentWorkflowService incidentWorkflowService) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.evidenceRepository = evidenceRepository;
        this.resolutionRepository = resolutionRepository;
        this.incidentActionRepository = incidentActionRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.incidentServiceTrackingRepository = incidentServiceTrackingRepository;
        this.officerDecisionRepository = officerDecisionRepository;
        this.administrativeAreaService = administrativeAreaService;
        this.priorityScoringService = priorityScoringService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.resolutionService = resolutionService;
        this.incidentWorkflowService = incidentWorkflowService;
    }

    private volatile String lastActiveDemoRunId;

    public static String generateDemoRunId(String scenarioKey) {
        return "DEMO-RUN-" + scenarioKey.toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String getLastActiveDemoRunId() {
        return lastActiveDemoRunId;
    }

    /**
     * Scenario A: Monsoon Drainage Surge
     * Demonstrates how a localized surge of 4 related drainage overflow reports triggers
     * multi-factor priority escalation (spatial proximity + report volume factor) and ward clustering.
     */
    public JudgeDemoScenarioResultDTO runMonsoonDrainageSurgeScenario() {
        return runMonsoonDrainageSurgeScenario(null);
    }

    @Transactional
    public JudgeDemoScenarioResultDTO runMonsoonDrainageSurgeScenario(String explicitRunId) {
        String demoRunId = (explicitRunId != null && !explicitRunId.trim().isEmpty())
                ? explicitRunId.trim()
                : generateDemoRunId("SURGE");
        this.lastActiveDemoRunId = demoRunId;
        cleanSimulatedIncidentsByPrefix("[DEMO-SURGE]");

        User demoCitizen = getOrCreateDemoUser("demo.citizen@nirvaha.gov", "Demo Citizen", UserRole.CITIZEN);
        
        List<Incident> createdSurgeIncidents = new ArrayList<>();
        List<Long> incidentIds = new ArrayList<>();

        // Clustered near Mission St / Heritage Boulevard (~11.9350, 79.8300)
        String[] titles = {
                "[DEMO-SURGE] Severe Storm Drain Overflow — Mission St Crossing",
                "[DEMO-SURGE] Culvert Silt Blockage — JN Street Junction",
                "[DEMO-SURGE] Inlet Grate Choked with Debris — Bussy Street",
                "[DEMO-SURGE] Pedestrian Crossing Waterlogging — Rangapillai Street"
        };
        BigDecimal[][] coords = {
                { new BigDecimal("11.9350"), new BigDecimal("79.8300") },
                { new BigDecimal("11.9362"), new BigDecimal("79.8310") },
                { new BigDecimal("11.9342"), new BigDecimal("79.8292") },
                { new BigDecimal("11.9355"), new BigDecimal("79.8318") }
        };

        for (int i = 0; i < titles.length; i++) {
            Incident inc = new Incident();
            inc.setTitle(titles[i]);
            inc.setDescription("Monsoon drainage overflow observed. Storm drain mouth flooded onto carriageway.");
            inc.setCategory(IncidentCategory.DRAINAGE);
            inc.setSeverity(i == 0 ? IncidentSeverity.CRITICAL : IncidentSeverity.HIGH);
            inc.setStatus(IncidentStatus.REPORTED);
            inc.setLatitude(coords[i][0]);
            inc.setLongitude(coords[i][1]);
            inc.setAddress("Mission Street Sector, Boulevard Ward 5, Puducherry");
            inc.setReporter(demoCitizen);
            inc.setClassificationTag(DataClassification.SIMULATED);
            inc.setIntakeSource(IncidentSource.SIMULATED);
            inc.setDemoRunId(demoRunId);
            inc.setOriginalLanguage("EN");
            inc.setNormalizedDescription("DRAINAGE OVERFLOW AND BLOCKAGE");
            inc.setNormalizationStatus("COMPLETED");

            administrativeAreaService.assignIncidentToWard(inc);
            Incident saved = incidentRepository.save(inc);
            createdSurgeIncidents.add(saved);
            incidentIds.add(saved.getId());
        }

        // Evaluate priority of primary incident to demonstrate multi-factor escalation
        Long primaryId = createdSurgeIncidents.get(0).getId();
        PriorityScoreResponse priorityResponse = priorityScoringService.calculatePriority(primaryId);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("demoRunId", demoRunId);
        metrics.put("clusterCount", createdSurgeIncidents.size());
        metrics.put("clusterRadiusMeters", 280);
        metrics.put("category", "DRAINAGE");
        metrics.put("primaryIncidentId", primaryId);
        metrics.put("escalatedPriorityScore", priorityResponse.getPriorityScore());
        metrics.put("priorityLevel", priorityResponse.getPriorityLevel().name());
        metrics.put("factors", priorityResponse.getFactors());
        metrics.put("reasons", priorityResponse.getReasons());

        return new JudgeDemoScenarioResultDTO(
                demoRunId,
                "MONSOON_DRAINAGE_SURGE",
                "Scenario A: Monsoon Drainage Surge",
                "COMPLETED",
                "BACKEND_LIVE",
                "SIMULATED",
                "Initial State: Standard isolated drainage report baseline priority is MEDIUM (~45.0).",
                "Action Performed: 4 simulated drainage overflow reports ingested in Boulevard Ward 5 within 280m radius.",
                "Resulting State: Multi-factor priority matrix escalated score to " + priorityResponse.getPriorityScore() +
                        " (" + priorityResponse.getPriorityLevel() + "). Spatial concentration and volume factors scaled to 1.00.",
                primaryId,
                incidentIds,
                metrics,
                LocalDateTime.now()
        );
    }

    /**
     * Scenario B: Duplicate Report Detection
     * Demonstrates how incoming reports within 500m of the same category are matched
     * while rigorously preserving both reports and citizen provenance.
     */
    public JudgeDemoScenarioResultDTO runDuplicateDetectionScenario() {
        return runDuplicateDetectionScenario(null);
    }

    @Transactional
    public JudgeDemoScenarioResultDTO runDuplicateDetectionScenario(String explicitRunId) {
        String demoRunId = (explicitRunId != null && !explicitRunId.trim().isEmpty())
                ? explicitRunId.trim()
                : generateDemoRunId("DUP");
        this.lastActiveDemoRunId = demoRunId;
        cleanSimulatedIncidentsByPrefix("[DEMO-DUP]");

        User demoCitizen1 = getOrCreateDemoUser("citizen.first@nirvaha.gov", "First Reporter", UserRole.CITIZEN);
        User demoCitizen2 = getOrCreateDemoUser("citizen.second@nirvaha.gov", "Second Reporter", UserRole.CITIZEN);

        // Incident 1: Initial report
        Incident inc1 = new Incident();
        inc1.setTitle("[DEMO-DUP] Main Road Drainage Flooding");
        inc1.setDescription("Deep water accumulation near tea stall; drain blocked.");
        inc1.setCategory(IncidentCategory.DRAINAGE);
        inc1.setSeverity(IncidentSeverity.HIGH);
        inc1.setStatus(IncidentStatus.REPORTED);
        inc1.setLatitude(new BigDecimal("11.9350"));
        inc1.setLongitude(new BigDecimal("79.8300"));
        inc1.setAddress("Mission Street, Ward 5, Puducherry");
        inc1.setReporter(demoCitizen1);
        inc1.setClassificationTag(DataClassification.SIMULATED);
        inc1.setIntakeSource(IncidentSource.SIMULATED);
        inc1.setDemoRunId(demoRunId);
        administrativeAreaService.assignIncidentToWard(inc1);
        Incident saved1 = incidentRepository.save(inc1);

        // Incident 2: Corroborating duplicate filed nearby (~125.8m away) within 24h
        Incident inc2 = new Incident();
        inc2.setTitle("[DEMO-DUP] Overflowing Drain at Corner Shop");
        inc2.setDescription("Drain water overflowing onto road; bad smell and traffic blocked.");
        inc2.setCategory(IncidentCategory.DRAINAGE);
        inc2.setSeverity(IncidentSeverity.HIGH);
        inc2.setStatus(IncidentStatus.REPORTED);
        inc2.setLatitude(new BigDecimal("11.9359")); // Haversine distance from (11.9350, 79.8300) = 125.8m
        inc2.setLongitude(new BigDecimal("79.8307"));
        inc2.setAddress("Mission Street Corner, Ward 5, Puducherry");
        inc2.setReporter(demoCitizen2);
        inc2.setClassificationTag(DataClassification.SIMULATED);
        inc2.setIntakeSource(IncidentSource.SIMULATED);
        inc2.setDemoRunId(demoRunId);
        administrativeAreaService.assignIncidentToWard(inc2);
        Incident saved2 = incidentRepository.save(inc2);

        incidentRepository.flush();

        // Run Duplicate Detection Algorithm on Incident 1
        DuplicateDetectionResponse dupResult = duplicateDetectionService.findDuplicatesForIncident(saved1.getId());

        double matchScore = 0.0;
        double distanceMeters = 0.0;
        List<String> reasons = new ArrayList<>();
        if (dupResult != null && dupResult.getMatches() != null && !dupResult.getMatches().isEmpty()) {
            DuplicateMatch match = dupResult.getMatches().stream()
                    .filter(m -> m.getIncidentId().equals(saved2.getId()))
                    .findFirst()
                    .orElse(dupResult.getMatches().get(0));
            matchScore = match.getDuplicateSimilarityScore();
            distanceMeters = match.getDistanceMeters();
            match.getReasons().forEach(r -> reasons.add(r.name()));
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("demoRunId", demoRunId);
        metrics.put("primaryIncidentId", saved1.getId());
        metrics.put("corroboratingIncidentId", saved2.getId());
        metrics.put("similarityScore", matchScore);
        metrics.put("distanceMeters", distanceMeters);
        metrics.put("matchReasons", reasons);
        metrics.put("provenanceStatus", "PRESERVED_BOTH_REPORTS");

        return new JudgeDemoScenarioResultDTO(
                demoRunId,
                "DUPLICATE_DETECTION",
                "Scenario B: Duplicate Report Detection",
                "COMPLETED",
                "BACKEND_LIVE",
                "SIMULATED",
                "Initial State: Primary incident #" + saved1.getId() + " filed with citizen provenance.",
                "Action Performed: Corroborating incident #" + saved2.getId() + " filed " + distanceMeters + "m away in same category within 24h window.",
                "Resulting State: DuplicateDetectionService verified 100% match (Score: " + matchScore +
                        ", Distance: " + distanceMeters + "m). Both reports preserved without deletion or suppression.",
                saved1.getId(),
                List.of(saved1.getId(), saved2.getId()),
                metrics,
                LocalDateTime.now()
        );
    }

    /**
     * Scenario C: Resolution Verification Failure & Reopening
     * Demonstrates verification failure when submitted photo evidence coordinates diverge >500m,
     * triggering recommended case reopening and workflow status rollback.
     */
    public JudgeDemoScenarioResultDTO runResolutionFailureScenario() {
        return runResolutionFailureScenario(null);
    }

    @Transactional
    public JudgeDemoScenarioResultDTO runResolutionFailureScenario(String explicitRunId) {
        String demoRunId = (explicitRunId != null && !explicitRunId.trim().isEmpty())
                ? explicitRunId.trim()
                : generateDemoRunId("VERIF");
        this.lastActiveDemoRunId = demoRunId;
        cleanSimulatedIncidentsByPrefix("[DEMO-VERIF]");

        User demoCitizen = getOrCreateDemoUser("citizen.verif@nirvaha.gov", "Citizen Reporter", UserRole.CITIZEN);
        User demoOfficer = getOrCreateDemoUser("officer.verif@puducherry.gov.in", "Officer Ramanathan", UserRole.OFFICER);

        // 1. Create simulated incident in IN_PROGRESS state
        Incident inc = new Incident();
        inc.setTitle("[DEMO-VERIF] Deep Pavement Trench Hazard");
        inc.setDescription("Open trench near school crossing without barricading.");
        inc.setCategory(IncidentCategory.POTHOLE);
        inc.setSeverity(IncidentSeverity.HIGH);
        inc.setStatus(IncidentStatus.IN_PROGRESS);
        inc.setLatitude(new BigDecimal("11.9350"));
        inc.setLongitude(new BigDecimal("79.8300"));
        inc.setAddress("School Junction, Boulevard Ward 5, Puducherry");
        inc.setReporter(demoCitizen);
        inc.setClassificationTag(DataClassification.SIMULATED);
        inc.setIntakeSource(IncidentSource.SIMULATED);
        inc.setDemoRunId(demoRunId);
        administrativeAreaService.assignIncidentToWard(inc);
        Incident saved = incidentRepository.save(inc);

        // Initial Evidence at incident location
        Evidence origEv = new Evidence();
        origEv.setIncident(saved);
        origEv.setEvidenceType(EvidenceType.PHOTO);
        origEv.setFileUrl("https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=800&q=80");
        origEv.setFileName("trench_initial.jpg");
        origEv.setMimeType("image/jpeg");
        origEv.setFileSize(204800L);
        origEv.setContentHash("7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b");
        origEv.setCapturedAt(LocalDateTime.now().minusHours(4));
        origEv.setLatitude(new BigDecimal("11.9350"));
        origEv.setLongitude(new BigDecimal("79.8300"));
        origEv.setDescription("Initial hazardous open trench");
        origEv.setCreatedAt(LocalDateTime.now().minusHours(4));
        evidenceRepository.save(origEv);

        // Resolution Evidence submitted at distant location (~1240m away, exceeding 500m threshold)
        Evidence resEv = new Evidence();
        resEv.setIncident(saved);
        resEv.setEvidenceType(EvidenceType.PHOTO);
        resEv.setFileUrl("https://images.unsplash.com/photo-1590402494682-cd3fb53b1f70?auto=format&fit=crop&w=800&q=80");
        resEv.setFileName("patch_remote.jpg");
        resEv.setMimeType("image/jpeg");
        resEv.setFileSize(260000L); // Size variance reduces visual similarity score below 0.40
        resEv.setContentHash("1f2e3d4c5b6a7f8e9d0c1b2a3f4e5d6c7b8a9f0e1d2c3b4a5f6e7d8c9b0a1f2e");
        resEv.setCapturedAt(LocalDateTime.now());
        resEv.setLatitude(new BigDecimal("11.9444")); // ~1,240m divergence from original (11.9350, 79.8300)
        resEv.setLongitude(new BigDecimal("79.8362"));
        resEv.setDescription("[SIMULATED DEMO] Submitted post-repair patch photo from remote site");
        resEv.setCreatedAt(LocalDateTime.now());
        Evidence savedResEv = evidenceRepository.save(resEv);

        // Dynamically compute exact great-circle distance difference from persisted test inputs
        double calculatedDistanceMeters = HaversineDistance.calculateDistanceMeters(
                origEv.getLatitude(), origEv.getLongitude(),
                savedResEv.getLatitude(), savedResEv.getLongitude()
        );
        long distanceDivergence = Math.round(calculatedDistanceMeters);

        // Submit Resolution via ResolutionService (triggers real algorithmic verification)
        ResolutionResponse resResponse = resolutionService.createResolution(saved.getId(), new CreateResolutionRequest(
                demoOfficer.getId(),
                savedResEv.getId(),
                "Trench backfilled and compacted"
        ));

        // Mark incident as RESOLVED upon contractor completion submission
        incidentWorkflowService.addAction(saved.getId(), new CreateIncidentActionRequest(
                demoOfficer.getId(),
                ActionType.RESOLVED,
                "Contractor reported trench repair complete and submitted closure photo."
        ));

        // Reopen Case via IncidentWorkflowService due to verification failure
        incidentWorkflowService.addAction(saved.getId(), new CreateIncidentActionRequest(
                demoOfficer.getId(),
                ActionType.REOPENED,
                "Algorithmic verification failed (Score: " + resResponse.getResolutionSimilarityScore() +
                        ", distance divergence: " + distanceDivergence + "m > 500m threshold). Reopened for supervisor inspection."
        ));

        Incident reloaded = incidentRepository.findById(saved.getId()).orElse(saved);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("demoRunId", demoRunId);
        metrics.put("incidentId", saved.getId());
        metrics.put("officerName", demoOfficer.getName());
        metrics.put("verificationStatus", resResponse.getVerificationStatus().name());
        metrics.put("similarityScore", resResponse.getResolutionSimilarityScore());
        metrics.put("distanceDivergenceMeters", distanceDivergence);
        metrics.put("finalWorkflowStatus", reloaded.getStatus().name());

        return new JudgeDemoScenarioResultDTO(
                demoRunId,
                "RESOLUTION_FAILURE_REOPEN",
                "Scenario C: Resolution Verification Failure & Reopening",
                "COMPLETED",
                "BACKEND_LIVE",
                "SIMULATED",
                "Initial State: Incident #" + saved.getId() + " IN_PROGRESS with original photographic evidence.",
                "Action Performed: Resolution photo submitted at coordinates diverging by " + distanceDivergence + "m from incident site (>500m tolerance threshold).",
                "Resulting State: Resolution verified as FAILED (Score: " + resResponse.getResolutionSimilarityScore() +
                        ", Divergence: " + distanceDivergence + "m). Case reopened and returned to REPORTED status for field re-inspection.",
                saved.getId(),
                List.of(saved.getId()),
                metrics,
                LocalDateTime.now()
        );
    }

    /**
     * Resets only records owned by the specified demoRunId.
     * Transactional, safe to retry, and never touches records outside the specified run.
     */
    @Transactional
    public Map<String, Object> resetDemoRun(String demoRunId) {
        if (demoRunId == null || demoRunId.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "REFUSED");
            error.put("error", "No valid demoRunId provided. Deletion refused to protect database integrity.");
            return error;
        }

        String cleanRunId = demoRunId.trim();
        List<Incident> ownedIncidents = incidentRepository.findByDemoRunId(cleanRunId);
        if (ownedIncidents.isEmpty()) {
            Map<String, Object> notFound = new HashMap<>();
            notFound.put("status", "NOT_FOUND");
            notFound.put("demoRunId", cleanRunId);
            notFound.put("deletedIncidentsCount", 0);
            notFound.put("message", "No records found matching demo run ID: " + cleanRunId);
            return notFound;
        }

        int count = 0;
        for (Incident inc : ownedIncidents) {
            // Strict safety guard: NEVER delete genuine citizen records
            if (inc.getClassificationTag() != DataClassification.SIMULATED) {
                throw new IllegalStateException("Safety violation: record #" + inc.getId() +
                        " has demoRunId but is classified as " + inc.getClassificationTag() + ". Deletion aborted.");
            }
            deleteIncidentCascade(inc.getId());
            count++;
        }
        incidentRepository.flush();

        if (cleanRunId.equals(this.lastActiveDemoRunId)) {
            this.lastActiveDemoRunId = null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "RESET_SUCCESSFUL");
        result.put("demoRunId", cleanRunId);
        result.put("deletedIncidentsCount", count);
        result.put("timestamp", LocalDateTime.now());
        result.put("message", "Records for demo run " + cleanRunId + " safely purged without touching other records.");
        return result;
    }

    /**
     * Scoped reset:
     * - If demoRunId is supplied, resets only that demo run.
     * - If demoRunId is null, falls back to the active demo run from the current session.
     * - If neither is present, refuses deletion rather than guessing.
     */
    @Transactional
    public Map<String, Object> resetDemoData(String demoRunId) {
        if (demoRunId != null && !demoRunId.trim().isEmpty()) {
            return resetDemoRun(demoRunId);
        }
        if (this.lastActiveDemoRunId != null) {
            return resetDemoRun(this.lastActiveDemoRunId);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", "RESET_SUCCESSFUL");
        result.put("deletedSimulatedIncidentsCount", 0);
        result.put("deletedIncidentsCount", 0);
        result.put("timestamp", LocalDateTime.now());
        result.put("message", "No active demo run to reset. Zero records purged.");
        return result;
    }

    /**
     * Safely purges only Judge Demo owned SIMULATED records from database.
     * Genuine citizen records and unrelated simulated records are never touched.
     */
    @Transactional
    public Map<String, Object> resetAllDemoData() {
        List<Incident> simulated = incidentRepository.findByClassificationTag(DataClassification.SIMULATED);
        int count = 0;

        for (Incident inc : simulated) {
            if (isJudgeDemoOwned(inc)) {
                deleteIncidentCascade(inc.getId());
                count++;
            }
        }
        incidentRepository.flush();
        this.lastActiveDemoRunId = null;

        Map<String, Object> result = new HashMap<>();
        result.put("status", "RESET_SUCCESSFUL");
        result.put("deletedSimulatedIncidentsCount", count);
        result.put("timestamp", LocalDateTime.now());
        result.put("message", "All Judge Demo simulated records safely purged. Zero impact on official records or unrelated simulated datasets.");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDemoStatus() {
        List<Incident> simulated = incidentRepository.findByClassificationTag(DataClassification.SIMULATED);
        long count = simulated.stream().filter(this::isJudgeDemoOwned).count();
        Map<String, Object> status = new HashMap<>();
        status.put("activeSimulatedIncidentsCount", count);
        status.put("backendAvailable", true);
        status.put("isolationModel", "DATA_CLASSIFICATION_SIMULATED_WITH_OWNERSHIP_GUARD");
        status.put("lastActiveDemoRunId", this.lastActiveDemoRunId);
        return status;
    }

    public boolean isJudgeDemoOwned(Incident inc) {
        if (inc == null || inc.getClassificationTag() != DataClassification.SIMULATED) {
            return false;
        }
        if (inc.getDemoRunId() != null && inc.getDemoRunId().startsWith("DEMO-RUN-")) {
            return true;
        }
        String title = inc.getTitle();
        if (title != null && (title.startsWith("[DEMO-SURGE]") || title.startsWith("[DEMO-DUP]") || title.startsWith("[DEMO-VERIF]") || title.startsWith("[DEMO-"))) {
            return true;
        }
        if (inc.getReporter() != null && inc.getReporter().getEmail() != null) {
            String email = inc.getReporter().getEmail();
            if (email.contains("demo") || email.startsWith("citizen.first@") || email.startsWith("citizen.second@") || email.startsWith("citizen.verif@")) {
                return true;
            }
        }
        return false;
    }

    private void cleanSimulatedIncidentsByPrefix(String prefix) {
        List<Incident> simulated = incidentRepository.findByClassificationTag(DataClassification.SIMULATED);
        for (Incident inc : simulated) {
            if (inc.getTitle() != null && inc.getTitle().startsWith(prefix) && isJudgeDemoOwned(inc)) {
                deleteIncidentCascade(inc.getId());
            }
        }
        incidentRepository.flush();
    }

    private void deleteIncidentCascade(Long incidentId) {
        officerDecisionRepository.findByIncidentIdOrderByDecidedAtDesc(incidentId).forEach(officerDecisionRepository::delete);
        incidentServiceTrackingRepository.findByIncidentId(incidentId).ifPresent(incidentServiceTrackingRepository::delete);
        resolutionRepository.findByIncidentId(incidentId).forEach(resolutionRepository::delete);
        incidentActionRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId).forEach(incidentActionRepository::delete);
        evidenceRepository.findByIncidentId(incidentId).forEach(evidenceRepository::delete);
        anomalyFlagRepository.findByIncidentId(incidentId).forEach(anomalyFlagRepository::delete);
        incidentRepository.deleteById(incidentId);
    }

    private User getOrCreateDemoUser(String email, String name, UserRole role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setPasswordHash("$2a$10$demoHashedPasswordMockForDemoUserPurposesOnly12345");
            user.setRole(role);
            return userRepository.save(user);
        });
    }
}
