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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IntegrityAnomalyLiveDemoTest {

    @Autowired
    private IntegrityAnomalyService integrityAnomalyService;

    @Autowired
    private UserService userService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private ResolutionService resolutionService;

    @Autowired
    private IncidentWorkflowService incidentWorkflowService;

    @Autowired
    private AnomalyFlagRepository anomalyFlagRepository;

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

    private Long citizenId;
    private Long officerId;

    @BeforeEach
    void setUp() {
        anomalyFlagRepository.deleteAll();
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        UserResponse citizen = userService.createUser(new CreateUserRequest("Demo Citizen", "citizen.demo@nirvaha.gov", "password", UserRole.CITIZEN));
        citizenId = citizen.getId();

        UserResponse officer = userService.createUser(new CreateUserRequest("Demo Officer", "officer.demo@nirvaha.gov", "password", UserRole.OFFICER));
        officerId = officer.getId();
    }

    @Test
    @DisplayName("Demo Scenario A: Resolution Evidence Reuse Detection")
    void demoScenarioA_ResolutionEvidenceReuse() {
        // Incident 101
        IncidentResponse inc1 = incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Broken Water Pipeline Street 1", "Water gushing onto main street", IncidentCategory.WATER_SUPPLY, IncidentSeverity.HIGH, new BigDecimal("11.933000"), new BigDecimal("79.830000"), "Street 1"
        ));
        EvidenceResponse orig1 = evidenceService.createEvidence(new CreateEvidenceRequest(
                inc1.getId(), EvidenceType.PHOTO, "http://storage.puducherry.gov.in/evidence/orig_water1.jpg", "orig_water1.jpg", "image/jpeg", 2048L, LocalDateTime.now(), new BigDecimal("11.933000"), new BigDecimal("79.830000"), "Initial photo"
        ));
        EvidenceResponse resEv1 = evidenceService.createEvidence(new CreateEvidenceRequest(
                inc1.getId(), EvidenceType.PHOTO, "http://storage.puducherry.gov.in/evidence/shared_repair_pipe.jpg", "shared_repair_pipe.jpg", "image/jpeg", 2048L, LocalDateTime.now(), new BigDecimal("11.933000"), new BigDecimal("79.830000"), "Repaired pipe photo"
        ));
        incidentWorkflowService.addAction(inc1.getId(), new CreateIncidentActionRequest(officerId, ActionType.VERIFIED, "Verified issue"));
        resolutionService.createResolution(inc1.getId(), new CreateResolutionRequest(officerId, resEv1.getId(), "Replaced main valve"));

        // Incident 108 (different location, same resolution evidence reused)
        IncidentResponse inc2 = incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Leaking Pipe Sector 4", "Minor leakage near public park", IncidentCategory.WATER_SUPPLY, IncidentSeverity.MEDIUM, new BigDecimal("12.050000"), new BigDecimal("80.150000"), "Sector 4"
        ));
        EvidenceResponse orig2 = evidenceService.createEvidence(new CreateEvidenceRequest(
                inc2.getId(), EvidenceType.PHOTO, "http://storage.puducherry.gov.in/evidence/orig_water2.jpg", "orig_water2.jpg", "image/jpeg", 2048L, LocalDateTime.now(), new BigDecimal("12.050000"), new BigDecimal("80.150000"), "Initial photo"
        ));
        EvidenceResponse resEv2 = evidenceService.createEvidence(new CreateEvidenceRequest(
                inc2.getId(), EvidenceType.PHOTO, "http://storage.puducherry.gov.in/evidence/shared_repair_pipe.jpg", "shared_repair_pipe.jpg", "image/jpeg", 2048L, LocalDateTime.now(), new BigDecimal("12.050000"), new BigDecimal("80.150000"), "Repaired pipe photo"
        ));
        incidentWorkflowService.addAction(inc2.getId(), new CreateIncidentActionRequest(officerId, ActionType.VERIFIED, "Verified issue"));
        resolutionService.createResolution(inc2.getId(), new CreateResolutionRequest(officerId, resEv2.getId(), "Fixed valve"));

        // Execute Detection
        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc2.getId());

        System.out.println("\n--- DEMO SCENARIO A RESULT ---");
        System.out.println("Incident ID: " + response.getIncidentId());
        response.getAnomalies().forEach(a -> {
            System.out.println("Anomaly Type: " + a.getAnomalyType());
            System.out.println("Severity: " + a.getSeverity());
            System.out.println("Description: " + a.getDescription());
            System.out.println("Related Incidents: " + a.getRelatedIncidentIds());
        });

        assertThat(response.getAnomalies()).hasSize(1);
        assertThat(response.getAnomalies().get(0).getAnomalyType()).isEqualTo(AnomalyType.RESOLUTION_EVIDENCE_REUSE);
        assertThat(response.getAnomalies().get(0).getSeverity()).isEqualTo(AnomalySeverity.HIGH);
    }

    @Test
    @DisplayName("Demo Scenario B: Suspicious Bulk Closures Detection")
    void demoScenarioB_BulkClosures() {
        IncidentCategory[] categories = {IncidentCategory.POTHOLE, IncidentCategory.DRAINAGE, IncidentCategory.GARBAGE, IncidentCategory.STREETLIGHT, IncidentCategory.WATER_SUPPLY, IncidentCategory.OTHER, IncidentCategory.POTHOLE};
        IncidentResponse targetInc = null;
        for (int i = 0; i < 7; i++) {
            BigDecimal lat = new BigDecimal(10 + i + ".000000");
            BigDecimal lng = new BigDecimal(70 + i + ".000000");
            IncidentResponse inc = incidentService.createIncident(new CreateIncidentRequest(
                    citizenId, "Civic Ticket " + (i + 1), "Description " + (i + 1), categories[i], IncidentSeverity.LOW, lat, lng, "Location " + (i + 1)
            ));
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officerId, ActionType.VERIFIED, "Verified"));
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officerId, ActionType.RESOLVED, "Quick administrative resolution " + (i + 1)));
            if (i == 6) {
                targetInc = inc;
            }
        }

        // Execute Detection
        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(targetInc.getId());

        System.out.println("\n--- DEMO SCENARIO B RESULT ---");
        System.out.println("Incident ID: " + response.getIncidentId());
        response.getAnomalies().forEach(a -> {
            System.out.println("Anomaly Type: " + a.getAnomalyType());
            System.out.println("Severity: " + a.getSeverity());
            System.out.println("Description: " + a.getDescription());
            System.out.println("Related Incidents: " + a.getRelatedIncidentIds());
        });

        assertThat(response.getAnomalies()).hasSize(1);
        assertThat(response.getAnomalies().get(0).getAnomalyType()).isEqualTo(AnomalyType.BULK_CLOSURE);
        assertThat(response.getAnomalies().get(0).getSeverity()).isEqualTo(AnomalySeverity.MEDIUM);
        assertThat(response.getAnomalies().get(0).getDescription()).contains("7 incidents were marked resolved by the same officer within 60 minutes. Review recommended.");
    }

    @Test
    @DisplayName("Demo Scenario C: Coordinated / Repeated Reporting Pattern Detection")
    void demoScenarioC_RepeatedReportingPattern() {
        // 3 citizens report drainage issues within 150m in 35 minutes
        IncidentResponse inc1 = incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Severe Drain Overflow", "Water clogging market road", IncidentCategory.DRAINAGE, IncidentSeverity.HIGH, new BigDecimal("11.933000"), new BigDecimal("79.830000"), "Market Road"
        ));
        IncidentResponse inc2 = incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Drain Clogged Near Shop 12", "Drainage backing up", IncidentCategory.DRAINAGE, IncidentSeverity.MEDIUM, new BigDecimal("11.933800"), new BigDecimal("79.830500"), "Market Road West"
        ));
        IncidentResponse inc3 = incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Waterlogging at Junction", "Drainage overflow at junction", IncidentCategory.DRAINAGE, IncidentSeverity.MEDIUM, new BigDecimal("11.934100"), new BigDecimal("79.831000"), "Market Junction"
        ));

        // Execute Detection
        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc3.getId());

        System.out.println("\n--- DEMO SCENARIO C RESULT ---");
        System.out.println("Incident ID: " + response.getIncidentId());
        response.getAnomalies().forEach(a -> {
            System.out.println("Anomaly Type: " + a.getAnomalyType());
            System.out.println("Severity: " + a.getSeverity());
            System.out.println("Description: " + a.getDescription());
            System.out.println("Related Incidents: " + a.getRelatedIncidentIds());
        });

        assertThat(response.getAnomalies()).hasSize(1);
        assertThat(response.getAnomalies().get(0).getAnomalyType()).isEqualTo(AnomalyType.REPEATED_REPORTING);
        assertThat(response.getAnomalies().get(0).getSeverity()).isEqualTo(AnomalySeverity.LOW);
        assertThat(response.getAnomalies().get(0).getDescription()).contains("3 similar drainage reports were submitted within 500 metres and 24 hours. Review recommended.");
    }
}
