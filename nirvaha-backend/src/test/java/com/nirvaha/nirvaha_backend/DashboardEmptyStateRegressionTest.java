package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.security.JwtService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class DashboardEmptyStateRegressionTest {

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

    private User citizenUser;
    private User officerUser;
    private User adminUser;
    private String citizenToken;
    private String officerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        citizenUser = userRepository.findByEmail("dash.diag.citizen@test.puducherry.gov.in").orElseGet(() -> {
            CreateUserRequest req = new CreateUserRequest();
            req.setEmail("dash.diag.citizen@test.puducherry.gov.in");
            req.setPassword("CitizenPass#123");
            req.setName("Dashboard Citizen Tester");
            req.setRole(UserRole.CITIZEN);
            userService.createUser(req);
            return userRepository.findByEmail("dash.diag.citizen@test.puducherry.gov.in").orElseThrow();
        });
        citizenToken = jwtService.generateToken(citizenUser.getId(), citizenUser.getEmail(), citizenUser.getRole().name());

        officerUser = userRepository.findByEmail("dash.diag.officer@test.puducherry.gov.in").orElseGet(() -> {
            CreateUserRequest req = new CreateUserRequest();
            req.setEmail("dash.diag.officer@test.puducherry.gov.in");
            req.setPassword("OfficerPass#123");
            req.setName("Dashboard Officer Tester");
            req.setRole(UserRole.OFFICER);
            userService.createUser(req);
            return userRepository.findByEmail("dash.diag.officer@test.puducherry.gov.in").orElseThrow();
        });
        officerToken = jwtService.generateToken(officerUser.getId(), officerUser.getEmail(), officerUser.getRole().name());

        adminUser = userRepository.findByEmail("dash.diag.admin@test.puducherry.gov.in").orElseGet(() -> {
            CreateUserRequest req = new CreateUserRequest();
            req.setEmail("dash.diag.admin@test.puducherry.gov.in");
            req.setPassword("AdminPass#123");
            req.setName("Dashboard Admin Tester");
            req.setRole(UserRole.ADMIN);
            userService.createUser(req);
            return userRepository.findByEmail("dash.diag.admin@test.puducherry.gov.in").orElseThrow();
        });
        adminToken = jwtService.generateToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole().name());
    }

    @Test
    void unauthenticatedUserCannotAccessIncidentListing() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void citizenIsForbiddenFromGeneralIncidentListing() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void officerCanAccessIncidentListingAndGetsValidResponse() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminCanAccessIncidentListingAndGetsValidResponse() throws Exception {
        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void citizenIsForbiddenFromActionCenterQueue() throws Exception {
        mockMvc.perform(get("/api/action-center/queue")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void officerCanAccessActionCenterQueue() throws Exception {
        mockMvc.perform(get("/api/action-center/queue")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations").isArray())
                .andExpect(jsonPath("$.totalActionableCount").isNumber());
    }

    @Test
    void officialIncidentsAreSeparatedFromSimulatedIncidentsByDefault() throws Exception {
        // Create an official incident
        Incident officialInc = new Incident();
        officialInc.setTitle("Official Road Damage on Mission St");
        officialInc.setDescription("Official pothole reported by verified resident");
        officialInc.setCategory(IncidentCategory.POTHOLE);
        officialInc.setSeverity(IncidentSeverity.MEDIUM);
        officialInc.setStatus(IncidentStatus.REPORTED);
        officialInc.setLatitude(new BigDecimal("11.9330"));
        officialInc.setLongitude(new BigDecimal("79.8300"));
        officialInc.setAddress("Mission Street, Puducherry");
        officialInc.setReporter(citizenUser);
        officialInc.setIntakeSource(IncidentSource.RESIDENT_SUBMITTED);
        officialInc.setClassificationTag(DataClassification.CITIZEN);
        officialInc.setCreatedAt(LocalDateTime.now());
        Incident savedOfficial = incidentRepository.save(officialInc);

        // Create a simulated incident
        Incident simulatedInc = new Incident();
        simulatedInc.setTitle("Simulated Drainage Flood");
        simulatedInc.setDescription("Simulated flood for demo purposes");
        simulatedInc.setCategory(IncidentCategory.DRAINAGE);
        simulatedInc.setSeverity(IncidentSeverity.HIGH);
        simulatedInc.setStatus(IncidentStatus.REPORTED);
        simulatedInc.setLatitude(new BigDecimal("11.9340"));
        simulatedInc.setLongitude(new BigDecimal("79.8310"));
        simulatedInc.setAddress("Simulated St, Puducherry");
        simulatedInc.setReporter(citizenUser);
        simulatedInc.setIntakeSource(IncidentSource.SIMULATED);
        simulatedInc.setClassificationTag(DataClassification.SIMULATED);
        simulatedInc.setDemoRunId("DEMO-RUN-REGRESSION-TEST");
        simulatedInc.setCreatedAt(LocalDateTime.now());
        Incident savedSimulated = incidentRepository.save(simulatedInc);

        try {
            // Default query (includeSimulated = false): should contain official, exclude simulated
            mockMvc.perform(get("/api/incidents")
                            .header("Authorization", "Bearer " + officerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(savedOfficial.getId().intValue())))
                    .andExpect(jsonPath("$[*].id", not(hasItem(savedSimulated.getId().intValue()))));

            // Explicit query (includeSimulated = true): should contain both
            mockMvc.perform(get("/api/incidents?includeSimulated=true")
                            .header("Authorization", "Bearer " + officerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].id", hasItem(savedOfficial.getId().intValue())))
                    .andExpect(jsonPath("$[*].id", hasItem(savedSimulated.getId().intValue())));
        } finally {
            incidentRepository.delete(savedSimulated);
            incidentRepository.delete(savedOfficial);
        }
    }

    @Test
    void reportedAndVerifiedIncidentsExposeExactEnumsForFrontendFiltering() throws Exception {
        Incident reportedInc = new Incident();
        reportedInc.setTitle("Freshly Reported Civic Issue");
        reportedInc.setDescription("Newly logged civic complaint");
        reportedInc.setCategory(IncidentCategory.DRAINAGE);
        reportedInc.setSeverity(IncidentSeverity.HIGH);
        reportedInc.setStatus(IncidentStatus.REPORTED);
        reportedInc.setLatitude(new BigDecimal("11.9330"));
        reportedInc.setLongitude(new BigDecimal("79.8300"));
        reportedInc.setAddress("Bussy Street, Puducherry");
        reportedInc.setReporter(citizenUser);
        reportedInc.setIntakeSource(IncidentSource.RESIDENT_SUBMITTED);
        reportedInc.setClassificationTag(DataClassification.CITIZEN);
        reportedInc.setCreatedAt(LocalDateTime.now());
        Incident savedReported = incidentRepository.save(reportedInc);

        Incident verifiedInc = new Incident();
        verifiedInc.setTitle("Assigned and Verified Issue");
        verifiedInc.setDescription("Assigned to field officer");
        verifiedInc.setCategory(IncidentCategory.STREETLIGHT);
        verifiedInc.setSeverity(IncidentSeverity.MEDIUM);
        verifiedInc.setStatus(IncidentStatus.VERIFIED);
        verifiedInc.setLatitude(new BigDecimal("11.9350"));
        verifiedInc.setLongitude(new BigDecimal("79.8320"));
        verifiedInc.setAddress("JN Street, Puducherry");
        verifiedInc.setReporter(citizenUser);
        verifiedInc.setIntakeSource(IncidentSource.RESIDENT_SUBMITTED);
        verifiedInc.setClassificationTag(DataClassification.CITIZEN);
        verifiedInc.setCreatedAt(LocalDateTime.now());
        Incident savedVerified = incidentRepository.save(verifiedInc);

        try {
            mockMvc.perform(get("/api/incidents/" + savedReported.getId())
                            .header("Authorization", "Bearer " + officerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("REPORTED")));

            mockMvc.perform(get("/api/incidents/" + savedVerified.getId())
                            .header("Authorization", "Bearer " + officerToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("VERIFIED")));
        } finally {
            incidentRepository.delete(savedReported);
            incidentRepository.delete(savedVerified);
        }
    }
}
