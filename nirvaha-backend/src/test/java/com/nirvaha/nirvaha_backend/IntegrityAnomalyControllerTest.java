package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.IntegrityAnomalyController;
import com.nirvaha.nirvaha_backend.dto.AnomalyFlagResponse;
import com.nirvaha.nirvaha_backend.dto.IncidentAnomaliesResponse;
import com.nirvaha.nirvaha_backend.entity.AnomalySeverity;
import com.nirvaha.nirvaha_backend.entity.AnomalyStatus;
import com.nirvaha.nirvaha_backend.entity.AnomalyType;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.service.IntegrityAnomalyService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IntegrityAnomalyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IntegrityAnomalyService integrityAnomalyService;

    @InjectMocks
    private IntegrityAnomalyController integrityAnomalyController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(integrityAnomalyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetAnomaliesForIncident_Success() throws Exception {
        AnomalyFlagResponse anomaly = new AnomalyFlagResponse(
                1L, 101L, AnomalyType.BULK_CLOSURE, AnomalySeverity.MEDIUM,
                "5 incidents were marked resolved by the same officer within 60 minutes. Review recommended.",
                "102,103,104,105", AnomalyStatus.OPEN, LocalDateTime.now()
        );
        IncidentAnomaliesResponse response = new IncidentAnomaliesResponse(101L, List.of(anomaly));

        when(integrityAnomalyService.getAnomaliesForIncident(101L)).thenReturn(response);

        mockMvc.perform(get("/api/incidents/101/anomalies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(101))
                .andExpect(jsonPath("$.anomalies[0].id").value(1))
                .andExpect(jsonPath("$.anomalies[0].anomalyType").value("BULK_CLOSURE"))
                .andExpect(jsonPath("$.anomalies[0].severity").value("MEDIUM"))
                .andExpect(jsonPath("$.anomalies[0].description").value("5 incidents were marked resolved by the same officer within 60 minutes. Review recommended."));
    }

    @Test
    void testDetectAnomaliesForIncident_Success() throws Exception {
        AnomalyFlagResponse anomaly = new AnomalyFlagResponse(
                2L, 201L, AnomalyType.REPEATED_REPORTING, AnomalySeverity.LOW,
                "3 similar drainage reports were submitted within 500 metres and 24 hours. Review recommended.",
                "202,203", AnomalyStatus.OPEN, LocalDateTime.now()
        );
        IncidentAnomaliesResponse response = new IncidentAnomaliesResponse(201L, List.of(anomaly));

        when(integrityAnomalyService.detectAnomalies(201L)).thenReturn(response);

        mockMvc.perform(post("/api/incidents/201/anomalies/detect")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(201))
                .andExpect(jsonPath("$.anomalies[0].anomalyType").value("REPEATED_REPORTING"))
                .andExpect(jsonPath("$.anomalies[0].severity").value("LOW"));
    }

    @Test
    void testGetAnomaliesForIncident_NotFound() throws Exception {
        when(integrityAnomalyService.getAnomaliesForIncident(999L))
                .thenThrow(new ResourceNotFoundException("Incident with ID 999 not found"));

        mockMvc.perform(get("/api/incidents/999/anomalies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Incident with ID 999 not found"));
    }

    @Test
    void testGetAnomaliesForOfficer_Success() throws Exception {
        AnomalyFlagResponse anomaly = new AnomalyFlagResponse(
                3L, 301L, AnomalyType.RESOLUTION_EVIDENCE_REUSE, AnomalySeverity.HIGH,
                "The same resolution evidence reference is associated with multiple incidents. Review recommended.",
                "302", AnomalyStatus.OPEN, LocalDateTime.now()
        );

        when(integrityAnomalyService.getAnomaliesForOfficer(50L)).thenReturn(List.of(anomaly));

        mockMvc.perform(get("/api/officers/50/anomalies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].anomalyType").value("RESOLUTION_EVIDENCE_REUSE"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"));
    }

    @Test
    void testGetOpenAnomalies_Success() throws Exception {
        AnomalyFlagResponse anomaly = new AnomalyFlagResponse(
                4L, 401L, AnomalyType.BULK_CLOSURE, AnomalySeverity.MEDIUM,
                "5 incidents marked resolved within 60 minutes",
                "402,403", AnomalyStatus.OPEN, LocalDateTime.now()
        );

        when(integrityAnomalyService.getOpenAnomalies()).thenReturn(List.of(anomaly));

        mockMvc.perform(get("/api/integrity/open")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].anomalyType").value("BULK_CLOSURE"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }
}
