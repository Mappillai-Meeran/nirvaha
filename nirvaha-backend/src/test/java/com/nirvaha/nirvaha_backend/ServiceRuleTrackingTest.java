package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.ServiceRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ServiceRuleTrackingTest {

    @Autowired
    private ServiceRuleService serviceRuleService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testEvaluateServiceTrackingForDrainageIncident() {
        User reporter = userRepository.findByEmail("sla.test@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("sla.test@example.com");
            u.setName("SLA Test User");
            u.setRole(UserRole.CITIZEN);
            u.setPasswordHash("hash123");
            return userRepository.save(u);
        });

        // Drainage standard is official 3 days (PWD Citizen Charter Section 2)
        Incident incident = new Incident();
        incident.setTitle("Drainage Overflow");
        incident.setDescription("Severe drain blockage test");
        incident.setCategory(IncidentCategory.DRAINAGE);
        incident.setSeverity(IncidentSeverity.HIGH);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setLatitude(BigDecimal.valueOf(11.9400));
        incident.setLongitude(BigDecimal.valueOf(79.8000));
        incident.setReporter(reporter);
        incident.setCreatedAt(LocalDateTime.now().minusDays(5));

        Incident saved = incidentRepository.save(incident);

        IncidentServiceTracking tracking = serviceRuleService.evaluateServiceTracking(saved);
        assertThat(tracking).isNotNull();
        assertThat(tracking.getServiceRule()).isNotNull();
        assertThat(tracking.getServiceRule().getDepartment()).isEqualTo("PWD");
        assertThat(tracking.getServiceRule().getTargetDays()).isEqualTo(3);
        assertThat(tracking.getServiceRule().getIsOfficialSla()).isTrue();
        assertThat(tracking.getServiceRule().getVerificationStatus()).isEqualTo("OFFICIALLY_PUBLISHED_CHARTER");
        // 5 days elapsed > 3 days target = OVERDUE
        assertThat(tracking.getSlaStatus()).isEqualTo(SlaStatus.OVERDUE);
    }

    @Test
    void testEvaluateServiceTrackingForPotholeIncident() {
        User reporter = userRepository.findByEmail("sla.test@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("sla.test@example.com");
            u.setName("SLA Test User");
            u.setRole(UserRole.CITIZEN);
            u.setPasswordHash("hash123");
            return userRepository.save(u);
        });

        // Pothole standard is official 15 days (PWD Citizen Charter Section 3.1)
        Incident incident = new Incident();
        incident.setTitle("Deep Pothole");
        incident.setDescription("Road surface pothole test");
        incident.setCategory(IncidentCategory.POTHOLE);
        incident.setSeverity(IncidentSeverity.HIGH);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setLatitude(BigDecimal.valueOf(11.9400));
        incident.setLongitude(BigDecimal.valueOf(79.8000));
        incident.setReporter(reporter);
        incident.setCreatedAt(LocalDateTime.now().minusDays(2));

        Incident saved = incidentRepository.save(incident);

        IncidentServiceTracking tracking = serviceRuleService.evaluateServiceTracking(saved);
        assertThat(tracking).isNotNull();
        assertThat(tracking.getServiceRule()).isNotNull();
        assertThat(tracking.getServiceRule().getDepartment()).isEqualTo("PWD");
        assertThat(tracking.getServiceRule().getTargetDays()).isEqualTo(15);
        assertThat(tracking.getServiceRule().getIsOfficialSla()).isTrue();
        assertThat(tracking.getSlaStatus()).isEqualTo(SlaStatus.WITHIN_STANDARD);
    }

    @Test
    void testUnverifiedRuleCannotProduceBreachClaims() {
        User reporter = userRepository.findByEmail("sla.test@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("sla.test@example.com");
            u.setName("SLA Test User");
            u.setRole(UserRole.CITIZEN);
            u.setPasswordHash("hash123");
            return userRepository.save(u);
        });

        // Garbage standard is an internal municipal target (5 days), isOfficialSla = false
        Incident incident = new Incident();
        incident.setTitle("Garbage Dump");
        incident.setDescription("Uncollected garbage backlog");
        incident.setCategory(IncidentCategory.GARBAGE);
        incident.setSeverity(IncidentSeverity.MEDIUM);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setLatitude(BigDecimal.valueOf(11.9400));
        incident.setLongitude(BigDecimal.valueOf(79.8000));
        incident.setReporter(reporter);
        incident.setCreatedAt(LocalDateTime.now().minusDays(10)); // 10 days elapsed > 5 days target

        Incident saved = incidentRepository.save(incident);

        IncidentServiceTracking tracking = serviceRuleService.evaluateServiceTracking(saved);
        assertThat(tracking).isNotNull();
        assertThat(tracking.getServiceRule()).isNotNull();
        assertThat(tracking.getServiceRule().getIsOfficialSla()).isFalse();
        assertThat(tracking.getServiceRule().getVerificationStatus()).isEqualTo("PROPOSED_INTERNAL_TARGET");
        // Must NOT produce official breach claim (OVERDUE), must be NOT_APPLICABLE
        assertThat(tracking.getSlaStatus()).isEqualTo(SlaStatus.NOT_APPLICABLE);
    }

    @Test
    void testApproachingDeadlineForPotholeAtWarningThreshold() {
        User reporter = userRepository.findByEmail("sla.test@example.com").orElseThrow();

        // Pothole: 15 days target, warning threshold = 3 days.
        // Created 13 days ago -> 2 days remaining (<= 3 days) -> APPROACHING_DEADLINE
        Incident incident = new Incident();
        incident.setTitle("Warning Pothole");
        incident.setDescription("Pothole entering warning window");
        incident.setCategory(IncidentCategory.POTHOLE);
        incident.setSeverity(IncidentSeverity.MEDIUM);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setLatitude(BigDecimal.valueOf(11.9400));
        incident.setLongitude(BigDecimal.valueOf(79.8000));
        incident.setReporter(reporter);
        incident.setCreatedAt(LocalDateTime.now().minusDays(13));

        Incident saved = incidentRepository.save(incident);

        IncidentServiceTracking tracking = serviceRuleService.evaluateServiceTracking(saved);
        assertThat(tracking).isNotNull();
        assertThat(tracking.getServiceRule().getWarningThresholdDays()).isEqualTo(3);
        assertThat(tracking.getSlaStatus()).isEqualTo(SlaStatus.APPROACHING_DEADLINE);
    }

    @Test
    void testDrainageWarningThresholdIsOneDay() {
        User reporter = userRepository.findByEmail("sla.test@example.com").orElseThrow();

        // Drainage: 3 days target, warning threshold = 1 day.
        // Created 2 hours ago -> ~70 hours remaining (> 1 day warning threshold) -> WITHIN_STANDARD
        Incident incident = new Incident();
        incident.setTitle("Drainage 2 hours old");
        incident.setDescription("Drainage within standard");
        incident.setCategory(IncidentCategory.DRAINAGE);
        incident.setSeverity(IncidentSeverity.HIGH);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setLatitude(BigDecimal.valueOf(11.9400));
        incident.setLongitude(BigDecimal.valueOf(79.8000));
        incident.setReporter(reporter);
        incident.setCreatedAt(LocalDateTime.now().minusHours(2));

        Incident saved = incidentRepository.save(incident);

        IncidentServiceTracking tracking = serviceRuleService.evaluateServiceTracking(saved);
        assertThat(tracking).isNotNull();
        assertThat(tracking.getServiceRule().getWarningThresholdDays()).isEqualTo(1);
        assertThat(tracking.getSlaStatus()).isEqualTo(SlaStatus.WITHIN_STANDARD);

        // Incident created 2 days ago -> ~1 day remaining (<= 1 day warning threshold) -> APPROACHING_DEADLINE
        Incident incidentApproaching = new Incident();
        incidentApproaching.setTitle("Drainage 2 days old");
        incidentApproaching.setDescription("Drainage approaching deadline");
        incidentApproaching.setCategory(IncidentCategory.DRAINAGE);
        incidentApproaching.setSeverity(IncidentSeverity.HIGH);
        incidentApproaching.setStatus(IncidentStatus.REPORTED);
        incidentApproaching.setLatitude(BigDecimal.valueOf(11.9400));
        incidentApproaching.setLongitude(BigDecimal.valueOf(79.8000));
        incidentApproaching.setReporter(reporter);
        incidentApproaching.setCreatedAt(LocalDateTime.now().minusDays(2));

        Incident savedApproaching = incidentRepository.save(incidentApproaching);

        IncidentServiceTracking tracking2 = serviceRuleService.evaluateServiceTracking(savedApproaching);
        assertThat(tracking2.getSlaStatus()).isEqualTo(SlaStatus.APPROACHING_DEADLINE);
    }

    @Test
    void testResolvedIncidentSetsCompletedStatus() {
        User reporter = userRepository.findByEmail("sla.test@example.com").orElseThrow();

        Incident incident = new Incident();
        incident.setTitle("Resolved Pothole");
        incident.setDescription("Pothole fixed");
        incident.setCategory(IncidentCategory.POTHOLE);
        incident.setSeverity(IncidentSeverity.LOW);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setLatitude(BigDecimal.valueOf(11.9400));
        incident.setLongitude(BigDecimal.valueOf(79.8000));
        incident.setReporter(reporter);
        incident.setCreatedAt(LocalDateTime.now().minusDays(2));

        Incident saved = incidentRepository.save(incident);

        IncidentServiceTracking tracking = serviceRuleService.evaluateServiceTracking(saved);
        assertThat(tracking).isNotNull();
        assertThat(tracking.getSlaStatus()).isEqualTo(SlaStatus.COMPLETED);
    }
}
