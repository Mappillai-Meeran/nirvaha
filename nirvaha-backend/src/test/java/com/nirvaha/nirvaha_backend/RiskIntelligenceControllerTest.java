package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.RiskIntelligenceController;
import com.nirvaha.nirvaha_backend.dto.RiskFactorContribution;
import com.nirvaha.nirvaha_backend.dto.WardRiskResponse;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.RiskLevel;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.service.RiskIntelligenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RiskIntelligenceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RiskIntelligenceService riskIntelligenceService;

    @InjectMocks
    private RiskIntelligenceController riskIntelligenceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(riskIntelligenceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetAllWardRisks_Success() throws Exception {
        WardRiskResponse response = new WardRiskResponse(
                1L, 1, "Boulevard North", 0.65, 65, RiskLevel.HIGH,
                IncidentCategory.DRAINAGE, 5, 3,
                List.of(new RiskFactorContribution("Historical Activity", 0.8, 0.16, "High volume")),
                "Risk for drainage in Boulevard North is HIGH.",
                "Inspect storm drains and pumps.",
                "Open-Meteo Weather API Live Data"
        );

        when(riskIntelligenceService.getAllWardRisks()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/risk/wards")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wardId").value(1))
                .andExpect(jsonPath("$[0].wardName").value("Boulevard North"))
                .andExpect(jsonPath("$[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].riskScore").value(0.65))
                .andExpect(jsonPath("$[0].riskScore100").value(65));
    }

    @Test
    void testGetRiskForWard_Success() throws Exception {
        WardRiskResponse response = new WardRiskResponse(
                2L, 2, "Mudaliarpet", 0.81, 81, RiskLevel.CRITICAL,
                IncidentCategory.DRAINAGE, 8, 5,
                List.of(new RiskFactorContribution("Recurrence", 0.9, 0.18, "Multi-month pattern")),
                "Risk for drainage in Mudaliarpet is CRITICAL.",
                "Inspect drainage hotspots and verify pumping readiness.",
                "Open-Meteo Weather API Live Data"
        );

        when(riskIntelligenceService.evaluateWardRisk(2L, IncidentCategory.DRAINAGE)).thenReturn(response);

        mockMvc.perform(get("/api/risk/wards/2?category=DRAINAGE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wardId").value(2))
                .andExpect(jsonPath("$.topRiskCategory").value("DRAINAGE"))
                .andExpect(jsonPath("$.riskLevel").value("CRITICAL"));
    }

    @Test
    void testGetRiskForWard_NotFound() throws Exception {
        when(riskIntelligenceService.evaluateWardRisk(999L, null))
                .thenThrow(new ResourceNotFoundException("Ward with ID 999 not found"));

        mockMvc.perform(get("/api/risk/wards/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ward with ID 999 not found"));
    }

    @Test
    void testEvaluateAllRisks_Success() throws Exception {
        WardRiskResponse response = new WardRiskResponse(
                1L, 1, "Boulevard North", 0.30, 30, RiskLevel.MODERATE,
                IncidentCategory.POTHOLE, 2, 1,
                List.of(), "Risk is MODERATE.", "Schedule field inspection.", "Fallback weather"
        );

        when(riskIntelligenceService.getAllWardRisks()).thenReturn(List.of(response));

        mockMvc.perform(post("/api/risk/assessments/evaluate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wardId").value(1))
                .andExpect(jsonPath("$[0].riskLevel").value("MODERATE"));
    }
}
