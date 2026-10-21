package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.JudgeDemoScenarioResultDTO;
import com.nirvaha.nirvaha_backend.service.JudgeDemoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class JudgeDemoController {

    private final JudgeDemoService judgeDemoService;

    public JudgeDemoController(JudgeDemoService judgeDemoService) {
        this.judgeDemoService = judgeDemoService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDemoStatus() {
        return ResponseEntity.ok(judgeDemoService.getDemoStatus());
    }

    @PostMapping("/scenarios/monsoon-drainage-surge")
    public ResponseEntity<JudgeDemoScenarioResultDTO> runMonsoonDrainageSurgeScenario(
            @RequestParam(value = "demoRunId", required = false) String demoRunId) {
        JudgeDemoScenarioResultDTO result = judgeDemoService.runMonsoonDrainageSurgeScenario(demoRunId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/scenarios/duplicate-detection")
    public ResponseEntity<JudgeDemoScenarioResultDTO> runDuplicateDetectionScenario(
            @RequestParam(value = "demoRunId", required = false) String demoRunId) {
        JudgeDemoScenarioResultDTO result = judgeDemoService.runDuplicateDetectionScenario(demoRunId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/scenarios/resolution-failure")
    public ResponseEntity<JudgeDemoScenarioResultDTO> runResolutionFailureScenario(
            @RequestParam(value = "demoRunId", required = false) String demoRunId) {
        JudgeDemoScenarioResultDTO result = judgeDemoService.runResolutionFailureScenario(demoRunId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetDemoData(
            @RequestParam(value = "demoRunId", required = false) String demoRunId) {
        Map<String, Object> result = judgeDemoService.resetDemoData(demoRunId);
        if ("REFUSED".equals(result.get("status"))) {
            return ResponseEntity.badRequest().body(result);
        }
        if ("NOT_FOUND".equals(result.get("status"))) {
            return ResponseEntity.status(404).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset/{demoRunId}")
    public ResponseEntity<Map<String, Object>> resetDemoRun(@PathVariable("demoRunId") String demoRunId) {
        Map<String, Object> result = judgeDemoService.resetDemoRun(demoRunId);
        if ("REFUSED".equals(result.get("status"))) {
            return ResponseEntity.badRequest().body(result);
        }
        if ("NOT_FOUND".equals(result.get("status"))) {
            return ResponseEntity.status(404).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
