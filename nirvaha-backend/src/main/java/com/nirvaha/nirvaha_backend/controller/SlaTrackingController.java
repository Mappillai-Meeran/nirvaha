package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.SlaRuleResponse;
import com.nirvaha.nirvaha_backend.dto.SlaTrackingResponse;
import com.nirvaha.nirvaha_backend.entity.IncidentServiceTracking;
import com.nirvaha.nirvaha_backend.scheduler.SlaEvaluationScheduler;
import com.nirvaha.nirvaha_backend.service.ServiceRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sla")
public class SlaTrackingController {

    private final ServiceRuleService serviceRuleService;
    private final SlaEvaluationScheduler slaEvaluationScheduler;

    public SlaTrackingController(ServiceRuleService serviceRuleService,
                                 SlaEvaluationScheduler slaEvaluationScheduler) {
        this.serviceRuleService = serviceRuleService;
        this.slaEvaluationScheduler = slaEvaluationScheduler;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<List<SlaTrackingResponse>> getAllTracking() {
        List<IncidentServiceTracking> trackingList = serviceRuleService.getAllTracking();
        List<SlaTrackingResponse> response = trackingList.stream()
                .map(SlaTrackingResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<List<SlaTrackingResponse>> getOverdueTracking() {
        List<IncidentServiceTracking> overdueList = serviceRuleService.getOverdueTracking();
        List<SlaTrackingResponse> response = overdueList.stream()
                .map(SlaTrackingResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getSlaSummary() {
        Map<String, Object> summary = serviceRuleService.getSlaSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<List<SlaRuleResponse>> getAllRules() {
        List<SlaRuleResponse> response = serviceRuleService.getAllServiceRules().stream()
                .map(SlaRuleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/evaluate")
    @PreAuthorize("hasAnyRole('OFFICER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerEvaluation() {
        int evaluated = slaEvaluationScheduler.runSlaEvaluationCycle();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "SLA evaluation cycle completed successfully",
                "evaluatedCount", evaluated
        ));
    }
}
