package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateEvidenceRequest;
import com.nirvaha.nirvaha_backend.dto.CreateIncidentRequest;
import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.EvidenceResponse;
import com.nirvaha.nirvaha_backend.dto.IncidentResponse;
import com.nirvaha.nirvaha_backend.dto.UserResponse;
import com.nirvaha.nirvaha_backend.entity.EvidenceType;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.repository.EvidenceRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentActionRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.ResolutionRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.EvidenceService;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ResidentPrivacyIsolationTest {

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
    private ResolutionRepository resolutionRepository;

    @Autowired
    private IncidentActionRepository incidentActionRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    private Long citizenAId;
    private Long citizenBId;
    private Long officerId;
    private Long adminId;

    private Long incidentAId;
    private Long incidentBId;

    private Long evidenceAId;
    private Long evidenceBId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        org.springframework.security.core.Authentication currentAuth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        try {
            resolutionRepository.deleteAll();
            incidentActionRepository.deleteAll();
            evidenceRepository.deleteAll();
            incidentRepository.deleteAll();
            userRepository.deleteAll();

            // 1. Seed test users
            UserResponse citizenA = userService.createUser(new CreateUserRequest(
                    "Citizen A", "citizen.a@example.com", "Password123!", UserRole.CITIZEN
            ));
            citizenAId = citizenA.getId();

            UserResponse citizenB = userService.createUser(new CreateUserRequest(
                    "Citizen B", "citizen.b@example.com", "Password123!", UserRole.CITIZEN
            ));
            citizenBId = citizenB.getId();

            userService.createUser(new CreateUserRequest(
                    "Fresh Citizen", "fresh.citizen@example.com", "Password123!", UserRole.CITIZEN
            ));

            UserResponse officer = userService.createUser(new CreateUserRequest(
                    "Officer Puducherry", "officer@puducherry.gov.in", "Officer123!", UserRole.OFFICER
            ));
            officerId = officer.getId();

            UserResponse admin = userService.createUser(new CreateUserRequest(
                    "Admin Puducherry", "admin@puducherry.gov.in", "Admin123!", UserRole.ADMIN
            ));
            adminId = admin.getId();

            // 2. Create incidents
            IncidentResponse incA = incidentService.createIncident(new CreateIncidentRequest(
                    citizenAId, "Pothole on Beach Road", "Large crater causing traffic hazard",
                    IncidentCategory.POTHOLE, IncidentSeverity.MEDIUM,
                    new BigDecimal("11.9338"), new BigDecimal("79.8354"), "Beach Road, Puducherry"
            ));
            incidentAId = incA.getId();

            IncidentResponse incB = incidentService.createIncident(new CreateIncidentRequest(
                    citizenBId, "Waterlogging at Mission Street", "Storm drain blocked with silt",
                    IncidentCategory.DRAINAGE, IncidentSeverity.HIGH,
                    new BigDecimal("11.9350"), new BigDecimal("79.8300"), "Mission Street, Puducherry"
            ));
            incidentBId = incB.getId();

            // 3. Attach evidence to incidents
            EvidenceResponse evA = evidenceService.createEvidence(new CreateEvidenceRequest(
                    incidentAId, EvidenceType.PHOTO, "/uploads/evidence/beach_road_pothole.jpg",
                    "beach_road_pothole.jpg", "image/jpeg", 250000L,
                    LocalDateTime.now(), new BigDecimal("11.9338"), new BigDecimal("79.8354"),
                    "Initial citizen photo of pothole"
            ));
            evidenceAId = evA.getId();

            EvidenceResponse evB = evidenceService.createEvidence(new CreateEvidenceRequest(
                    incidentBId, EvidenceType.PHOTO, "/uploads/evidence/mission_street_drain.jpg",
                    "mission_street_drain.jpg", "image/jpeg", 310000L,
                    LocalDateTime.now(), new BigDecimal("11.9350"), new BigDecimal("79.8300"),
                    "Initial citizen photo of clogged drain"
            ));
            evidenceBId = evB.getId();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(currentAuth);
        }
    }

    @Test
    @DisplayName("Citizen role cannot call GET /api/incidents and receives 403 Forbidden")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenCannotAccessGeneralIncidentsEndpoint() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Citizen A receives only their own reports via GET /api/incidents/my-reports")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenASeesOnlyOwnReports() throws Exception {
        mockMvc.perform(get("/api/incidents/my-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id", is(incidentAId.intValue())))
                .andExpect(jsonPath("$[0].title", is("Pothole on Beach Road")));
    }

    @Test
    @DisplayName("Citizen B receives only their own reports via GET /api/incidents/my-reports")
    @WithMockUser(username = "citizen.b@example.com", roles = {"CITIZEN"})
    void citizenBSeesOnlyOwnReports() throws Exception {
        mockMvc.perform(get("/api/incidents/my-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id", is(incidentBId.intValue())))
                .andExpect(jsonPath("$[0].title", is("Waterlogging at Mission Street")));
    }

    @Test
    @DisplayName("Citizen with zero reports receives empty list with 200 OK")
    @WithMockUser(username = "fresh.citizen@example.com", roles = {"CITIZEN"})
    void citizenWithNoReportsGetsEmptyList() throws Exception {
        mockMvc.perform(get("/api/incidents/my-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    @DisplayName("Officer calling GET /api/incidents/my-reports receives 200 OK and empty list when no reports authored")
    @WithMockUser(username = "officer@puducherry.gov.in", roles = {"OFFICER"})
    void officerCanCallMyReportsWithoutError() throws Exception {
        mockMvc.perform(get("/api/incidents/my-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    @DisplayName("Citizen A cannot read Citizen B's incident details via GET /api/incidents/{b_id} (403 Forbidden)")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenACannotReadCitizenBIncidentDetails() throws Exception {
        mockMvc.perform(get("/api/incidents/" + incidentBId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Citizen A can read their own incident details via GET /api/incidents/{a_id} (200 OK)")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenACanReadOwnIncidentDetails() throws Exception {
        mockMvc.perform(get("/api/incidents/" + incidentAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(incidentAId.intValue())))
                .andExpect(jsonPath("$.title", is("Pothole on Beach Road")));
    }

    @Test
    @DisplayName("Citizen A cannot access Citizen B's evidence list via GET /api/evidence/incident/{b_id} (403 Forbidden)")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenACannotAccessCitizenBEvidenceList() throws Exception {
        mockMvc.perform(get("/api/evidence/incident/" + incidentBId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Citizen A can access their own evidence list via GET /api/evidence/incident/{a_id} (200 OK)")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenACanAccessOwnEvidenceList() throws Exception {
        mockMvc.perform(get("/api/evidence/incident/" + incidentAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id", is(evidenceAId.intValue())));
    }

    @Test
    @DisplayName("Citizen A cannot access Citizen B's individual evidence record via GET /api/evidence/{b_ev_id} (403 Forbidden)")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenACannotAccessCitizenBSingleEvidence() throws Exception {
        mockMvc.perform(get("/api/evidence/" + evidenceBId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Citizen A cannot access Citizen B's actions via GET /api/incidents/{b_id}/actions (403 Forbidden)")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenACannotAccessCitizenBActions() throws Exception {
        mockMvc.perform(get("/api/incidents/" + incidentBId + "/actions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Citizen A cannot access Citizen B's workflow via GET /api/incidents/{b_id}/workflow (403 Forbidden)")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenACannotAccessCitizenBWorkflow() throws Exception {
        mockMvc.perform(get("/api/incidents/" + incidentBId + "/workflow"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Citizen A cannot access Citizen B's resolution record via GET /api/incidents/{b_id}/resolution (403 Forbidden)")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenACannotAccessCitizenBResolution() throws Exception {
        mockMvc.perform(get("/api/incidents/" + incidentBId + "/resolution"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Citizen cannot perform workflow actions via POST /api/incidents/{id}/actions (403 Forbidden)")
    @WithMockUser(username = "citizen.a@example.com", roles = {"CITIZEN"})
    void citizenCannotPerformWorkflowActions() throws Exception {
        mockMvc.perform(post("/api/incidents/" + incidentAId + "/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":" + officerId + ",\"actionType\":\"ASSIGNED\",\"remarks\":\"Attempted citizen override\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Officer can access general incidents list via GET /api/incidents (200 OK)")
    @WithMockUser(username = "officer@puducherry.gov.in", roles = {"OFFICER"})
    void officerCanAccessGeneralIncidentsList() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    @DisplayName("Admin can access general incidents list via GET /api/incidents (200 OK)")
    @WithMockUser(username = "admin@puducherry.gov.in", roles = {"ADMIN"})
    void adminCanAccessGeneralIncidentsList() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    @DisplayName("Officer can access any incident details and sub-resources across residents")
    @WithMockUser(username = "officer@puducherry.gov.in", roles = {"OFFICER"})
    void officerCanAccessAnyIncidentAndSubResources() throws Exception {
        mockMvc.perform(get("/api/incidents/" + incidentAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(incidentAId.intValue())));

        mockMvc.perform(get("/api/incidents/" + incidentBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(incidentBId.intValue())));

        mockMvc.perform(get("/api/evidence/incident/" + incidentAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)));

        mockMvc.perform(get("/api/incidents/" + incidentAId + "/actions"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/incidents/" + incidentAId + "/workflow"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated requests return 401 Unauthorized")
    void unauthenticatedRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/incidents/my-reports"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/incidents/" + incidentAId))
                .andExpect(status().isUnauthorized());
    }
}
