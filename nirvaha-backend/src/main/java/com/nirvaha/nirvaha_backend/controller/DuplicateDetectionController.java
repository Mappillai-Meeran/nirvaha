package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.DuplicateDetectionResponse;
import com.nirvaha.nirvaha_backend.service.DuplicateDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class DuplicateDetectionController {

    private final DuplicateDetectionService duplicateDetectionService;

    public DuplicateDetectionController(DuplicateDetectionService duplicateDetectionService) {
        this.duplicateDetectionService = duplicateDetectionService;
    }

    @GetMapping("/{incidentId}/duplicates")
    public ResponseEntity<DuplicateDetectionResponse> getDuplicateIncidents(@PathVariable Long incidentId) {
        DuplicateDetectionResponse response = duplicateDetectionService.findDuplicatesForIncident(incidentId);
        return ResponseEntity.ok(response);
    }
}
