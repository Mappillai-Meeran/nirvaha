package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ResolutionServiceTest {

    @Autowired
    private ResolutionService resolutionService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserService userService;

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private IncidentWorkflowService incidentWorkflowService;

    @Autowired
    private ResolutionRepository resolutionRepository;

    @Autowired
    private IncidentActionRepository incidentActionRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    private Long citizenId;
    private Long officerId;
    private Long incidentId;
    private Long originalEvidenceId;
    private Long resolutionEvidenceId;

    @BeforeEach
    void setUp() {
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        UserResponse citizen = userService.createUser(new CreateUserRequest(
                "Citizen Alex", "alex.citizen@example.com", "pass123", UserRole.CITIZEN
        ));
        citizenId = citizen.getId();

        UserResponse officer = userService.createUser(new CreateUserRequest(
                "Officer Sarah", "sarah.officer@puducherry.gov.in", "pass123", UserRole.OFFICER
        ));
        officerId = officer.getId();

        IncidentResponse incident = incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Severe Pothole", "Pothole on Main St",
                IncidentCategory.POTHOLE, IncidentSeverity.HIGH,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Main St"
        ));
        incidentId = incident.getId();

        // Add action to move incident to IN_PROGRESS
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officerId, ActionType.STARTED, "Work started on pothole"
        ));

        // Original citizen evidence
        EvidenceResponse origEv = evidenceService.createEvidence(new CreateEvidenceRequest(
                incidentId, EvidenceType.PHOTO, "http://example.com/orig.jpg",
                "orig.jpg", "image/jpeg", 2048L, LocalDateTime.now(),
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Original Pothole Photo"
        ));
        originalEvidenceId = origEv.getId();

        // Resolution evidence (nearby location)
        EvidenceResponse resEv = evidenceService.createEvidence(new CreateEvidenceRequest(
                incidentId, EvidenceType.PHOTO, "http://example.com/res.jpg",
                "res.jpg", "image/jpeg", 2048L, LocalDateTime.now(),
                new BigDecimal("11.9417"), new BigDecimal("79.8084"), "Repaired Pothole Photo"
        ));
        resolutionEvidenceId = resEv.getId();
    }

    @Test
    void test1_shouldVerifyResolutionSuccessfullyWhenHighSimilarity() {
        CreateResolutionRequest request = new CreateResolutionRequest(
                officerId, resolutionEvidenceId, "Road resurfacing completed."
        );

        ResolutionResponse response = resolutionService.createResolution(incidentId, request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getIncidentId()).isEqualTo(incidentId);
        assertThat(response.getOfficerId()).isEqualTo(officerId);
        assertThat(response.getOfficerName()).isEqualTo("Officer Sarah");
        assertThat(response.getResolutionEvidenceId()).isEqualTo(resolutionEvidenceId);
        assertThat(response.getResolutionSimilarityScore()).isGreaterThanOrEqualTo(0.80);
        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(response.getRemarks()).contains("Evidence supports resolution");
    }

    @Test
    void test2_shouldAssignManualReviewForMediumSimilarity() {
        // Create resolution evidence with further distance (~300 meters away)
        EvidenceResponse mediumEv = evidenceService.createEvidence(new CreateEvidenceRequest(
                incidentId, EvidenceType.DOCUMENT, "http://example.com/doc.pdf",
                "doc.pdf", "application/pdf", 1024L, LocalDateTime.now(),
                new BigDecimal("11.9440"), new BigDecimal("79.8100"), "Work completion document"
        ));

        CreateResolutionRequest request = new CreateResolutionRequest(
                officerId, mediumEv.getId(), "Document evidence submitted."
        );

        ResolutionResponse response = resolutionService.createResolution(incidentId, request);

        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.MANUAL_REVIEW);
        assertThat(response.getRemarks()).contains("Manual review recommended");
    }

    @Test
    void test3_shouldAssignFailedForLowSimilarity() {
        // Far away evidence (> 5km away)
        EvidenceResponse farEv = evidenceService.createEvidence(new CreateEvidenceRequest(
                incidentId, EvidenceType.DOCUMENT, "http://example.com/far.pdf",
                "far.pdf", "application/pdf", 512L, LocalDateTime.now(),
                new BigDecimal("11.9800"), new BigDecimal("79.8500"), "Unrelated far photo"
        ));

        CreateResolutionRequest request = new CreateResolutionRequest(
                officerId, farEv.getId(), "Low similarity photo."
        );

        ResolutionResponse response = resolutionService.createResolution(incidentId, request);

        assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.FAILED);
        assertThat(response.getRemarks()).contains("Insufficient evidence to verify resolution");
    }

    @Test
    void test4_shouldThrowExceptionForMissingIncident() {
        assertThatThrownBy(() -> resolutionService.createResolution(99999L, new CreateResolutionRequest(
                officerId, resolutionEvidenceId, "Remarks"
        )))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident with ID 99999 not found");
    }

    @Test
    void test5_shouldThrowExceptionForMissingOfficer() {
        assertThatThrownBy(() -> resolutionService.createResolution(incidentId, new CreateResolutionRequest(
                88888L, resolutionEvidenceId, "Remarks"
        )))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with ID 88888 not found");
    }

    @Test
    void test6_shouldPreventCitizenFromSubmittingResolution() {
        assertThatThrownBy(() -> resolutionService.createResolution(incidentId, new CreateResolutionRequest(
                citizenId, resolutionEvidenceId, "Citizen trying to submit resolution"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not authorized to submit resolution evidence");
    }

    @Test
    void test7_shouldThrowExceptionForMissingEvidence() {
        assertThatThrownBy(() -> resolutionService.createResolution(incidentId, new CreateResolutionRequest(
                officerId, 77777L, "Remarks"
        )))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Evidence with ID 77777 not found");
    }

    @Test
    void test8_shouldRejectEvidenceBelongingToAnotherIncident() {
        // Create secondary incident
        IncidentResponse incident2 = incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Garbage Dump", "Garbage overflow",
                IncidentCategory.GARBAGE, IncidentSeverity.LOW,
                new BigDecimal("11.9400"), new BigDecimal("79.8000"), "Heritage Town"
        ));

        // Evidence created for incident 2
        EvidenceResponse ev2 = evidenceService.createEvidence(new CreateEvidenceRequest(
                incident2.getId(), EvidenceType.PHOTO, "http://example.com/other.jpg",
                "other.jpg", "image/jpeg", 1024L, LocalDateTime.now(),
                new BigDecimal("11.9400"), new BigDecimal("79.8000"), "Garbage Photo"
        ));

        // Attempting to use ev2 to resolve incident 1 must fail
        assertThatThrownBy(() -> resolutionService.createResolution(incidentId, new CreateResolutionRequest(
                officerId, ev2.getId(), "Cross incident evidence"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to incident " + incidentId);
    }

    @Test
    void test9_shouldRejectResolutionForUnverifiedReportedIncident() {
        IncidentResponse reportedInc = incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Unverified Inc", "Unverified report",
                IncidentCategory.STREETLIGHT, IncidentSeverity.LOW,
                new BigDecimal("11.9300"), new BigDecimal("79.8300"), "Loc"
        ));

        EvidenceResponse evReported = evidenceService.createEvidence(new CreateEvidenceRequest(
                reportedInc.getId(), EvidenceType.PHOTO, "http://example.com/rep.jpg",
                "rep.jpg", "image/jpeg", 1024L, LocalDateTime.now(),
                new BigDecimal("11.9300"), new BigDecimal("79.8300"), "Photo"
        ));

        assertThatThrownBy(() -> resolutionService.createResolution(reportedInc.getId(), new CreateResolutionRequest(
                officerId, evReported.getId(), "Resolving unverified incident"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resolution evidence cannot be submitted for an unverified REPORTED incident");
    }

    @Test
    void test10_shouldEnsureScoreIsStrictlyBoundedBetween0And1() {
        CreateResolutionRequest request = new CreateResolutionRequest(
                officerId, resolutionEvidenceId, "Check bounds"
        );

        ResolutionResponse response = resolutionService.createResolution(incidentId, request);

        assertThat(response.getResolutionSimilarityScore()).isGreaterThanOrEqualTo(0.0);
        assertThat(response.getResolutionSimilarityScore()).isLessThanOrEqualTo(1.0);
    }

    @Test
    void test11_shouldRetrieveResolutionSuccessfully() {
        resolutionService.createResolution(incidentId, new CreateResolutionRequest(
                officerId, resolutionEvidenceId, "Resolution remarks"
        ));

        ResolutionResponse fetched = resolutionService.getResolutionForIncident(incidentId);

        assertThat(fetched.getIncidentId()).isEqualTo(incidentId);
        assertThat(fetched.getOfficerId()).isEqualTo(officerId);
        assertThat(fetched.getResolutionEvidenceId()).isEqualTo(resolutionEvidenceId);
    }

    @Test
    void test12_shouldThrowExceptionWhenResolutionNotFound() {
        assertThatThrownBy(() -> resolutionService.getResolutionForIncident(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident with ID 99999 not found");
    }
}
