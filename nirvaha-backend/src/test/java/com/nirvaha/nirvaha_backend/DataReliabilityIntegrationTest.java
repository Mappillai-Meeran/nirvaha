package com.nirvaha.nirvaha_backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirvaha.nirvaha_backend.dto.IntegrationHealthDTO;
import com.nirvaha.nirvaha_backend.dto.PriorityScoreResponse;
import com.nirvaha.nirvaha_backend.dto.WeatherData;
import com.nirvaha.nirvaha_backend.entity.*;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class DataReliabilityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private ImdWeatherAdapter imdWeatherAdapter;

    @Autowired
    private IntegrationHealthService integrationHealthService;

    @Autowired
    private RiskIntelligenceService riskIntelligenceService;

    @Autowired
    private PriorityScoringService priorityScoringService;

    @Autowired
    private ServiceRuleService serviceRuleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private WeatherObservationRepository weatherRepository;

    @Autowired
    private IncidentServiceTrackingRepository trackingRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private IncidentActionRepository actionRepository;

    @Autowired
    private ResolutionRepository resolutionRepository;

    @Autowired
    private IncidentFeedbackRepository feedbackRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private User officerUser;
    private User citizenUser;
    private String officerJwt;
    private String citizenJwt;
    private Ward boulevardWard;

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
        trackingRepository.deleteAll();
        evidenceRepository.deleteAll();
        weatherRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        officerUser = new User();
        officerUser.setName("Officer Narayanan");
        officerUser.setEmail("narayanan@puducherry.gov.in");
        officerUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        officerUser.setRole(UserRole.OFFICER);
        officerUser = userRepository.save(officerUser);

        citizenUser = new User();
        citizenUser.setName("Citizen Selvam");
        citizenUser.setEmail("selvam@gmail.test");
        citizenUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
        citizenUser.setRole(UserRole.CITIZEN);
        citizenUser = userRepository.save(citizenUser);

        officerJwt = jwtService.generateToken(officerUser.getId(), officerUser.getEmail(), "OFFICER");
        citizenJwt = jwtService.generateToken(citizenUser.getId(), citizenUser.getEmail(), "CITIZEN");

        List<Ward> wards = wardRepository.findAll();
        if (!wards.isEmpty()) {
            boulevardWard = wards.get(0);
        }
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        feedbackRepository.deleteAll();
        resolutionRepository.deleteAll();
        actionRepository.deleteAll();
        trackingRepository.deleteAll();
        weatherRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Reliability 1: /api/system/integration-health reports genuine statuses and sanitizes credentials")
    void testIntegrationHealthEndpointAccuracyAndSanitization() throws Exception {
        String json = mockMvc.perform(get("/api/system/integration-health")
                        .header("Authorization", "Bearer " + officerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemStatus").exists())
                .andExpect(jsonPath("$.integrations").isArray())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
        assertThat(root.has("systemStatus")).isTrue();
        assertThat(root.has("integrations")).isTrue();
        com.fasterxml.jackson.databind.JsonNode integrations = root.path("integrations");
        assertThat(integrations.isArray()).isTrue();
        assertThat(integrations.size()).isGreaterThanOrEqualTo(5);

        boolean foundImd = false;
        boolean foundOpenMeteo = false;
        for (com.fasterxml.jackson.databind.JsonNode item : integrations) {
            String name = item.path("name").asText("");
            if (name.contains("IMD")) {
                foundImd = true;
                String imdStatus = item.path("status").asText();
                assertThat(imdStatus).isIn("NOT_CONFIGURED", "PENDING_AUTHORIZATION");
                assertThat(item.path("endpoint").asText()).doesNotContain("api_key=");
                assertThat(item.path("observationSummary").asText()).doesNotContain("password=");
            }
            if (name.contains("Open-Meteo")) {
                foundOpenMeteo = true;
                String openMeteoStatus = item.path("status").asText();
                assertThat(openMeteoStatus).isIn("CONNECTED", "STALE", "UNAVAILABLE");
            }
        }
        assertThat(foundImd).isTrue();
        assertThat(foundOpenMeteo).isTrue();
    }

    @Test
    @DisplayName("Reliability 2: Live weather endpoint /api/provenance/weather/live returns telemetry and timestamp provenance")
    void testLiveWeatherEndpointProvenance() throws Exception {
        mockMvc.perform(get("/api/provenance/weather/live")
                        .param("latitude", "11.9156")
                        .param("longitude", "79.8296")
                        .header("Authorization", "Bearer " + citizenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("Open-Meteo"))
                .andExpect(jsonPath("$.temperatureUnit").value("°C"))
                .andExpect(jsonPath("$.rainfallUnit").value("mm"))
                .andExpect(jsonPath("$.humidityUnit").value("%"))
                .andExpect(jsonPath("$.retrievedAt").exists());
    }

    @Test
    @DisplayName("Reliability 3: Weather observations are strictly partitioned from citizen incidents")
    void testWeatherObservationsDoNotBecomeIncidents() {
        int initialIncidentCount = incidentRepository.findAll().size();

        // Record a verified official weather observation
        WeatherObservation obs = new WeatherObservation();
        obs.setProvider("India Meteorological Department (IMD)");
        obs.setStationOrDistrictCode("PUDUCHERRY_COASTAL");
        obs.setObservationType("DAILY_SUMMARY");
        obs.setObservedAt(LocalDateTime.now().minusHours(2));
        obs.setRainfallMm(18.5);
        obs.setNormalRainfallMm(15.0);
        obs.setDeparturePercent(23.3);
        obs.setRainfallCategory("MODERATE");
        obs.setRetrievedAt(LocalDateTime.now());
        obs.setProvenanceStatus("VERIFIED_OFFICIAL_IMD");
        obs.setRawPayloadHash("test_hash_payload_12345");
        weatherRepository.save(obs);

        // Verify incident table remains completely uncontaminated
        assertThat(incidentRepository.findAll().size()).isEqualTo(initialIncidentCount);
        assertThat(weatherRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Reliability 4: Citizen incident provenance is preserved and exposed with full audit trail")
    void testCitizenIncidentProvenancePreserved() throws Exception {
        Incident incident = new Incident();
        incident.setTitle("Pothole on Romain Rolland St");
        incident.setDescription("Deep pothole damaging scooters");
        incident.setCategory(IncidentCategory.POTHOLE);
        incident.setSeverity(IncidentSeverity.MEDIUM);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setLatitude(new BigDecimal("11.9330"));
        incident.setLongitude(new BigDecimal("79.8350"));
        incident.setAddress("Romain Rolland Street, White Town");
        incident.setReporter(citizenUser);
        incident.setClassificationTag(DataClassification.OFFICIAL);
        incident.setIntakeSource(IncidentSource.RESIDENT_SUBMITTED);
        incident.setExternalRecordId("CIT-2026-9081");
        incident.setExternalSourceIdentifier("MOBILE_APP_PUDUCHERRY");
        incident.setRetrievalTimestamp(LocalDateTime.now());
        incident.setSourceTimestamp(LocalDateTime.now().minusMinutes(10));
        incident.setIngestionOutcome("ACCEPTED_VERIFIED");
        incident = incidentRepository.save(incident);

        // Inspect through API
        mockMvc.perform(get("/api/incidents/" + incident.getId())
                        .header("Authorization", "Bearer " + citizenJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.intakeSource").value("RESIDENT_SUBMITTED"))
                .andExpect(jsonPath("$.externalRecordId").value("CIT-2026-9081"))
                .andExpect(jsonPath("$.externalSourceIdentifier").value("MOBILE_APP_PUDUCHERRY"))
                .andExpect(jsonPath("$.ingestionOutcome").value("ACCEPTED_VERIFIED"))
                .andExpect(jsonPath("$.retrievalTimestamp").exists())
                .andExpect(jsonPath("$.sourceTimestamp").exists());
    }

    @Test
    @DisplayName("Reliability 5: Simulated records are strictly isolated from official SLA metrics and risk scores")
    void testSimulatedRecordsCannotContaminateOfficialMetrics() {
        // 1. Create official incident
        Incident official = new Incident();
        official.setTitle("Official Drainage Choke");
        official.setDescription("Stormwater drain clogged");
        official.setCategory(IncidentCategory.DRAINAGE);
        official.setSeverity(IncidentSeverity.HIGH);
        official.setStatus(IncidentStatus.IN_PROGRESS);
        official.setLatitude(new BigDecimal("11.9310"));
        official.setLongitude(new BigDecimal("79.8300"));
        official.setAddress("MG Road");
        official.setReporter(citizenUser);
        official.setClassificationTag(DataClassification.OFFICIAL);
        official.setIntakeSource(IncidentSource.RESIDENT_SUBMITTED);
        official = incidentRepository.save(official);

        // 2. Create simulated incident in same area that is overdue
        Incident simulated = new Incident();
        simulated.setTitle("SIMULATED Stress Flood");
        simulated.setDescription("Mock stress surge");
        simulated.setCategory(IncidentCategory.DRAINAGE);
        simulated.setSeverity(IncidentSeverity.CRITICAL);
        simulated.setStatus(IncidentStatus.IN_PROGRESS);
        simulated.setCreatedAt(LocalDateTime.now().minusDays(14)); // Overdue
        simulated.setLatitude(new BigDecimal("11.9311"));
        simulated.setLongitude(new BigDecimal("79.8301"));
        simulated.setAddress("MG Road Simulation");
        simulated.setReporter(officerUser);
        simulated.setClassificationTag(DataClassification.SIMULATED);
        simulated.setIntakeSource(IncidentSource.SIMULATED);
        Incident savedSimulated = incidentRepository.save(simulated);

        // Evaluate SLAs
        serviceRuleService.evaluateAndEscalate(official);
        serviceRuleService.evaluateAndEscalate(savedSimulated);

        // Query official overdue tracking
        List<IncidentServiceTracking> overdue = serviceRuleService.getOverdueTracking();
        boolean simulatedInOverdue = overdue.stream()
                .anyMatch(t -> t.getIncident().getId().equals(savedSimulated.getId()));
        assertThat(simulatedInOverdue).isFalse();

        // Priority calculation of official incident does NOT treat simulated record as cluster density
        PriorityScoreResponse priorityResp = priorityScoringService.calculatePriority(official.getId());
        assertThat(priorityResp.getFactors().getLocationImpactFactor()).isEqualTo(0.00);
    }

    @Test
    @DisplayName("Reliability 6: Public Civic Provenance catalog is accessible without auth and contains 9 verified sources with honest operational statuses")
    void testPublicProvenanceEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/provenance/sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[?(@.name == 'Open-Meteo Real-Time Weather & Forecast API')].status").value("ACTIVE"))
                .andExpect(jsonPath("$[?(@.name == 'India Open Government Data Platform (OGD)')].status").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$[?(@.name == 'India Meteorological Department (IMD)')].status").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$[?(@.name == 'Puducherry LG\\'s Grievance Redressal System')].status").value("RESTRICTED_ACCESS"))
                .andExpect(jsonPath("$[?(@.name == 'Pondicherry Municipality Grievance Redressal')].status").value("POLICY_REFERENCE"))
                .andExpect(jsonPath("$[?(@.name == 'Open-Meteo Real-Time Weather & Forecast API')].license").value("Open Database License (ODbL) / CC BY 4.0"))
                .andExpect(jsonPath("$[?(@.name == 'Puducherry Local Administration Department (LAD)')].license").value("Official Public Domain / Govt. of Puducherry"))
                .andExpect(jsonPath("$[?(@.name == 'India Open Government Data Platform (OGD)')].license").value("Open Government Data License - India"));

        // Administrative areas honestly report APPROXIMATE geometry status and REVIEW_REQUIRED validation
        mockMvc.perform(get("/api/provenance/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].geometryStatus").value("APPROXIMATE"))
                .andExpect(jsonPath("$[0].validationStatus").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$[0].reviewerNotes").exists());

        // Service rules separate official statutory SLAs from proposed internal operational targets
        mockMvc.perform(get("/api/provenance/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.serviceCategory == 'POTHOLE' && @.department == 'PWD')].isOfficialSla").value(true))
                .andExpect(jsonPath("$[?(@.serviceCategory == 'DRAINAGE' && @.department == 'PWD')].isOfficialSla").value(true))
                .andExpect(jsonPath("$[?(@.serviceCategory == 'GARBAGE')].isOfficialSla").value(false))
                .andExpect(jsonPath("$[?(@.serviceCategory == 'STREETLIGHT')].isOfficialSla").value(false));
    }
}



