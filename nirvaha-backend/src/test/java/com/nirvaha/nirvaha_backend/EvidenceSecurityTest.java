package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateIncidentRequest;
import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentResponse;
import com.nirvaha.nirvaha_backend.dto.LoginRequest;
import com.nirvaha.nirvaha_backend.dto.LoginResponse;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.repository.EvidenceRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.security.FileSecurityValidator;
import com.nirvaha.nirvaha_backend.service.AuthService;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class EvidenceSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private FileSecurityValidator fileSecurityValidator;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.ResolutionRepository resolutionRepository;

    private String citizenToken;
    private Long incidentId;

    // Standard 1x1 8-bit PNG bytes
    private static final byte[] VALID_PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
            0x42, 0x60, (byte) 0x82
    };

    private static final byte[] VALID_WEBP_BYTES = new byte[] {
            0x52, 0x49, 0x46, 0x46, 0x1A, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50, // RIFF....WEBP
            0x56, 0x50, 0x38, 0x20, 0x0E, 0x00, 0x00, 0x00, (byte) 0xB0, 0x01, 0x00, (byte) 0x9D,
            0x01, 0x2A, 0x01, 0x00, 0x01, 0x00, 0x00, 0x47, 0x08, (byte) 0x85, (byte) 0x85, (byte) 0x88
    };

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        resolutionRepository.deleteAll();
        evidenceRepository.deleteAll();

        String citizenEmail = "evidence_user@example.com";
        if (userRepository.findByEmail(citizenEmail).isEmpty()) {
            userService.createUser(new CreateUserRequest("Evidence User", citizenEmail, "Password123!", UserRole.CITIZEN));
        }
        LoginResponse loginResponse = authService.login(new LoginRequest(citizenEmail, "Password123!"));
        citizenToken = loginResponse.getToken();

        CreateIncidentRequest incReq = new CreateIncidentRequest();
        incReq.setTitle("Evidence Test Pothole");
        incReq.setDescription("Pothole for evidence security validation");
        incReq.setCategory(IncidentCategory.POTHOLE);
        incReq.setSeverity(IncidentSeverity.MEDIUM);
        incReq.setLatitude(java.math.BigDecimal.valueOf(12.9716));
        incReq.setLongitude(java.math.BigDecimal.valueOf(77.5946));
        incReq.setReporterId(loginResponse.getUser().getId());
        IncidentResponse createdInc = incidentService.createIncident(incReq);
        incidentId = createdInc.getId();
    }

    @Test
    void testValidPngValidation() {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", VALID_PNG_BYTES);
        fileSecurityValidator.validateFile(file);
    }

    @Test
    void testValidWebpValidationAndUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.webp", "image/webp", VALID_WEBP_BYTES);
        fileSecurityValidator.validateFile(file);

        mockMvc.perform(multipart("/api/evidence/upload")
                        .file(file)
                        .param("incidentId", String.valueOf(incidentId))
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.mimeType").value("image/webp"));
    }

    @Test
    void testInvalidMagicBytesThrowsException() {
        byte[] fakeBytes = "<?php echo 'shell'; ?>".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", fakeBytes);

        assertThrows(IllegalArgumentException.class, () -> fileSecurityValidator.validateFile(file));
    }

    @Test
    void testEmptyFileThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> fileSecurityValidator.validateFile(file));
    }

    @Test
    void testFilenameSanitizationPathTraversal() {
        String unsafeName = "../../../etc/passwd.png";
        String sanitized = fileSecurityValidator.sanitizeFilename(unsafeName);
        assertFalse(sanitized.contains(".."));
        assertFalse(sanitized.contains("/"));
        assertFalse(sanitized.contains("\\"));
        assertTrue(sanitized.endsWith(".png"));
    }

    @Test
    void testSecureUploadEndpointWithValidFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "valid_evidence.png", "image/png", VALID_PNG_BYTES);

        mockMvc.perform(multipart("/api/evidence/upload")
                        .file(file)
                        .param("incidentId", String.valueOf(incidentId))
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.incidentId").value(incidentId));
    }

    @Test
    void testSecureUploadEndpointWithInvalidFileReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "malicious.php.jpg", "image/jpeg", "not an image".getBytes());

        mockMvc.perform(multipart("/api/evidence/upload")
                        .file(file)
                        .param("incidentId", String.valueOf(incidentId))
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testUnauthorizedEvidenceRetrievalFailsForNonExistentOrUnauthenticated() throws Exception {
        // Unauthenticated access attempt to evidence endpoint fails with 401
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/evidence/999999")
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/evidence/999999"))
                .andExpect(status().isUnauthorized());

        // Authenticated request to non-existent evidence ID returns 404 Not Found
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/evidence/999999")
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void testPhase7ResolutionEvidenceSecurityValidation() throws Exception {
        // Upload secure evidence for resolution
        MockMultipartFile file = new MockMultipartFile("file", "resolution_proof.png", "image/png", VALID_PNG_BYTES);

        String responseStr = mockMvc.perform(multipart("/api/evidence/upload")
                        .file(file)
                        .param("incidentId", String.valueOf(incidentId))
                        .header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertTrue(responseStr.contains("resolution_proof") || responseStr.contains(".png"));
    }
}
