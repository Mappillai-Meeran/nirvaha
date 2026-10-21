package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.IncidentResponse;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import com.nirvaha.nirvaha_backend.service.IntelligenceEligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IncidentProvenanceIntegrationTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IntelligenceEligibilityService eligibilityService;

    private User testReporter;

    @BeforeEach
    void setUp() {
        testReporter = userRepository.findByEmail("citizen.test@nirvaha.gov.in")
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail("citizen.test@nirvaha.gov.in");
                    u.setName("Citizen Test User");
                    u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz123456");
                    u.setRole(UserRole.CITIZEN);
                    return userRepository.save(u);
                });
    }

    @Test
    void shouldDefaultIntakeSourceToResidentSubmitted() {
        Incident incident = new Incident();
        incident.setTitle("Drainage issue on MG Road");
        incident.setDescription("Severe drain overflow near clock tower");
        incident.setCategory(IncidentCategory.DRAINAGE);
        incident.setSeverity(IncidentSeverity.HIGH);
        incident.setReporter(testReporter);
        incident.setLatitude(new BigDecimal("11.9340"));
        incident.setLongitude(new BigDecimal("79.8300"));
        incident.setAddress("MG Road, Puducherry");

        Incident saved = incidentRepository.save(incident);

        assertNotNull(saved.getId());
        assertEquals(IncidentSource.RESIDENT_SUBMITTED, saved.getIntakeSource());
        assertNotNull(saved.getSourceTimestamp());
        assertEquals("SUCCESS", saved.getIngestionOutcome());
    }

    @Test
    void shouldExcludeSimulatedIncidentsFromOfficialQuery() {
        // Genuine citizen incident
        Incident official = new Incident();
        official.setTitle("Pothole on Mission Street");
        official.setDescription("Deep pothole damaging scooters");
        official.setCategory(IncidentCategory.POTHOLE);
        official.setSeverity(IncidentSeverity.MEDIUM);
        official.setReporter(testReporter);
        official.setLatitude(new BigDecimal("11.9320"));
        official.setLongitude(new BigDecimal("79.8280"));
        official.setAddress("Mission Street, Puducherry");
        official.setIntakeSource(IncidentSource.RESIDENT_SUBMITTED);
        official.setClassificationTag(DataClassification.CITIZEN);
        incidentRepository.save(official);

        // Simulated incident
        Incident simulated = new Incident();
        simulated.setTitle("SIMULATED Pothole for stress test");
        simulated.setDescription("Generated mock record for load testing");
        simulated.setCategory(IncidentCategory.POTHOLE);
        simulated.setSeverity(IncidentSeverity.LOW);
        simulated.setReporter(testReporter);
        simulated.setLatitude(new BigDecimal("11.9325"));
        simulated.setLongitude(new BigDecimal("79.8285"));
        simulated.setAddress("Mission Street Simulated, Puducherry");
        simulated.setIntakeSource(IncidentSource.SIMULATED);
        simulated.setClassificationTag(DataClassification.SIMULATED);
        incidentRepository.save(simulated);

        List<Incident> officialOnly = incidentRepository.findOfficialIncidents();
        List<IncidentResponse> serviceResponses = incidentService.getAllIncidents(false);

        // Official query must contain the real incident and exclude the simulated incident
        assertTrue(officialOnly.stream().anyMatch(i -> i.getTitle().equals("Pothole on Mission Street")));
        assertFalse(officialOnly.stream().anyMatch(i -> i.getTitle().contains("SIMULATED")));

        assertTrue(serviceResponses.stream().anyMatch(r -> r.getTitle().equals("Pothole on Mission Street")));
        assertFalse(serviceResponses.stream().anyMatch(r -> r.getTitle().contains("SIMULATED")));

        // Including simulated must return both
        List<IncidentResponse> allResponses = incidentService.getAllIncidents(true);
        assertTrue(allResponses.stream().anyMatch(r -> r.getTitle().contains("SIMULATED")));
    }

    @Test
    void shouldDeemSimulatedIncidentIneligibleForCivicIntelligence() {
        Incident simulated = new Incident();
        simulated.setTitle("Simulated Drainage Block");
        simulated.setCategory(IncidentCategory.DRAINAGE);
        simulated.setStatus(IncidentStatus.REPORTED);
        simulated.setCreatedAt(LocalDateTime.now());
        simulated.setIntakeSource(IncidentSource.SIMULATED);

        boolean eligible = eligibilityService.isEligibleForIntelligence(simulated);
        assertFalse(eligible, "Incidents with intakeSource SIMULATED must never contaminate civic intelligence");

        Incident genuine = new Incident();
        genuine.setTitle("Genuine Drainage Block");
        genuine.setCategory(IncidentCategory.DRAINAGE);
        genuine.setStatus(IncidentStatus.REPORTED);
        genuine.setCreatedAt(LocalDateTime.now());
        genuine.setIntakeSource(IncidentSource.RESIDENT_SUBMITTED);
        genuine.setClassificationTag(DataClassification.CITIZEN);

        assertTrue(eligibilityService.isEligibleForIntelligence(genuine));
    }

    @Test
    void shouldRejectUnverifiedGovernmentClaimAndDowngradeToResidentSubmitted() {
        Incident bogusGov = new Incident();
        bogusGov.setTitle("Bogus Government Claim Report");
        bogusGov.setDescription("Claiming official government intake without credentials");
        bogusGov.setCategory(IncidentCategory.POTHOLE);
        bogusGov.setSeverity(IncidentSeverity.MEDIUM);
        bogusGov.setReporter(testReporter);
        bogusGov.setLatitude(new BigDecimal("11.9320"));
        bogusGov.setLongitude(new BigDecimal("79.8280"));
        bogusGov.setAddress("Mission Street, Puducherry");
        bogusGov.setIntakeSource(IncidentSource.AUTHORIZED_GOVERNMENT_SOURCE);
        bogusGov.setClassificationTag(DataClassification.OFFICIAL);
        // Neither externalSourceIdentifier nor externalRecordId provided

        Incident saved = incidentRepository.save(bogusGov);

        assertEquals(IncidentSource.RESIDENT_SUBMITTED, saved.getIntakeSource(),
                "Government provenance claim without authorized integration must be downgraded to resident submitted");
        assertEquals(DataClassification.CITIZEN, saved.getClassificationTag());
    }

    @Test
    void shouldAcceptAuthorizedGovernmentRecordWhenCredentialsProvided() {
        Incident validGov = new Incident();
        validGov.setTitle("Official NIC Redressal Ingestion");
        validGov.setDescription("Authorized ingestion from Puducherry PG Portal");
        validGov.setCategory(IncidentCategory.DRAINAGE);
        validGov.setSeverity(IncidentSeverity.HIGH);
        validGov.setReporter(testReporter);
        validGov.setLatitude(new BigDecimal("11.9350"));
        validGov.setLongitude(new BigDecimal("79.8310"));
        validGov.setAddress("Boulevard Sector 4, Puducherry");
        validGov.setIntakeSource(IncidentSource.AUTHORIZED_GOVERNMENT_SOURCE);
        validGov.setClassificationTag(DataClassification.OFFICIAL);
        validGov.setExternalSourceIdentifier("NIC_PGRS_PUDUCHERRY");
        validGov.setExternalRecordId("PY-PGRS-2025-004912");

        Incident saved = incidentRepository.save(validGov);

        assertEquals(IncidentSource.AUTHORIZED_GOVERNMENT_SOURCE, saved.getIntakeSource());
        assertEquals(DataClassification.OFFICIAL, saved.getClassificationTag());
        assertEquals("NIC_PGRS_PUDUCHERRY", saved.getExternalSourceIdentifier());
        assertEquals("PY-PGRS-2025-004912", saved.getExternalRecordId());
    }
}
