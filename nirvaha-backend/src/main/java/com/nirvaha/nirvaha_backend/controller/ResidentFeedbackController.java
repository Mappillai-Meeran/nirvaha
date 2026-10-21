package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.DisputeReviewRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentFeedbackResponse;
import com.nirvaha.nirvaha_backend.dto.ResidentFeedbackRequest;
import com.nirvaha.nirvaha_backend.service.ResidentFeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class ResidentFeedbackController {

    private final ResidentFeedbackService feedbackService;

    public ResidentFeedbackController(ResidentFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/{incidentId}/feedback")
    public ResponseEntity<IncidentFeedbackResponse> submitFeedback(
            @PathVariable Long incidentId,
            @Valid @RequestBody ResidentFeedbackRequest request) {
        IncidentFeedbackResponse response = feedbackService.submitFeedback(incidentId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{incidentId}/feedback")
    public ResponseEntity<List<IncidentFeedbackResponse>> getFeedback(
            @PathVariable Long incidentId) {
        List<IncidentFeedbackResponse> responses = feedbackService.getFeedbackForIncident(incidentId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{incidentId}/feedback/{feedbackId}/review")
    public ResponseEntity<IncidentFeedbackResponse> reviewDispute(
            @PathVariable Long incidentId,
            @PathVariable Long feedbackId,
            @Valid @RequestBody DisputeReviewRequest request) {
        IncidentFeedbackResponse response = feedbackService.reviewDispute(incidentId, feedbackId, request);
        return ResponseEntity.ok(response);
    }
}
