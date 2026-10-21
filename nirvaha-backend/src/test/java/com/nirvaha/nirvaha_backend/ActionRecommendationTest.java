package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.ActionRecommendationDTO;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.service.ActionRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ActionRecommendationTest {

    @Autowired
    private ActionRecommendationService recommendationService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResolutionRepository resolutionRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    private User citizen;
    private User officer;

    @BeforeEach
    public void setup() {
        citizen = new User();
        citizen.setEmail("citizen.rec@nirvaha.gov.in");
        citizen.setName("Citizen Rec");
        citizen.setPasswordHash("hash");
        citizen.setRole(UserRole.CITIZEN);
        userRepository.save(citizen);

        officer = new User();
        officer.setEmail("officer.rec@nirvaha.gov.in");
        officer.setName("Officer Rec");
        officer.setPasswordHash("hash");
        officer.setRole(UserRole.OFFICER);
        userRepository.save(officer);
    }

    @Test
    public void testCriticalIncidentGeneratesFieldInspection() {
        Incident inc = new Incident();
        inc.setTitle("Critical Boulevard Drainage Block");
        inc.setDescription("Severe flooding on main corridor");
        inc.setCategory(IncidentCategory.DRAINAGE);
        inc.setSeverity(IncidentSeverity.HIGH);
        inc.setStatus(IncidentStatus.REPORTED);
        inc.setClassificationTag(DataClassification.CITIZEN);
        inc.setLatitude(new BigDecimal("11.9416"));
        inc.setLongitude(new BigDecimal("79.8083"));
        inc.setReporter(citizen);
        incidentRepository.save(inc);

        ActionRecommendationDTO rec = recommendationService.generateRecommendation(inc, PriorityLevel.CRITICAL, RiskLevel.HIGH);

        assertNotNull(rec);
        assertEquals(RecommendedActionType.FIELD_INSPECTION, rec.getRecommendedAction());
        assertEquals("PUBLIC WORKS DEPARTMENT (PWD)", rec.getSuggestedDepartment());
        assertTrue(rec.getReason().contains("High civic risk concentration"));
        assertNotNull(rec.getSupportingSignals());
        assertFalse(rec.getSupportingSignals().isEmpty());
        assertEquals("nirvaha-action-v1", rec.getModelVersion());
    }

    @Test
    public void testFailedResolutionVerificationGeneratesReopenCase() {
        Incident inc = new Incident();
        inc.setTitle("Pothole Maintenance");
        inc.setDescription("Road repair submitted");
        inc.setCategory(IncidentCategory.POTHOLE);
        inc.setSeverity(IncidentSeverity.MEDIUM);
        inc.setStatus(IncidentStatus.RESOLVED);
        inc.setClassificationTag(DataClassification.CITIZEN);
        inc.setLatitude(new BigDecimal("11.9416"));
        inc.setLongitude(new BigDecimal("79.8083"));
        inc.setReporter(citizen);
        incidentRepository.save(inc);

        Evidence ev = new Evidence();
        ev.setIncident(inc);
        ev.setFileName("res_ev.jpg");
        ev.setFileUrl("http://localhost/res.jpg");
        ev.setMimeType("image/jpeg");
        ev.setFileSize(100L);
        ev.setEvidenceType(EvidenceType.PHOTO);
        evidenceRepository.save(ev);

        Resolution res = new Resolution();
        res.setIncident(inc);
        res.setOfficer(officer);
        res.setResolutionEvidence(ev);
        res.setResolutionSimilarityScore(0.35);
        res.setVerificationStatus(VerificationStatus.FAILED);
        res.setRemarks("Evidence does not match location");
        resolutionRepository.save(res);

        ActionRecommendationDTO rec = recommendationService.generateRecommendation(inc, PriorityLevel.MEDIUM, RiskLevel.MODERATE);

        assertNotNull(rec);
        assertEquals(RecommendedActionType.REOPEN_CASE, rec.getRecommendedAction());
        assertTrue(rec.getReason().contains("failed verification"));
    }

    @Test
    public void testSimulatedIncidentExcludedFromRecommendations() {
        Incident inc = new Incident();
        inc.setTitle("Simulated Drainage Test");
        inc.setDescription("Test fixture");
        inc.setCategory(IncidentCategory.DRAINAGE);
        inc.setSeverity(IncidentSeverity.LOW);
        inc.setStatus(IncidentStatus.REPORTED);
        inc.setClassificationTag(DataClassification.SIMULATED);
        inc.setLatitude(new BigDecimal("11.9416"));
        inc.setLongitude(new BigDecimal("79.8083"));
        inc.setReporter(citizen);
        incidentRepository.save(inc);

        ActionRecommendationDTO rec = recommendationService.generateRecommendation(inc, PriorityLevel.HIGH, RiskLevel.HIGH);
        assertNull(rec);
    }
}
