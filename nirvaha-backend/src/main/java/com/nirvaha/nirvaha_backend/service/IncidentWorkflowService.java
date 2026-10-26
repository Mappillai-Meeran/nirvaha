package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.CreateIncidentActionRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentActionResponse;
import com.nirvaha.nirvaha_backend.dto.IncidentWorkflowResponse;
import com.nirvaha.nirvaha_backend.entity.ActionType;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentAction;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.entity.Resolution;
import com.nirvaha.nirvaha_backend.entity.User;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.entity.VerificationStatus;
import com.nirvaha.nirvaha_backend.exception.InvalidWorkflowException;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.IncidentActionRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.ResolutionRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentWorkflowService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final IncidentActionRepository incidentActionRepository;
    private final ResolutionRepository resolutionRepository;
    private final com.nirvaha.nirvaha_backend.security.IncidentAccessValidator incidentAccessValidator;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public IncidentWorkflowService(IncidentRepository incidentRepository,
                                  UserRepository userRepository,
                                  IncidentActionRepository incidentActionRepository,
                                  ResolutionRepository resolutionRepository,
                                  com.nirvaha.nirvaha_backend.security.IncidentAccessValidator incidentAccessValidator,
                                  org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.incidentActionRepository = incidentActionRepository;
        this.resolutionRepository = resolutionRepository;
        this.incidentAccessValidator = incidentAccessValidator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Incident assignOfficer(Long incidentId, Long officerId) {
        Incident incident = getIncidentOrThrow(incidentId);

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new InvalidWorkflowException("Cannot assign an officer to a resolved incident.");
        }

        User actingUser = getAuthenticatedUserOrFallback(officerId);
        verifyGovernmentRole(actingUser);

        incident.setStatus(IncidentStatus.VERIFIED);
        Incident savedIncident = incidentRepository.save(incident);

        IncidentAction action = recordAction(savedIncident, actingUser, ActionType.ASSIGNED, "Assigned to officer ID " + actingUser.getId());

        // Publish notification for assigned officer
        eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                actingUser.getId(),
                com.nirvaha.nirvaha_backend.entity.NotificationEventType.INCIDENT_ASSIGNED,
                "Incident Assigned: #" + savedIncident.getId(),
                "You have been assigned to investigate incident #" + savedIncident.getId() + ": " + (savedIncident.getTitle() != null ? savedIncident.getTitle() : "Civic Issue"),
                savedIncident.getId(),
                "action-" + action.getId() + "-assigned-" + actingUser.getId()
        ));

        // Publish notification for reporting resident
        if (savedIncident.getReporter() != null) {
            eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                    savedIncident.getReporter().getId(),
                    com.nirvaha.nirvaha_backend.entity.NotificationEventType.INCIDENT_ASSIGNED,
                    "Officer Assigned: Incident #" + savedIncident.getId(),
                    "An officer has been assigned to investigate your reported incident: " + (savedIncident.getTitle() != null ? savedIncident.getTitle() : "Civic Issue"),
                    savedIncident.getId(),
                    "action-" + action.getId() + "-assigned-res-" + actingUser.getId()
            ));
        }

        return savedIncident;
    }

    @Transactional
    public Incident startWork(Long incidentId, Long officerId) {
        Incident incident = getIncidentOrThrow(incidentId);

        if (incident.getStatus() != IncidentStatus.VERIFIED && incident.getStatus() != IncidentStatus.REPORTED) {
            throw new InvalidWorkflowException("Work can only start when incident is REPORTED or VERIFIED. Current status: " + incident.getStatus());
        }

        User actingUser = getAuthenticatedUserOrFallback(officerId);
        verifyGovernmentRole(actingUser);

        incident.setStatus(IncidentStatus.IN_PROGRESS);
        Incident savedIncident = incidentRepository.save(incident);

        IncidentAction action = recordAction(savedIncident, actingUser, ActionType.STARTED, "Field operations started by officer ID " + actingUser.getId());

        // Publish notification for reporting resident
        if (savedIncident.getReporter() != null) {
            eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                    savedIncident.getReporter().getId(),
                    com.nirvaha.nirvaha_backend.entity.NotificationEventType.STATUS_CHANGED,
                    "Field Work Started: Incident #" + savedIncident.getId(),
                    "Field operations have commenced on your reported incident: " + (savedIncident.getTitle() != null ? savedIncident.getTitle() : "Civic Issue"),
                    savedIncident.getId(),
                    "action-" + action.getId() + "-status-IN_PROGRESS"
            ));
        }

        return savedIncident;
    }

    @Transactional
    public Incident resolveIncident(Long incidentId, Long officerId) {
        Incident incident = getIncidentOrThrow(incidentId);

        if (incident.getStatus() != IncidentStatus.IN_PROGRESS) {
            throw new InvalidWorkflowException("Incident can only be resolved when it is IN_PROGRESS. Current status: " + incident.getStatus());
        }

        User actingUser = getAuthenticatedUserOrFallback(officerId);
        verifyGovernmentRole(actingUser);

        // Confirm incident cannot become resolved without required resolution evidence
        Resolution resolution = resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId).orElse(null);
        if (resolution == null || resolution.getResolutionEvidence() == null
                || resolution.getResolutionEvidence().getFileUrl() == null
                || resolution.getResolutionEvidence().getFileUrl().trim().isEmpty()
                || resolution.getVerificationStatus() == com.nirvaha.nirvaha_backend.entity.VerificationStatus.FAILED) {
            throw new InvalidWorkflowException("Cannot resolve incident: verified resolution evidence must be submitted first");
        }
        if (resolution.getRemarks() == null || resolution.getRemarks().trim().isEmpty()) {
            throw new InvalidWorkflowException("Cannot resolve incident: resolution notes must not be blank");
        }

        incident.setStatus(IncidentStatus.RESOLVED);
        Incident savedIncident = incidentRepository.save(incident);

        recordAction(savedIncident, actingUser, ActionType.RESOLVED, "Resolved by officer ID " + actingUser.getId());
        return savedIncident;
    }

    @Transactional
    public IncidentActionResponse addAction(Long incidentId, CreateIncidentActionRequest request) {
        Incident incident = getIncidentOrThrow(incidentId);
        User actingUser = getAuthenticatedUserOrFallback(request.getOfficerId());
        verifyGovernmentRole(actingUser);

        if (request.getActionType() == ActionType.ASSIGNED) {
            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                throw new InvalidWorkflowException("Cannot assign an officer to a resolved incident.");
            }
            incident.setStatus(IncidentStatus.VERIFIED);
        } else if (request.getActionType() == ActionType.VERIFIED) {
            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                throw new InvalidWorkflowException("Cannot mark a resolved incident as verified. Reopen it first.");
            }
            incident.setStatus(IncidentStatus.VERIFIED);
        } else if (request.getActionType() == ActionType.STARTED) {
            if (incident.getStatus() != IncidentStatus.VERIFIED && incident.getStatus() != IncidentStatus.REPORTED) {
                throw new InvalidWorkflowException("Work can only start when incident is REPORTED or VERIFIED. Current status: " + incident.getStatus());
            }
            incident.setStatus(IncidentStatus.IN_PROGRESS);
        } else if (request.getActionType() == ActionType.PROGRESS_UPDATE) {
            if (incident.getStatus() != IncidentStatus.IN_PROGRESS) {
                throw new IllegalArgumentException("Progress updates can only be posted when incident is IN_PROGRESS");
            }
        } else if (request.getActionType() == ActionType.RESOLVED) {
            if (request.getRemarks() == null || request.getRemarks().trim().isEmpty()) {
                throw new IllegalArgumentException("Meaningful remarks are required to resolve an incident");
            }
            if (incident.getStatus() != IncidentStatus.IN_PROGRESS) {
                throw new InvalidWorkflowException("Incident can only be resolved when it is IN_PROGRESS. Current status: " + incident.getStatus());
            }
            Resolution resolution = resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId).orElse(null);
            if (resolution == null || resolution.getResolutionEvidence() == null
                    || resolution.getResolutionEvidence().getFileUrl() == null
                    || resolution.getResolutionEvidence().getFileUrl().trim().isEmpty()
                    || resolution.getVerificationStatus() == VerificationStatus.FAILED) {
                throw new InvalidWorkflowException("Cannot resolve incident: verified resolution evidence must be submitted first");
            }
            incident.setStatus(IncidentStatus.RESOLVED);
        } else if (request.getActionType() == ActionType.REOPENED) {
            if (incident.getStatus() != IncidentStatus.RESOLVED) {
                Resolution resolution = resolutionRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId).orElse(null);
                if (resolution == null || resolution.getVerificationStatus() != VerificationStatus.FAILED) {
                    throw new IllegalArgumentException("Only RESOLVED incidents can be reopened (or incidents with failed resolution verification)");
                }
            }
            incident.setStatus(IncidentStatus.REPORTED);
        }
        Incident savedIncident = incidentRepository.save(incident);

        IncidentAction action = new IncidentAction();
        action.setIncident(savedIncident);
        action.setOfficer(actingUser);
        action.setActionType(request.getActionType());
        action.setRemarks(request.getRemarks());
        IncidentAction saved = incidentActionRepository.save(action);

        if (request.getActionType() == ActionType.ASSIGNED) {
            // Publish notification for assigned officer
            eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                    actingUser.getId(),
                    com.nirvaha.nirvaha_backend.entity.NotificationEventType.INCIDENT_ASSIGNED,
                    "Incident Assigned: #" + savedIncident.getId(),
                    "You have been assigned to investigate incident #" + savedIncident.getId() + ": " + (savedIncident.getTitle() != null ? savedIncident.getTitle() : "Civic Issue"),
                    savedIncident.getId(),
                    "action-" + saved.getId() + "-assigned-" + actingUser.getId()
            ));

            // Publish notification for reporting resident
            if (savedIncident.getReporter() != null) {
                eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                        savedIncident.getReporter().getId(),
                        com.nirvaha.nirvaha_backend.entity.NotificationEventType.INCIDENT_ASSIGNED,
                        "Officer Assigned: Incident #" + savedIncident.getId(),
                        "An officer has been assigned to investigate your reported incident: " + (savedIncident.getTitle() != null ? savedIncident.getTitle() : "Civic Issue"),
                        savedIncident.getId(),
                        "action-" + saved.getId() + "-assigned-res-" + actingUser.getId()
                ));
            }
        } else if (request.getActionType() == ActionType.STARTED) {
            // Publish notification for reporting resident
            if (savedIncident.getReporter() != null) {
                eventPublisher.publishEvent(new com.nirvaha.nirvaha_backend.event.IncidentLifecycleNotificationEvent(
                        savedIncident.getReporter().getId(),
                        com.nirvaha.nirvaha_backend.entity.NotificationEventType.STATUS_CHANGED,
                        "Field Work Started: Incident #" + savedIncident.getId(),
                        "Field operations have commenced on your reported incident: " + (savedIncident.getTitle() != null ? savedIncident.getTitle() : "Civic Issue"),
                        savedIncident.getId(),
                        "action-" + saved.getId() + "-status-IN_PROGRESS"
                ));
            }
        }

        return IncidentActionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentActionResponse> getActionsForIncident(Long incidentId) {
        Incident incident = getIncidentOrThrow(incidentId);
        incidentAccessValidator.verifyIncidentAccess(incident);
        return incidentActionRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId).stream()
                .map(IncidentActionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncidentWorkflowResponse getWorkflowForIncident(Long incidentId) {
        Incident incident = getIncidentOrThrow(incidentId);
        incidentAccessValidator.verifyIncidentAccess(incident);
        List<IncidentActionResponse> actions = getActionsForIncident(incidentId);

        Long assignedOfficerId = null;
        String assignedOfficerName = null;
        for (int i = actions.size() - 1; i >= 0; i--) {
            IncidentActionResponse act = actions.get(i);
            if (act.getActionType() == ActionType.ASSIGNED) {
                assignedOfficerId = act.getOfficerId();
                assignedOfficerName = act.getOfficerName();
                break;
            }
        }

        return new IncidentWorkflowResponse(
                incident.getId(),
                incident.getStatus(),
                assignedOfficerId,
                assignedOfficerName,
                actions
        );
    }

    private Incident getIncidentOrThrow(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + incidentId + " not found"));
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

    private void verifyGovernmentRole(User user) {
        if (user.getRole() != UserRole.OFFICER && user.getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("User " + user.getId() + " is not authorized to perform government incident actions");
        }
    }

    private IncidentAction recordAction(Incident incident, User officer, ActionType actionType, String remarks) {
        IncidentAction action = new IncidentAction();
        action.setIncident(incident);
        action.setOfficer(officer);
        action.setActionType(actionType);
        action.setRemarks(remarks);
        return incidentActionRepository.save(action);
    }
}
