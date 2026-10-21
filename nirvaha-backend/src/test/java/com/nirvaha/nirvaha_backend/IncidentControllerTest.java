package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.IncidentController;
import com.nirvaha.nirvaha_backend.dto.CreateIncidentRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentResponse;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IncidentService incidentService;

    @InjectMocks
    private IncidentController incidentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(incidentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateIncidentSuccessfully() throws Exception {
        IncidentResponse response = new IncidentResponse(
                10L, 3L, "Citizen One", "Waterlogging on Road", "Water accumulation due to blocked drain",
                IncidentCategory.DRAINAGE, IncidentSeverity.HIGH, IncidentStatus.REPORTED,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Puducherry",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(incidentService.createIncident(any(CreateIncidentRequest.class))).thenReturn(response);

        String jsonPayload = """
                {
                    "reporterId": 3,
                    "title": "Waterlogging on Road",
                    "description": "Water accumulation due to blocked drain",
                    "category": "DRAINAGE",
                    "severity": "HIGH",
                    "latitude": 11.9416,
                    "longitude": 79.8083,
                    "address": "Puducherry"
                }
                """;

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.reporterId").value(3))
                .andExpect(jsonPath("$.title").value("Waterlogging on Road"))
                .andExpect(jsonPath("$.category").value("DRAINAGE"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    @Test
    void shouldReturn404WhenReporterDoesNotExist() throws Exception {
        when(incidentService.createIncident(any(CreateIncidentRequest.class)))
                .thenThrow(new ResourceNotFoundException("User with ID 9999 not found"));

        String jsonPayload = """
                {
                    "reporterId": 9999,
                    "title": "Pothole issue",
                    "description": "Deep pothole causing road danger.",
                    "category": "POTHOLE",
                    "severity": "MEDIUM",
                    "latitude": 11.9416,
                    "longitude": 79.8083
                }
                """;

        mockMvc.perform(post("/api/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User with ID 9999 not found"));
    }

    @Test
    void shouldGetIncidentByIdSuccessfully() throws Exception {
        IncidentResponse response = new IncidentResponse(
                10L, 3L, "Citizen One", "Waterlogging on Road", "Water accumulation",
                IncidentCategory.DRAINAGE, IncidentSeverity.HIGH, IncidentStatus.REPORTED,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Puducherry",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(incidentService.getIncidentById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/incidents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Waterlogging on Road"));
    }

    @Test
    void shouldGetAllIncidentsSuccessfully() throws Exception {
        IncidentResponse item1 = new IncidentResponse(
                1L, 3L, "Citizen One", "Issue 1", "Desc 1",
                IncidentCategory.POTHOLE, IncidentSeverity.LOW, IncidentStatus.REPORTED,
                new BigDecimal("11.91"), new BigDecimal("79.81"), "Loc 1",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(incidentService.getAllIncidents()).thenReturn(List.of(item1));

        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void shouldGetMyReportsSuccessfully() throws Exception {
        IncidentResponse item1 = new IncidentResponse(
                1L, 3L, "Citizen One", "Issue 1", "Desc 1",
                IncidentCategory.POTHOLE, IncidentSeverity.LOW, IncidentStatus.REPORTED,
                new BigDecimal("11.91"), new BigDecimal("79.81"), "Loc 1",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(incidentService.getMyReports()).thenReturn(List.of(item1));

        mockMvc.perform(get("/api/incidents/my-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
