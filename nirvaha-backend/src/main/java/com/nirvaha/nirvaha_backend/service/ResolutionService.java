package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.CreateResolutionRequest;
import com.nirvaha.nirvaha_backend.dto.ResolutionResponse;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.DuplicateResourceException;
import com.nirvaha.nirvaha_backend.exception.InvalidWorkflowException;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.EvidenceRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentActionRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.ResolutionRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.util.HaversineDistance;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResolutionService {

    private final ResolutionRepository resolutionRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final EvidenceRepository evidenceRepository;
    private final IncidentActionRepository incidentActionRepository;
    private final com.nirvaha.nirvaha_backend.security.IncidentAccessValidator incidentAccessValidator;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public ResolutionService(ResolutionRepository resolutionRepository,
                             IncidentRepository incidentRepository,
                             UserRepository userRepository,
                             EvidenceRepository evidenceRepository,
                             IncidentActionRepository incidentActionRepository,
                             com.nirvaha.nirvaha_backend.security.IncidentAccessValidator incidentAccessValidator,
                             org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.resolutionRepository = resolutionRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.evidenceRepository = evidenceRepository;
        this.incidentActionRepository = incidentActionRepository;
        this.incidentAccessValidator = incidentAccessValidator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ResolutionResponse createResolution(Long incidentId, CreateResolutionRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));

        if (!resolutionRepository.findByIncidentId(incidentId).isEmpty()) {
            throw new DuplicateResourceException("Resolution already submitted for incident ID " + incidentId);
        }

        User officer = getAuthenticatedUserOrFallback(request != null ? request.getOfficerId() : null);
        if (officer.getRole() != UserRole.OFFICER && officer.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("User " + officer.getId() + " is not authorized to submit resolution evidence");
        }

        Evidence resolutionEvidence = evidenceRepository.findById(request.getEvidenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Evidence with ID " + request.getEvidenceId() + " not found"));

        if (resolutionEvidence.getIncident() == null || !resolutionEvidence.getIncident().getId().equals(incidentId)) {
            throw new IllegalArgumentException("Evidence ID " + request.getEvidenceId() + " does not belong to incident " + incidentId);
        }

        if (incident.getStatus() == IncidentStatus.REPORTED) {
            throw new IllegalArgumentException("Resolution evidence cannot be submitted for an unverified REPORTED incident");
        }

        List<Evidence> originalEvidences = evidenceRepository.findByIncidentId(incidentId);
        double similarityScore = calculateVisualSimilarity(originalEvidences, resolutionEvidence);

        VerificationStatus verificationStatus;
        if (similarityScore >= 0.75) {
            verificationStatus = VerificationStatus.VERIFIED;
        } else if (similarityScore >= 0.40) {
            verificationStatus = VerificationStatus.MANUAL_REVIEW;
        } else {
            verificationStatus = VerificationStatus.FAILED;
        }

        String finalRemarks = request.getRemarks() != null ? request.getRemarks() : "";
        if (verificationStatus == VerificationStatus.VERIFIED) {
            finalRemarks += " (Evidence supports resolution)";
        } else if (verificationStatus == VerificationStatus.MANUAL_REVIEW) {
            finalRemarks += " (Manual review recommended)";
        } else {
            finalRemarks += " (Insufficient evidence to verify resolution)";
        }

        Resolution resolution = new Resolution();
        resolution.setIncident(incident);
        resolution.setOfficer(officer);
        resolution.setResolutionEvidence(resolutionEvidence);
        resolution.setResolutionSimilarityScore(similarityScore);
        resolution.setVerificationStatus(verificationStatus);
        resolution.setRemarks(finalRemarks);

        Resolution saved = resolutionRepository.save(resolution);

        if (verificationStatus == VerificationStatus.VERIFIED) {
            // Update incident lifecycle status to RESOLVED only when verified!
            incident.setStatus(IncidentStatus.RESOLVED);
            incidentRepository.save(incident);

            // Record auditable transition action
            IncidentAction action = new IncidentAction();
            action.setIncident(incident);
            action.setOfficer(officer);
            action.setActionType(ActionType.RESOLVED);
            action.setRemarks("Resolution evidence submitted and verified (" + verificationStatus + "): " + finalRemarks);
            incidentActionRepository.save(action);

            // Publish notification for reporting resident
            if (incident.getReporter() != null) {
                eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                        incident.getReporter().getId(),
                        com.nirvaha.nirvaha_backend.entity.NotificationEventType.RESOLUTION_SUBMITTED,
                        "Resolution Submitted: Incident #" + incident.getId(),
                        "Resolution evidence has been submitted for your reported incident: " + (incident.getTitle() != null ? incident.getTitle() : "Civic Issue") + ". Please review and verify the resolution.",
                        incident.getId(),
                        "incident-" + incident.getId() + "-resolution-" + saved.getId()
                ));
            }
        } else {
            // For MANUAL_REVIEW or FAILED: incident remains in its current status (e.g. IN_PROGRESS)
            IncidentAction action = new IncidentAction();
            action.setIncident(incident);
            action.setOfficer(officer);
            action.setActionType(ActionType.PROGRESS_UPDATE);
            action.setRemarks("Resolution evidence submitted (" + verificationStatus + "): " + finalRemarks);
            incidentActionRepository.save(action);
        }

        return ResolutionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ResolutionResponse getResolutionForIncident(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));
        incidentAccessValidator.verifyIncidentAccess(incident);
        return getResolutionByIncidentId(incidentId);
    }

    @Transactional(readOnly = true)
    public ResolutionResponse getResolutionByIncidentId(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));
        incidentAccessValidator.verifyIncidentAccess(incident);
        Resolution resolution = resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Resolution for incident ID " + incidentId + " not found"));
        return ResolutionResponse.fromEntity(resolution);
    }

    private User getAuthenticatedUserOrFallback(Long requestOfficerId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userRepository.findByEmail(auth.getName())
                    .orElseGet(() -> userRepository.findById(requestOfficerId)
                            .orElseThrow(() -> new ResourceNotFoundException("User with ID " + requestOfficerId + " not found")));
        }
        return userRepository.findById(requestOfficerId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + requestOfficerId + " not found"));
    }

    private double calculateVisualSimilarity(List<Evidence> originals, Evidence resolutionEvidence) {
        if (originals == null || originals.isEmpty() || resolutionEvidence == null) {
            return 0.50;
        }
        Evidence original = originals.get(0);

        double distanceMeters = HaversineDistance.calculateDistanceMeters(
                original.getLatitude(), original.getLongitude(),
                resolutionEvidence.getLatitude(), resolutionEvidence.getLongitude()
        );

        double spatialScore = Math.max(0.0, 1.0 - (distanceMeters / 500.0));
        double sizeDifferenceRatio = Math.abs(original.getFileSize() - resolutionEvidence.getFileSize()) / (double) Math.max(original.getFileSize(), 1);
        double sizeScore = Math.max(0.0, 1.0 - sizeDifferenceRatio);

        return Math.round(((spatialScore * 0.6) + (sizeScore * 0.4)) * 100.0) / 100.0;
    }
}
