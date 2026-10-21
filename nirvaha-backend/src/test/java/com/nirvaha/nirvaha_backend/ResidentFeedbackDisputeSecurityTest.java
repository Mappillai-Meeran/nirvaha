package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.UserResponse;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.security.JwtService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ResidentFeedbackDisputeSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private ResolutionRepository resolutionRepository;

    @Autowired
    private IncidentActionRepository actionRepository;

    @Autowired
    private IncidentFeedbackRepository feedbackRepository;

    private static final String REPORTER_EMAIL = "reporter.citizen@test.puducherry.gov.in";
    private static final String OTHER_CITIZEN_EMAIL = "other.citizen@test.puducherry.gov.in";
    private static final String OFFICER_EMAIL = "field.officer@test.puducherry.gov.in";
    private static final String ADMIN_EMAIL = "admin.supervisor@test.puducherry.gov.in";

    private User reporterUser;
    private User otherCitizenUser;
    private User officerUser;
    private User adminUser;

    private String reporterJwt;
    private String otherCitizenJwt;
    private String officerJwt;
    private String adminJwt;

    private Incident resolvedIncident;
    private Resolution existingResolution;
    private Evidence resolutionEvidence;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        SecurityContextHolder.clearContext();
        cleanupTestData();

        // 1. Create Users
        UserResponse rResp = userService.createUser(new CreateUserRequest("Reporter Citizen", REPORTER_EMAIL, "Password123!", UserRole.CITIZEN));
        reporterUser = userRepository.findById(rResp.getId()).orElseThrow();
        reporterJwt = jwtService.generateToken(reporterUser.getId(), reporterUser.getEmail(), reporterUser.getRole().name());

        UserResponse oResp = userService.createUser(new CreateUserRequest("Other Citizen", OTHER_CITIZEN_EMAIL, "Password123!", UserRole.CITIZEN));
        otherCitizenUser = userRepository.findById(oResp.getId()).orElseThrow();
        otherCitizenJwt = jwtService.generateToken(otherCitizenUser.getId(), otherCitizenUser.getEmail(), otherCitizenUser.getRole().name());

        UserResponse offResp = userService.createUser(new CreateUserRequest("Officer Murugan", OFFICER_EMAIL, "Password123!", UserRole.OFFICER));
        officerUser = userRepository.findById(offResp.getId()).orElseThrow();
        officerJwt = jwtService.generateToken(officerUser.getId(), officerUser.getEmail(), officerUser.getRole().name());

        UserResponse admResp = userService.createUser(new CreateUserRequest("Admin Commissioner", ADMIN_EMAIL, "Password123!", UserRole.ADMIN));
        adminUser = userRepository.findById(admResp.getId()).orElseThrow();
        adminJwt = jwtService.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());

        // 2. Create Incident marked RESOLVED
        Incident inc = new Incident();
        inc.setTitle("Damaged Stormwater Drain near Goubert Market");
        inc.setDescription("Severe drain blockage causing road inundation.");
        inc.setCategory(IncidentCategory.DRAINAGE);
        inc.setSeverity(IncidentSeverity.HIGH);
        inc.setStatus(IncidentStatus.RESOLVED);
        inc.setLatitude(new BigDecimal("11.9310000"));
        inc.setLongitude(new BigDecimal("79.8320000"));
        inc.setAddress("Goubert Avenue, Puducherry");
        inc.setReporter(reporterUser);
        inc.setClassificationTag(DataClassification.OFFICIAL);
        resolvedIncident = incidentRepository.save(inc);

        // 3. Create Evidence & Resolution
        Evidence ev = new Evidence();
        ev.setIncident(resolvedIncident);
        ev.setEvidenceType(EvidenceType.PHOTO);
        ev.setFileName("drain_remediated.jpg");
        ev.setFileUrl("https://storage.nirvaha.puducherry.gov.in/evidence/drain_remediated.jpg");
        ev.setMimeType("image/jpeg");
        ev.setFileSize(450200L);
        ev.setContentHash("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");
        ev.setLatitude(new BigDecimal("11.9310000"));
        ev.setLongitude(new BigDecimal("79.8320000"));
        ev.setDescription("Completed drain desilting and slab placement.");
        resolutionEvidence = evidenceRepository.save(ev);

        Resolution res = new Resolution();
        res.setIncident(resolvedIncident);
        res.setOfficer(officerUser);
        res.setResolutionEvidence(resolutionEvidence);
        res.setResolutionSimilarityScore(0.92);
        res.setVerificationStatus(VerificationStatus.VERIFIED);
        res.setRemarks("Drain cleared and concrete culvert restored.");
        existingResolution = resolutionRepository.save(res);

        // 4. Initial Resolution Action in Audit Log
        IncidentAction act = new IncidentAction();
        act.setIncident(resolvedIncident);
        act.setOfficer(officerUser);
        act.setActionType(ActionType.RESOLVED);
        act.setRemarks("Resolution evidence uploaded and verified.");
        actionRepository.save(act);
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        if (resolvedIncident != null && resolvedIncident.getId() != null) {
            feedbackRepository.findByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId())
                    .forEach(feedbackRepository::delete);
            actionRepository.findByIncidentIdOrderByCreatedAtAsc(resolvedIncident.getId())
                    .forEach(actionRepository::delete);
            resolutionRepository.findByIncidentId(resolvedIncident.getId())
                    .forEach(resolutionRepository::delete);
            evidenceRepository.findByIncidentId(resolvedIncident.getId())
                    .forEach(evidenceRepository::delete);
            incidentRepository.deleteById(resolvedIncident.getId());
            resolvedIncident = null;
        }

        userRepository.findByEmail(REPORTER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(OTHER_CITIZEN_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(OFFICER_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(ADMIN_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("1. Original reporter can confirm a valid resolution")
    void originalReporterCanConfirmResolution() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Issue is fully resolved, drain is flowing clear.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedbackType").value("CONFIRMED"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.reviewed").value(true))
                .andExpect(jsonPath("$.residentName").value("Reporter Citizen"))
                .andExpect(jsonPath("$.residentEmail").doesNotExist())
                .andExpect(jsonPath("$.resolutionId").value(existingResolution.getId()));

        // Incident remains RESOLVED
        Incident current = incidentRepository.findById(resolvedIncident.getId()).orElseThrow();
        assertThat(current.getStatus()).isEqualTo(IncidentStatus.RESOLVED);

        // Audit action was recorded
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(resolvedIncident.getId());
        assertThat(actions).anyMatch(a -> a.getActionType() == ActionType.RESIDENT_CONFIRMED);
    }

    @Test
    @DisplayName("2. Original reporter can dispute a valid resolution with mandatory explanation")
    void originalReporterCanDisputeResolution() throws Exception {
        String explanation = "Water is still overflowing into shops during rainfall; silt was not removed completely.";
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"" + explanation + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedbackType").value("DISPUTED"))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.reviewed").value(false))
                .andExpect(jsonPath("$.explanation").value(explanation))
                .andExpect(jsonPath("$.resolutionId").value(existingResolution.getId()));

        // Incident transitions to REPORTED (review/reopening workflow)
        Incident current = incidentRepository.findById(resolvedIncident.getId()).orElseThrow();
        assertThat(current.getStatus()).isEqualTo(IncidentStatus.REPORTED);

        // Audit history contains both RESIDENT_DISPUTED and REOPENED actions
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(resolvedIncident.getId());
        assertThat(actions).anyMatch(a -> a.getActionType() == ActionType.RESIDENT_DISPUTED && a.getRemarks().contains(explanation));
        assertThat(actions).anyMatch(a -> a.getActionType() == ActionType.REOPENED);
    }

    @Test
    @DisplayName("3. Another resident cannot submit feedback on incident they did not report")
    void anotherResidentCannotSubmitFeedback() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + otherCitizenJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Looks fixed to me.\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(containsString("personally reported")));
    }

    @Test
    @DisplayName("4. Unauthenticated requests are rejected")
    void unauthenticatedFeedbackIsRejected() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("5. Officer and admin accounts cannot use resident-only feedback endpoint")
    void officersAndAdminsCannotImpersonateResidentFeedback() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Officer confirming on behalf of resident.\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Admin confirming.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Feedback against an unresolved incident is rejected")
    void feedbackAgainstUnresolvedIncidentIsRejected() throws Exception {
        // Change status to IN_PROGRESS
        resolvedIncident.setStatus(IncidentStatus.IN_PROGRESS);
        incidentRepository.save(resolvedIncident);

        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Work in progress confirmed.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Feedback can only be submitted for RESOLVED incidents")));
    }

    @Test
    @DisplayName("7. Blank and oversized dispute explanations are rejected")
    void blankAndOversizedDisputeExplanationsAreRejected() throws Exception {
        // Blank explanation for dispute
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("mandatory explanation is required")));

        // Oversized explanation (> 2000 chars)
        String oversized = "A".repeat(2005);
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"" + oversized + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.explanation").value(containsString("must not exceed 2000 characters")));
    }

    @Test
    @DisplayName("8. Duplicate submissions cannot create conflicting outcomes")
    void duplicateSubmissionsAreBlocked() throws Exception {
        // Confirm first
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"First confirmation.\"}"))
                .andExpect(status().isCreated());

        // Attempting to confirm or dispute again is blocked (HTTP 409 Conflict)
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Second confirmation.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already been confirmed")));

        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Trying to dispute after confirming.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already been confirmed")));
    }

    @Test
    @DisplayName("9. Dispute preserves resolution evidence and prior audit history")
    void disputePreservesResolutionEvidenceAndPriorAuditHistory() throws Exception {
        int initialActionsCount = actionRepository.findByIncidentIdOrderByCreatedAtAsc(resolvedIncident.getId()).size();

        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Work was incomplete.\"}"))
                .andExpect(status().isCreated());

        // Resolution record is NOT deleted or modified
        Resolution resAfter = resolutionRepository.findById(existingResolution.getId()).orElseThrow();
        assertThat(resAfter.getResolutionSimilarityScore()).isEqualTo(0.92);
        assertThat(resAfter.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(resAfter.getResolutionEvidence().getId()).isEqualTo(resolutionEvidence.getId());

        // Resolution evidence file is NOT deleted
        Evidence evAfter = evidenceRepository.findById(resolutionEvidence.getId()).orElseThrow();
        assertThat(evAfter.getFileName()).isEqualTo("drain_remediated.jpg");

        // Action history is strictly APPENDED (not overwritten)
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(resolvedIncident.getId());
        assertThat(actions.size()).isGreaterThan(initialActionsCount);
        assertThat(actions.get(0).getActionType()).isEqualTo(ActionType.RESOLVED);
    }

    @Test
    @DisplayName("10. Disputed incident enters the correct existing review/reopening state (REPORTED)")
    void disputedIncidentEntersReportedState() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"The trench is still open.\"}"))
                .andExpect(status().isCreated());

        Incident reloaded = incidentRepository.findById(resolvedIncident.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(IncidentStatus.REPORTED);
    }

    @Test
    @DisplayName("11. Authorized officer review is enforced")
    void authorizedOfficerReviewIsEnforced() throws Exception {
        // 1. Resident files dispute
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Debris left on site.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();

        // 2. Resident cannot review dispute
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewNotes\":\"Citizen attempting self-review.\"}"))
                .andExpect(status().isForbidden());

        // 3. Authorized officer can review dispute
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPTED_FOR_REWORK\",\"reviewNotes\":\"Inspected site; contractor instructed to clear debris within 24h.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewed").value(true))
                .andExpect(jsonPath("$.status").value("ACCEPTED_FOR_REWORK"))
                .andExpect(jsonPath("$.reviewedByOfficerId").value(officerUser.getId()))
                .andExpect(jsonPath("$.reviewNotes").value(containsString("contractor instructed to clear debris")));

        // Action history includes DISPUTE_REVIEWED
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(resolvedIncident.getId());
        assertThat(actions).anyMatch(a -> a.getActionType() == ActionType.DISPUTE_REVIEWED);
    }

    @Test
    @DisplayName("12. Feedback list endpoint returns audit trail with access control")
    void feedbackListRespectsIncidentAccessControl() throws Exception {
        // Confirm resolution
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Good work.\"}"))
                .andExpect(status().isCreated());

        // Reporter can view feedback
        mockMvc.perform(get("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].feedbackType").value("CONFIRMED"));

        // Officer can view feedback
        mockMvc.perform(get("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + officerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Other citizen cannot view feedback (resident privacy isolation)
        mockMvc.perform(get("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + otherCitizenJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("13. Officer review with DISMISSED disposition restores incident status to RESOLVED")
    void officerReviewWithDismissedDispositionRestoresResolved() throws Exception {
        // Resident disputes
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"I think it looks imperfect.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();
        assertThat(incidentRepository.findById(resolvedIncident.getId()).orElseThrow().getStatus()).isEqualTo(IncidentStatus.REPORTED);

        // Officer reviews with DISMISSED disposition (resolution affirmed)
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"DISMISSED\",\"reviewNotes\":\"On-site photographic inspection confirms work meets municipal specification. Resolution affirmed.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.reviewed").value(true));

        // Workflow outcome: Incident status restored to RESOLVED
        Incident reloaded = incidentRepository.findById(resolvedIncident.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(IncidentStatus.RESOLVED);

        // Audit action logged
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(resolvedIncident.getId());
        assertThat(actions).anyMatch(a -> a.getActionType() == ActionType.DISPUTE_REVIEWED && a.getRemarks().contains("affirmed"));
    }

    @Test
    @DisplayName("14. Officer review with UNDER_SUPERVISION disposition keeps incident in REPORTED")
    void officerReviewWithUnderSupervisionDispositionKeepsReported() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Water pooling persists after light rain.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();

        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"UNDER_SUPERVISION\",\"reviewNotes\":\"Assigned to Assistant Engineer for live rainfall gradient inspection.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_SUPERVISION"))
                .andExpect(jsonPath("$.reviewed").value(true));

        Incident reloaded = incidentRepository.findById(resolvedIncident.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(IncidentStatus.REPORTED);
    }

    @Test
    @DisplayName("15. Invalid review disposition is rejected with HTTP 400 Bad Request")
    void invalidReviewDispositionIsRejected() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Issue persists.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();

        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"INVALID_STATUS\",\"reviewNotes\":\"Notes.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Invalid review disposition")));
    }

    @Test
    @DisplayName("16. Disputed incident can be resolved again and receive feedback on new resolution")
    void disputedIncidentCanBeResolvedAgainAndReceiveNewFeedback() throws Exception {
        // Step 1: Resident disputes Resolution 1
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Culvert still blocked by gravel.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resolutionId").value(existingResolution.getId()));

        IncidentFeedback disputeFeedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();

        // Step 2: Officer reviews dispute and accepts for rework
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + disputeFeedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPTED_FOR_REWORK\",\"reviewNotes\":\"Rework crew deployed.\"}"))
                .andExpect(status().isOk());

        // Step 3: Rework is performed: field operations start, work finishes, new resolution (Resolution 2) submitted
        resolvedIncident.setStatus(IncidentStatus.IN_PROGRESS);
        incidentRepository.save(resolvedIncident);

        Evidence ev2 = new Evidence();
        ev2.setIncident(resolvedIncident);
        ev2.setEvidenceType(EvidenceType.PHOTO);
        ev2.setFileName("drain_remediated_v2.jpg");
        ev2.setFileUrl("https://storage.nirvaha.puducherry.gov.in/evidence/drain_remediated_v2.jpg");
        ev2.setMimeType("image/jpeg");
        ev2.setFileSize(490000L);
        ev2.setContentHash("1111222233334444555566667777888899990000aaaabbbbccccddddeeeeffff");
        ev2.setLatitude(new BigDecimal("11.9310000"));
        ev2.setLongitude(new BigDecimal("79.8320000"));
        ev2.setDescription("Gravel removed, water flowing freely.");
        Evidence savedEv2 = evidenceRepository.save(ev2);

        Resolution res2 = new Resolution();
        res2.setIncident(resolvedIncident);
        res2.setOfficer(officerUser);
        res2.setResolutionEvidence(savedEv2);
        res2.setResolutionSimilarityScore(0.96);
        res2.setVerificationStatus(VerificationStatus.VERIFIED);
        res2.setRemarks("Second remediation verified complete.");
        Resolution savedRes2 = resolutionRepository.save(res2);

        resolvedIncident.setStatus(IncidentStatus.RESOLVED);
        incidentRepository.save(resolvedIncident);

        // Step 4: Resident now confirms the NEW resolution (Resolution 2)
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Verified second repair on site, looks perfect.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedbackType").value("CONFIRMED"))
                .andExpect(jsonPath("$.resolutionId").value(savedRes2.getId()));

        // Step 5: Verify both feedback records exist and are correctly associated with their respective resolutions
        List<IncidentFeedback> allFeedbacks = feedbackRepository.findByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId());
        assertThat(allFeedbacks).hasSize(2);

        IncidentFeedback latest = allFeedbacks.get(0);
        IncidentFeedback initial = allFeedbacks.get(1);

        assertThat(latest.getFeedbackType()).isEqualTo(FeedbackType.CONFIRMED);
        assertThat(latest.getResolution().getId()).isEqualTo(savedRes2.getId());

        assertThat(initial.getFeedbackType()).isEqualTo(FeedbackType.DISPUTED);
        assertThat(initial.getResolution().getId()).isEqualTo(existingResolution.getId());
    }

    @Test
    @DisplayName("17. Cross-incident probing: feedback ID from another incident is rejected")
    void crossIncidentProbingIsRejected() throws Exception {
        // Create a second incident
        Incident inc2 = new Incident();
        inc2.setTitle("Pothole on Mission Street");
        inc2.setDescription("Pothole near church.");
        inc2.setCategory(IncidentCategory.POTHOLE);
        inc2.setSeverity(IncidentSeverity.MEDIUM);
        inc2.setStatus(IncidentStatus.RESOLVED);
        inc2.setLatitude(new BigDecimal("11.9330000"));
        inc2.setLongitude(new BigDecimal("79.8300000"));
        inc2.setAddress("Mission Street, Puducherry");
        inc2.setReporter(reporterUser);
        inc2.setClassificationTag(DataClassification.OFFICIAL);
        Incident savedInc2 = incidentRepository.save(inc2);

        // Feedback on incident 1
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Still broken.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedbackOnInc1 = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();

        // Attempt to review feedbackOnInc1 through inc2's endpoint
        mockMvc.perform(post("/api/incidents/" + savedInc2.getId() + "/feedback/" + feedbackOnInc1.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPTED_FOR_REWORK\",\"reviewNotes\":\"Notes.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("does not belong to incident ID")));

        // Cleanup inc2
        incidentRepository.deleteById(savedInc2.getId());
    }

    @Test
    @DisplayName("18. Invalid feedback submission is atomic and does not leave partial state")
    void invalidFeedbackSubmissionIsAtomic() throws Exception {
        int initialFeedbackCount = feedbackRepository.findAll().size();
        int initialActionCount = actionRepository.findAll().size();

        // Send invalid request (blank explanation for dispute)
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"\"}"))
                .andExpect(status().isBadRequest());

        // Ensure database state is completely unchanged
        assertThat(feedbackRepository.findAll().size()).isEqualTo(initialFeedbackCount);
        assertThat(actionRepository.findAll().size()).isEqualTo(initialActionCount);
        assertThat(incidentRepository.findById(resolvedIncident.getId()).orElseThrow().getStatus()).isEqualTo(IncidentStatus.RESOLVED);
    }

    @Test
    @DisplayName("19. Already reviewed dispute cannot be reviewed a second time")
    void alreadyReviewedDisputeCannotBeReviewedAgain() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Road surface uneven.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();

        // First review succeeds
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPTED_FOR_REWORK\",\"reviewNotes\":\"First review notes.\"}"))
                .andExpect(status().isOk());

        // Second review attempt is rejected
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"DISMISSED\",\"reviewNotes\":\"Trying to override with dismissal.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already been reviewed")));
    }

    @Test
    @DisplayName("20. Dispute review requires incident to be in REPORTED awaiting review status")
    void disputeReviewRequiresIncidentToBeInReportedStatus() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Issue still present.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();

        // Simulate incident being moved to IN_PROGRESS by field crew before officer reviews dispute
        resolvedIncident.setStatus(IncidentStatus.IN_PROGRESS);
        incidentRepository.save(resolvedIncident);

        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"DISMISSED\",\"reviewNotes\":\"Dismissing while in progress.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("REPORTED state awaiting review")));
    }

    @Test
    @DisplayName("21. Dismissed disposition without resolution evidence is rejected")
    void dismissedDispositionWithoutResolutionEvidenceIsRejected() throws Exception {
        // Create an un-evidenced resolved incident
        Incident noResIncident = new Incident();
        noResIncident.setTitle("No Resolution Incident");
        noResIncident.setDescription("Test without resolution record");
        noResIncident.setCategory(IncidentCategory.POTHOLE);
        noResIncident.setStatus(IncidentStatus.RESOLVED);
        noResIncident.setSeverity(IncidentSeverity.MEDIUM);
        noResIncident.setLatitude(new BigDecimal("11.9300000"));
        noResIncident.setLongitude(new BigDecimal("79.8300000"));
        noResIncident.setAddress("Rue Romain Rolland, Puducherry");
        noResIncident.setReporter(reporterUser);
        noResIncident.setClassificationTag(DataClassification.OFFICIAL);
        Incident savedNoRes = incidentRepository.save(noResIncident);

        // Resident disputes
        mockMvc.perform(post("/api/incidents/" + savedNoRes.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Never fixed.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(savedNoRes.getId()).orElseThrow();

        // Officer tries to DISMISS without an existing resolution with evidence
        mockMvc.perform(post("/api/incidents/" + savedNoRes.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"DISMISSED\",\"reviewNotes\":\"Dismissing without evidence.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("does not have an existing resolution with evidence")));

        // Cleanup
        feedbackRepository.delete(feedback);
        actionRepository.findByIncidentIdOrderByCreatedAtAsc(savedNoRes.getId()).forEach(actionRepository::delete);
        incidentRepository.deleteById(savedNoRes.getId());
    }

    @Test
    @DisplayName("22. Dismissed review preserves dispute, rationale, original evidence, and does not log VERIFIED")
    void dismissedReviewPreservesAllDataAndAuditSemantics() throws Exception {
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"The drain grate has slight gap.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();
        int actionsBeforeReview = actionRepository.findByIncidentIdOrderByCreatedAtAsc(resolvedIncident.getId()).size();

        // Officer reviews and dismisses
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"DISMISSED\",\"reviewNotes\":\"Standard municipal tolerance allows 2mm gap. Work complies.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.reviewNotes").value("Standard municipal tolerance allows 2mm gap. Work complies."));

        // 1. Incident restored to RESOLVED
        Incident inc = incidentRepository.findById(resolvedIncident.getId()).orElseThrow();
        assertThat(inc.getStatus()).isEqualTo(IncidentStatus.RESOLVED);

        // 2. Feedback record preserves resident explanation and officer notes
        IncidentFeedback reloadedFb = feedbackRepository.findById(feedback.getId()).orElseThrow();
        assertThat(reloadedFb.getExplanation()).isEqualTo("The drain grate has slight gap.");
        assertThat(reloadedFb.getReviewNotes()).isEqualTo("Standard municipal tolerance allows 2mm gap. Work complies.");
        assertThat(reloadedFb.getStatus()).isEqualTo("DISMISSED");

        // 3. Original resolution evidence and similarity score are untouched
        Resolution res = resolutionRepository.findById(existingResolution.getId()).orElseThrow();
        assertThat(res.getResolutionSimilarityScore()).isEqualTo(0.92);
        assertThat(res.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(res.getResolutionEvidence().getId()).isEqualTo(resolutionEvidence.getId());

        Evidence ev = evidenceRepository.findById(resolutionEvidence.getId()).orElseThrow();
        assertThat(ev.getContentHash()).isEqualTo("abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890");

        // 4. Audit actions logged: DISPUTE_REVIEWED is recorded, NEVER falsely claiming ActionType.VERIFIED
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(resolvedIncident.getId());
        assertThat(actions.size()).isEqualTo(actionsBeforeReview + 1);
        IncidentAction latestAction = actions.get(actions.size() - 1);
        assertThat(latestAction.getActionType()).isEqualTo(ActionType.DISPUTE_REVIEWED);
        assertThat(latestAction.getRemarks()).contains("Dispute dismissed: officer affirmed original resolution");
        assertThat(latestAction.getRemarks()).contains("Standard municipal tolerance allows 2mm gap");
        // Verify no false ActionType.VERIFIED was injected
        long verifiedActionsCount = actions.stream().filter(a -> a.getActionType() == ActionType.VERIFIED).count();
        assertThat(verifiedActionsCount).isZero();
    }

    @Test
    @DisplayName("23. Cross-incident resolution tampering in dispute review is strictly rejected")
    void crossIncidentResolutionTamperingInDisputeReviewRejected() throws Exception {
        // Create an unrelated second incident with its own resolution
        Incident incB = new Incident();
        incB.setTitle("Incident B");
        incB.setDescription("Description B");
        incB.setCategory(IncidentCategory.POTHOLE);
        incB.setStatus(IncidentStatus.RESOLVED);
        incB.setSeverity(IncidentSeverity.LOW);
        incB.setLatitude(new BigDecimal("11.9350000"));
        incB.setLongitude(new BigDecimal("79.8350000"));
        incB.setAddress("Aurobindo Street, Puducherry");
        incB.setReporter(reporterUser);
        incB.setClassificationTag(DataClassification.OFFICIAL);
        Incident savedIncB = incidentRepository.save(incB);

        Evidence evB = new Evidence();
        evB.setIncident(savedIncB);
        evB.setEvidenceType(EvidenceType.PHOTO);
        evB.setFileName("ev_b.jpg");
        evB.setFileUrl("https://storage.nirvaha.puducherry.gov.in/ev_b.jpg");
        evB.setMimeType("image/jpeg");
        evB.setFileSize(200000L);
        evB.setContentHash("bbbb222233334444555566667777888899990000aaaabbbbccccddddeeeeffff");
        evB.setLatitude(new BigDecimal("11.9350000"));
        evB.setLongitude(new BigDecimal("79.8350000"));
        Evidence savedEvB = evidenceRepository.save(evB);

        Resolution resB = new Resolution();
        resB.setIncident(savedIncB);
        resB.setOfficer(officerUser);
        resB.setResolutionEvidence(savedEvB);
        resB.setResolutionSimilarityScore(0.85);
        resB.setVerificationStatus(VerificationStatus.VERIFIED);
        resB.setRemarks("Resolution for B");
        Resolution savedResB = resolutionRepository.save(resB);

        // Dispute on resolvedIncident
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"DISPUTED\",\"explanation\":\"Tampering test dispute.\"}"))
                .andExpect(status().isCreated());

        IncidentFeedback feedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId()).orElseThrow();

        // Tamper feedback in database to reference resolution of Incident B
        feedback.setResolution(savedResB);
        feedbackRepository.save(feedback);

        // Attempting to review dispute with mismatched resolution must be rejected
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback/" + feedback.getId() + "/review")
                        .header("Authorization", "Bearer " + officerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disposition\":\"ACCEPTED_FOR_REWORK\",\"reviewNotes\":\"Attempting review with tampered resolution.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Feedback resolution does not belong to incident ID")));

        // Cleanup
        feedbackRepository.delete(feedback);
        resolutionRepository.delete(savedResB);
        evidenceRepository.delete(savedEvB);
        incidentRepository.delete(savedIncB);
    }

    @Test
    @DisplayName("24. Database constraint violation is handled predictably as HTTP 409 Conflict")
    void databaseConstraintViolationHandledPredictablyAsConflict() throws Exception {
        // Simulate a scenario where a feedback already exists for the resolution
        IncidentFeedback fb1 = new IncidentFeedback();
        fb1.setIncident(resolvedIncident);
        fb1.setResident(reporterUser);
        fb1.setResolution(existingResolution);
        fb1.setFeedbackType(FeedbackType.CONFIRMED);
        fb1.setExplanation("Direct insert 1");
        fb1.setStatus("CONFIRMED");
        fb1.setReviewed(true);
        fb1.setCreatedAt(LocalDateTime.now());
        feedbackRepository.saveAndFlush(fb1);

        // Submit via MockMvc attempting duplicate on same resolution
        mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                        .header("Authorization", "Bearer " + reporterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Duplicate attempt.\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409));

        feedbackRepository.delete(fb1);
    }

    @Test
    @DisplayName("25. Concurrent feedback submissions are serialized by row lock; exactly one succeeds")
    void concurrentFeedbackSubmissionsSerializedByRowLock() throws Exception {
        int threads = 2;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threads);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger conflictCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    var result = mockMvc.perform(post("/api/incidents/" + resolvedIncident.getId() + "/feedback")
                                    .header("Authorization", "Bearer " + reporterJwt)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"feedbackType\":\"CONFIRMED\",\"explanation\":\"Concurrent confirmation " + threadIdx + "\"}"))
                            .andReturn();

                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // exception caught
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release both threads simultaneously
        startLatch.countDown();
        doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one submission must succeed; the other must receive Conflict (409)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Exactly one feedback record exists in the database
        List<IncidentFeedback> feedbacks = feedbackRepository.findByIncidentIdOrderByCreatedAtDesc(resolvedIncident.getId());
        assertThat(feedbacks).hasSize(1);
    }
}
