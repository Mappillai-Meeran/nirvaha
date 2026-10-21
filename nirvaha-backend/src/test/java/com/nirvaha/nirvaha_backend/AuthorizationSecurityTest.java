package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.LoginRequest;
import com.nirvaha.nirvaha_backend.dto.LoginResponse;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.AuthService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthorizationSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.ResolutionRepository resolutionRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.IncidentActionRepository incidentActionRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.EvidenceRepository evidenceRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.IncidentRepository incidentRepository;

    private String citizenToken;
    private String officerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        // Create CITIZEN user & generate JWT
        userService.createUser(new CreateUserRequest("Citizen User", "citizen.sec@example.com", "password123", UserRole.CITIZEN));
        LoginResponse citizenLogin = authService.login(new LoginRequest("citizen.sec@example.com", "password123"));
        citizenToken = citizenLogin.getToken();

        // Create OFFICER user & generate JWT
        userService.createUser(new CreateUserRequest("Officer User", "officer.sec@puducherry.gov.in", "password123", UserRole.OFFICER));
        LoginResponse officerLogin = authService.login(new LoginRequest("officer.sec@puducherry.gov.in", "password123"));
        officerToken = officerLogin.getToken();

        // Create ADMIN user & generate JWT
        userService.createUser(new CreateUserRequest("Admin User", "admin.sec@puducherry.gov.in", "password123", UserRole.ADMIN));
        LoginResponse adminLogin = authService.login(new LoginRequest("admin.sec@puducherry.gov.in", "password123"));
        adminToken = adminLogin.getToken();
    }

    @Test
    void unauthenticatedProtectedRequestShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void citizenAccessingOfficerWorkflowShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(post("/api/workflow/incidents/1/assign/1")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void citizenAccessingIntegrityRadarShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/integrity/open")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void citizenAccessingWardRiskShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/risk/wards")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void officerAccessingAdminRiskEvaluationShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(post("/api/risk/assessments/evaluate")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void officerAccessingOperationalRiskOverviewShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/risk/wards")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminAccessingAdminRiskEvaluationShouldReturn200() throws Exception {
        mockMvc.perform(post("/api/risk/assessments/evaluate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void citizenCanAccessAllowedEndpoints() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isOk());
    }
}
