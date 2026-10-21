package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class JudgeDemoRuntimeAuditTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private JudgeDemoService judgeDemoService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private ResolutionRepository resolutionRepository;

    @Autowired
    private IncidentActionRepository incidentActionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private IntelligenceEligibilityService eligibilityService;

    private String citizenToken;
    private String officerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Safe cleanup of any previous demo/test records
        judgeDemoService.resetAllDemoData();

        // Create test users for authentication testing if not existing
        String citizenEmail = "audit.citizen@nirvaha.gov";
        String officerEmail = "audit.officer@puducherry.gov.in";
        String adminEmail = "audit.admin@puducherry.gov.in";
        String password = "auditPassword123!";

        if (userRepository.findByEmail(citizenEmail).isEmpty()) {
            userService.createUser(new CreateUserRequest("Audit Citizen", citizenEmail, password, UserRole.CITIZEN));
        }
        if (userRepository.findByEmail(officerEmail).isEmpty()) {
            userService.createUser(new CreateUserRequest("Audit Officer", officerEmail, password, UserRole.OFFICER));
        }
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            userService.createUser(new CreateUserRequest("Audit Admin", adminEmail, password, UserRole.ADMIN));
        }

        citizenToken = authService.login(new LoginRequest(citizenEmail, password)).getToken();
        officerToken = authService.login(new LoginRequest(officerEmail, password)).getToken();
        adminToken = authService.login(new LoginRequest(adminEmail, password)).getToken();
    }

    @Test
    @DisplayName("Scenario A Runtime: Monsoon Drainage Surge generates 4 SIMULATED incidents and escalates priority via live algorithm")
    void testMonsoonDrainageSurgeRuntime() {
        JudgeDemoScenarioResultDTO result = judgeDemoService.runMonsoonDrainageSurgeScenario();

        assertThat(result.getScenarioKey()).isEqualTo("MONSOON_DRAINAGE_SURGE");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getExecutionMode()).isEqualTo("BACKEND_LIVE");
        assertThat(result.getDataClassification()).isEqualTo("SIMULATED");

        // Verify database persistence of 4 simulated incidents
        List<Incident> simulated = incidentRepository.findByClassificationTag(DataClassification.SIMULATED);
        assertThat(simulated).hasSize(4);
        for (Incident inc : simulated) {
            assertThat(inc.getClassificationTag()).isEqualTo(DataClassification.SIMULATED);
            assertThat(inc.getCategory()).isEqualTo(IncidentCategory.DRAINAGE);
            assertThat(inc.getTitle()).startsWith("[DEMO-SURGE]");

            // Verify production isolation rule: IntelligenceEligibilityService must reject SIMULATED incidents
            assertThat(eligibilityService.isEligibleForIntelligence(inc))
                    .as("SIMULATED records must be excluded from civic intelligence")
                    .isFalse();
        }

        // Verify actual priority calculation
        Map<String, Object> metrics = result.getMetrics();
        assertThat(metrics.get("priorityLevel")).isEqualTo("CRITICAL");
        double score = Double.parseDouble(metrics.get("escalatedPriorityScore").toString());
        assertThat(score).isGreaterThanOrEqualTo(80.0);
    }

    @Test
    @DisplayName("Scenario B Runtime: Duplicate Detection evaluates candidate and preserves both records without deletion")
    void testDuplicateDetectionRuntime() {
        JudgeDemoScenarioResultDTO result = judgeDemoService.runDuplicateDetectionScenario();

        assertThat(result.getScenarioKey()).isEqualTo("DUPLICATE_DETECTION");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        // Verify database persistence of 2 separate records
        List<Incident> simulated = incidentRepository.findByClassificationTag(DataClassification.SIMULATED);
        assertThat(simulated).hasSize(2);

        Incident inc1 = incidentRepository.findById(result.getTargetIncidentId()).orElseThrow();
        Long secondId = result.getAffectedIncidentIds().stream()
                .filter(id -> !id.equals(result.getTargetIncidentId()))
                .findFirst()
                .orElseThrow();
        Incident inc2 = incidentRepository.findById(secondId).orElseThrow();

        assertThat(inc1.getId()).isNotEqualTo(inc2.getId());
        assertThat(inc1.getTitle()).startsWith("[DEMO-DUP]");
        assertThat(inc2.getTitle()).startsWith("[DEMO-DUP]");

        // Provenance verification: both records exist with distinct reporters
        assertThat(inc1.getReporter().getId()).isNotEqualTo(inc2.getReporter().getId());

        // Verify duplicate evaluation metrics from real DuplicateDetectionService
        Map<String, Object> metrics = result.getMetrics();
        double similarityScore = Double.parseDouble(metrics.get("similarityScore").toString());
        assertThat(similarityScore).isEqualTo(1.0);
        double distanceMeters = Double.parseDouble(metrics.get("distanceMeters").toString());
        assertThat(distanceMeters).isBetween(120.0, 130.0);
        assertThat(metrics.get("provenanceStatus")).isEqualTo("PRESERVED_BOTH_REPORTS");
    }

    @Test
    @DisplayName("Scenario C Runtime: Resolution Verification Failure dynamically calculates distance > 500m and persists reopening transition")
    void testResolutionVerificationFailureRuntime() {
        JudgeDemoScenarioResultDTO result = judgeDemoService.runResolutionFailureScenario();

        assertThat(result.getScenarioKey()).isEqualTo("RESOLUTION_FAILURE_REOPEN");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");

        Long incidentId = result.getTargetIncidentId();
        Incident incident = incidentRepository.findById(incidentId).orElseThrow();

        // 1. Verify resolution verification status is FAILED
        List<Resolution> resolutions = resolutionRepository.findByIncidentId(incidentId);
        assertThat(resolutions).isNotEmpty();
        Resolution resolution = resolutions.get(0);
        assertThat(resolution.getVerificationStatus()).isEqualTo(VerificationStatus.FAILED);

        // 2. Verify dynamic distance calculation from persisted coordinates (~1240m)
        Map<String, Object> metrics = result.getMetrics();
        long distanceDivergence = Long.parseLong(metrics.get("distanceDivergenceMeters").toString());
        assertThat(distanceDivergence).isBetween(1200L, 1300L);

        // 3. Verify workflow transition to REPORTED
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.REPORTED);

        // 4. Verify audit trail persisted in incident_actions table
        List<IncidentAction> actions = incidentActionRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
        assertThat(actions).isNotEmpty();
        IncidentAction latestAction = actions.get(actions.size() - 1);
        assertThat(latestAction.getActionType()).isEqualTo(ActionType.REOPENED);
        assertThat(latestAction.getRemarks()).contains("Algorithmic verification failed");
    }

    @Test
    @DisplayName("Reset & Isolation Runtime: Safely purges only Judge Demo records; genuine incidents and unrelated simulated records remain untouched")
    void testResetAndIsolationRuntime() {
        // Step 1: Create a genuine citizen incident with evidence
        User genuineCitizen = userRepository.findByEmail("audit.citizen@nirvaha.gov").orElseThrow();
        Incident genuineIncident = new Incident();
        genuineIncident.setTitle("Genuine Citizen Water Supply Contamination");
        genuineIncident.setDescription("Turbid water supply from main distribution line.");
        genuineIncident.setCategory(IncidentCategory.WATER_SUPPLY);
        genuineIncident.setSeverity(IncidentSeverity.MEDIUM);
        genuineIncident.setStatus(IncidentStatus.REPORTED);
        genuineIncident.setLatitude(new BigDecimal("11.9310"));
        genuineIncident.setLongitude(new BigDecimal("79.8250"));
        genuineIncident.setAddress("Genuine Citizen Home, Ward 4");
        genuineIncident.setReporter(genuineCitizen);
        genuineIncident.setClassificationTag(DataClassification.OFFICIAL);
        Incident savedGenuine = incidentRepository.save(genuineIncident);

        Evidence genuineEvidence = new Evidence();
        genuineEvidence.setIncident(savedGenuine);
        genuineEvidence.setEvidenceType(EvidenceType.PHOTO);
        genuineEvidence.setFileUrl("https://example.com/genuine.jpg");
        genuineEvidence.setFileName("genuine.jpg");
        genuineEvidence.setMimeType("image/jpeg");
        genuineEvidence.setFileSize(102400L);
        evidenceRepository.save(genuineEvidence);

        // Step 1B: Create an unrelated SIMULATED incident (e.g. from external stress tests/benchmarks)
        Incident unrelatedSimulated = new Incident();
        unrelatedSimulated.setTitle("External Benchmark Load Incident");
        unrelatedSimulated.setDescription("Stress testing simulated incident from external benchmark tool.");
        unrelatedSimulated.setCategory(IncidentCategory.DRAINAGE);
        unrelatedSimulated.setSeverity(IncidentSeverity.LOW);
        unrelatedSimulated.setLatitude(new BigDecimal("11.9320"));
        unrelatedSimulated.setLongitude(new BigDecimal("79.8260"));
        unrelatedSimulated.setAddress("Benchmark Test Point, Ward 4");
        unrelatedSimulated.setReporter(genuineCitizen);
        unrelatedSimulated.setStatus(IncidentStatus.REPORTED);
        unrelatedSimulated.setClassificationTag(DataClassification.SIMULATED);
        Incident savedUnrelatedSimulated = incidentRepository.save(unrelatedSimulated);

        // Step 2: Run all 3 demo scenarios
        judgeDemoService.runMonsoonDrainageSurgeScenario();
        judgeDemoService.runDuplicateDetectionScenario();
        judgeDemoService.runResolutionFailureScenario();

        List<Incident> simulatedBeforeReset = incidentRepository.findByClassificationTag(DataClassification.SIMULATED);
        assertThat(simulatedBeforeReset).hasSize(4 + 2 + 1 + 1); // 7 demo + 1 unrelated = 8

        // Step 3: Execute Reset
        Map<String, Object> resetResult = judgeDemoService.resetAllDemoData();
        assertThat(resetResult.get("status")).isEqualTo("RESET_SUCCESSFUL");
        assertThat(resetResult.get("deletedSimulatedIncidentsCount")).isEqualTo(7);

        // Step 4: Verify ONLY unrelated simulated record remains (0 demo records remain)
        List<Incident> simulatedAfterReset = incidentRepository.findByClassificationTag(DataClassification.SIMULATED);
        assertThat(simulatedAfterReset).hasSize(1);
        assertThat(simulatedAfterReset.get(0).getTitle()).isEqualTo("External Benchmark Load Incident");

        // Step 5: Verify genuine citizen incident and evidence are completely unharmed
        Incident reloadedGenuine = incidentRepository.findById(savedGenuine.getId()).orElse(null);
        assertThat(reloadedGenuine).isNotNull();
        assertThat(reloadedGenuine.getTitle()).isEqualTo("Genuine Citizen Water Supply Contamination");

        List<Evidence> reloadedEvidence = evidenceRepository.findByIncidentId(savedGenuine.getId());
        assertThat(reloadedEvidence).isNotEmpty();

        // Cleanup test genuine and test unrelated records
        evidenceRepository.deleteAll(reloadedEvidence);
        incidentRepository.delete(reloadedGenuine);
        incidentRepository.delete(savedUnrelatedSimulated);
    }

    @Test
    @DisplayName("Repeatability Runtime: Consecutive executions cleanly replace previous run without creating runaway duplicates")
    void testRepeatabilityRuntime() {
        // Run Scenario A twice
        judgeDemoService.runMonsoonDrainageSurgeScenario();
        judgeDemoService.runMonsoonDrainageSurgeScenario();
        List<Incident> surgeIncidents = incidentRepository.findByClassificationTag(DataClassification.SIMULATED).stream()
                .filter(i -> i.getTitle().startsWith("[DEMO-SURGE]"))
                .toList();
        assertThat(surgeIncidents).hasSize(4);

        // Run Scenario B twice
        judgeDemoService.runDuplicateDetectionScenario();
        judgeDemoService.runDuplicateDetectionScenario();
        List<Incident> dupIncidents = incidentRepository.findByClassificationTag(DataClassification.SIMULATED).stream()
                .filter(i -> i.getTitle().startsWith("[DEMO-DUP]"))
                .toList();
        assertThat(dupIncidents).hasSize(2);

        // Run Scenario C twice
        judgeDemoService.runResolutionFailureScenario();
        judgeDemoService.runResolutionFailureScenario();
        List<Incident> verifIncidents = incidentRepository.findByClassificationTag(DataClassification.SIMULATED).stream()
                .filter(i -> i.getTitle().startsWith("[DEMO-VERIF]"))
                .toList();
        assertThat(verifIncidents).hasSize(1);
    }

    @Test
    @DisplayName("Security & Authorization Runtime: Unauthenticated returns 401, CITIZEN returns 403, OFFICER/ADMIN returns 200")
    void testSecurityAndAuthorizationRuntime() throws Exception {
        // 1. Unauthenticated request -> 401 Unauthorized
        mockMvc.perform(get("/api/demo/status"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/demo/reset"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/demo/scenarios/monsoon-drainage-surge"))
                .andExpect(status().isUnauthorized());

        // 2. CITIZEN role -> 403 Forbidden
        mockMvc.perform(get("/api/demo/status")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/demo/reset")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/demo/scenarios/monsoon-drainage-surge")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        // 3. OFFICER role -> 200 OK
        mockMvc.perform(get("/api/demo/status")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backendAvailable").value(true));

        mockMvc.perform(post("/api/demo/reset")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESET_SUCCESSFUL"));

        // 4. ADMIN role -> 200 OK
        mockMvc.perform(post("/api/demo/scenarios/monsoon-drainage-surge")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioKey").value("MONSOON_DRAINAGE_SURGE"));
    }

    @Test
    @DisplayName("Phase C Regression 1: Explicit Demo Run can be reset specifically by demoRunId and all child records are verified")
    void testExplicitDemoRunReset() {
        String customRunId = "DEMO-RUN-CUSTOM-TEST-001";
        JudgeDemoScenarioResultDTO result = judgeDemoService.runResolutionFailureScenario(customRunId);

        assertThat(result.getDemoRunId()).isEqualTo(customRunId);
        Long targetId = result.getTargetIncidentId();

        // Verify that incident, evidence, resolutions, and actions carry this demoRunId
        Incident incident = incidentRepository.findById(targetId).orElseThrow();
        assertThat(incident.getDemoRunId()).isEqualTo(customRunId);

        List<Evidence> evidences = evidenceRepository.findByDemoRunId(customRunId);
        assertThat(evidences).isNotEmpty();
        for (Evidence ev : evidences) {
            assertThat(ev.getDemoRunId()).isEqualTo(customRunId);
        }

        List<Resolution> resolutions = resolutionRepository.findByDemoRunId(customRunId);
        assertThat(resolutions).isNotEmpty();
        for (Resolution res : resolutions) {
            assertThat(res.getDemoRunId()).isEqualTo(customRunId);
        }

        List<IncidentAction> actions = incidentActionRepository.findByDemoRunId(customRunId);
        assertThat(actions).isNotEmpty();
        for (IncidentAction act : actions) {
            assertThat(act.getDemoRunId()).isEqualTo(customRunId);
        }

        // Execute scoped reset
        Map<String, Object> resetOutcome = judgeDemoService.resetDemoRun(customRunId);
        assertThat(resetOutcome.get("status")).isEqualTo("RESET_SUCCESSFUL");
        assertThat(resetOutcome.get("demoRunId")).isEqualTo(customRunId);
        assertThat(resetOutcome.get("deletedIncidentsCount")).isEqualTo(1);

        // Verify that all records for this run are deleted
        assertThat(incidentRepository.findByDemoRunId(customRunId)).isEmpty();
        assertThat(evidenceRepository.findByDemoRunId(customRunId)).isEmpty();
        assertThat(resolutionRepository.findByDemoRunId(customRunId)).isEmpty();
        assertThat(incidentActionRepository.findByDemoRunId(customRunId)).isEmpty();
    }

    @Test
    @DisplayName("Phase C Regression 2: Two separate demo runs do not delete each other's records")
    void testTwoSeparateDemoRunsDoNotDeleteEachOther() {
        String runA = "DEMO-RUN-A-SURGE-101";
        String runB = "DEMO-RUN-B-VERIF-202";

        JudgeDemoScenarioResultDTO resultA = judgeDemoService.runMonsoonDrainageSurgeScenario(runA);
        JudgeDemoScenarioResultDTO resultB = judgeDemoService.runResolutionFailureScenario(runB);

        assertThat(resultA.getDemoRunId()).isEqualTo(runA);
        assertThat(resultB.getDemoRunId()).isEqualTo(runB);

        List<Incident> incidentsA = incidentRepository.findByDemoRunId(runA);
        List<Incident> incidentsB = incidentRepository.findByDemoRunId(runB);
        assertThat(incidentsA).hasSize(4);
        assertThat(incidentsB).hasSize(1);

        // Reset Run A only
        Map<String, Object> resetA = judgeDemoService.resetDemoRun(runA);
        assertThat(resetA.get("status")).isEqualTo("RESET_SUCCESSFUL");
        assertThat(resetA.get("deletedIncidentsCount")).isEqualTo(4);

        // Verify Run A is deleted
        assertThat(incidentRepository.findByDemoRunId(runA)).isEmpty();

        // Verify Run B is 100% intact and undamaged
        List<Incident> survivingB = incidentRepository.findByDemoRunId(runB);
        assertThat(survivingB).hasSize(1);
        assertThat(survivingB.get(0).getId()).isEqualTo(resultB.getTargetIncidentId());
        assertThat(evidenceRepository.findByDemoRunId(runB)).isNotEmpty();
        assertThat(resolutionRepository.findByDemoRunId(runB)).isNotEmpty();

        // Clean up Run B
        judgeDemoService.resetDemoRun(runB);
    }

    @Test
    @DisplayName("Phase C Regression 3: Unrelated SIMULATED incident survives scoped demo reset")
    void testUnrelatedSimulatedIncidentSurvivesScopedReset() {
        User genuineCitizen = userRepository.findByEmail("audit.citizen@nirvaha.gov").orElseThrow();

        // Create an unrelated SIMULATED incident without any demoRunId
        Incident unrelatedSimulated = new Incident();
        unrelatedSimulated.setTitle("Unrelated Simulation From External Benchmark");
        unrelatedSimulated.setDescription("External simulation load test.");
        unrelatedSimulated.setCategory(IncidentCategory.DRAINAGE);
        unrelatedSimulated.setSeverity(IncidentSeverity.LOW);
        unrelatedSimulated.setStatus(IncidentStatus.REPORTED);
        unrelatedSimulated.setLatitude(new BigDecimal("11.9320"));
        unrelatedSimulated.setLongitude(new BigDecimal("79.8260"));
        unrelatedSimulated.setAddress("Benchmark Station Point");
        unrelatedSimulated.setReporter(genuineCitizen);
        unrelatedSimulated.setClassificationTag(DataClassification.SIMULATED);
        unrelatedSimulated.setDemoRunId(null); // Explicitly unrelated to demo runs
        Incident savedUnrelated = incidentRepository.save(unrelatedSimulated);

        // Run a demo scenario
        String runId = "DEMO-RUN-SCOPED-TEST-303";
        judgeDemoService.runMonsoonDrainageSurgeScenario(runId);

        // Reset the demo run
        Map<String, Object> resetOutcome = judgeDemoService.resetDemoRun(runId);
        assertThat(resetOutcome.get("status")).isEqualTo("RESET_SUCCESSFUL");

        // Verify the unrelated SIMULATED incident still exists in database
        Incident reloadedUnrelated = incidentRepository.findById(savedUnrelated.getId()).orElse(null);
        assertThat(reloadedUnrelated).isNotNull();
        assertThat(reloadedUnrelated.getTitle()).isEqualTo("Unrelated Simulation From External Benchmark");

        // Clean up test entity
        incidentRepository.delete(reloadedUnrelated);
    }

    @Test
    @DisplayName("Phase C Regression 4: Genuine citizen incident and evidence survive scoped reset")
    void testGenuineCitizenIncidentAndEvidenceSurviveReset() {
        User genuineCitizen = userRepository.findByEmail("audit.citizen@nirvaha.gov").orElseThrow();

        Incident genuineIncident = new Incident();
        genuineIncident.setTitle("Genuine Citizen Water Pipeline Breach");
        genuineIncident.setDescription("Water distribution pipeline leakage on main road.");
        genuineIncident.setCategory(IncidentCategory.WATER_SUPPLY);
        genuineIncident.setSeverity(IncidentSeverity.HIGH);
        genuineIncident.setStatus(IncidentStatus.REPORTED);
        genuineIncident.setLatitude(new BigDecimal("11.9315"));
        genuineIncident.setLongitude(new BigDecimal("79.8255"));
        genuineIncident.setAddress("Puducherry Water Line #4");
        genuineIncident.setReporter(genuineCitizen);
        genuineIncident.setClassificationTag(DataClassification.OFFICIAL);
        genuineIncident.setDemoRunId(null);
        Incident savedGenuine = incidentRepository.save(genuineIncident);

        Evidence genuineEvidence = new Evidence();
        genuineEvidence.setIncident(savedGenuine);
        genuineEvidence.setEvidenceType(EvidenceType.PHOTO);
        genuineEvidence.setFileUrl("https://example.com/genuine-pipe.jpg");
        genuineEvidence.setFileName("genuine_pipe.jpg");
        genuineEvidence.setMimeType("image/jpeg");
        genuineEvidence.setFileSize(512000L);
        genuineEvidence.setContentHash("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789");
        Evidence savedGenuineEvidence = evidenceRepository.save(genuineEvidence);

        // Run a demo scenario
        String runId = "DEMO-RUN-GENUINE-TEST-404";
        judgeDemoService.runResolutionFailureScenario(runId);

        // Reset the demo run
        judgeDemoService.resetDemoRun(runId);

        // Verify genuine citizen incident and its evidence survive 100% intact
        Incident reloadedGenuine = incidentRepository.findById(savedGenuine.getId()).orElse(null);
        assertThat(reloadedGenuine).isNotNull();
        assertThat(reloadedGenuine.getTitle()).isEqualTo("Genuine Citizen Water Pipeline Breach");

        List<Evidence> reloadedEvidence = evidenceRepository.findByIncidentId(savedGenuine.getId());
        assertThat(reloadedEvidence).isNotEmpty();
        assertThat(reloadedEvidence.get(0).getId()).isEqualTo(savedGenuineEvidence.getId());

        // Clean up
        evidenceRepository.delete(savedGenuineEvidence);
        incidentRepository.delete(reloadedGenuine);
    }

    @Test
    @DisplayName("Phase C Regression 5: Failed or repeated reset attempts do not corrupt database")
    void testFailedOrRepeatedResetAttemptsAreSafe() {
        // 1. Reset non-existent run ID -> NOT_FOUND, no mutation, no exception
        Map<String, Object> notFoundResult = judgeDemoService.resetDemoRun("DEMO-RUN-NON-EXISTENT-999");
        assertThat(notFoundResult.get("status")).isEqualTo("NOT_FOUND");
        assertThat(notFoundResult.get("deletedIncidentsCount")).isEqualTo(0);

        // 2. Reset null/empty -> REFUSED, deletion refused
        Map<String, Object> refusedResult1 = judgeDemoService.resetDemoRun(null);
        assertThat(refusedResult1.get("status")).isEqualTo("REFUSED");

        Map<String, Object> refusedResult2 = judgeDemoService.resetDemoRun("   ");
        assertThat(refusedResult2.get("status")).isEqualTo("REFUSED");

        // 3. Create a demo run, reset it, then immediately repeat reset -> returns NOT_FOUND cleanly
        String runId = "DEMO-RUN-REPEAT-TEST-505";
        judgeDemoService.runMonsoonDrainageSurgeScenario(runId);
        Map<String, Object> firstReset = judgeDemoService.resetDemoRun(runId);
        assertThat(firstReset.get("status")).isEqualTo("RESET_SUCCESSFUL");

        Map<String, Object> secondReset = judgeDemoService.resetDemoRun(runId);
        assertThat(secondReset.get("status")).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("Phase C Regression 6: Unauthorized users cannot create or reset demo runs via HTTP endpoints")
    void testUnauthorizedUsersCannotCreateOrResetDemo() throws Exception {
        String testRunId = "DEMO-RUN-SECURITY-TEST-606";

        // Unauthenticated reset attempt -> 401
        mockMvc.perform(post("/api/demo/reset/" + testRunId))
                .andExpect(status().isUnauthorized());

        // CITIZEN reset attempt -> 403
        mockMvc.perform(post("/api/demo/reset/" + testRunId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        // CITIZEN scenario creation with demoRunId -> 403
        mockMvc.perform(post("/api/demo/scenarios/monsoon-drainage-surge?demoRunId=" + testRunId)
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());

        // OFFICER creation with demoRunId -> 200
        mockMvc.perform(post("/api/demo/scenarios/monsoon-drainage-surge?demoRunId=" + testRunId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demoRunId").value(testRunId));

        // OFFICER reset by demoRunId in URL path -> 200
        mockMvc.perform(post("/api/demo/reset/" + testRunId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESET_SUCCESSFUL"))
                .andExpect(jsonPath("$.deletedIncidentsCount").value(4));
    }

    @Test
    @DisplayName("Gate Requirement 1: Child records strictly inherit parent demoRunId and cannot be attached to mismatched demo run")
    void testChildRecordsInheritAuthoritativeDemoRunId() {
        String parentRunId = "DEMO-RUN-PARENT-AUTH-707";
        User demoCitizen = userRepository.findByEmail("audit.citizen@nirvaha.gov").orElseThrow();
        User demoOfficer = userRepository.findByEmail("audit.officer@puducherry.gov.in").orElseThrow();

        Incident parentInc = new Incident();
        parentInc.setTitle("[DEMO-CHILD] Test Parent Incident for Inheritance");
        parentInc.setDescription("Testing child entity demoRunId inheritance.");
        parentInc.setCategory(IncidentCategory.POTHOLE);
        parentInc.setSeverity(IncidentSeverity.LOW);
        parentInc.setStatus(IncidentStatus.IN_PROGRESS);
        parentInc.setLatitude(new BigDecimal("11.9300"));
        parentInc.setLongitude(new BigDecimal("79.8200"));
        parentInc.setAddress("Test Boulevard");
        parentInc.setReporter(demoCitizen);
        parentInc.setClassificationTag(DataClassification.SIMULATED);
        parentInc.setDemoRunId(parentRunId);
        Incident savedParent = incidentRepository.save(parentInc);

        // Attempt to attach evidence with a mismatched run ID
        Evidence childEvidence = new Evidence();
        childEvidence.setIncident(savedParent);
        childEvidence.setEvidenceType(EvidenceType.PHOTO);
        childEvidence.setFileUrl("https://example.com/test-evidence.jpg");
        childEvidence.setFileName("test_evidence.jpg");
        childEvidence.setMimeType("image/jpeg");
        childEvidence.setFileSize(204800L);
        childEvidence.setDemoRunId("MISMATCHED-RUN-ID-ATTEMPT");
        Evidence savedEvidence = evidenceRepository.save(childEvidence);

        // Verify pre-persist forced inheritance to parent's demoRunId
        assertThat(savedEvidence.getDemoRunId()).isEqualTo(parentRunId);

        // Attempt to attach action with a mismatched run ID
        IncidentAction childAction = new IncidentAction();
        childAction.setIncident(savedParent);
        childAction.setOfficer(demoOfficer);
        childAction.setActionType(ActionType.PROGRESS_UPDATE);
        childAction.setRemarks("Assigned to field team");
        childAction.setDemoRunId("ANOTHER-MISMATCHED-RUN-ID");
        IncidentAction savedAction = incidentActionRepository.save(childAction);

        assertThat(savedAction.getDemoRunId()).isEqualTo(parentRunId);

        // Attempt to attach resolution with a mismatched run ID
        Resolution childRes = new Resolution();
        childRes.setIncident(savedParent);
        childRes.setOfficer(demoOfficer);
        childRes.setResolutionEvidence(savedEvidence);
        childRes.setVerificationStatus(VerificationStatus.VERIFIED);
        childRes.setResolutionSimilarityScore(0.95);
        childRes.setRemarks("Repaired cleanly");
        childRes.setDemoRunId("THIRD-MISMATCHED-RUN-ID");
        Resolution savedRes = resolutionRepository.save(childRes);

        assertThat(savedRes.getDemoRunId()).isEqualTo(parentRunId);

        // Clean up via scoped reset
        Map<String, Object> resetOutcome = judgeDemoService.resetDemoRun(parentRunId);
        assertThat(resetOutcome.get("status")).isEqualTo("RESET_SUCCESSFUL");
    }

    @Test
    @DisplayName("Gate Requirement 2: Transaction completely rolls back when reset encounters classification/ownership violation")
    void testResetRollsBackCompletelyOnClassificationViolation() {
        String violationRunId = "DEMO-RUN-VIOLATION-808";
        User demoCitizen = userRepository.findByEmail("audit.citizen@nirvaha.gov").orElseThrow();

        // 1. Create a simulated incident that would be processed first
        Incident simulatedInc = new Incident();
        simulatedInc.setTitle("[DEMO-SURGE] First In Batch - Simulated");
        simulatedInc.setDescription("Simulated incident in batch.");
        simulatedInc.setCategory(IncidentCategory.DRAINAGE);
        simulatedInc.setSeverity(IncidentSeverity.MEDIUM);
        simulatedInc.setStatus(IncidentStatus.REPORTED);
        simulatedInc.setLatitude(new BigDecimal("11.9301"));
        simulatedInc.setLongitude(new BigDecimal("79.8201"));
        simulatedInc.setAddress("Batch St 1");
        simulatedInc.setReporter(demoCitizen);
        simulatedInc.setClassificationTag(DataClassification.SIMULATED);
        simulatedInc.setDemoRunId(violationRunId);
        Incident savedSimulated = incidentRepository.save(simulatedInc);

        // 2. Create an OFFICIAL (genuine citizen) incident with the same demoRunId (adversarial/corrupted state)
        Incident genuineInc = new Incident();
        genuineInc.setTitle("Second In Batch - Genuine Official Record");
        genuineInc.setDescription("Official citizen report.");
        genuineInc.setCategory(IncidentCategory.WATER_SUPPLY);
        genuineInc.setSeverity(IncidentSeverity.HIGH);
        genuineInc.setStatus(IncidentStatus.REPORTED);
        genuineInc.setLatitude(new BigDecimal("11.9302"));
        genuineInc.setLongitude(new BigDecimal("79.8202"));
        genuineInc.setAddress("Batch St 2");
        genuineInc.setReporter(demoCitizen);
        genuineInc.setClassificationTag(DataClassification.OFFICIAL); // NOT SIMULATED!
        genuineInc.setDemoRunId(violationRunId);
        Incident savedGenuine = incidentRepository.save(genuineInc);

        // 3. Attempt reset — must fail fast with IllegalStateException
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> judgeDemoService.resetDemoRun(violationRunId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Safety violation")
                .hasMessageContaining("OFFICIAL");

        // 4. Verify transactional rollback: NEITHER record was deleted
        assertThat(incidentRepository.findById(savedSimulated.getId())).isPresent();
        assertThat(incidentRepository.findById(savedGenuine.getId())).isPresent();

        // Safe cleanup for test
        incidentRepository.delete(savedSimulated);
        incidentRepository.delete(savedGenuine);
    }
}
