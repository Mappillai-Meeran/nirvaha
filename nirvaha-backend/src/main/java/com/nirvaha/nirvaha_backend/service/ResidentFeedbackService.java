package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.DisputeReviewRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentFeedbackResponse;
import com.nirvaha.nirvaha_backend.dto.ResidentFeedbackRequest;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.DuplicateResourceException;
import com.nirvaha.nirvaha_backend.exception.InvalidWorkflowException;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.IncidentActionRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentFeedbackRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.ResolutionRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.security.IncidentAccessValidator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResidentFeedbackService {

    private final IncidentFeedbackRepository feedbackRepository;
    private final IncidentRepository incidentRepository;
    private final ResolutionRepository resolutionRepository;
    private final IncidentActionRepository actionRepository;
    private final UserRepository userRepository;
    private final IncidentAccessValidator accessValidator;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public ResidentFeedbackService(IncidentFeedbackRepository feedbackRepository,
                                   IncidentRepository incidentRepository,
                                   ResolutionRepository resolutionRepository,
                                   IncidentActionRepository actionRepository,
                                   UserRepository userRepository,
                                   IncidentAccessValidator accessValidator,
                                   org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.feedbackRepository = feedbackRepository;
        this.incidentRepository = incidentRepository;
        this.resolutionRepository = resolutionRepository;
        this.actionRepository = actionRepository;
        this.userRepository = userRepository;
        this.accessValidator = accessValidator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public IncidentFeedbackResponse submitFeedback(Long incidentId, ResidentFeedbackRequest request) {
        if (request == null || request.getFeedbackType() == null) {
            throw new IllegalArgumentException("Feedback type is required (CONFIRMED or DISPUTED)");
        }

        User currentUser = getAuthenticatedUser();

        // Officers and Admins must not impersonate residents through resident feedback endpoints
        if (currentUser.getRole() == UserRole.OFFICER || currentUser.getRole() == UserRole.ADMIN) {
            throw new AccessDeniedException("Officers and administrators cannot submit resident feedback");
        }

        Incident incident = incidentRepository.findByIdForUpdate(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));

        // Only the genuine reporting resident can submit feedback
        if (incident.getReporter() == null || !incident.getReporter().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You can only submit feedback for incidents you personally reported");
        }

        // Reject feedback for incidents that are not in the RESOLVED state
        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new InvalidWorkflowException("Feedback can only be submitted for RESOLVED incidents. Current status: " + incident.getStatus());
        }

        // Reference the active resolution without modifying it
        Resolution activeResolution = resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId).orElse(null);
        if (activeResolution != null && activeResolution.getIncident() != null && !activeResolution.getIncident().getId().equals(incident.getId())) {
            throw new InvalidWorkflowException("Active resolution does not belong to incident ID " + incidentId);
        }

        // Prevent duplicate or conflicting feedback submissions scoped to the active resolution
        if (activeResolution != null) {
            Optional<IncidentFeedback> existingForResolution = feedbackRepository.findByResolutionId(activeResolution.getId());
            if (existingForResolution.isPresent()) {
                IncidentFeedback prev = existingForResolution.get();
                if (prev.getFeedbackType() == FeedbackType.CONFIRMED) {
                    throw new DuplicateResourceException("Resolution has already been confirmed and closed by the reporting resident");
                }
                throw new DuplicateResourceException("Feedback has already been submitted for this resolution");
            }
        } else {
            Optional<IncidentFeedback> latestFeedback = feedbackRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId);
            if (latestFeedback.isPresent()) {
                IncidentFeedback prev = latestFeedback.get();
                if (prev.getFeedbackType() == FeedbackType.CONFIRMED) {
                    throw new DuplicateResourceException("Resolution has already been confirmed and closed by the reporting resident");
                }
                if (prev.getFeedbackType() == FeedbackType.DISPUTED && !Boolean.TRUE.equals(prev.getReviewed())) {
                    throw new DuplicateResourceException("A dispute is already pending review for this incident");
                }
            }
        }

        // Validate explanation input
        String cleanExplanation = request.getExplanation() != null ? request.getExplanation().trim() : null;
        if (request.getFeedbackType() == FeedbackType.DISPUTED) {
            if (cleanExplanation == null || cleanExplanation.isEmpty()) {
                throw new IllegalArgumentException("A mandatory explanation is required when disputing an incident resolution");
            }
            if (cleanExplanation.length() > 2000) {
                throw new IllegalArgumentException("Explanation exceeds maximum allowed length of 2000 characters");
            }
        } else if (cleanExplanation != null && cleanExplanation.length() > 2000) {
            throw new IllegalArgumentException("Remarks exceed maximum allowed length of 2000 characters");
        }

        IncidentFeedback feedback = new IncidentFeedback();
        feedback.setIncident(incident);
        feedback.setResident(currentUser);
        feedback.setResolution(activeResolution);
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setExplanation(cleanExplanation);
        feedback.setCreatedAt(LocalDateTime.now());

        if (request.getFeedbackType() == FeedbackType.CONFIRMED) {
            feedback.setStatus("CONFIRMED");
            feedback.setReviewed(true);
            IncidentFeedback saved = feedbackRepository.save(feedback);

            // Append audit event for resident confirmation
            recordAction(incident, currentUser, ActionType.RESIDENT_CONFIRMED,
                    "Resident confirmed resolution" + (cleanExplanation != null ? ": " + cleanExplanation : " to their satisfaction."));

            // Notify responsible officer
            User responsibleOfficer = resolveResponsibleOfficer(incident, activeResolution);
            if (responsibleOfficer != null) {
                eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                        responsibleOfficer.getId(),
                        com.nirvaha.nirvaha_backend.entity.NotificationEventType.RESOLUTION_CONFIRMED,
                        "Resolution Confirmed: Incident #" + incident.getId(),
                        "The reporting resident has confirmed the resolution for incident #" + incident.getId() + ": " + (incident.getTitle() != null ? incident.getTitle() : "Civic Issue") + ".",
                        incident.getId(),
                        "feedback-" + saved.getId() + "-confirmed"
                ));
            }

            return IncidentFeedbackResponse.fromEntity(saved);
        } else {
            // FeedbackType.DISPUTED
            feedback.setStatus("PENDING_REVIEW");
            feedback.setReviewed(false);
            IncidentFeedback saved = feedbackRepository.save(feedback);

            // Append audit event for resident dispute
            recordAction(incident, currentUser, ActionType.RESIDENT_DISPUTED,
                    "Resident disputed resolution: " + cleanExplanation);

            // Route disputed incident into the existing supported review/reopening workflow (REPORTED status)
            incident.setStatus(IncidentStatus.REPORTED);
            incidentRepository.save(incident);

            // Record reopening action in audit trail
            recordAction(incident, currentUser, ActionType.REOPENED,
                    "Incident reopened following resident dispute. Routed to supervisory review queue.");

            // Notify responsible officer
            User responsibleOfficer = resolveResponsibleOfficer(incident, activeResolution);
            if (responsibleOfficer != null) {
                eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                        responsibleOfficer.getId(),
                        com.nirvaha.nirvaha_backend.entity.NotificationEventType.RESOLUTION_DISPUTED,
                        "Resolution Disputed: Incident #" + incident.getId(),
                        "Resident disputed resolution for incident #" + incident.getId() + ": '" + cleanExplanation + "'. Case reopened.",
                        incident.getId(),
                        "feedback-" + saved.getId() + "-disputed"
                ));
            }

            return IncidentFeedbackResponse.fromEntity(saved);
        }
    }

    @Transactional
    public IncidentFeedbackResponse reviewDispute(Long incidentId, Long feedbackId, DisputeReviewRequest request) {
        if (request == null || request.getReviewNotes() == null || request.getReviewNotes().trim().isEmpty()) {
            throw new IllegalArgumentException("Review notes are mandatory for dispute review");
        }
        if (request.getReviewNotes().trim().length() > 2000) {
            throw new IllegalArgumentException("Review notes exceed maximum allowed length of 2000 characters");
        }

        User actingOfficer = getAuthenticatedUser();
        if (actingOfficer.getRole() != UserRole.OFFICER && actingOfficer.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only authorized officers and administrators can review resident disputes");
        }

        Incident incident = incidentRepository.findByIdForUpdate(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));

        IncidentFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback record with ID " + feedbackId + " not found"));

        if (!feedback.getIncident().getId().equals(incidentId)) {
            throw new IllegalArgumentException("Feedback ID " + feedbackId + " does not belong to incident ID " + incidentId);
        }

        if (feedback.getResolution() != null && feedback.getResolution().getIncident() != null && !feedback.getResolution().getIncident().getId().equals(incidentId)) {
            throw new IllegalArgumentException("Feedback resolution does not belong to incident ID " + incidentId);
        }

        if (feedback.getFeedbackType() != FeedbackType.DISPUTED) {
            throw new IllegalArgumentException("Only DISPUTED feedback requires supervisory officer review");
        }

        if (Boolean.TRUE.equals(feedback.getReviewed())) {
            throw new InvalidWorkflowException("Dispute has already been reviewed with status: " + feedback.getStatus());
        }

        if (incident.getStatus() != IncidentStatus.REPORTED) {
            throw new InvalidWorkflowException("Dispute review can only be performed when incident is in REPORTED state awaiting review. Current status: " + incident.getStatus());
        }

        // Determine disposition
        String disposition = request.getDisposition() != null ? request.getDisposition().trim().toUpperCase() : "ACCEPTED_FOR_REWORK";
        if (!"ACCEPTED_FOR_REWORK".equals(disposition) && !"UNDER_SUPERVISION".equals(disposition) && !"DISMISSED".equals(disposition)) {
            throw new IllegalArgumentException("Invalid review disposition: " + disposition + ". Supported dispositions: ACCEPTED_FOR_REWORK, UNDER_SUPERVISION, DISMISSED");
        }

        // Workflow outcome based on disposition
        if ("DISMISSED".equals(disposition)) {
            // Verify that an existing resolution record with evidence exists before affirming it
            Resolution originalResolution = feedback.getResolution() != null
                    ? feedback.getResolution()
                    : resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId).orElse(null);

            if (originalResolution == null || originalResolution.getResolutionEvidence() == null) {
                throw new InvalidWorkflowException("Cannot dismiss dispute: incident ID " + incidentId + " does not have an existing resolution with evidence to affirm");
            }

            if (originalResolution.getIncident() != null && !originalResolution.getIncident().getId().equals(incidentId)) {
                throw new IllegalArgumentException("Resolution record does not belong to incident ID " + incidentId);
            }

            // Resolution affirmed: restore incident status to RESOLVED without falsely claiming new repair verification
            incident.setStatus(IncidentStatus.RESOLVED);
            incidentRepository.save(incident);
            recordAction(incident, actingOfficer, ActionType.DISPUTE_REVIEWED,
                    "Dispute dismissed: officer affirmed original resolution. Notes: " + request.getReviewNotes().trim());
        } else if ("ACCEPTED_FOR_REWORK".equals(disposition)) {
            // Valid dispute: remains in or moved to REPORTED for assignment/rework queue
            incident.setStatus(IncidentStatus.REPORTED);
            incidentRepository.save(incident);
            recordAction(incident, actingOfficer, ActionType.DISPUTE_REVIEWED,
                    "Dispute accepted for rework by officer. Notes: " + request.getReviewNotes().trim());
        } else {
            // UNDER_SUPERVISION: remains under supervisory field investigation
            incident.setStatus(IncidentStatus.REPORTED);
            incidentRepository.save(incident);
            recordAction(incident, actingOfficer, ActionType.DISPUTE_REVIEWED,
                    "Dispute assigned for supervisory on-site inspection. Notes: " + request.getReviewNotes().trim());
        }

        feedback.setReviewed(true);
        feedback.setReviewedBy(actingOfficer);
        feedback.setReviewedAt(LocalDateTime.now());
        feedback.setReviewNotes(request.getReviewNotes().trim());
        feedback.setStatus(disposition);

        IncidentFeedback saved = feedbackRepository.save(feedback);

        // Notify reporting resident of review outcome
        if (incident.getReporter() != null) {
            String title;
            String message;
            String dedupKey;
            if ("ACCEPTED_FOR_REWORK".equals(disposition)) {
                title = "Dispute Accepted for Rework: Incident #" + incident.getId();
                message = "Your dispute for incident #" + incident.getId() + " has been reviewed and accepted for field rework.";
                dedupKey = "feedback-" + saved.getId() + "-review-rework";
            } else if ("UNDER_SUPERVISION".equals(disposition)) {
                title = "Dispute Under Supervision: Incident #" + incident.getId();
                message = "Your dispute for incident #" + incident.getId() + " is scheduled for supervisory field inspection.";
                dedupKey = "feedback-" + saved.getId() + "-review-supervision";
            } else {
                title = "Dispute Review Completed: Incident #" + incident.getId();
                message = "Your dispute for incident #" + incident.getId() + " was reviewed and dismissed. Original resolution affirmed. Notes: " + request.getReviewNotes().trim();
                dedupKey = "feedback-" + saved.getId() + "-review-dismissed";
            }
            eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                    incident.getReporter().getId(),
                    com.nirvaha.nirvaha_backend.entity.NotificationEventType.DISPUTE_REVIEWED,
                    title,
                    message,
                    incident.getId(),
                    dedupKey
            ));
        }

        return IncidentFeedbackResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentFeedbackResponse> getFeedbackForIncident(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));
        accessValidator.verifyIncidentAccess(incident);

        return feedbackRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
                .map(IncidentFeedbackResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required to perform this action");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found in municipal records"));
    }

    private void recordAction(Incident incident, User user, ActionType actionType, String remarks) {
        IncidentAction action = new IncidentAction();
        action.setIncident(incident);
        action.setOfficer(user);
        action.setActionType(actionType);
        action.setRemarks(remarks);
        actionRepository.save(action);
    }

    private User resolveResponsibleOfficer(Incident incident, Resolution activeResolution) {
        if (activeResolution != null && activeResolution.getOfficer() != null) {
            return activeResolution.getOfficer();
        }
        List<IncidentAction> actions = actionRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId());
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i).getOfficer() != null && actions.get(i).getOfficer().getRole() != UserRole.CITIZEN) {
                return actions.get(i).getOfficer();
            }
        }
        return null;
    }
}
