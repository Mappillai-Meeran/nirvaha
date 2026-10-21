package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.CreateIncidentActionRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentActionResponse;
import com.nirvaha.nirvaha_backend.dto.IncidentWorkflowResponse;
import com.nirvaha.nirvaha_backend.service.IncidentWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentWorkflowController {

    private final IncidentWorkflowService incidentWorkflowService;

    public IncidentWorkflowController(IncidentWorkflowService incidentWorkflowService) {
        this.incidentWorkflowService = incidentWorkflowService;
    }

    @PostMapping("/{incidentId}/actions")
    public ResponseEntity<IncidentActionResponse> addAction(
            @PathVariable Long incidentId,
            @Valid @RequestBody CreateIncidentActionRequest request) {
        IncidentActionResponse response = incidentWorkflowService.addAction(incidentId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{incidentId}/actions")
    public ResponseEntity<List<IncidentActionResponse>> getActions(@PathVariable Long incidentId) {
        List<IncidentActionResponse> responses = incidentWorkflowService.getActionsForIncident(incidentId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{incidentId}/workflow")
    public ResponseEntity<IncidentWorkflowResponse> getWorkflow(@PathVariable Long incidentId) {
        IncidentWorkflowResponse response = incidentWorkflowService.getWorkflowForIncident(incidentId);
        return ResponseEntity.ok(response);
    }
}
