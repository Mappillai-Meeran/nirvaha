package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.dto.CreateIncidentRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentResponse;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentServiceTracking;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.entity.User;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final AdministrativeAreaService administrativeAreaService;
    private final ImdWeatherAdapter imdWeatherAdapter;
    private final ServiceRuleService serviceRuleService;
    private final LanguageService languageService;
    private final com.nirvaha.nirvaha_backend.security.IncidentAccessValidator incidentAccessValidator;
    private final RoadReferenceService roadReferenceService;

    public IncidentService(IncidentRepository incidentRepository,
                           UserRepository userRepository,
                           AdministrativeAreaService administrativeAreaService,
                           ImdWeatherAdapter imdWeatherAdapter,
                           ServiceRuleService serviceRuleService,
                           LanguageService languageService,
                           com.nirvaha.nirvaha_backend.security.IncidentAccessValidator incidentAccessValidator,
                           RoadReferenceService roadReferenceService) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.administrativeAreaService = administrativeAreaService;
        this.imdWeatherAdapter = imdWeatherAdapter;
        this.serviceRuleService = serviceRuleService;
        this.languageService = languageService;
        this.incidentAccessValidator = incidentAccessValidator;
        this.roadReferenceService = roadReferenceService;
    }

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User reporter;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            reporter = userRepository.findByEmail(auth.getName())
                    .orElseGet(() -> userRepository.findById(request.getReporterId())
                            .orElseThrow(() -> new ResourceNotFoundException("User with ID " + request.getReporterId() + " not found")));
        } else {
            reporter = userRepository.findById(request.getReporterId())
                    .orElseThrow(() -> new ResourceNotFoundException("User with ID " + request.getReporterId() + " not found"));
        }

        Incident incident = new Incident();
        incident.setTitle(request.getTitle());
        incident.setCategory(request.getCategory());
        incident.setDescription(request.getDescription());
        incident.setLatitude(request.getLatitude());
        incident.setLongitude(request.getLongitude());
        incident.setAddress(request.getAddress());
        incident.setSeverity(request.getSeverity());
        // Only the workflow service may move a case through its lifecycle.
        // A report submitted by a citizen always begins in the reported state.
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setReporter(reporter);

        // Multilingual Access & Civic Normalization
        languageService.processIncidentMultilingualSupport(incident, request.getOriginalLanguage());

        // Perform Geospatial Point-In-Polygon Ward Assignment
        administrativeAreaService.assignIncidentToWard(incident);

        Incident saved = incidentRepository.save(incident);

        // Attach Policy SLA Rule Tracking
        IncidentServiceTracking tracking = serviceRuleService.evaluateServiceTracking(saved);

        IncidentResponse response = IncidentResponse.fromEntity(saved);
        if (tracking != null && tracking.getServiceRule() != null) {
            response.setServiceRuleDescription(tracking.getServiceRule().getServiceDescription());
            response.setSlaStatus(tracking.getSlaStatus() != null ? tracking.getSlaStatus().name() : "NOT_APPLICABLE");
            response.setDueAt(tracking.getDueAt());
            response.setIsOfficialSla(tracking.getServiceRule().getIsOfficialSla());
            response.setSlaVerificationStatus(tracking.getServiceRule().getVerificationStatus());
            response.setTargetDays(tracking.getServiceRule().getTargetDays());
            response.setWarningThresholdDays(tracking.getServiceRule().getWarningThresholdDays());
            response.setEscalationLevel(tracking.getEscalationLevel());
            response.setApproachingNotifiedAt(tracking.getApproachingNotifiedAt());
            response.setOverdueNotifiedAt(tracking.getOverdueNotifiedAt());
            if (tracking.getDueAt() != null) {
                response.setRemainingDays(Duration.between(LocalDateTime.now(), tracking.getDueAt()).toDays());
            }
        }
        response.setWeatherContext(imdWeatherAdapter.generateRainfallContextForIncident(saved));

        if (saved.getLatitude() != null && saved.getLongitude() != null) {
            com.nirvaha.nirvaha_backend.dto.RoadMatchContext roadMatch = roadReferenceService.findNearestRoad(saved.getLatitude(), saved.getLongitude());
            if (roadMatch != null && Boolean.TRUE.equals(roadMatch.getRoadFound())) {
                response.setNearestRoadName(roadMatch.getRoadName());
                response.setNearestRoadDistanceMeters(roadMatch.getDistanceMeters());
                response.setNearestRoadConfidence(roadMatch.getConfidence());
                response.setRoadContextExplanation(roadMatch.getQualityExplanation());
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getAllIncidents() {
        return getAllIncidents(false);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getAllIncidents(boolean includeSimulated) {
        List<Incident> incidents = includeSimulated
                ? incidentRepository.findAll()
                : incidentRepository.findOfficialIncidents();
        return incidents.stream()
                .map(this::mapToEnrichedResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> getMyReports() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required to view your reports");
        }
        String email = auth.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found with email: " + email));

        List<Incident> incidents = incidentRepository.findByReporterIdOrderByCreatedAtDesc(currentUser.getId());
        return incidents.stream()
                .map(this::mapToEnrichedResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident with ID " + id + " not found"));

        incidentAccessValidator.verifyIncidentAccess(incident);

        return mapToEnrichedResponse(incident);
    }

    public void verifyIncidentAccess(Incident incident) {
        incidentAccessValidator.verifyIncidentAccess(incident);
    }

    private IncidentResponse mapToEnrichedResponse(Incident incident) {
        IncidentResponse response = IncidentResponse.fromEntity(incident);
        response.setWeatherContext(imdWeatherAdapter.generateRainfallContextForIncident(incident));

        IncidentServiceTracking tracking = serviceRuleService.getOrEvaluateTrackingReadOnly(incident);
        if (tracking != null && tracking.getServiceRule() != null) {
            response.setServiceRuleDescription(tracking.getServiceRule().getServiceDescription());
            response.setSlaStatus(tracking.getSlaStatus() != null ? tracking.getSlaStatus().name() : "NOT_APPLICABLE");
            response.setDueAt(tracking.getDueAt());
            response.setIsOfficialSla(tracking.getServiceRule().getIsOfficialSla());
            response.setSlaVerificationStatus(tracking.getServiceRule().getVerificationStatus());
            response.setTargetDays(tracking.getServiceRule().getTargetDays());
            response.setWarningThresholdDays(tracking.getServiceRule().getWarningThresholdDays());
            response.setEscalationLevel(tracking.getEscalationLevel());
            response.setApproachingNotifiedAt(tracking.getApproachingNotifiedAt());
            response.setOverdueNotifiedAt(tracking.getOverdueNotifiedAt());
            if (tracking.getDueAt() != null) {
                response.setRemainingDays(Duration.between(LocalDateTime.now(), tracking.getDueAt()).toDays());
            }
        }

        if (incident.getLatitude() != null && incident.getLongitude() != null) {
            com.nirvaha.nirvaha_backend.dto.RoadMatchContext roadMatch = roadReferenceService.findNearestRoad(incident.getLatitude(), incident.getLongitude());
            if (roadMatch != null && Boolean.TRUE.equals(roadMatch.getRoadFound())) {
                response.setNearestRoadName(roadMatch.getRoadName());
                response.setNearestRoadDistanceMeters(roadMatch.getDistanceMeters());
                response.setNearestRoadConfidence(roadMatch.getConfidence());
                response.setRoadContextExplanation(roadMatch.getQualityExplanation());
            }
        }

        return response;
    }
}
