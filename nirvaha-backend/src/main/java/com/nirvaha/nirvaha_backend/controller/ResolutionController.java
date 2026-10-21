package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.CreateResolutionRequest;
import com.nirvaha.nirvaha_backend.dto.ResolutionResponse;
import com.nirvaha.nirvaha_backend.service.ResolutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
public class ResolutionController {

    private final ResolutionService resolutionService;

    public ResolutionController(ResolutionService resolutionService) {
        this.resolutionService = resolutionService;
    }

    @PostMapping("/{incidentId}/resolutions")
    public ResponseEntity<ResolutionResponse> createResolution(
            @PathVariable Long incidentId,
            @Valid @RequestBody CreateResolutionRequest request) {
        ResolutionResponse response = resolutionService.createResolution(incidentId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{incidentId}/resolution")
    public ResponseEntity<ResolutionResponse> getResolution(@PathVariable Long incidentId) {
        ResolutionResponse response = resolutionService.getResolutionForIncident(incidentId);
        return ResponseEntity.ok(response);
    }
}
