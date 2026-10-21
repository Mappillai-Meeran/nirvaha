package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.InvalidWorkflowException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ProductionReadinessAuditTest {

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
    private PriorityScoringService priorityScoringService;

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

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
    private JwtService jwtService;

    private User citizenA;
    private User citizenB;
    private User officer;
    private String citizenAJwt;
    private String citizenBJwt;
    private String officerJwt;

    private Incident incidentA;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        SecurityContextHolder.clearContext();

        feedbackRepository.deleteAll();
        resolutionRepository.deleteAll();
        actionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Users
        citizenA = createUser("Citizen A", "citizen.a@test.puducherry.gov.in", UserRole.CITIZEN);
        citizenB = createUser("Citizen B", "citizen.b@test.puducherry.gov.in", UserRole.CITIZEN);
        officer = createUser("Officer Raman", "raman.officer@test.puducherry.gov.in", UserRole.OFFICER);

        citizenAJwt = jwtService.generateToken(citizenA.getId(), citizenA.getEmail(), "CITIZEN");
        citizenBJwt = jwtService.generateToken(citizenB.getId(), citizenB.getEmail(), "CITIZEN");
        officerJwt = jwtService.generateToken(officer.getId(), officer.getEmail(), "OFFICER");

        // 2. Create Incident for Citizen A
        incidentA = new Incident();
        incidentA.setTitle("Broken Water Pipe");
        incidentA.setDescription("Major leakage on Mission Street");
        incidentA.setCategory(IncidentCategory.WATER_SUPPLY);
        incidentA.setSeverity(IncidentSeverity.HIGH);
        incidentA.setStatus(IncidentStatus.IN_PROGRESS);
        incidentA.setLatitude(new BigDecimal("11.9320"));
        incidentA.setLongitude(new BigDecimal("79.8280"));
        incidentA.setAddress("Mission Street, Puducherry");
        incidentA.setReporter(citizenA);
        incidentA.setClassificationTag(DataClassification.OFFICIAL);
        incidentA.setIntakeSource(IncidentSource.RESIDENT_SUBMITTED);
        incidentA = incidentRepository.save(incidentA);
    }

    @AfterEach
    void tearDown() {
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

    @Test
    @DisplayName("Audit 1: User Profile IDOR Protection blocks cross-citizen enumeration")
    void testUserProfileIdorProtection() {
        authenticateAs(citizenA);

        // Citizen A accessing Citizen A -> Allowed
        UserResponse self = userService.getUserById(citizenA.getId());
        assertThat(self.getEmail()).isEqualTo(citizenA.getEmail());

        // Citizen A attempting to inspect Citizen B -> Forbidden (AccessDeniedException)
        assertThatThrownBy(() -> userService.getUserById(citizenB.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You do not have permission to view other user profiles");

        // Officer accessing Citizen B -> Allowed
        authenticateAs(officer);
        UserResponse inspected = userService.getUserById(citizenB.getId());
        assertThat(inspected.getEmail()).isEqualTo(citizenB.getEmail());
    }

    @Test
    @DisplayName("Audit 2: Priority Scoring IDOR blocks unauthorized incident inspection")
    void testPriorityScoringIdorProtection() {
        // Citizen B attempting to inspect Citizen A's incident priority
        authenticateAs(citizenB);
        assertThatThrownBy(() -> priorityScoringService.calculatePriority(incidentA.getId()))
                .isInstanceOf(AccessDeniedException.class);

        // Citizen A inspecting own incident -> Allowed
        authenticateAs(citizenA);
        PriorityScoreResponse response = priorityScoringService.calculatePriority(incidentA.getId());
        assertThat(response).isNotNull();
        assertThat(response.getPriorityScore()).isGreaterThan(0.0);

        // Officer inspecting incident -> Allowed
        authenticateAs(officer);
        PriorityScoreResponse officerResp = priorityScoringService.calculatePriority(incidentA.getId());
        assertThat(officerResp).isNotNull();
    }

    @Test
    @DisplayName("Audit 3: Priority Scoring strictly isolates SIMULATED records from cluster density")
    void testPriorityScoringExcludesSimulatedIncidents() {
        // Create a SIMULATED incident in the exact same location and category
        Incident simulated = new Incident();
        simulated.setTitle("SIMULATED Pothole Stress Test");
        simulated.setDescription("Mock stress data");
        simulated.setCategory(IncidentCategory.WATER_SUPPLY);
        simulated.setSeverity(IncidentSeverity.MEDIUM);
        simulated.setStatus(IncidentStatus.REPORTED);
        simulated.setLatitude(new BigDecimal("11.9321"));
        simulated.setLongitude(new BigDecimal("79.8281"));
        simulated.setAddress("Mission Street Test");
        simulated.setReporter(citizenA);
        simulated.setIntakeSource(IncidentSource.SIMULATED);
        simulated.setClassificationTag(DataClassification.SIMULATED);
        incidentRepository.save(simulated);

        // Evaluate priority of official incidentA
        authenticateAs(officer);
        PriorityScoreResponse response = priorityScoringService.calculatePriority(incidentA.getId());

        // Location impact factor must be 0.00 ("No nearby related incidents reported"),
        // proving simulated records did NOT inflate the score.
        assertThat(response.getFactors().getLocationImpactFactor()).isEqualTo(0.00);
        assertThat(response.getReasons()).contains("No nearby related incidents reported");
    }

    @Test
    @DisplayName("Audit 4: Duplicate Detection IDOR blocks cross-resident duplicate queries")
    void testDuplicateDetectionIdorProtection() {
        authenticateAs(citizenB);
        assertThatThrownBy(() -> duplicateDetectionService.findDuplicatesForIncident(incidentA.getId()))
                .isInstanceOf(AccessDeniedException.class);

        authenticateAs(citizenA);
        DuplicateDetectionResponse dupResp = duplicateDetectionService.findDuplicatesForIncident(incidentA.getId());
        assertThat(dupResp).isNotNull();
        assertThat(dupResp.getIncidentId()).isEqualTo(incidentA.getId());
    }

    @Test
    @DisplayName("Audit 5: Duplicate Detection strictly excludes SIMULATED incidents from candidate matches")
    void testDuplicateDetectionExcludesSimulatedIncidents() {
        // Create duplicate SIMULATED incident right at the same location
        Incident simulated = new Incident();
        simulated.setTitle("SIMULATED Duplicate");
        simulated.setDescription("Synthetic duplicate data");
        simulated.setCategory(IncidentCategory.WATER_SUPPLY);
        simulated.setSeverity(IncidentSeverity.HIGH);
        simulated.setStatus(IncidentStatus.REPORTED);
        simulated.setLatitude(new BigDecimal("11.9320"));
        simulated.setLongitude(new BigDecimal("79.8280"));
        simulated.setAddress("Mission Street");
        simulated.setReporter(citizenA);
        simulated.setIntakeSource(IncidentSource.SIMULATED);
        simulated.setClassificationTag(DataClassification.SIMULATED);
        incidentRepository.save(simulated);

        // Run duplicate detection on official incidentA
        authenticateAs(officer);
        DuplicateDetectionResponse dupResp = duplicateDetectionService.findDuplicatesForIncident(incidentA.getId());

        // Official incident must have 0 duplicate matches (simulated record ignored)
        assertThat(dupResp.getMatches()).isEmpty();
    }

    @Test
    @DisplayName("Audit 6: resolveIncident rejects resolution without verified evidence")
    void testCannotResolveIncidentWithoutEvidence() {
        authenticateAs(officer);
        assertThatThrownBy(() -> incidentWorkflowService.resolveIncident(incidentA.getId(), officer.getId()))
                .isInstanceOf(InvalidWorkflowException.class)
                .hasMessageContaining("verified resolution evidence must be submitted first");
    }

    @Test
    @DisplayName("Audit 7: createResolution transitions incident to RESOLVED and creates audit record")
    void testCreateResolutionTransitionsToResolved() {
        authenticateAs(officer);

        // 0. Create initial evidence for incidentA
        Evidence orig = new Evidence();
        orig.setIncident(incidentA);
        orig.setEvidenceType(EvidenceType.PHOTO);
        orig.setFileName("pipe_broken.jpg");
        orig.setFileUrl("https://storage.nirvaha.puducherry.gov.in/evidence/pipe_broken.jpg");
        orig.setMimeType("image/jpeg");
        orig.setFileSize(204800L);
        orig.setContentHash("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
        orig.setLatitude(new BigDecimal("11.9320"));
        orig.setLongitude(new BigDecimal("79.8280"));
        orig.setDescription("Broken water pipe leaking heavily");
        evidenceRepository.save(orig);

        // 1. Create resolution evidence
        Evidence evidence = new Evidence();
        evidence.setIncident(incidentA);
        evidence.setEvidenceType(EvidenceType.PHOTO);
        evidence.setFileName("pipe_fixed.jpg");
        evidence.setFileUrl("https://storage.nirvaha.puducherry.gov.in/evidence/pipe_fixed.jpg");
        evidence.setMimeType("image/jpeg");
        evidence.setFileSize(204800L);
        evidence.setContentHash("11223344556677889900aabbccddeeff11223344556677889900aabbccddeeff");
        evidence.setLatitude(new BigDecimal("11.9320"));
        evidence.setLongitude(new BigDecimal("79.8280"));
        evidence.setDescription("New copper pipe installed and pressure tested");
        evidence = evidenceRepository.save(evidence);

        // 2. Submit resolution
        CreateResolutionRequest req = new CreateResolutionRequest(officer.getId(), evidence.getId(), "Water line replaced successfully");
        ResolutionResponse response = resolutionService.createResolution(incidentA.getId(), req);

        assertThat(response.getId()).isNotNull();

        // 3. Verify incident status is updated to RESOLVED
        Incident reloaded = incidentRepository.findById(incidentA.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(IncidentStatus.RESOLVED);

        // 4. Verify audit trail action was logged
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(incidentA.getId());
        assertThat(actions).isNotEmpty();
        IncidentAction lastAction = actions.get(actions.size() - 1);
        assertThat(lastAction.getActionType()).isEqualTo(ActionType.RESOLVED);
        assertThat(lastAction.getRemarks()).contains("Resolution evidence submitted");
    }

    @Test
    @DisplayName("Audit 8: HTTP REST Endpoints enforce IDOR via MockMvc")
    void testHttpEndpointsEnforceIdor() throws Exception {
        // 1. Citizen A querying Citizen B's profile -> 403 Forbidden
        mockMvc.perform(get("/api/users/" + citizenB.getId())
                        .header("Authorization", "Bearer " + citizenAJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 2. Citizen A querying own profile -> 200 OK
        mockMvc.perform(get("/api/users/" + citizenA.getId())
                        .header("Authorization", "Bearer " + citizenAJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 3. Citizen B querying Citizen A's incident priority -> 403 Forbidden
        mockMvc.perform(get("/api/incidents/" + incidentA.getId() + "/priority")
                        .header("Authorization", "Bearer " + citizenBJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 4. Citizen A querying own incident priority -> 200 OK
        mockMvc.perform(get("/api/incidents/" + incidentA.getId() + "/priority")
                        .header("Authorization", "Bearer " + citizenAJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 5. Citizen B querying Citizen A's incident duplicates -> 403 Forbidden
        mockMvc.perform(get("/api/incidents/" + incidentA.getId() + "/duplicates")
                        .header("Authorization", "Bearer " + citizenBJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
