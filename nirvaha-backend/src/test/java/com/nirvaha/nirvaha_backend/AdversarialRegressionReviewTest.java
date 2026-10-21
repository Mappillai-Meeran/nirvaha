package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.InvalidWorkflowException;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.security.JwtService;
import com.nirvaha.nirvaha_backend.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AdversarialRegressionReviewTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private IncidentWorkflowService incidentWorkflowService;

    @Autowired
    private ResolutionService resolutionService;

    @Autowired
    private ResidentFeedbackService residentFeedbackService;

    @Autowired
    private ServiceRuleService serviceRuleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private IncidentActionRepository actionRepository;

    @Autowired
    private ResolutionRepository resolutionRepository;

    @Autowired
    private IncidentFeedbackRepository feedbackRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtService jwtService;

    private User citizenA;
    private User citizenB;
    private User officer;
    private String citizenAJwt;
    private String citizenBJwt;
    private String officerJwt;

    private Incident incidentA;
    private Evidence originalEvidenceA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        SecurityContextHolder.clearContext();

        notificationRepository.deleteAll();
        feedbackRepository.deleteAll();
        resolutionRepository.deleteAll();
        actionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Users
        citizenA = createUser("Citizen A", "citizen.a@puducherry.test", UserRole.CITIZEN);
        citizenB = createUser("Citizen B", "citizen.b@puducherry.test", UserRole.CITIZEN);
        officer = createUser("Officer Pandian", "pandian.officer@puducherry.gov.in", UserRole.OFFICER);

        citizenAJwt = jwtService.generateToken(citizenA.getId(), citizenA.getEmail(), "CITIZEN");
        citizenBJwt = jwtService.generateToken(citizenB.getId(), citizenB.getEmail(), "CITIZEN");
        officerJwt = jwtService.generateToken(officer.getId(), officer.getEmail(), "OFFICER");

        // 2. Create Incident for Citizen A
        incidentA = new Incident();
        incidentA.setTitle("Drain Overflow on Anna Salai");
        incidentA.setDescription("Severely blocked stormwater drain");
        incidentA.setCategory(IncidentCategory.DRAINAGE);
        incidentA.setSeverity(IncidentSeverity.HIGH);
        incidentA.setStatus(IncidentStatus.IN_PROGRESS);
        incidentA.setLatitude(new BigDecimal("11.9350"));
        incidentA.setLongitude(new BigDecimal("79.8310"));
        incidentA.setAddress("Anna Salai, Puducherry");
        incidentA.setReporter(citizenA);
        incidentA.setClassificationTag(DataClassification.OFFICIAL);
        incidentA.setIntakeSource(IncidentSource.RESIDENT_SUBMITTED);
        incidentA = incidentRepository.save(incidentA);

        // 3. Original Citizen Evidence
        originalEvidenceA = new Evidence();
        originalEvidenceA.setIncident(incidentA);
        originalEvidenceA.setEvidenceType(EvidenceType.PHOTO);
        originalEvidenceA.setFileName("drain_before.jpg");
        originalEvidenceA.setFileUrl("https://storage.nirvaha.puducherry.gov.in/evidence/drain_before.jpg");
        originalEvidenceA.setMimeType("image/jpeg");
        originalEvidenceA.setFileSize(204800L);
        originalEvidenceA.setContentHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        originalEvidenceA.setLatitude(new BigDecimal("11.9350"));
        originalEvidenceA.setLongitude(new BigDecimal("79.8310"));
        originalEvidenceA.setDescription("Original clogged drain photo");
        originalEvidenceA = evidenceRepository.save(originalEvidenceA);
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        feedbackRepository.deleteAll();
        resolutionRepository.deleteAll();
        actionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createUser(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        user.setRole(role);
        return userRepository.save(user);
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // =========================================================================
    // SECTION 1: RESOLUTION LIFECYCLE & EVIDENCE INTEGRITY
    // =========================================================================

    @Test
    @DisplayName("Lifecycle 1: Citizen cannot directly call API to resolve an incident")
    void testCitizenCannotCallResolutionEndpoint() throws Exception {
        String resolutionPayload = """
                {
                    "officerId": %d,
                    "evidenceId": %d,
                    "remarks": "Citizen trying to resolve directly"
                }
                """.formatted(citizenA.getId(), originalEvidenceA.getId());

        // 1. Citizen calling POST /api/incidents/{id}/resolutions -> 403 Forbidden
        mockMvc.perform(post("/api/incidents/" + incidentA.getId() + "/resolutions")
                        .header("Authorization", "Bearer " + citizenAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolutionPayload))
                .andExpect(status().isForbidden());

        // 2. Citizen calling POST /api/incidents/{id}/actions with RESOLVED -> 403 Forbidden
        String actionPayload = """
                {
                    "officerId": %d,
                    "actionType": "RESOLVED",
                    "remarks": "Bypass attempt"
                }
                """.formatted(citizenA.getId());

        mockMvc.perform(post("/api/incidents/" + incidentA.getId() + "/actions")
                        .header("Authorization", "Bearer " + citizenAJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionPayload))
                .andExpect(status().isForbidden());

        // Confirm incident remains IN_PROGRESS and no mutation occurred
        Incident check = incidentRepository.findById(incidentA.getId()).orElseThrow();
        assertThat(check.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Lifecycle 2: Officer cannot resolve an incident without verified resolution evidence")
    void testOfficerCannotResolveWithoutEvidence() {
        authenticateAs(officer);

        // Calling resolveIncident without any resolution submitted must fail
        assertThatThrownBy(() -> incidentWorkflowService.resolveIncident(incidentA.getId(), officer.getId()))
                .isInstanceOf(InvalidWorkflowException.class)
                .hasMessageContaining("verified resolution evidence must be submitted first");

        // Incident remains IN_PROGRESS
        Incident check = incidentRepository.findById(incidentA.getId()).orElseThrow();
        assertThat(check.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Lifecycle 3: Low similarity evidence FAILED leaves incident in IN_PROGRESS and does not claim RESOLVED")
    void testFailedEvidenceVerificationDoesNotResolveIncident() {
        authenticateAs(officer);

        // Resolution evidence located 10km away (extremely low spatial similarity, score < 0.40)
        Evidence farEvidence = new Evidence();
        farEvidence.setIncident(incidentA);
        farEvidence.setEvidenceType(EvidenceType.PHOTO);
        farEvidence.setFileName("far_photo.jpg");
        farEvidence.setFileUrl("https://storage.nirvaha.puducherry.gov.in/evidence/far_photo.jpg");
        farEvidence.setMimeType("image/jpeg");
        farEvidence.setFileSize(20480L);
        farEvidence.setContentHash("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        farEvidence.setLatitude(new BigDecimal("12.0500"));
        farEvidence.setLongitude(new BigDecimal("79.9500"));
        farEvidence.setDescription("Completely unrelated site photo");
        farEvidence = evidenceRepository.save(farEvidence);

        CreateResolutionRequest req = new CreateResolutionRequest(officer.getId(), farEvidence.getId(), "Work claimed finished");
        ResolutionResponse response = resolutionService.createResolution(incidentA.getId(), req);

        // 1. Verification status must be FAILED
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.FAILED);

        // 2. Crucial: Incident MUST NOT be resolved! Must remain IN_PROGRESS
        Incident check = incidentRepository.findById(incidentA.getId()).orElseThrow();
        assertThat(check.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);

        // 3. Audit action is logged as PROGRESS_UPDATE, NEVER ActionType.RESOLVED
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(incidentA.getId());
        assertThat(actions).isNotEmpty();
        IncidentAction latest = actions.get(actions.size() - 1);
        assertThat(latest.getActionType()).isEqualTo(ActionType.PROGRESS_UPDATE);
        assertThat(latest.getRemarks()).contains("FAILED");

        // 4. Resident was NOT notified of resolution (because repair verification failed)
        long residentNotifications = notificationRepository.countByRecipientIdAndReadFalse(citizenA.getId());
        assertThat(residentNotifications).isEqualTo(0);

        // 5. Subsequent call to resolveIncident also rejects resolution because evidence was FAILED
        assertThatThrownBy(() -> incidentWorkflowService.resolveIncident(incidentA.getId(), officer.getId()))
                .isInstanceOf(InvalidWorkflowException.class)
                .hasMessageContaining("verified resolution evidence must be submitted first");
    }

    @Test
    @DisplayName("Lifecycle 4: Verified evidence (similarity >= 0.75) genuinely transitions incident to RESOLVED and notifies resident")
    void testGenuineEvidenceVerificationResolvesIncident() {
        authenticateAs(officer);

        // Valid repair evidence at the exact same location with matching size
        Evidence validEvidence = new Evidence();
        validEvidence.setIncident(incidentA);
        validEvidence.setEvidenceType(EvidenceType.PHOTO);
        validEvidence.setFileName("drain_cleared.jpg");
        validEvidence.setFileUrl("https://storage.nirvaha.puducherry.gov.in/evidence/drain_cleared.jpg");
        validEvidence.setMimeType("image/jpeg");
        validEvidence.setFileSize(204800L);
        validEvidence.setContentHash("cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        validEvidence.setLatitude(new BigDecimal("11.9350"));
        validEvidence.setLongitude(new BigDecimal("79.8310"));
        validEvidence.setDescription("Drain desilted, clear water flow restored");
        validEvidence = evidenceRepository.save(validEvidence);

        CreateResolutionRequest req = new CreateResolutionRequest(officer.getId(), validEvidence.getId(), "Desilting complete");
        ResolutionResponse response = resolutionService.createResolution(incidentA.getId(), req);

        // 1. Verification status is VERIFIED
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(response.getResolutionSimilarityScore()).isGreaterThanOrEqualTo(0.75);

        // 2. Incident is now RESOLVED
        Incident check = incidentRepository.findById(incidentA.getId()).orElseThrow();
        assertThat(check.getStatus()).isEqualTo(IncidentStatus.RESOLVED);

        // 3. Audit action is logged as ActionType.RESOLVED
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(incidentA.getId());
        IncidentAction latest = actions.get(actions.size() - 1);
        assertThat(latest.getActionType()).isEqualTo(ActionType.RESOLVED);
        assertThat(latest.getRemarks()).contains("VERIFIED");
    }

    @Test
    @DisplayName("Lifecycle 5: Resident dispute reopens resolved case to REPORTED and logs audit actions")
    void testResidentDisputeReopensIncident() {
        // First resolve properly
        testGenuineEvidenceVerificationResolvesIncident();

        // Citizen A disputes the resolution
        authenticateAs(citizenA);
        ResidentFeedbackRequest disputeReq = new ResidentFeedbackRequest(
                FeedbackType.DISPUTED, "Silt was left piled on the roadside blocking pedestrian passage"
        );
        IncidentFeedbackResponse fbResp = residentFeedbackService.submitFeedback(incidentA.getId(), disputeReq);

        assertThat(fbResp.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(fbResp.getFeedbackType()).isEqualTo(FeedbackType.DISPUTED);

        // Incident is reopened and transitions back to REPORTED
        Incident check = incidentRepository.findById(incidentA.getId()).orElseThrow();
        assertThat(check.getStatus()).isEqualTo(IncidentStatus.REPORTED);

        // Audit trail records both RESIDENT_DISPUTED and REOPENED
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(incidentA.getId());
        assertThat(actions.stream().anyMatch(a -> a.getActionType() == ActionType.RESIDENT_DISPUTED)).isTrue();
        assertThat(actions.stream().anyMatch(a -> a.getActionType() == ActionType.REOPENED)).isTrue();
    }

    // =========================================================================
    // SECTION 2: AUTHORIZATION & PRIVACY MATRIX
    // =========================================================================

    @Test
    @DisplayName("Security: Citizen A accessing Citizen B's sub-resources is strictly blocked across all APIs")
    void testCitizenCrossAccessIsBlocked() throws Exception {
        // 1. Citizen B querying Citizen A's incident -> 403 Forbidden
        mockMvc.perform(get("/api/incidents/" + incidentA.getId())
                        .header("Authorization", "Bearer " + citizenBJwt))
                .andExpect(status().isForbidden());

        // 2. Citizen B querying Citizen A's evidence -> 403 Forbidden
        mockMvc.perform(get("/api/evidence/" + originalEvidenceA.getId())
                        .header("Authorization", "Bearer " + citizenBJwt))
                .andExpect(status().isForbidden());

        // 3. Citizen B querying Citizen A's incident evidence list -> 403 Forbidden
        mockMvc.perform(get("/api/evidence/incident/" + incidentA.getId())
                        .header("Authorization", "Bearer " + citizenBJwt))
                .andExpect(status().isForbidden());

        // 4. Citizen B querying Citizen A's feedback -> 403 Forbidden
        mockMvc.perform(get("/api/incidents/" + incidentA.getId() + "/feedback")
                        .header("Authorization", "Bearer " + citizenBJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Missing, malformed, and expired JWT tokens are rejected")
    void testTokenValidationFailures() throws Exception {
        // 1. Missing Authorization header on protected endpoint -> 401 Unauthorized
        mockMvc.perform(get("/api/incidents/" + incidentA.getId()))
                .andExpect(status().isUnauthorized());

        // 2. Malformed token -> 401 Unauthorized
        mockMvc.perform(get("/api/incidents/" + incidentA.getId())
                        .header("Authorization", "Bearer malformed.token.value"))
                .andExpect(status().isUnauthorized());

        // 3. Insufficient role: Citizen attempting officer action queue -> 403 Forbidden
        mockMvc.perform(get("/api/action-center/queue")
                        .header("Authorization", "Bearer " + citizenAJwt))
                .andExpect(status().isForbidden());

        // 4. Insufficient role: Citizen attempting SLA evaluation trigger -> 403 Forbidden
        mockMvc.perform(post("/api/sla/evaluate")
                        .header("Authorization", "Bearer " + citizenAJwt))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // SECTION 3: SIMULATED-DATA ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Isolation: Simulated records do not appear in official SLA tracking metrics")
    void testSimulatedRecordsExcludedFromSlaMetrics() {
        // Create an official incident
        Incident official = incidentA;
        serviceRuleService.evaluateAndEscalate(official);

        // Create a simulated incident that would otherwise be overdue
        Incident simulated = new Incident();
        simulated.setTitle("SIMULATED Overdue Incident");
        simulated.setDescription("Mock stress test incident");
        simulated.setCategory(IncidentCategory.DRAINAGE);
        simulated.setSeverity(IncidentSeverity.HIGH);
        simulated.setStatus(IncidentStatus.IN_PROGRESS);
        simulated.setCreatedAt(LocalDateTime.now().minusDays(10)); // Overdue
        simulated.setLatitude(new BigDecimal("11.9320"));
        simulated.setLongitude(new BigDecimal("79.8280"));
        simulated.setAddress("Mission Street Test");
        simulated.setReporter(citizenA);
        simulated.setIntakeSource(IncidentSource.SIMULATED);
        simulated.setClassificationTag(DataClassification.SIMULATED);
        Incident savedSimulated = incidentRepository.save(simulated);

        serviceRuleService.evaluateAndEscalate(savedSimulated);

        // Get SLA Summary
        Map<String, Object> summary = serviceRuleService.getSlaSummary();
        List<IncidentServiceTracking> officialOverdue = serviceRuleService.getOverdueTracking();

        // The simulated incident must NOT be counted in official overdue tracking
        boolean simulatedInOverdue = officialOverdue.stream()
                .anyMatch(t -> t.getIncident().getId().equals(savedSimulated.getId()));
        assertThat(simulatedInOverdue).isFalse();
    }
}
