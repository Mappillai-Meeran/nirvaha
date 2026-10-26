package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class IntegrityAnomalyServiceTest {

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

    private User citizenUser;
    private Long citizenId;
    private Long officer1Id;
    private Long officer2Id;
    private User officer1User;

    @BeforeEach
    void setUp() {
        anomalyFlagRepository.deleteAll();
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        UserResponse citizen = userService.createUser(new CreateUserRequest("Citizen Test", "citizen@nirvaha.gov", "password", UserRole.CITIZEN));
        citizenId = citizen.getId();
        citizenUser = userRepository.findById(citizenId).orElseThrow();

        UserResponse officer1 = userService.createUser(new CreateUserRequest("Officer One", "officer1@nirvaha.gov", "password", UserRole.OFFICER));
        officer1Id = officer1.getId();
        officer1User = userRepository.findById(officer1Id).orElseThrow();

        UserResponse officer2 = userService.createUser(new CreateUserRequest("Officer Two", "officer2@nirvaha.gov", "password", UserRole.OFFICER));
        officer2Id = officer2.getId();
    }

    // --- Rule 1: Resolution Evidence Reuse Tests ---

    @Test
    void testResolutionEvidenceReuse_SameFileUrlAcrossIncidents_TriggersAnomaly() {
        IncidentResponse inc1 = createIncident("Pothole Main St", IncidentCategory.POTHOLE, new BigDecimal("11.933055"), new BigDecimal("79.830000"));
        EvidenceResponse origEv1 = createEvidence(inc1.getId(), "http://storage.gov/orig1.jpg");
        EvidenceResponse resEv1 = createEvidence(inc1.getId(), "http://storage.gov/reused_repair.jpg");
        incidentWorkflowService.addAction(inc1.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.VERIFIED, "Verified"));
        resolutionService.createResolution(inc1.getId(), new CreateResolutionRequest(officer1Id, resEv1.getId(), "Resolved issue"));

        IncidentResponse inc2 = createIncident("Pothole Beach Rd", IncidentCategory.POTHOLE, new BigDecimal("12.950000"), new BigDecimal("80.850000"));
        EvidenceResponse origEv2 = createEvidence(inc2.getId(), "http://storage.gov/orig2.jpg");
        EvidenceResponse resEv2 = createEvidence(inc2.getId(), "http://storage.gov/reused_repair.jpg"); // Same URL!
        incidentWorkflowService.addAction(inc2.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.VERIFIED, "Verified"));
        resolutionService.createResolution(inc2.getId(), new CreateResolutionRequest(officer1Id, resEv2.getId(), "Resolved issue"));

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc2.getId());

        assertThat(response.getAnomalies()).hasSize(1);
        AnomalyFlagResponse flag = response.getAnomalies().get(0);
        assertThat(flag.getAnomalyType()).isEqualTo(AnomalyType.RESOLUTION_EVIDENCE_REUSE);
        assertThat(flag.getSeverity()).isEqualTo(AnomalySeverity.HIGH);
        assertThat(flag.getDescription()).contains("The same resolution evidence reference is associated with multiple incidents. Review recommended.");
        assertThat(flag.getRelatedIncidentIds()).contains(inc1.getId().toString());
    }

    @Test
    void testResolutionEvidenceReuse_DifferentEvidence_NoAnomaly() {
        IncidentResponse inc1 = createIncident("Drainage Block A", IncidentCategory.DRAINAGE, new BigDecimal("11.933055"), new BigDecimal("79.830000"));
        EvidenceResponse resEv1 = createEvidence(inc1.getId(), "http://storage.gov/repair1.jpg");
        incidentWorkflowService.addAction(inc1.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.VERIFIED, "Verified"));
        resolutionService.createResolution(inc1.getId(), new CreateResolutionRequest(officer1Id, resEv1.getId(), "Resolved"));

        IncidentResponse inc2 = createIncident("Drainage Block B", IncidentCategory.DRAINAGE, new BigDecimal("12.950000"), new BigDecimal("80.850000"));
        EvidenceResponse resEv2 = createEvidence(inc2.getId(), "http://storage.gov/repair2.jpg"); // Different URL
        incidentWorkflowService.addAction(inc2.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.VERIFIED, "Verified"));
        resolutionService.createResolution(inc2.getId(), new CreateResolutionRequest(officer1Id, resEv2.getId(), "Resolved"));

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc2.getId());

        assertThat(response.getAnomalies()).isEmpty();
    }

    // --- Rule 2: Bulk Closure Tests ---

    private void attachResolutionEvidence(Long incId, Long officerId, String remarks) {
        Incident incident = incidentRepository.findById(incId).orElseThrow();
        User officer = userRepository.findById(officerId).orElseThrow();

        Evidence ev = new Evidence();
        ev.setIncident(incident);
        ev.setEvidenceType(EvidenceType.PHOTO);
        ev.setFileUrl("https://storage.nirvaha.gov.in/test/bulk-res-" + incId + ".jpg");
        ev.setFileName("bulk-res-" + incId + ".jpg");
        ev.setMimeType("image/jpeg");
        ev.setFileSize(204800L);
        ev.setLatitude(incident.getLatitude());
        ev.setLongitude(incident.getLongitude());
        ev.setCreatedAt(LocalDateTime.now());
        Evidence savedEv = evidenceRepository.save(ev);

        Resolution res = new Resolution();
        res.setIncident(incident);
        res.setOfficer(officer);
        res.setResolutionEvidence(savedEv);
        res.setResolutionSimilarityScore(0.95);
        res.setVerificationStatus(VerificationStatus.VERIFIED);
        res.setRemarks(remarks != null ? remarks : "Resolution completed with verified evidence");
        res.setCreatedAt(LocalDateTime.now());
        res.setUpdatedAt(LocalDateTime.now());
        resolutionRepository.save(res);
    }

    @Test
    void testBulkClosure_5ResolutionsWithin60Minutes_TriggersAnomaly() {
        IncidentCategory[] categories = {IncidentCategory.POTHOLE, IncidentCategory.DRAINAGE, IncidentCategory.GARBAGE, IncidentCategory.STREETLIGHT, IncidentCategory.WATER_SUPPLY};
        IncidentResponse targetInc = null;
        for (int i = 0; i < 5; i++) {
            BigDecimal lat = new BigDecimal(10 + i + ".000000");
            BigDecimal lng = new BigDecimal(70 + i + ".000000");
            IncidentResponse inc = createIncident("Issue " + i, categories[i], lat, lng);
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.VERIFIED, "Verified"));
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.STARTED, "Started"));
            attachResolutionEvidence(inc.getId(), officer1Id, "Resolved ticket " + i);
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.RESOLVED, "Resolved ticket " + i));
            if (i == 4) {
                targetInc = inc;
            }
        }

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(targetInc.getId());

        assertThat(response.getAnomalies()).hasSize(1);
        AnomalyFlagResponse flag = response.getAnomalies().get(0);
        assertThat(flag.getAnomalyType()).isEqualTo(AnomalyType.BULK_CLOSURE);
        assertThat(flag.getSeverity()).isEqualTo(AnomalySeverity.MEDIUM);
        assertThat(flag.getDescription()).contains("5 incidents were marked resolved by the same officer within 60 minutes. Review recommended.");
    }

    @Test
    void testBulkClosure_6ResolutionsWithin60Minutes_TriggersAnomaly() {
        IncidentCategory[] categories = {IncidentCategory.POTHOLE, IncidentCategory.DRAINAGE, IncidentCategory.GARBAGE, IncidentCategory.STREETLIGHT, IncidentCategory.WATER_SUPPLY, IncidentCategory.OTHER};
        IncidentResponse targetInc = null;
        for (int i = 0; i < 6; i++) {
            BigDecimal lat = new BigDecimal(10 + i + ".000000");
            BigDecimal lng = new BigDecimal(70 + i + ".000000");
            IncidentResponse inc = createIncident("Issue Bulk " + i, categories[i], lat, lng);
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.VERIFIED, "Verified"));
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.STARTED, "Started"));
            attachResolutionEvidence(inc.getId(), officer1Id, "Resolved ticket " + i);
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.RESOLVED, "Resolved ticket " + i));
            if (i == 5) {
                targetInc = inc;
            }
        }

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(targetInc.getId());

        assertThat(response.getAnomalies()).hasSize(1);
        AnomalyFlagResponse flag = response.getAnomalies().get(0);
        assertThat(flag.getAnomalyType()).isEqualTo(AnomalyType.BULK_CLOSURE);
        assertThat(flag.getDescription()).contains("6 incidents were marked resolved by the same officer within 60 minutes. Review recommended.");
    }

    @Test
    void testBulkClosure_4Resolutions_NoAnomaly() {
        IncidentCategory[] categories = {IncidentCategory.POTHOLE, IncidentCategory.DRAINAGE, IncidentCategory.GARBAGE, IncidentCategory.STREETLIGHT};
        IncidentResponse targetInc = null;
        for (int i = 0; i < 4; i++) {
            BigDecimal lat = new BigDecimal(10 + i + ".000000");
            BigDecimal lng = new BigDecimal(70 + i + ".000000");
            IncidentResponse inc = createIncident("Issue " + i, categories[i], lat, lng);
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.VERIFIED, "Verified"));
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.STARTED, "Started"));
            attachResolutionEvidence(inc.getId(), officer1Id, "Resolved ticket " + i);
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.RESOLVED, "Resolved ticket " + i));
            if (i == 3) {
                targetInc = inc;
            }
        }

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(targetInc.getId());

        assertThat(response.getAnomalies()).isEmpty();
    }

    @Test
    void testBulkClosure_5ResolutionsSpreadBeyond60Minutes_NoAnomaly() {
        IncidentCategory[] categories = {IncidentCategory.POTHOLE, IncidentCategory.DRAINAGE, IncidentCategory.GARBAGE, IncidentCategory.STREETLIGHT, IncidentCategory.WATER_SUPPLY};
        IncidentResponse targetInc = null;
        for (int i = 0; i < 5; i++) {
            BigDecimal lat = new BigDecimal(10 + i + ".000000");
            BigDecimal lng = new BigDecimal(70 + i + ".000000");
            IncidentResponse inc = createIncident("Issue " + i, categories[i], lat, lng);
            
            Incident incEntity = incidentRepository.findById(inc.getId()).orElseThrow();
            
            // Create RESOLVED action directly with pre-set createdAt timestamp before persistence
            IncidentAction action = new IncidentAction();
            action.setIncident(incEntity);
            action.setOfficer(officer1User);
            action.setActionType(ActionType.RESOLVED);
            action.setRemarks("Resolved ticket " + i);
            action.setCreatedAt(LocalDateTime.now().minusHours((i + 1) * 3));
            incidentActionRepository.save(action);

            incEntity.setStatus(IncidentStatus.RESOLVED);
            incidentRepository.save(incEntity);

            if (i == 0) {
                targetInc = inc;
            }
        }

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(targetInc.getId());

        assertThat(response.getAnomalies()).isEmpty();
    }

    @Test
    void testBulkClosure_DifferentOfficers_NoFalseGrouping() {
        IncidentCategory[] categories = {IncidentCategory.POTHOLE, IncidentCategory.DRAINAGE, IncidentCategory.GARBAGE, IncidentCategory.STREETLIGHT, IncidentCategory.WATER_SUPPLY};
        for (int i = 0; i < 3; i++) {
            BigDecimal lat = new BigDecimal(10 + i + ".000000");
            BigDecimal lng = new BigDecimal(70 + i + ".000000");
            IncidentResponse inc = createIncident("Off1 Issue " + i, categories[i], lat, lng);
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.VERIFIED, "Verified"));
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.STARTED, "Started"));
            attachResolutionEvidence(inc.getId(), officer1Id, "Resolved");
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer1Id, ActionType.RESOLVED, "Resolved"));
        }
        IncidentResponse targetInc = null;
        for (int i = 3; i < 5; i++) {
            BigDecimal lat = new BigDecimal(10 + i + ".000000");
            BigDecimal lng = new BigDecimal(70 + i + ".000000");
            IncidentResponse inc = createIncident("Off2 Issue " + i, categories[i], lat, lng);
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer2Id, ActionType.VERIFIED, "Verified"));
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer2Id, ActionType.STARTED, "Started"));
            attachResolutionEvidence(inc.getId(), officer2Id, "Resolved");
            incidentWorkflowService.addAction(inc.getId(), new CreateIncidentActionRequest(officer2Id, ActionType.RESOLVED, "Resolved"));
            targetInc = inc;
        }

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(targetInc.getId());

        assertThat(response.getAnomalies()).isEmpty();
    }

    // --- Rule 3: Repeated Reporting Tests ---

    @Test
    void testRepeatedReporting_3SameCategoryWithin500mAnd24h_TriggersAnomaly() {
        IncidentResponse inc1 = createIncident("Drainage leak 1", IncidentCategory.DRAINAGE, new BigDecimal("11.933000"), new BigDecimal("79.830000"));
        IncidentResponse inc2 = createIncident("Drainage leak 2", IncidentCategory.DRAINAGE, new BigDecimal("11.933100"), new BigDecimal("79.830100")); // ~15m apart
        IncidentResponse inc3 = createIncident("Drainage leak 3", IncidentCategory.DRAINAGE, new BigDecimal("11.933200"), new BigDecimal("79.830200")); // ~30m apart

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc3.getId());

        assertThat(response.getAnomalies()).hasSize(1);
        AnomalyFlagResponse flag = response.getAnomalies().get(0);
        assertThat(flag.getAnomalyType()).isEqualTo(AnomalyType.REPEATED_REPORTING);
        assertThat(flag.getSeverity()).isEqualTo(AnomalySeverity.LOW);
        assertThat(flag.getDescription()).contains("3 similar drainage reports were submitted within 500 metres and 24 hours. Review recommended.");
    }

    @Test
    void testRepeatedReporting_2Incidents_NoAnomaly() {
        IncidentResponse inc1 = createIncident("Drainage leak 1", IncidentCategory.DRAINAGE, new BigDecimal("11.933000"), new BigDecimal("79.830000"));
        IncidentResponse inc2 = createIncident("Drainage leak 2", IncidentCategory.DRAINAGE, new BigDecimal("11.933100"), new BigDecimal("79.830100"));

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc2.getId());

        assertThat(response.getAnomalies()).isEmpty();
    }

    @Test
    void testRepeatedReporting_DifferentCategories_NoAnomaly() {
        createIncident("Drainage issue", IncidentCategory.DRAINAGE, new BigDecimal("11.933000"), new BigDecimal("79.830000"));
        createIncident("Road issue", IncidentCategory.POTHOLE, new BigDecimal("11.933100"), new BigDecimal("79.830100"));
        IncidentResponse inc3 = createIncident("Water issue", IncidentCategory.WATER_SUPPLY, new BigDecimal("11.933200"), new BigDecimal("79.830200"));

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc3.getId());

        assertThat(response.getAnomalies()).isEmpty();
    }

    @Test
    void testRepeatedReporting_MoreThan500mApart_NoAnomaly() {
        createIncident("Garbage 1", IncidentCategory.GARBAGE, new BigDecimal("11.933000"), new BigDecimal("79.830000"));
        createIncident("Garbage 2", IncidentCategory.GARBAGE, new BigDecimal("11.933100"), new BigDecimal("79.830000"));
        // Incident 3 is ~10km away
        IncidentResponse inc3 = createIncident("Garbage 3", IncidentCategory.GARBAGE, new BigDecimal("12.033000"), new BigDecimal("79.930000"));

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc3.getId());

        assertThat(response.getAnomalies()).isEmpty();
    }

    @Test
    void testRepeatedReporting_MoreThan24HoursApart_NoAnomaly() {
        // Create inc1 and inc2 directly with pre-set createdAt before persistence so @PrePersist uses the past timestamp
        Incident inc1Entity = new Incident(null, citizenUser, "Streetlight 1", "Desc", IncidentCategory.STREETLIGHT,
                IncidentSeverity.MEDIUM, IncidentStatus.REPORTED, new BigDecimal("11.933000"), new BigDecimal("79.830000"),
                "Address", LocalDateTime.now().minusHours(30), LocalDateTime.now().minusHours(30));
        incidentRepository.save(inc1Entity);

        Incident inc2Entity = new Incident(null, citizenUser, "Streetlight 2", "Desc", IncidentCategory.STREETLIGHT,
                IncidentSeverity.MEDIUM, IncidentStatus.REPORTED, new BigDecimal("11.933100"), new BigDecimal("79.830100"),
                "Address", LocalDateTime.now().minusHours(30), LocalDateTime.now().minusHours(30));
        incidentRepository.save(inc2Entity);

        IncidentResponse inc3 = createIncident("Streetlight 3", IncidentCategory.STREETLIGHT, new BigDecimal("11.933200"), new BigDecimal("79.830200"));

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc3.getId());

        assertThat(response.getAnomalies()).isEmpty();
    }

    // --- Validation & Deduplication Tests ---

    @Test
    void testValidation_MissingIncident_Throws404() {
        assertThatThrownBy(() -> integrityAnomalyService.detectAnomalies(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident with ID 999 not found");

        assertThatThrownBy(() -> integrityAnomalyService.getAnomaliesForIncident(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident with ID 999 not found");
    }

    @Test
    void testValidation_NoAnomalies_ReturnsEmptyResult() {
        IncidentResponse inc = createIncident("Normal Incident", IncidentCategory.OTHER, new BigDecimal("11.933000"), new BigDecimal("79.830000"));

        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc.getId());

        assertThat(response.getIncidentId()).isEqualTo(inc.getId());
        assertThat(response.getAnomalies()).isEmpty();
    }

    @Test
    void testDeduplication_RepeatedDetectionCall_DoesNotCreateDuplicateFlags() {
        // Trigger repeated reporting anomaly
        createIncident("Drainage leak 1", IncidentCategory.DRAINAGE, new BigDecimal("11.933000"), new BigDecimal("79.830000"));
        createIncident("Drainage leak 2", IncidentCategory.DRAINAGE, new BigDecimal("11.933100"), new BigDecimal("79.830100"));
        IncidentResponse inc3 = createIncident("Drainage leak 3", IncidentCategory.DRAINAGE, new BigDecimal("11.933200"), new BigDecimal("79.830200"));

        // Call detect multiple times
        integrityAnomalyService.detectAnomalies(inc3.getId());
        integrityAnomalyService.detectAnomalies(inc3.getId());
        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(inc3.getId());

        assertThat(response.getAnomalies()).hasSize(1);
        assertThat(anomalyFlagRepository.count()).isEqualTo(1);
    }

    private IncidentResponse createIncident(String title, IncidentCategory category, BigDecimal lat, BigDecimal lng) {
        return incidentService.createIncident(new CreateIncidentRequest(
                citizenId, title, "Test Description for " + title, category, IncidentSeverity.MEDIUM, lat, lng, "Test Address"
        ));
    }

    private EvidenceResponse createEvidence(Long incidentId, String fileUrl) {
        return evidenceService.createEvidence(new CreateEvidenceRequest(
                incidentId, EvidenceType.PHOTO, fileUrl, "photo.jpg", "image/jpeg", 1024L, LocalDateTime.now(), new BigDecimal("11.933000"), new BigDecimal("79.830000"), "Evidence desc"
        ));
    }
}
