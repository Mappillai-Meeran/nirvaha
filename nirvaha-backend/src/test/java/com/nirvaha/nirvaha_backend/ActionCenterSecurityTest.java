package com.nirvaha.nirvaha_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.LoginRequest;
import com.nirvaha.nirvaha_backend.dto.LoginResponse;
import com.nirvaha.nirvaha_backend.dto.OfficerDecisionRequest;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.AuthService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class ActionCenterSecurityTest {

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
    private IncidentRepository incidentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String citizenToken;
    private String officerToken;
    private Incident testIncident;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String citizenEmail = "ac_citizen@nirvaha.gov.in";
        if (userRepository.findByEmail(citizenEmail).isEmpty()) {
            userService.createUser(new CreateUserRequest("AC Citizen", citizenEmail, "Password123!", UserRole.CITIZEN));
        }
        LoginResponse citLogin = authService.login(new LoginRequest(citizenEmail, "Password123!"));
        citizenToken = citLogin.getToken();

        String officerEmail = "ac_officer@nirvaha.gov.in";
        if (userRepository.findByEmail(officerEmail).isEmpty()) {
            User officer = new User();
            officer.setName("AC Officer");
            officer.setEmail(officerEmail);
            officer.setPasswordHash(new BCryptPasswordEncoder().encode("Password123!"));
            officer.setRole(UserRole.OFFICER);
            userRepository.save(officer);
        }
        LoginResponse offLogin = authService.login(new LoginRequest(officerEmail, "Password123!"));
        officerToken = offLogin.getToken();

        testIncident = new Incident();
        testIncident.setTitle("Security Test Incident");
        testIncident.setDescription("Test desc");
        testIncident.setCategory(IncidentCategory.DRAINAGE);
        testIncident.setSeverity(IncidentSeverity.HIGH);
        testIncident.setStatus(IncidentStatus.REPORTED);
        testIncident.setClassificationTag(DataClassification.CITIZEN);
        testIncident.setLatitude(new BigDecimal("11.9416"));
        testIncident.setLongitude(new BigDecimal("79.8083"));
        testIncident.setReporter(userRepository.findByEmail(officerEmail).orElse(null));
        incidentRepository.save(testIncident);
    }

    @Test
    public void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/action-center/queue"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCitizenAccessReturns403() throws Exception {
        mockMvc.perform(get("/api/action-center/queue")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testOfficerAccessReturns200() throws Exception {
        mockMvc.perform(get("/api/action-center/queue")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActionableCount").exists())
                .andExpect(jsonPath("$.recommendations").isArray());
    }

    @Test
    public void testOfficerDecisionPostReturns200() throws Exception {
        OfficerDecisionRequest req = new OfficerDecisionRequest(
                testIncident.getId(),
                "rec-sec-1",
                RecommendedActionType.FIELD_INSPECTION,
                OfficerDecisionType.ACCEPT,
                "Accepted recommendation for inspection"
        );

        mockMvc.perform(post("/api/action-center/decisions")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("ACCEPT"));
    }
}
