package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.PriorityScoreResponse;
import com.nirvaha.nirvaha_backend.service.PriorityScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
public class PriorityScoringController {

    private final PriorityScoringService priorityScoringService;

    public PriorityScoringController(PriorityScoringService priorityScoringService) {
        this.priorityScoringService = priorityScoringService;
    }

    @RequestMapping(value = "/{incidentId}/priority", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<PriorityScoreResponse> getIncidentPriority(
            @PathVariable Long incidentId,
            @RequestParam(required = false) Double rainfall) {
        PriorityScoreResponse response = priorityScoringService.calculatePriority(incidentId, rainfall);
        return ResponseEntity.ok(response);
    }
}
