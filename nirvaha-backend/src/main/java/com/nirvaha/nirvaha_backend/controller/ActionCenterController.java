package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/action-center")
public class ActionCenterController {

    private final ActionRecommendationService recommendationService;
    private final OfficerDecisionService decisionService;
    private final LearningSignalService learningService;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final EvidenceRepository evidenceRepository;
    private final IncidentActionRepository actionRepository;
    private final ResolutionRepository resolutionRepository;

    public ActionCenterController(ActionRecommendationService recommendationService,
                                  OfficerDecisionService decisionService,
                                  LearningSignalService learningService,
                                  IncidentRepository incidentRepository,
                                  UserRepository userRepository,
                                  EvidenceRepository evidenceRepository,
                                  IncidentActionRepository actionRepository,
                                  ResolutionRepository resolutionRepository) {
        this.recommendationService = recommendationService;
        this.decisionService = decisionService;
        this.learningService = learningService;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.evidenceRepository = evidenceRepository;
        this.actionRepository = actionRepository;
        this.resolutionRepository = resolutionRepository;
    }

    @GetMapping("/queue")
    public ResponseEntity<ActionCenterQueueDTO> getActionQueue(
            @RequestParam(name = "includeSimulated", required = false, defaultValue = "false") boolean includeSimulated) {
        List<Incident> incidents = includeSimulated
                ? incidentRepository.findAll()
                : incidentRepository.findOfficialIncidents();
        List<ActionRecommendationDTO> recommendations = recommendationService.generateRecommendations(incidents);
        List<LearningSignalDTO> learningSignals = learningService.evaluateLearningSignals();

        int criticalCount = (int) recommendations.stream()
                .filter(r -> r.getPriorityLevel() == PriorityLevel.CRITICAL || r.getRiskLevel() == RiskLevel.CRITICAL || r.getRecommendedAction() == RecommendedActionType.REOPEN_CASE)
                .count();

        ActionCenterQueueDTO queue = new ActionCenterQueueDTO(
                recommendations,
                learningSignals,
                recommendations.size(),
                criticalCount,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(queue);
    }

    @GetMapping("/recommendations/{incidentId}")
    public ResponseEntity<ActionRecommendationDTO> getRecommendationForIncident(@PathVariable Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        ActionRecommendationDTO dto = recommendationService.generateRecommendation(incident, PriorityLevel.HIGH, RiskLevel.MODERATE);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/decisions")
    public ResponseEntity<OfficerDecisionResponse> recordOfficerDecision(@Valid @RequestBody OfficerDecisionRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated officer user not found"));

        OfficerDecisionResponse response = decisionService.recordDecision(request, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/decisions/incident/{incidentId}")
    public ResponseEntity<List<OfficerDecisionResponse>> getDecisionsForIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(decisionService.getDecisionsForIncident(incidentId));
    }

    @GetMapping("/learning-signals")
    public ResponseEntity<List<LearningSignalDTO>> getLearningSignals() {
        return ResponseEntity.ok(learningService.evaluateLearningSignals());
    }

    @GetMapping("/incidents/{incidentId}/full-chain")
    public ResponseEntity<ClosedLoopChainDTO> getClosedLoopChain(@PathVariable Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + incidentId));

        IncidentResponse incResponse = IncidentResponse.fromEntity(incident);

        List<EvidenceResponse> evidenceResponses = evidenceRepository.findByIncidentId(incidentId).stream()
                .map(EvidenceResponse::fromEntity)
                .collect(Collectors.toList());

        ActionRecommendationDTO recommendation = recommendationService.generateRecommendation(incident, PriorityLevel.HIGH, RiskLevel.MODERATE);
        List<OfficerDecisionResponse> decisions = decisionService.getDecisionsForIncident(incidentId);

        List<IncidentActionResponse> history = actionRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
                .map(this::mapActionToResponse)
                .collect(Collectors.toList());

        List<Resolution> resolutions = resolutionRepository.findByIncidentId(incidentId);
        ResolutionResponse resResponse = !resolutions.isEmpty() ? ResolutionResponse.fromEntity(resolutions.get(0)) : null;

        List<LearningSignalDTO> learningSignals = learningService.evaluateLearningSignals().stream()
                .filter(l -> l.getCategory() == incident.getCategory())
                .collect(Collectors.toList());

        ClosedLoopChainDTO chain = new ClosedLoopChainDTO();
        chain.setIncident(incResponse);
        chain.setEvidence(evidenceResponses);
        chain.setActionRecommendation(recommendation);
        chain.setOfficerDecisions(decisions);
        chain.setWorkflowHistory(history);
        chain.setResolution(resResponse);
        chain.setLearningSignals(learningSignals);

        return ResponseEntity.ok(chain);
    }

    private IncidentActionResponse mapActionToResponse(IncidentAction a) {
        return new IncidentActionResponse(
                a.getId(),
                a.getIncident().getId(),
                a.getOfficer().getId(),
                a.getOfficer().getName(),
                a.getActionType(),
                a.getRemarks(),
                a.getCreatedAt()
        );
    }
}
