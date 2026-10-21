package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.PriorityScoringController;
import com.nirvaha.nirvaha_backend.dto.PriorityFactors;
import com.nirvaha.nirvaha_backend.dto.PriorityScoreResponse;
import com.nirvaha.nirvaha_backend.entity.PriorityLevel;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.service.PriorityScoringService;
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
class PriorityScoringControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PriorityScoringService priorityScoringService;

    @InjectMocks
    private PriorityScoringController priorityScoringController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(priorityScoringController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnPriorityScoreSuccessfully() throws Exception {
        PriorityFactors factors = new PriorityFactors(1.0, 1.0, 0.5, 0.0);
        List<String> reasons = List.of(
                "Incident severity is CRITICAL",
                "Multiple reports (3+) indicate the same civic issue",
                "Nearby related incidents increase local impact",
                "Rainfall data unavailable; no environmental contribution applied"
        );
        PriorityScoreResponse response = new PriorityScoreResponse(46L, 80.0, PriorityLevel.CRITICAL, factors, reasons);

        when(priorityScoringService.calculatePriority(46L, null)).thenReturn(response);

        mockMvc.perform(get("/api/incidents/46/priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(46))
                .andExpect(jsonPath("$.priorityScore").value(80.0))
                .andExpect(jsonPath("$.priorityLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.factors.severityFactor").value(1.0))
                .andExpect(jsonPath("$.factors.reportVolumeFactor").value(1.0))
                .andExpect(jsonPath("$.factors.locationImpactFactor").value(0.5))
                .andExpect(jsonPath("$.factors.rainfallFactor").value(0.0))
                .andExpect(jsonPath("$.reasons[0]").value("Incident severity is CRITICAL"));
    }

    @Test
    void shouldReturnPriorityScoreWithRainfallQueryParam() throws Exception {
        PriorityFactors factors = new PriorityFactors(0.75, 0.0, 0.0, 0.75);
        List<String> reasons = List.of(
                "Incident severity is HIGH",
                "Single report filed for this incident",
                "No nearby related incidents reported",
                "Heavy rainfall detected (50.0 mm)"
        );
        PriorityScoreResponse response = new PriorityScoreResponse(10L, 37.5, PriorityLevel.MEDIUM, factors, reasons);

        when(priorityScoringService.calculatePriority(10L, 50.0)).thenReturn(response);

        mockMvc.perform(get("/api/incidents/10/priority").param("rainfall", "50.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(10))
                .andExpect(jsonPath("$.priorityScore").value(37.5))
                .andExpect(jsonPath("$.priorityLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.factors.rainfallFactor").value(0.75))
                .andExpect(jsonPath("$.reasons[3]").value("Heavy rainfall detected (50.0 mm)"));
    }

    @Test
    void shouldReturn404WhenIncidentNotFound() throws Exception {
        when(priorityScoringService.calculatePriority(999999L, null))
                .thenThrow(new ResourceNotFoundException("Incident with ID 999999 not found"));

        mockMvc.perform(get("/api/incidents/999999/priority"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Incident with ID 999999 not found"));
    }
}
