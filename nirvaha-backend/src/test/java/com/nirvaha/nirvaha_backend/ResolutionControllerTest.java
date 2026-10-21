package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.ResolutionController;
import com.nirvaha.nirvaha_backend.dto.CreateResolutionRequest;
import com.nirvaha.nirvaha_backend.dto.ResolutionResponse;
import com.nirvaha.nirvaha_backend.entity.VerificationStatus;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.service.ResolutionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ResolutionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ResolutionService resolutionService;

    @InjectMocks
    private ResolutionController resolutionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(resolutionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateResolutionSuccessfullyAndReturn201() throws Exception {
        ResolutionResponse response = new ResolutionResponse(
                1L, 10L, 4L, "Officer Sarah", 25L, "http://example.com/res.jpg",
                0.85, VerificationStatus.VERIFIED, "Evidence supports resolution.",
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(resolutionService.createResolution(eq(10L), any(CreateResolutionRequest.class))).thenReturn(response);

        String jsonPayload = """
                {
                    "officerId": 4,
                    "evidenceId": 25,
                    "remarks": "Pothole repair work verified."
                }
                """;

        mockMvc.perform(post("/api/incidents/10/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.incidentId").value(10))
                .andExpect(jsonPath("$.officerId").value(4))
                .andExpect(jsonPath("$.officerName").value("Officer Sarah"))
                .andExpect(jsonPath("$.resolutionEvidenceId").value(25))
                .andExpect(jsonPath("$.resolutionSimilarityScore").value(0.85))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));
    }

    @Test
    void shouldGetResolutionSuccessfullyAndReturn200() throws Exception {
        ResolutionResponse response = new ResolutionResponse(
                1L, 10L, 4L, "Officer Sarah", 25L, "http://example.com/res.jpg",
                0.85, VerificationStatus.VERIFIED, "Evidence supports resolution.",
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(resolutionService.getResolutionForIncident(10L)).thenReturn(response);

        mockMvc.perform(get("/api/incidents/10/resolution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(10))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.resolutionSimilarityScore").value(0.85));
    }

    @Test
    void shouldReturn404WhenIncidentNotFound() throws Exception {
        when(resolutionService.getResolutionForIncident(999L))
                .thenThrow(new ResourceNotFoundException("Incident with ID 999 not found"));

        mockMvc.perform(get("/api/incidents/999/resolution"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Incident with ID 999 not found"));
    }

    @Test
    void shouldReturn400WhenCitizenSubmitsResolution() throws Exception {
        when(resolutionService.createResolution(eq(10L), any(CreateResolutionRequest.class)))
                .thenThrow(new IllegalArgumentException("User with ID 2 is not authorized to submit resolution evidence"));

        String jsonPayload = """
                {
                    "officerId": 2,
                    "evidenceId": 25,
                    "remarks": "Citizen submission attempt"
                }
                """;

        mockMvc.perform(post("/api/incidents/10/resolutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("User with ID 2 is not authorized to submit resolution evidence"));
    }
}
