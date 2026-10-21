package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.WardRiskResponse;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.service.RiskIntelligenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
public class RiskIntelligenceController {

    private final RiskIntelligenceService riskIntelligenceService;

    public RiskIntelligenceController(RiskIntelligenceService riskIntelligenceService) {
        this.riskIntelligenceService = riskIntelligenceService;
    }

    @GetMapping("/wards")
    public ResponseEntity<List<WardRiskResponse>> getAllWardRisks() {
        List<WardRiskResponse> response = riskIntelligenceService.getAllWardRisks();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wards/{wardId}")
    public ResponseEntity<WardRiskResponse> getRiskForWard(
            @PathVariable Long wardId,
            @RequestParam(required = false) IncidentCategory category) {
        WardRiskResponse response = riskIntelligenceService.evaluateWardRisk(wardId, category);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assessments/evaluate")
    public ResponseEntity<List<WardRiskResponse>> evaluateAllRisks() {
        List<WardRiskResponse> response = riskIntelligenceService.getAllWardRisks();
        return ResponseEntity.ok(response);
    }
}
