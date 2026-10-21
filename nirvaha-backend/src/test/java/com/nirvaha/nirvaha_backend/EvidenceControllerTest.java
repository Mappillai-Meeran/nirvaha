package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.EvidenceController;
import com.nirvaha.nirvaha_backend.dto.CreateEvidenceRequest;
import com.nirvaha.nirvaha_backend.dto.EvidenceResponse;
import com.nirvaha.nirvaha_backend.entity.EvidenceType;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.service.EvidenceService;
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
class EvidenceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EvidenceService evidenceService;

    @InjectMocks
    private EvidenceController evidenceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(evidenceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateEvidenceSuccessfully() throws Exception {
        EvidenceResponse response = new EvidenceResponse(
                1L, 3L, EvidenceType.PHOTO, "https://example.com/evidence/drainage.jpg",
                "drainage.jpg", "image/jpeg", 245678L, LocalDateTime.now(),
                new BigDecimal("11.9416"), new BigDecimal("79.8083"),
                "Photo showing drainage overflow", LocalDateTime.now()
        );
        when(evidenceService.createEvidence(any(CreateEvidenceRequest.class))).thenReturn(response);

        String jsonPayload = """
                {
                    "incidentId": 3,
                    "evidenceType": "PHOTO",
                    "fileUrl": "https://example.com/evidence/drainage.jpg",
                    "fileName": "drainage.jpg",
                    "mimeType": "image/jpeg",
                    "fileSize": 245678,
                    "latitude": 11.9416,
                    "longitude": 79.8083,
                    "description": "Photo showing drainage overflow"
                }
                """;

        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.incidentId").value(3))
                .andExpect(jsonPath("$.evidenceType").value("PHOTO"))
                .andExpect(jsonPath("$.fileUrl").value("https://example.com/evidence/drainage.jpg"));
    }

    @Test
    void shouldReturn404WhenIncidentDoesNotExist() throws Exception {
        when(evidenceService.createEvidence(any(CreateEvidenceRequest.class)))
                .thenThrow(new ResourceNotFoundException("Incident with ID 9999 not found"));

        String jsonPayload = """
                {
                    "incidentId": 9999,
                    "evidenceType": "PHOTO",
                    "fileUrl": "https://example.com/orphan.jpg"
                }
                """;

        mockMvc.perform(post("/api/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Incident with ID 9999 not found"));
    }

    @Test
    void shouldGetEvidenceByIdSuccessfully() throws Exception {
        EvidenceResponse response = new EvidenceResponse(
                1L, 3L, EvidenceType.PHOTO, "https://example.com/evidence/drainage.jpg",
                "drainage.jpg", "image/jpeg", 245678L, LocalDateTime.now(),
                new BigDecimal("11.9416"), new BigDecimal("79.8083"),
                "Photo showing drainage overflow", LocalDateTime.now()
        );
        when(evidenceService.getEvidenceById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/evidence/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileUrl").value("https://example.com/evidence/drainage.jpg"));
    }

    @Test
    void shouldGetEvidenceForIncidentSuccessfully() throws Exception {
        EvidenceResponse item1 = new EvidenceResponse(
                1L, 3L, EvidenceType.PHOTO, "https://example.com/e1.jpg", "e1.jpg",
                "image/jpeg", 100L, LocalDateTime.now(), new BigDecimal("11.94"),
                new BigDecimal("79.80"), "Photo 1", LocalDateTime.now()
        );
        when(evidenceService.getEvidenceForIncident(3L)).thenReturn(List.of(item1));

        mockMvc.perform(get("/api/evidence/incident/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
