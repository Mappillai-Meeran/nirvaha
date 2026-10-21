package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentServiceTrackingRepository;
import com.nirvaha.nirvaha_backend.repository.NotificationRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.ServiceRuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SlaEscalationNotificationTest {

    @Autowired
    private ServiceRuleService serviceRuleService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentServiceTrackingRepository trackingRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User testReporter;

    @BeforeEach
    void setUp() {
        testReporter = userRepository.findByEmail("sla.notification.test@example.com").orElseGet(() -> {
            User u = new User();
            u.setEmail("sla.notification.test@example.com");
            u.setName("SLA Notification Tester");
            u.setRole(UserRole.CITIZEN);
            u.setPasswordHash("hash123");
            return userRepository.save(u);
        });
    }

    @Test
    void testApproachingDeadlineNotificationTriggeredAndDeduplicated() {
        // Pothole SLA is 15 days, warning threshold is 3 days.
        // Created 13 days ago -> 2 days remaining (<= 3 days warning threshold) -> APPROACHING_DEADLINE
        Incident incident = new Incident();
        incident.setTitle("Approaching SLA Pothole");
        incident.setDescription("Pothole nearing target deadline");
        incident.setCategory(IncidentCategory.POTHOLE);
        incident.setSeverity(IncidentSeverity.MEDIUM);
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setLatitude(BigDecimal.valueOf(11.9300));
        incident.setLongitude(BigDecimal.valueOf(79.8200));
        incident.setReporter(testReporter);
        incident.setCreatedAt(LocalDateTime.now().minusDays(13));

        Incident saved = incidentRepository.save(incident);

        // First evaluation: should trigger notification and set escalationLevel = 1
        IncidentServiceTracking tracking1 = serviceRuleService.evaluateAndEscalate(saved);
        assertThat(tracking1.getSlaStatus()).isEqualTo(SlaStatus.APPROACHING_DEADLINE);
        assertThat(tracking1.getEscalationLevel()).isEqualTo(1);
        assertThat(tracking1.getApproachingNotifiedAt()).isNotNull();

        List<Notification> notifs1 = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                testReporter.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        long approachingCount1 = notifs1.stream()
                .filter(n -> n.getEventType() == NotificationEventType.SERVICE_TARGET_APPROACHING && saved.getId().equals(n.getIncidentId()))
                .count();
        assertThat(approachingCount1).isEqualTo(1);

        // Second evaluation: re-evaluation must NOT fire duplicate notification
        IncidentServiceTracking tracking2 = serviceRuleService.evaluateAndEscalate(saved);
        assertThat(tracking2.getEscalationLevel()).isEqualTo(1);

        List<Notification> notifs2 = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                testReporter.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        long approachingCount2 = notifs2.stream()
                .filter(n -> n.getEventType() == NotificationEventType.SERVICE_TARGET_APPROACHING && saved.getId().equals(n.getIncidentId()))
                .count();
        assertThat(approachingCount2).isEqualTo(1); // Still exactly 1
    }

    @Test
    void testOverdueNotificationTriggeredAndDeduplicated() {
        // Drainage SLA is 3 days. Created 5 days ago -> OVERDUE
        Incident incident = new Incident();
        incident.setTitle("Overdue Drainage Incident");
        incident.setDescription("Drainage overflow past statutory deadline");
        incident.setCategory(IncidentCategory.DRAINAGE);
        incident.setSeverity(IncidentSeverity.HIGH);
        incident.setStatus(IncidentStatus.VERIFIED);
        incident.setLatitude(BigDecimal.valueOf(11.9320));
        incident.setLongitude(BigDecimal.valueOf(79.8220));
        incident.setReporter(testReporter);
        incident.setCreatedAt(LocalDateTime.now().minusDays(5));

        Incident saved = incidentRepository.save(incident);

        // First evaluation: should trigger overdue notification and set escalationLevel = 2
        IncidentServiceTracking tracking1 = serviceRuleService.evaluateAndEscalate(saved);
        assertThat(tracking1.getSlaStatus()).isEqualTo(SlaStatus.OVERDUE);
        assertThat(tracking1.getEscalationLevel()).isEqualTo(2);
        assertThat(tracking1.getOverdueNotifiedAt()).isNotNull();

        List<Notification> notifs1 = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                testReporter.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        long overdueCount1 = notifs1.stream()
                .filter(n -> n.getEventType() == NotificationEventType.SERVICE_TARGET_EXCEEDED && saved.getId().equals(n.getIncidentId()))
                .count();
        assertThat(overdueCount1).isEqualTo(1);

        // Second evaluation: re-evaluation must NOT fire duplicate
        IncidentServiceTracking tracking2 = serviceRuleService.evaluateAndEscalate(saved);
        assertThat(tracking2.getEscalationLevel()).isEqualTo(2);

        List<Notification> notifs2 = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                testReporter.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        long overdueCount2 = notifs2.stream()
                .filter(n -> n.getEventType() == NotificationEventType.SERVICE_TARGET_EXCEEDED && saved.getId().equals(n.getIncidentId()))
                .count();
        assertThat(overdueCount2).isEqualTo(1); // Still exactly 1
    }

    @Test
    void testUnverifiedRuleNeverTriggersEscalationNotifications() {
        // Garbage is PROPOSED_INTERNAL_TARGET (isOfficialSla = false)
        Incident incident = new Incident();
        incident.setTitle("Unverified Target Sanitation");
        incident.setDescription("Municipal sanitation operational target");
        incident.setCategory(IncidentCategory.GARBAGE);
        incident.setSeverity(IncidentSeverity.MEDIUM);
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setLatitude(BigDecimal.valueOf(11.9330));
        incident.setLongitude(BigDecimal.valueOf(79.8230));
        incident.setReporter(testReporter);
        incident.setCreatedAt(LocalDateTime.now().minusDays(10)); // Past 5-day internal target

        Incident saved = incidentRepository.save(incident);

        IncidentServiceTracking tracking = serviceRuleService.evaluateAndEscalate(saved);
        assertThat(tracking.getSlaStatus()).isEqualTo(SlaStatus.NOT_APPLICABLE);
        assertThat(tracking.getApproachingNotifiedAt()).isNull();
        assertThat(tracking.getOverdueNotifiedAt()).isNull();
        assertThat(tracking.getEscalationLevel()).isEqualTo(0);

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                testReporter.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        long slaNotifCount = notifs.stream()
                .filter(n -> (n.getEventType() == NotificationEventType.SERVICE_TARGET_APPROACHING ||
                              n.getEventType() == NotificationEventType.SERVICE_TARGET_EXCEEDED) &&
                             saved.getId().equals(n.getIncidentId()))
                .count();
        assertThat(slaNotifCount).isEqualTo(0);
    }
}
