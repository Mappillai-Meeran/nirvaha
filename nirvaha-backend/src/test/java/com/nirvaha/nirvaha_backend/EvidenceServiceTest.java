package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateEvidenceRequest;
import com.nirvaha.nirvaha_backend.dto.CreateIncidentRequest;
import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.EvidenceResponse;
import com.nirvaha.nirvaha_backend.dto.IncidentResponse;
import com.nirvaha.nirvaha_backend.dto.UserResponse;
import com.nirvaha.nirvaha_backend.entity.EvidenceType;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.EvidenceRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.EvidenceService;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class EvidenceServiceTest {

    @Autowired
    private EvidenceService evidenceService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.ResolutionRepository resolutionRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.IncidentActionRepository incidentActionRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    private Long validIncidentId;

    @BeforeEach
    void setUp() {
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        UserResponse reporter = userService.createUser(new CreateUserRequest(
                "Evidence Tester",
                "evidence.tester@puducherry.gov.in",
                "pass123",
                UserRole.CITIZEN
        ));

        IncidentResponse incident = incidentService.createIncident(new CreateIncidentRequest(
                reporter.getId(),
                "Waterlogging Evidence Test",
                "Description for evidence test",
                IncidentCategory.DRAINAGE,
                IncidentSeverity.HIGH,
                new BigDecimal("11.9416"),
                new BigDecimal("79.8083"),
                "White Town"
        ));
        validIncidentId = incident.getId();
    }

    @Test
    void shouldCreateEvidenceSuccessfully() {
        CreateEvidenceRequest request = new CreateEvidenceRequest(
                validIncidentId,
                EvidenceType.PHOTO,
                "https://example.com/evidence/photo1.jpg",
                "photo1.jpg",
                "image/jpeg",
                102400L,
                LocalDateTime.now(),
                new BigDecimal("11.9416"),
                new BigDecimal("79.8083"),
                "Close-up photo of clogged drain"
        );

        EvidenceResponse response = evidenceService.createEvidence(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getIncidentId()).isEqualTo(validIncidentId);
        assertThat(response.getEvidenceType()).isEqualTo(EvidenceType.PHOTO);
        assertThat(response.getFileUrl()).isEqualTo("https://example.com/evidence/photo1.jpg");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenIncidentDoesNotExist() {
        CreateEvidenceRequest request = new CreateEvidenceRequest(
                99999L,
                EvidenceType.PHOTO,
                "https://example.com/evidence/photo99.jpg",
                "photo99.jpg",
                "image/jpeg",
                2000L,
                LocalDateTime.now(),
                new BigDecimal("11.9416"),
                new BigDecimal("79.8083"),
                "Orphan evidence"
        );

        assertThatThrownBy(() -> evidenceService.createEvidence(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident with ID 99999 not found");
    }

    @Test
    void shouldGetEvidenceByIdSuccessfully() {
        CreateEvidenceRequest request = new CreateEvidenceRequest(
                validIncidentId,
                EvidenceType.PHOTO,
                "https://example.com/evidence/photo2.jpg",
                "photo2.jpg",
                "image/jpeg",
                50000L,
                LocalDateTime.now(),
                new BigDecimal("11.9416"),
                new BigDecimal("79.8083"),
                "Second evidence photo"
        );

        EvidenceResponse created = evidenceService.createEvidence(request);
        EvidenceResponse fetched = evidenceService.getEvidenceById(created.getId());

        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getFileName()).isEqualTo("photo2.jpg");
    }

    @Test
    void shouldThrowExceptionWhenEvidenceNotFound() {
        assertThatThrownBy(() -> evidenceService.getEvidenceById(77777L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Evidence with ID 77777 not found");
    }

    @Test
    void shouldSupportMultipleEvidenceForSameIncident() {
        CreateEvidenceRequest req1 = new CreateEvidenceRequest(
                validIncidentId, EvidenceType.PHOTO, "https://example.com/e1.jpg", "e1.jpg", "image/jpeg",
                1000L, LocalDateTime.now(), new BigDecimal("11.94"), new BigDecimal("79.80"), "Photo 1"
        );
        CreateEvidenceRequest req2 = new CreateEvidenceRequest(
                validIncidentId, EvidenceType.PHOTO, "https://example.com/e2.jpg", "e2.jpg", "image/jpeg",
                2000L, LocalDateTime.now(), new BigDecimal("11.94"), new BigDecimal("79.80"), "Photo 2"
        );

        evidenceService.createEvidence(req1);
        evidenceService.createEvidence(req2);

        List<EvidenceResponse> list = evidenceService.getEvidenceForIncident(validIncidentId);
        assertThat(list).hasSize(2);
    }
}
