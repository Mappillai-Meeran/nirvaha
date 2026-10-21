package com.nirvaha.nirvaha_backend.scheduler;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentServiceTracking;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.service.ServiceRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled SLA Evaluation Engine.
 * Periodically evaluates open incidents against statutory service rules (every 6 hours by default).
 * Idempotent: repeated evaluations produce no duplicate side effects.
 * Concurrency-safe: evaluates each incident in an isolated transaction.
 */
@Component
public class SlaEvaluationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaEvaluationScheduler.class);

    private final IncidentRepository incidentRepository;
    private final ServiceRuleService serviceRuleService;

    @Value("${nirvaha.sla.evaluation-enabled:true}")
    private boolean evaluationEnabled;

    public SlaEvaluationScheduler(IncidentRepository incidentRepository,
                                  ServiceRuleService serviceRuleService) {
        this.incidentRepository = incidentRepository;
        this.serviceRuleService = serviceRuleService;
    }

    @Scheduled(fixedDelayString = "${nirvaha.sla.evaluation-interval-ms:21600000}", initialDelayString = "${nirvaha.sla.initial-delay-ms:30000}")
    public int evaluateOpenIncidents() {
        if (!evaluationEnabled) {
            log.info("SLA evaluation cycle skipped (disabled via configuration)");
            return 0;
        }

        log.info("Starting scheduled SLA evaluation cycle...");
        return runSlaEvaluationCycle();
    }

    public int runSlaEvaluationCycle() {
        List<Incident> openIncidents = incidentRepository.findOpenNonSimulatedIncidents();
        log.info("Evaluating SLA compliance for {} open non-simulated incidents", openIncidents.size());

        AtomicInteger evaluatedCount = new AtomicInteger(0);
        AtomicInteger approachingCount = new AtomicInteger(0);
        AtomicInteger overdueCount = new AtomicInteger(0);

        for (Incident incident : openIncidents) {
            try {
                IncidentServiceTracking tracking = serviceRuleService.evaluateAndEscalate(incident);
                if (tracking != null) {
                    evaluatedCount.incrementAndGet();
                    if (tracking.getSlaStatus() != null) {
                        switch (tracking.getSlaStatus()) {
                            case APPROACHING_DEADLINE -> approachingCount.incrementAndGet();
                            case OVERDUE -> overdueCount.incrementAndGet();
                            default -> {}
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to evaluate SLA tracking for incident #{}: {}", incident.getId(), e.getMessage(), e);
            }
        }

        log.info("SLA evaluation cycle complete: {} evaluated, {} approaching deadline, {} overdue",
                evaluatedCount.get(), approachingCount.get(), overdueCount.get());

        return evaluatedCount.get();
    }
}
