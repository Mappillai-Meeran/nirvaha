package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentServiceTrackingRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.scheduler.SlaEvaluationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SlaEvaluationSchedulerTest {

    @Autowired
    private SlaEvaluationScheduler scheduler;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentServiceTrackingRepository trackingRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByEmail("scheduler.test@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("scheduler.test@example.com");
            u.setName("Scheduler Test User");
            u.setRole(UserRole.CITIZEN);
            u.setPasswordHash("hash123");
            return userRepository.save(u);
        });
    }

    @Test
    void testScheduledEvaluationProcessesOpenIncidents() {
        Incident incident = new Incident();
        incident.setTitle("Scheduled Test Pothole");
        incident.setDescription("Pothole needing SLA evaluation");
        incident.setCategory(IncidentCategory.POTHOLE);
        incident.setSeverity(IncidentSeverity.MEDIUM);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setLatitude(BigDecimal.valueOf(11.9300));
        incident.setLongitude(BigDecimal.valueOf(79.8200));
        incident.setReporter(testUser);
        incident.setCreatedAt(LocalDateTime.now().minusDays(1));

        Incident saved = incidentRepository.save(incident);

        int count = scheduler.runSlaEvaluationCycle();
        assertThat(count).isGreaterThanOrEqualTo(1);

        Optional<IncidentServiceTracking> trackingOpt = trackingRepository.findByIncidentId(saved.getId());
        assertThat(trackingOpt).isPresent();
        IncidentServiceTracking tracking = trackingOpt.get();
        assertThat(tracking.getSlaStatus()).isEqualTo(SlaStatus.WITHIN_STANDARD);
        assertThat(tracking.getLastEvaluatedAt()).isNotNull();
    }

    @Test
    void testEvaluationCycleIsIdempotent() {
        Incident incident = new Incident();
        incident.setTitle("Idempotent Test Pothole");
        incident.setDescription("Testing idempotency of SLA evaluation");
        incident.setCategory(IncidentCategory.POTHOLE);
        incident.setSeverity(IncidentSeverity.LOW);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setLatitude(BigDecimal.valueOf(11.9350));
        incident.setLongitude(BigDecimal.valueOf(79.8250));
        incident.setReporter(testUser);
        incident.setCreatedAt(LocalDateTime.now().minusDays(1));

        Incident saved = incidentRepository.save(incident);

        // Run cycle once
        scheduler.runSlaEvaluationCycle();
        IncidentServiceTracking firstTracking = trackingRepository.findByIncidentId(saved.getId()).orElseThrow();

        // Run cycle again
        scheduler.runSlaEvaluationCycle();
        IncidentServiceTracking secondTracking = trackingRepository.findByIncidentId(saved.getId()).orElseThrow();

        // Should have identical status and ID (not recreated)
        assertThat(firstTracking.getId()).isEqualTo(secondTracking.getId());
        assertThat(firstTracking.getSlaStatus()).isEqualTo(secondTracking.getSlaStatus());
    }

    @Test
    void testResolvedIncidentsNotIncludedInOpenEvaluation() {
        Incident incident = new Incident();
        incident.setTitle("Resolved Test Incident");
        incident.setDescription("Resolved incident should not be evaluated by scheduler");
        incident.setCategory(IncidentCategory.POTHOLE);
        incident.setSeverity(IncidentSeverity.LOW);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setLatitude(BigDecimal.valueOf(11.9350));
        incident.setLongitude(BigDecimal.valueOf(79.8250));
        incident.setReporter(testUser);
        incident.setCreatedAt(LocalDateTime.now().minusDays(20));

        Incident saved = incidentRepository.save(incident);

        var openIncidents = incidentRepository.findOpenNonSimulatedIncidents();
        assertThat(openIncidents.stream().noneMatch(i -> i.getId().equals(saved.getId()))).isTrue();
    }
}
