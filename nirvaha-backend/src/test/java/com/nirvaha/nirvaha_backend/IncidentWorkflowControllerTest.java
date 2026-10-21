package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.IncidentWorkflowController;
import com.nirvaha.nirvaha_backend.dto.CreateIncidentActionRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentActionResponse;
import com.nirvaha.nirvaha_backend.dto.IncidentWorkflowResponse;
import com.nirvaha.nirvaha_backend.entity.ActionType;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.service.IncidentWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IncidentWorkflowControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IncidentWorkflowService incidentWorkflowService;

    @InjectMocks
    private IncidentWorkflowController incidentWorkflowController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(incidentWorkflowController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateActionSuccessfullyAndReturn201() throws Exception {
        IncidentActionResponse response = new IncidentActionResponse(
                1L, 10L, 4L, "Officer John", ActionType.ASSIGNED, "Assigned to team", LocalDateTime.now()
        );

        when(incidentWorkflowService.addAction(eq(10L), any(CreateIncidentActionRequest.class))).thenReturn(response);

        String jsonPayload = """
                {
                    "officerId": 4,
                    "actionType": "ASSIGNED",
                    "remarks": "Assigned to team"
                }
                """;

        mockMvc.perform(post("/api/incidents/10/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.incidentId").value(10))
                .andExpect(jsonPath("$.officerId").value(4))
                .andExpect(jsonPath("$.officerName").value("Officer John"))
                .andExpect(jsonPath("$.actionType").value("ASSIGNED"))
                .andExpect(jsonPath("$.remarks").value("Assigned to team"));
    }

    @Test
    void shouldGetActionsSuccessfullyAndReturn200() throws Exception {
        IncidentActionResponse action1 = new IncidentActionResponse(
                1L, 10L, 4L, "Officer John", ActionType.ASSIGNED, "Assigned", LocalDateTime.now()
        );
        IncidentActionResponse action2 = new IncidentActionResponse(
                2L, 10L, 4L, "Officer John", ActionType.VERIFIED, "Verified", LocalDateTime.now()
        );

        when(incidentWorkflowService.getActionsForIncident(10L)).thenReturn(List.of(action1, action2));

        mockMvc.perform(get("/api/incidents/10/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].actionType").value("ASSIGNED"))
                .andExpect(jsonPath("$[1].actionType").value("VERIFIED"));
    }

    @Test
    void shouldGetWorkflowSuccessfullyAndReturn200() throws Exception {
        IncidentActionResponse action = new IncidentActionResponse(
                1L, 10L, 4L, "Officer John", ActionType.ASSIGNED, "Assigned", LocalDateTime.now()
        );
        IncidentWorkflowResponse workflowResponse = new IncidentWorkflowResponse(
                10L, IncidentStatus.VERIFIED, 4L, "Officer John", List.of(action)
        );

        when(incidentWorkflowService.getWorkflowForIncident(10L)).thenReturn(workflowResponse);

        mockMvc.perform(get("/api/incidents/10/workflow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(10))
                .andExpect(jsonPath("$.currentStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.assignedOfficerId").value(4))
                .andExpect(jsonPath("$.assignedOfficerName").value("Officer John"))
                .andExpect(jsonPath("$.actions.length()").value(1));
    }

    @Test
    void shouldReturn404WhenIncidentNotFound() throws Exception {
        when(incidentWorkflowService.getWorkflowForIncident(999L))
                .thenThrow(new ResourceNotFoundException("Incident with ID 999 not found"));

        mockMvc.perform(get("/api/incidents/999/workflow"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Incident with ID 999 not found"));
    }

    @Test
    void shouldReturn400WhenInvalidTransitionOrRole() throws Exception {
        when(incidentWorkflowService.addAction(eq(10L), any(CreateIncidentActionRequest.class)))
                .thenThrow(new IllegalArgumentException("User with ID 2 is not authorized to perform government incident actions"));

        String jsonPayload = """
                {
                    "officerId": 2,
                    "actionType": "ASSIGNED",
                    "remarks": "Citizen assignment attempt"
                }
                """;

        mockMvc.perform(post("/api/incidents/10/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("User with ID 2 is not authorized to perform government incident actions"));
    }
}
