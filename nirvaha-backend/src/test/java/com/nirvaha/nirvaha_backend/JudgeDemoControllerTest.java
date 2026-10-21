package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.JudgeDemoController;
import com.nirvaha.nirvaha_backend.dto.JudgeDemoScenarioResultDTO;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.service.JudgeDemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JudgeDemoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JudgeDemoService judgeDemoService;

    @InjectMocks
    private JudgeDemoController judgeDemoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(judgeDemoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnDemoStatusSuccessfully() throws Exception {
        when(judgeDemoService.getDemoStatus()).thenReturn(Map.of(
                "activeSimulatedIncidentsCount", 0,
                "backendAvailable", true
        ));

        mockMvc.perform(get("/api/demo/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backendAvailable").value(true));
    }

    @Test
    void shouldRunMonsoonDrainageSurgeScenarioSuccessfully() throws Exception {
        JudgeDemoScenarioResultDTO result = new JudgeDemoScenarioResultDTO(
                "MONSOON_DRAINAGE_SURGE",
                "Scenario A: Monsoon Drainage Surge",
                "COMPLETED",
                "BACKEND_LIVE",
                "SIMULATED",
                "Initial baseline",
                "Action performed",
                "Resulting state",
                101L,
                List.of(101L, 102L, 103L, 104L),
                Map.of("clusterCount", 4),
                LocalDateTime.now()
        );

        when(judgeDemoService.runMonsoonDrainageSurgeScenario(any())).thenReturn(result);

        mockMvc.perform(post("/api/demo/scenarios/monsoon-drainage-surge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioKey").value("MONSOON_DRAINAGE_SURGE"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.executionMode").value("BACKEND_LIVE"))
                .andExpect(jsonPath("$.dataClassification").value("SIMULATED"));
    }

    @Test
    void shouldRunDuplicateDetectionScenarioSuccessfully() throws Exception {
        JudgeDemoScenarioResultDTO result = new JudgeDemoScenarioResultDTO(
                "DUPLICATE_DETECTION",
                "Scenario B: Duplicate Report Detection",
                "COMPLETED",
                "BACKEND_LIVE",
                "SIMULATED",
                "Initial baseline",
                "Action performed",
                "Resulting state",
                201L,
                List.of(201L, 202L),
                Map.of("similarityScore", 1.0),
                LocalDateTime.now()
        );

        when(judgeDemoService.runDuplicateDetectionScenario(any())).thenReturn(result);

        mockMvc.perform(post("/api/demo/scenarios/duplicate-detection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioKey").value("DUPLICATE_DETECTION"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldRunResolutionFailureScenarioSuccessfully() throws Exception {
        JudgeDemoScenarioResultDTO result = new JudgeDemoScenarioResultDTO(
                "RESOLUTION_FAILURE_REOPEN",
                "Scenario C: Resolution Verification Failure & Reopening",
                "COMPLETED",
                "BACKEND_LIVE",
                "SIMULATED",
                "Initial baseline",
                "Action performed",
                "Resulting state",
                301L,
                List.of(301L),
                Map.of("verificationStatus", "FAILED"),
                LocalDateTime.now()
        );

        when(judgeDemoService.runResolutionFailureScenario(any())).thenReturn(result);

        mockMvc.perform(post("/api/demo/scenarios/resolution-failure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioKey").value("RESOLUTION_FAILURE_REOPEN"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void shouldResetDemoDataSuccessfully() throws Exception {
        when(judgeDemoService.resetDemoData(any())).thenReturn(Map.of(
                "status", "RESET_SUCCESSFUL",
                "deletedSimulatedIncidentsCount", 4
        ));

        mockMvc.perform(post("/api/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESET_SUCCESSFUL"))
                .andExpect(jsonPath("$.deletedSimulatedIncidentsCount").value(4));
    }
}
