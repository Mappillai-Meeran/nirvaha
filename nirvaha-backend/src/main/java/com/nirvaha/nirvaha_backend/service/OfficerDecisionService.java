package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.OfficerDecisionRequest;
import com.nirvaha.nirvaha_backend.dto.OfficerDecisionResponse;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.OfficerDecision;
import com.nirvaha.nirvaha_backend.entity.OfficerDecisionType;
import com.nirvaha.nirvaha_backend.entity.User;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.OfficerDecisionRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfficerDecisionService {

    private final OfficerDecisionRepository decisionRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public OfficerDecisionService(OfficerDecisionRepository decisionRepository,
                                  IncidentRepository incidentRepository,
                                  UserRepository userRepository) {
        this.decisionRepository = decisionRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OfficerDecisionResponse recordDecision(OfficerDecisionRequest request, User officer) {
        if (request == null || request.getIncidentId() == null) {
            throw new IllegalArgumentException("Incident ID is required to record officer decision.");
        }

        if (request.getDecision() != OfficerDecisionType.ACCEPT) {
            if (request.getReason() == null || request.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException("A valid reason is required when modifying, deferring, or rejecting a recommendation.");
            }
        }

        Incident incident = incidentRepository.findById(request.getIncidentId())
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found with ID: " + request.getIncidentId()));

        OfficerDecision decision = new OfficerDecision();
        decision.setIncident(incident);
        decision.setRecommendationId(request.getRecommendationId());
        decision.setRecommendedAction(request.getRecommendedAction());
        decision.setOfficerDecision(request.getDecision());
        decision.setDecisionReason(request.getReason());
        decision.setDecidedBy(officer);

        OfficerDecision saved = decisionRepository.save(decision);
        return mapToResponse(saved);
    }

    public List<OfficerDecisionResponse> getDecisionsForIncident(Long incidentId) {
        return decisionRepository.findByIncidentIdOrderByDecidedAtDesc(incidentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<OfficerDecisionResponse> getDecisionsByOfficer(Long officerId) {
        return decisionRepository.findByDecidedByIdOrderByDecidedAtDesc(officerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OfficerDecisionResponse mapToResponse(OfficerDecision d) {
        return new OfficerDecisionResponse(
                d.getId(),
                d.getIncident().getId(),
                d.getRecommendationId(),
                d.getRecommendedAction(),
                d.getOfficerDecision(),
                d.getDecisionReason(),
                d.getDecidedBy().getId(),
                d.getDecidedBy().getName(),
                d.getDecidedAt()
        );
    }
}
