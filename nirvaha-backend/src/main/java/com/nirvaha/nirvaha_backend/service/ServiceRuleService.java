package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent;
import com.nirvaha.nirvaha_backend.repository.DataSourceRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentServiceTrackingRepository;
import com.nirvaha.nirvaha_backend.repository.ServiceRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ServiceRuleService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRuleService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ServiceRuleRepository serviceRuleRepository;
    private final IncidentServiceTrackingRepository trackingRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public ServiceRuleService(ServiceRuleRepository serviceRuleRepository,
                              IncidentServiceTrackingRepository trackingRepository,
                              DataSourceRepository dataSourceRepository,
                              ApplicationEventPublisher eventPublisher,
                              NotificationService notificationService,
                              org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.serviceRuleRepository = serviceRuleRepository;
        this.trackingRepository = trackingRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedOfficialCitizenCharterRules() {
        try {
            jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_event_type_check");
        } catch (Exception e) {
            log.warn("Could not drop notifications_event_type_check constraint: {}", e.getMessage());
        }
        Optional<DataSource> pwdSourceOpt = dataSourceRepository.findByName("Puducherry Public Works Department (PWD)");
        DataSource pwdSource = pwdSourceOpt.orElse(null);

        Optional<DataSource> munSourceOpt = dataSourceRepository.findByName("Pondicherry Municipality Grievance Redressal");
        DataSource munSource = munSourceOpt.orElse(pwdSource);

        // Official PWD Citizen's Charter 2024/2025 Section 3.1: 15-day statutory standard for potholes and road obstructions (warning at 3 days)
        seedOrUpdateRule(IncidentCategory.POTHOLE, "PWD", "Puducherry UT",
                "Repair of potholes and road obstruction complaints",
                15, 3, "PWD Citizen's Charter 2024/2025 Section 3.1 (Roads Wing)",
                true, "OFFICIALLY_PUBLISHED_CHARTER", "PWD Citizen's Charter 2024/2025 Section 3.1",
                pwdSource,
                "Official statutory 15-day rectification timeline for pothole and road obstructions published in PWD Citizen's Charter.");

        // Unverified municipal road restoration rule: disabled as official SLA, recorded as UNVERIFIED
        seedOrUpdateRule(IncidentCategory.POTHOLE, "Pondicherry Municipality", "Puducherry UT",
                "Road surface repair and trench restoration by Engineering Section (Unverified)",
                14, 3, "Pondicherry Municipality Engineering Wing Citizen Charter 2023",
                false, "UNVERIFIED", "Unverified municipal target",
                munSource,
                "Unverified 14-day road restoration target; disabled as official SLA in favor of official PWD Citizen Charter 15-day statutory standard.");

        // Official PWD Citizen's Charter 2024/2025 Section 2: 3-day statutory standard for underground sewerage/drainage overflows (warning at 1 day)
        seedOrUpdateRule(IncidentCategory.DRAINAGE, "PWD", "Puducherry UT",
                "Underground drainage overflows, blockages, or damaged manhole covers",
                3, 1, "PWD Citizen's Charter 2024/2025 Section 2 (Underground Sewerage Wing)",
                true, "OFFICIALLY_PUBLISHED_CHARTER", "PWD Citizen's Charter 2024/2025 Section 2",
                pwdSource,
                "Official PWD Underground Drainage charter standard specifies 3 days for clearing drain overflows and blockages.");

        // Water supply pipeline leaks: 1 day public health operational standard (Internal target pending statutory charter)
        seedOrUpdateRule(IncidentCategory.WATER_SUPPLY, "PWD", "Puducherry UT",
                "Repair of municipal water supply pipeline leakages and distribution disruptions",
                1, 1, "PWD Public Health Division Operational Target",
                false, "PROPOSED_INTERNAL_TARGET", "PWD Public Health Division Operational Standard",
                pwdSource,
                "Urgent pipeline bursts handled within 24h as operational target; general target recorded as proposed internal target pending published statutory charter.");

        // Municipal sanitation: 5-day internal operational target (Citizen charter statutory ceiling is 30 days)
        seedOrUpdateRule(IncidentCategory.GARBAGE, "Pondicherry Municipality", "Puducherry UT",
                "Public health sanitation, solid waste clearing, and unauthorized dumping removal",
                5, 2, "Pondicherry Municipal Public Health Section Target",
                false, "PROPOSED_INTERNAL_TARGET", "Pondicherry Municipality Swachh Guidelines",
                munSource,
                "5-day clearance window is an internal municipal target; general statutory grievance timeline under citizen charter is 30 days.");

        // Municipal streetlights: 3-day internal operational target (Citizen charter statutory ceiling is 30 days)
        seedOrUpdateRule(IncidentCategory.STREETLIGHT, "Pondicherry Municipality", "Puducherry UT",
                "Repair and restoration of non-burning street lighting fixtures",
                3, 1, "Pondicherry Municipality Electrical Wing Target",
                false, "PROPOSED_INTERNAL_TARGET", "Pondicherry Municipality Electrical Wing Target",
                munSource,
                "3-day turnaround is an internal operational target; statutory charter redressal ceiling is 30 days.");
    }

    private void seedOrUpdateRule(IncidentCategory category, String department, String jurisdiction,
                                  String description, int targetDays, int warningThresholdDays,
                                  String escalationRef, boolean isOfficialSla, String verificationStatus,
                                  String statutoryRef, DataSource source, String notes) {
        List<ServiceRule> existing = serviceRuleRepository.findApplicableRules(category, department, jurisdiction);
        ServiceRule rule;
        if (existing.isEmpty()) {
            rule = new ServiceRule();
            rule.setDepartment(department);
            rule.setServiceCategory(category);
            rule.setJurisdiction(jurisdiction);
            rule.setBusinessDayRule("CALENDAR_DAYS");
            rule.setEffectiveFrom(LocalDateTime.now().minusYears(1));
        } else {
            rule = existing.get(0);
        }
        rule.setServiceDescription(description);
        rule.setTargetDays(targetDays);
        rule.setWarningThresholdDays(warningThresholdDays);
        rule.setEscalationReference(escalationRef);
        rule.setIsOfficialSla(isOfficialSla);
        rule.setVerificationStatus(verificationStatus);
        rule.setStatutoryReference(statutoryRef);
        rule.setSource(source);
        rule.setNotes(notes);
        serviceRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public IncidentServiceTracking getOrEvaluateTrackingReadOnly(Incident incident) {
        if (incident == null) return null;

        Optional<IncidentServiceTracking> existingOpt = trackingRepository.findByIncidentId(incident.getId());
        IncidentServiceTracking tracking = existingOpt.orElseGet(() -> {
            IncidentServiceTracking t = new IncidentServiceTracking();
            t.setIncident(incident);
            return t;
        });

        // Determine applicable service rule
        List<ServiceRule> rules = serviceRuleRepository.findByServiceCategory(incident.getCategory());
        if (rules.isEmpty()) {
            return existingOpt.orElse(null);
        }

        ServiceRule matchedRule = rules.get(0);
        tracking.setServiceRule(matchedRule);

        LocalDateTime clockStart = incident.getCreatedAt() != null ? incident.getCreatedAt() : LocalDateTime.now();
        LocalDateTime dueAt = clockStart.plusDays(matchedRule.getTargetDays());
        long elapsed = Duration.between(clockStart, LocalDateTime.now()).toDays();

        tracking.setClockStartedAt(clockStart);
        tracking.setDueAt(dueAt);
        tracking.setElapsedDays(elapsed);

        // DATA INTEGRITY & AUDIT RULE:
        // Unverified rules and proposed internal targets MUST NOT produce official breach claims (OVERDUE).
        if (!Boolean.TRUE.equals(matchedRule.getIsOfficialSla())) {
            tracking.setSlaStatus(incident.getStatus() == IncidentStatus.RESOLVED ? SlaStatus.COMPLETED : SlaStatus.NOT_APPLICABLE);
        } else {
            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                tracking.setSlaStatus(SlaStatus.COMPLETED);
            } else if (LocalDateTime.now().isAfter(dueAt)) {
                tracking.setSlaStatus(SlaStatus.OVERDUE);
            } else {
                int warningDays = (matchedRule.getWarningThresholdDays() != null && matchedRule.getWarningThresholdDays() > 0)
                        ? matchedRule.getWarningThresholdDays() : 3;
                long remainingDays = Duration.between(LocalDateTime.now(), dueAt).toDays();
                if (remainingDays <= warningDays) {
                    tracking.setSlaStatus(SlaStatus.APPROACHING_DEADLINE);
                } else {
                    tracking.setSlaStatus(SlaStatus.WITHIN_STANDARD);
                }
            }
        }

        return tracking;
    }

    @Transactional
    public IncidentServiceTracking evaluateServiceTracking(Incident incident) {
        IncidentServiceTracking tracking = getOrEvaluateTrackingReadOnly(incident);
        if (tracking == null) {
            return null;
        }
        return trackingRepository.save(tracking);
    }

    @Transactional
    public IncidentServiceTracking evaluateAndEscalate(Incident incident) {
        IncidentServiceTracking tracking = evaluateServiceTracking(incident);
        if (tracking == null || tracking.getServiceRule() == null) {
            return tracking;
        }

        ServiceRule rule = tracking.getServiceRule();

        // Escalation notifications only apply to official statutory SLAs
        if (!Boolean.TRUE.equals(rule.getIsOfficialSla())) {
            return tracking;
        }

        Long reporterId = (incident.getReporter() != null) ? incident.getReporter().getId() : null;
        String formattedDue = tracking.getDueAt() != null ? tracking.getDueAt().format(DATE_FMT) : "prescribed date";

        if (tracking.getSlaStatus() == SlaStatus.APPROACHING_DEADLINE) {
            if (tracking.getApproachingNotifiedAt() == null) {
                tracking.setApproachingNotifiedAt(LocalDateTime.now());
                if (tracking.getEscalationLevel() == null || tracking.getEscalationLevel() < 1) {
                    tracking.setEscalationLevel(1);
                }
                tracking = trackingRepository.save(tracking);

                if (reporterId != null) {
                    String dedupKey = "sla-approaching-" + incident.getId() + "-" + rule.getId();
                    IncidentLifecycleNotificationEvent event = new IncidentLifecycleNotificationEvent(
                            reporterId,
                            NotificationEventType.SERVICE_TARGET_APPROACHING,
                            "Service target approaching: Incident #" + incident.getId(),
                            "Incident #" + incident.getId() + " (" + (incident.getTitle() != null ? incident.getTitle() : "Civic Issue") +
                                    ") is approaching its statutory service target deadline on " + formattedDue + ".",
                            incident.getId(),
                            dedupKey
                    );
                    notificationService.createNotification(event);
                    eventPublisher.publishEvent(event);
                    log.info("Dispatched SERVICE_TARGET_APPROACHING notification for incident #{}", incident.getId());
                }
            }
        } else if (tracking.getSlaStatus() == SlaStatus.OVERDUE) {
            if (tracking.getOverdueNotifiedAt() == null) {
                tracking.setOverdueNotifiedAt(LocalDateTime.now());
                tracking.setEscalationLevel(2);
                tracking = trackingRepository.save(tracking);

                if (reporterId != null) {
                    String dedupKey = "sla-exceeded-" + incident.getId() + "-" + rule.getId();
                    IncidentLifecycleNotificationEvent event = new IncidentLifecycleNotificationEvent(
                            reporterId,
                            NotificationEventType.SERVICE_TARGET_EXCEEDED,
                            "Service target exceeded: Incident #" + incident.getId(),
                            "Incident #" + incident.getId() + " (" + (incident.getTitle() != null ? incident.getTitle() : "Civic Issue") +
                                    ") has exceeded its official service target timeline.",
                            incident.getId(),
                            dedupKey
                    );
                    notificationService.createNotification(event);
                    eventPublisher.publishEvent(event);
                    log.info("Dispatched SERVICE_TARGET_EXCEEDED notification for incident #{}", incident.getId());
                }
            }
        }

        return tracking;
    }

    @Transactional(readOnly = true)
    public List<IncidentServiceTracking> getAllTracking() {
        return trackingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<IncidentServiceTracking> getOverdueTracking() {
        return trackingRepository.findOfficialOverdue();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSlaSummary() {
        Map<String, Object> summary = new HashMap<>();

        long withinStandard = 0;
        long approaching = 0;
        long overdue = 0;
        long completed = 0;
        long notApplicable = 0;

        List<Object[]> statusCounts = trackingRepository.countBySlaStatusGrouped();
        for (Object[] row : statusCounts) {
            SlaStatus status = (SlaStatus) row[0];
            Long count = (Long) row[1];
            if (status != null) {
                switch (status) {
                    case WITHIN_STANDARD -> withinStandard = count;
                    case APPROACHING_DEADLINE -> approaching = count;
                    case OVERDUE -> overdue = count;
                    case COMPLETED -> completed = count;
                    case NOT_APPLICABLE -> notApplicable = count;
                    default -> {}
                }
            }
        }

        long totalTracked = withinStandard + approaching + overdue + completed + notApplicable;
        long officialActive = withinStandard + approaching + overdue;
        double complianceRate = (officialActive + completed > 0)
                ? ((double) (withinStandard + completed) / (officialActive + completed)) * 100.0
                : 100.0;

        summary.put("withinStandardCount", withinStandard);
        summary.put("approachingCount", approaching);
        summary.put("overdueCount", overdue);
        summary.put("completedCount", completed);
        summary.put("notApplicableCount", notApplicable);
        summary.put("totalTracked", totalTracked);
        summary.put("complianceRate", Math.round(complianceRate * 10.0) / 10.0);

        return summary;
    }

    public List<ServiceRule> getAllServiceRules() {
        return serviceRuleRepository.findAll();
    }
}
