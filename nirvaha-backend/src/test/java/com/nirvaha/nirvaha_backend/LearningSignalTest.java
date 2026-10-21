package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.LearningSignalDTO;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.EvidenceRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.ResolutionRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.LearningSignalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class LearningSignalTest {

    @Autowired
    private LearningSignalService learningService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private ResolutionRepository resolutionRepository;

    private User citizen;
    private User officer;

    @BeforeEach
    public void setup() {
        citizen = new User();
        citizen.setEmail("citizen.learn@nirvaha.gov.in");
        citizen.setName("Citizen Learn");
        citizen.setPasswordHash("hash");
        citizen.setRole(UserRole.CITIZEN);
        userRepository.save(citizen);

        officer = new User();
        officer.setEmail("officer.learn@nirvaha.gov.in");
        officer.setName("Officer Learn");
        officer.setPasswordHash("hash");
        officer.setRole(UserRole.OFFICER);
        userRepository.save(officer);
    }

    @Test
    public void testReopenedResolutionGeneratesLearningSignal() {
        Incident inc = new Incident();
        inc.setTitle("Drainage Overflow");
        inc.setDescription("Water buildup");
        inc.setCategory(IncidentCategory.DRAINAGE);
        inc.setSeverity(IncidentSeverity.HIGH);
        inc.setStatus(IncidentStatus.RESOLVED);
        inc.setClassificationTag(DataClassification.CITIZEN);
        inc.setLatitude(new BigDecimal("11.9416"));
        inc.setLongitude(new BigDecimal("79.8083"));
        inc.setReporter(citizen);
        incidentRepository.save(inc);

        Evidence ev = new Evidence();
        ev.setIncident(inc);
        ev.setFileName("res.jpg");
        ev.setFileUrl("http://localhost/res.jpg");
        ev.setMimeType("image/jpeg");
        ev.setFileSize(200L);
        ev.setEvidenceType(EvidenceType.PHOTO);
        evidenceRepository.save(ev);

        Resolution res = new Resolution();
        res.setIncident(inc);
        res.setOfficer(officer);
        res.setResolutionEvidence(ev);
        res.setResolutionSimilarityScore(0.20);
        res.setVerificationStatus(VerificationStatus.FAILED);
        res.setRemarks("Failed visual similarity check");
        resolutionRepository.save(res);

        List<LearningSignalDTO> signals = learningService.evaluateLearningSignals();

        assertNotNull(signals);
        assertFalse(signals.isEmpty());
        assertTrue(signals.stream().anyMatch(s -> "ELEVATED_REOPEN_FREQUENCY".equals(s.getSignalType())));
        assertEquals("nirvaha-learning-v1", signals.get(0).getModelVersion());
    }

    @Test
    public void testSimulatedIncidentsExcludedFromLearningSignals() {
        Incident inc = new Incident();
        inc.setTitle("Simulated Drainage Test");
        inc.setDescription("Test fixture");
        inc.setCategory(IncidentCategory.DRAINAGE);
        inc.setSeverity(IncidentSeverity.LOW);
        inc.setStatus(IncidentStatus.RESOLVED);
        inc.setClassificationTag(DataClassification.SIMULATED);
        inc.setLatitude(new BigDecimal("11.9416"));
        inc.setLongitude(new BigDecimal("79.8083"));
        inc.setReporter(citizen);
        incidentRepository.save(inc);

        Evidence ev = new Evidence();
        ev.setIncident(inc);
        ev.setFileName("sim.jpg");
        ev.setFileUrl("http://localhost/sim.jpg");
        ev.setMimeType("image/jpeg");
        ev.setFileSize(50L);
        ev.setEvidenceType(EvidenceType.PHOTO);
        evidenceRepository.save(ev);

        Resolution res = new Resolution();
        res.setIncident(inc);
        res.setOfficer(officer);
        res.setResolutionEvidence(ev);
        res.setResolutionSimilarityScore(0.10);
        res.setVerificationStatus(VerificationStatus.FAILED);
        res.setRemarks("Simulated test rejection");
        resolutionRepository.save(res);

        List<LearningSignalDTO> signals = learningService.evaluateLearningSignals();
        assertTrue(signals.stream().noneMatch(s -> s.getObservation().contains("Simulated")));
    }
}
