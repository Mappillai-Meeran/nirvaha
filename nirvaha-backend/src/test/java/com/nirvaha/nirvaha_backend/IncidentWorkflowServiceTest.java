package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.EvidenceRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentActionRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.ResolutionRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import com.nirvaha.nirvaha_backend.service.IncidentWorkflowService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class IncidentWorkflowServiceTest {

    @Autowired
    private IncidentWorkflowService incidentWorkflowService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserService userService;

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
    private Long officer1Id;
    private Long officer2Id;
    private Long incidentId;

    @BeforeEach
    void setUp() {
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        UserResponse citizen = userService.createUser(new CreateUserRequest(
                "Citizen Jane", "jane.citizen@example.com", "pass123", UserRole.CITIZEN
        ));
        citizenId = citizen.getId();

        UserResponse officer1 = userService.createUser(new CreateUserRequest(
                "Officer John", "john.officer@puducherry.gov.in", "pass123", UserRole.OFFICER
        ));
        officer1Id = officer1.getId();

        UserResponse officer2 = userService.createUser(new CreateUserRequest(
                "Officer Mary", "mary.officer@puducherry.gov.in", "pass123", UserRole.OFFICER
        ));
        officer2Id = officer2.getId();

        IncidentResponse incident = incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Broken Pipeline", "Water main burst near market",
                IncidentCategory.WATER_SUPPLY, IncidentSeverity.HIGH,
                new BigDecimal("11.9300"), new BigDecimal("79.8300"), "Market Road"
        ));
        incidentId = incident.getId();
    }

    @Test
    void test1_shouldCreateAssignedActionSuccessfully() {
        CreateIncidentActionRequest request = new CreateIncidentActionRequest(
                officer1Id, ActionType.ASSIGNED, "Assigned to Puducherry water response team."
        );

        IncidentActionResponse response = incidentWorkflowService.addAction(incidentId, request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getIncidentId()).isEqualTo(incidentId);
        assertThat(response.getOfficerId()).isEqualTo(officer1Id);
        assertThat(response.getOfficerName()).isEqualTo("Officer John");
        assertThat(response.getActionType()).isEqualTo(ActionType.ASSIGNED);
        assertThat(response.getRemarks()).isEqualTo("Assigned to Puducherry water response team.");
    }

    @Test
    void test2_shouldIdentifyCorrectCurrentAssignedOfficer() {
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.ASSIGNED, "Assigned to Officer John"
        ));

        IncidentWorkflowResponse workflow = incidentWorkflowService.getWorkflowForIncident(incidentId);

        assertThat(workflow.getAssignedOfficerId()).isEqualTo(officer1Id);
        assertThat(workflow.getAssignedOfficerName()).isEqualTo("Officer John");
    }

    @Test
    void test3_shouldChangeStatusToVerified() {
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.VERIFIED, "Field officer inspected and verified location"
        ));

        IncidentResponse updatedIncident = incidentService.getIncidentById(incidentId);
        assertThat(updatedIncident.getStatus()).isEqualTo(IncidentStatus.VERIFIED);
    }

    @Test
    void test4_shouldChangeStatusToInProgress() {
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.STARTED, "Excavation and pipe repair started"
        ));

        IncidentResponse updatedIncident = incidentService.getIncidentById(incidentId);
        assertThat(updatedIncident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    void test5_shouldAllowProgressUpdateOnlyWhileInProgress() {
        // Attempting PROGRESS_UPDATE while REPORTED must fail
        assertThatThrownBy(() -> incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.PROGRESS_UPDATE, "50% done"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Progress updates can only be posted when incident is IN_PROGRESS");

        // Start work first
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.STARTED, "Work started"
        ));

        // Now PROGRESS_UPDATE should succeed
        IncidentActionResponse response = incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.PROGRESS_UPDATE, "Pipeline replaced, testing pressure"
        ));
        assertThat(response.getActionType()).isEqualTo(ActionType.PROGRESS_UPDATE);
    }

    @Test
    void test6_shouldChangeStatusToResolved() {
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.STARTED, "Work started"
        ));

        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.RESOLVED, "Pipeline replaced, water supply fully restored."
        ));

        IncidentResponse updatedIncident = incidentService.getIncidentById(incidentId);
        assertThat(updatedIncident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
    }

    @Test
    void test7_shouldReopenResolvedIncidentToReported() {
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.RESOLVED, "Issue fixed"
        ));

        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.REOPENED, "Water leak recurring at joint"
        ));

        IncidentResponse updatedIncident = incidentService.getIncidentById(incidentId);
        assertThat(updatedIncident.getStatus()).isEqualTo(IncidentStatus.REPORTED);
    }

    @Test
    void test8_shouldThrowExceptionForMissingIncident() {
        assertThatThrownBy(() -> incidentWorkflowService.addAction(99999L, new CreateIncidentActionRequest(
                officer1Id, ActionType.ASSIGNED, "Remarks"
        )))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident with ID 99999 not found");
    }

    @Test
    void test9_shouldThrowExceptionForMissingOfficer() {
        assertThatThrownBy(() -> incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                88888L, ActionType.ASSIGNED, "Remarks"
        )))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with ID 88888 not found");
    }

    @Test
    void test10_shouldPreventCitizenFromPerformingOfficerActions() {
        assertThatThrownBy(() -> incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                citizenId, ActionType.ASSIGNED, "Citizen trying to assign"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not authorized to perform government incident actions");
    }

    @Test
    void test11_shouldRejectInvalidStatusTransitions() {
        // Cannot reopen an incident that is currently REPORTED
        assertThatThrownBy(() -> incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.REOPENED, "Reopening active incident"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only RESOLVED incidents can be reopened");
    }

    @Test
    void test12_shouldRejectResolutionWithoutMeaningfulRemarks() {
        assertThatThrownBy(() -> incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.RESOLVED, "   "
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Meaningful remarks are required to resolve an incident");
    }

    @Test
    void test13_shouldReturnActionHistoryChronologically() {
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.ASSIGNED, "Assigned"
        ));
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.VERIFIED, "Verified"
        ));
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.STARTED, "Started"
        ));

        List<IncidentActionResponse> history = incidentWorkflowService.getActionsForIncident(incidentId);

        assertThat(history).hasSize(3);
        assertThat(history.get(0).getActionType()).isEqualTo(ActionType.ASSIGNED);
        assertThat(history.get(1).getActionType()).isEqualTo(ActionType.VERIFIED);
        assertThat(history.get(2).getActionType()).isEqualTo(ActionType.STARTED);
    }

    @Test
    void test14_shouldDetermineCurrentOfficerFromLatestAssignedAction() {
        // Initial assignment to Officer 1
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer1Id, ActionType.ASSIGNED, "Assigned to Officer 1"
        ));

        // Reassignment to Officer 2
        incidentWorkflowService.addAction(incidentId, new CreateIncidentActionRequest(
                officer2Id, ActionType.ASSIGNED, "Reassigned to Officer 2"
        ));

        IncidentWorkflowResponse workflow = incidentWorkflowService.getWorkflowForIncident(incidentId);

        assertThat(workflow.getAssignedOfficerId()).isEqualTo(officer2Id);
        assertThat(workflow.getAssignedOfficerName()).isEqualTo("Officer Mary");
    }
}
