package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.OfficerDecisionRequest;
import com.nirvaha.nirvaha_backend.dto.OfficerDecisionResponse;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.OfficerDecisionService;
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
public class OfficerDecisionTest {

    @Autowired
    private OfficerDecisionService decisionService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    private Incident testIncident;
    private User officer;

    @BeforeEach
    public void setup() {
        officer = new User();
        officer.setEmail("officer.decision@nirvaha.gov.in");
        officer.setName("Officer Decision");
        officer.setPasswordHash("hash");
        officer.setRole(UserRole.OFFICER);
        userRepository.save(officer);

        testIncident = new Incident();
        testIncident.setTitle("Streetlight Failure");
        testIncident.setDescription("Main road dark");
        testIncident.setCategory(IncidentCategory.STREETLIGHT);
        testIncident.setSeverity(IncidentSeverity.MEDIUM);
        testIncident.setStatus(IncidentStatus.REPORTED);
        testIncident.setClassificationTag(DataClassification.CITIZEN);
        testIncident.setLatitude(new BigDecimal("11.9416"));
        testIncident.setLongitude(new BigDecimal("79.8083"));
        testIncident.setReporter(officer);
        incidentRepository.save(testIncident);
    }

    @Test
    public void testOfficerAcceptsRecommendation() {
        OfficerDecisionRequest req = new OfficerDecisionRequest(
                testIncident.getId(),
                "rec-123",
                RecommendedActionType.FIELD_INSPECTION,
                OfficerDecisionType.ACCEPT,
                "Accepted system recommendation for inspection."
        );

        OfficerDecisionResponse response = decisionService.recordDecision(req, officer);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(testIncident.getId(), response.getIncidentId());
        assertEquals(OfficerDecisionType.ACCEPT, response.getDecision());
        assertEquals(officer.getId(), response.getDecidedById());

        List<OfficerDecisionResponse> list = decisionService.getDecisionsForIncident(testIncident.getId());
        assertEquals(1, list.size());
    }

    @Test
    public void testOfficerModifiesWithoutReasonThrowsException() {
        OfficerDecisionRequest req = new OfficerDecisionRequest(
                testIncident.getId(),
                "rec-123",
                RecommendedActionType.FIELD_INSPECTION,
                OfficerDecisionType.MODIFY,
                "" // Empty reason
        );

        assertThrows(IllegalArgumentException.class, () -> {
            decisionService.recordDecision(req, officer);
        });
    }

    @Test
    public void testOfficerModifiesWithValidReason() {
        OfficerDecisionRequest req = new OfficerDecisionRequest(
                testIncident.getId(),
                "rec-123",
                RecommendedActionType.FIELD_INSPECTION,
                OfficerDecisionType.MODIFY,
                "Already inspected at 09:00 AM by morning patrol."
        );

        OfficerDecisionResponse response = decisionService.recordDecision(req, officer);

        assertNotNull(response);
        assertEquals(OfficerDecisionType.MODIFY, response.getDecision());
        assertEquals("Already inspected at 09:00 AM by morning patrol.", response.getReason());
    }
}
