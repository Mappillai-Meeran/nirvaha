package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.entity.User;
import com.nirvaha.nirvaha_backend.entity.UserRole;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SlaTrackingControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private String citizenToken;
    private String officerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        User citizen = userRepository.findByEmail("sla.ctrl.citizen@test.puducherry.gov.in").orElseGet(() -> {
            CreateUserRequest req = new CreateUserRequest();
            req.setEmail("sla.ctrl.citizen@test.puducherry.gov.in");
            req.setPassword("CitizenPass#123");
            req.setName("Citizen Sla Tester");
            req.setRole(UserRole.CITIZEN);
            userService.createUser(req);
            return userRepository.findByEmail("sla.ctrl.citizen@test.puducherry.gov.in").orElseThrow();
        });
        citizenToken = jwtService.generateToken(citizen.getId(), citizen.getEmail(), citizen.getRole().name());

        User officer = userRepository.findByEmail("sla.ctrl.officer@test.puducherry.gov.in").orElseGet(() -> {
            CreateUserRequest req = new CreateUserRequest();
            req.setEmail("sla.ctrl.officer@test.puducherry.gov.in");
            req.setPassword("OfficerPass#123");
            req.setName("Officer Sla Tester");
            req.setRole(UserRole.OFFICER);
            userService.createUser(req);
            return userRepository.findByEmail("sla.ctrl.officer@test.puducherry.gov.in").orElseThrow();
        });
        officerToken = jwtService.generateToken(officer.getId(), officer.getEmail(), officer.getRole().name());
    }

    @Test
    void testGetStatusRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/sla/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetStatusForbiddenForCitizen() throws Exception {
        mockMvc.perform(get("/api/sla/status")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetStatusAllowedForOfficer() throws Exception {
        mockMvc.perform(get("/api/sla/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testGetOverdueAllowedForOfficer() throws Exception {
        mockMvc.perform(get("/api/sla/overdue")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testGetSummaryAllowedForOfficer() throws Exception {
        mockMvc.perform(get("/api/sla/summary")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTracked", notNullValue()))
                .andExpect(jsonPath("$.complianceRate", notNullValue()));
    }

    @Test
    void testGetRulesAllowedForOfficer() throws Exception {
        mockMvc.perform(get("/api/sla/rules")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].isOfficialSla", notNullValue()));
    }

    @Test
    void testTriggerEvaluationAllowedForOfficer() throws Exception {
        mockMvc.perform(post("/api/sla/evaluate")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.evaluatedCount", notNullValue()));
    }
}
