package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.AnomalyFlagResponse;
import com.nirvaha.nirvaha_backend.dto.IncidentAnomaliesResponse;
import com.nirvaha.nirvaha_backend.service.IntegrityAnomalyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class IntegrityAnomalyController {

    private final IntegrityAnomalyService integrityAnomalyService;

    public IntegrityAnomalyController(IntegrityAnomalyService integrityAnomalyService) {
        this.integrityAnomalyService = integrityAnomalyService;
    }

    @GetMapping({"/integrity/open", "/anomalies/open"})
    public ResponseEntity<List<AnomalyFlagResponse>> getOpenAnomalies() {
        List<AnomalyFlagResponse> response = integrityAnomalyService.getOpenAnomalies();
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/incidents/{incidentId}/anomalies", "/integrity/incident/{incidentId}"})
    public ResponseEntity<IncidentAnomaliesResponse> getAnomaliesForIncident(@PathVariable Long incidentId) {
        IncidentAnomaliesResponse response = integrityAnomalyService.getAnomaliesForIncident(incidentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/incidents/{incidentId}/anomalies/detect", "/integrity/evaluate/{incidentId}"})
    public ResponseEntity<IncidentAnomaliesResponse> detectAnomaliesForIncident(@PathVariable Long incidentId) {
        IncidentAnomaliesResponse response = integrityAnomalyService.detectAnomalies(incidentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/officers/{officerId}/anomalies", "/integrity/officer/{officerId}"})
    public ResponseEntity<List<AnomalyFlagResponse>> getAnomaliesForOfficer(@PathVariable Long officerId) {
        List<AnomalyFlagResponse> response = integrityAnomalyService.getAnomaliesForOfficer(officerId);
        return ResponseEntity.ok(response);
    }
}
