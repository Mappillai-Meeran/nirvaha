package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.CreateEvidenceRequest;
import com.nirvaha.nirvaha_backend.dto.EvidenceResponse;
import com.nirvaha.nirvaha_backend.service.EvidenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping
    public ResponseEntity<EvidenceResponse> createEvidence(@Valid @RequestBody CreateEvidenceRequest request) {
        EvidenceResponse response = evidenceService.createEvidence(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/upload/incident/{incidentId}")
    public ResponseEntity<EvidenceResponse> uploadEvidence(
            @PathVariable Long incidentId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        EvidenceResponse response = evidenceService.uploadEvidence(incidentId, file, description);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/upload")
    public ResponseEntity<EvidenceResponse> uploadEvidenceParam(
            @RequestParam("incidentId") Long incidentId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        EvidenceResponse response = evidenceService.uploadEvidence(incidentId, file, description);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvidenceResponse> getEvidenceById(@PathVariable Long id) {
        EvidenceResponse response = evidenceService.getEvidenceById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/incident/{incidentId}")
    public ResponseEntity<List<EvidenceResponse>> getEvidenceForIncident(@PathVariable Long incidentId) {
        List<EvidenceResponse> response = evidenceService.getEvidenceForIncident(incidentId);
        return ResponseEntity.ok(response);
    }
}
