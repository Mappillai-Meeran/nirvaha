package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.DuplicateDetectionController;
import com.nirvaha.nirvaha_backend.dto.DuplicateDetectionResponse;
import com.nirvaha.nirvaha_backend.dto.DuplicateMatch;
import com.nirvaha.nirvaha_backend.dto.DuplicateMatchReason;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.service.DuplicateDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DuplicateDetectionService duplicateDetectionService;

    @InjectMocks
    private DuplicateDetectionController duplicateDetectionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(duplicateDetectionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnDuplicatesSuccessfully() throws Exception {
        DuplicateMatch match = new DuplicateMatch(
                11L, 1.0, 18.4, 32L,
                List.of(
                        DuplicateMatchReason.SAME_CATEGORY,
                        DuplicateMatchReason.LOCATION_WITHIN_THRESHOLD,
                        DuplicateMatchReason.TIME_WITHIN_THRESHOLD
                )
        );
        DuplicateDetectionResponse response = new DuplicateDetectionResponse(15L, List.of(match));

        when(duplicateDetectionService.findDuplicatesForIncident(15L)).thenReturn(response);

        mockMvc.perform(get("/api/incidents/15/duplicates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(15))
                .andExpect(jsonPath("$.matches.length()").value(1))
                .andExpect(jsonPath("$.matches[0].incidentId").value(11))
                .andExpect(jsonPath("$.matches[0].duplicateSimilarityScore").value(1.0))
                .andExpect(jsonPath("$.matches[0].distanceMeters").value(18.4))
                .andExpect(jsonPath("$.matches[0].reasons[0]").value("SAME_CATEGORY"));
    }

    @Test
    void shouldReturn404WhenIncidentNotFound() throws Exception {
        when(duplicateDetectionService.findDuplicatesForIncident(999L))
                .thenThrow(new ResourceNotFoundException("Incident with ID 999 not found"));

        mockMvc.perform(get("/api/incidents/999/duplicates"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Incident with ID 999 not found"));
    }
}
