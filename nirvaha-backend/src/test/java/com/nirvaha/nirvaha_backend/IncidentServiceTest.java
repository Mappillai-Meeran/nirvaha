package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateIncidentRequest;
import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentResponse;
import com.nirvaha.nirvaha_backend.dto.UserResponse;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class IncidentServiceTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.ResolutionRepository resolutionRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.IncidentActionRepository incidentActionRepository;

    @Autowired
    private com.nirvaha.nirvaha_backend.repository.EvidenceRepository evidenceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    private Long validReporterId;

    @BeforeEach
    void setUp() {
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        UserResponse reporter = userService.createUser(new CreateUserRequest(
                "Puducherry Reporter",
                "reporter@puducherry.gov.in",
                "password123",
                UserRole.CITIZEN
        ));
        validReporterId = reporter.getId();
    }

    @Test
    void shouldCreateIncidentSuccessfully() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                validReporterId,
                "Drainage Overflow near Boulevard",
                "Severe drainage water overflow blocking traffic.",
                IncidentCategory.DRAINAGE,
                IncidentSeverity.HIGH,
                new BigDecimal("11.9416"),
                new BigDecimal("79.8083"),
                "White Town, Puducherry"
        );

        IncidentResponse response = incidentService.createIncident(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getReporterId()).isEqualTo(validReporterId);
        assertThat(response.getReporterName()).isEqualTo("Puducherry Reporter");
        assertThat(response.getTitle()).isEqualTo("Drainage Overflow near Boulevard");
        assertThat(response.getCategory()).isEqualTo(IncidentCategory.DRAINAGE);
        assertThat(response.getSeverity()).isEqualTo(IncidentSeverity.HIGH);
        assertThat(response.getStatus()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldAlwaysStartNewCitizenReportsAsReported() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                validReporterId,
                "Attempted status override",
                "A client must not be able to create a resolved civic case.",
                IncidentCategory.OTHER,
                IncidentSeverity.LOW,
                new BigDecimal("11.9416"),
                new BigDecimal("79.8083"),
                "Puducherry"
        );
        request.setStatus(IncidentStatus.RESOLVED);

        IncidentResponse response = incidentService.createIncident(request);

        assertThat(response.getStatus()).isEqualTo(IncidentStatus.REPORTED);
    }

    @Test
    void shouldThrowExceptionWhenReporterDoesNotExist() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                99999L,
                "Pothole on Main Road",
                "Large pothole creating hazard.",
                IncidentCategory.POTHOLE,
                IncidentSeverity.MEDIUM,
                new BigDecimal("11.9300"),
                new BigDecimal("79.8300"),
                "Puducherry Main Road"
        );

        assertThatThrownBy(() -> incidentService.createIncident(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User with ID 99999 not found");
    }

    @Test
    void shouldGetIncidentByIdSuccessfully() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                validReporterId,
                "Broken Streetlight",
                "Streetlight not working near hospital.",
                IncidentCategory.STREETLIGHT,
                IncidentSeverity.LOW,
                new BigDecimal("11.9350"),
                new BigDecimal("79.8250"),
                "Mission Street, Puducherry"
        );

        IncidentResponse created = incidentService.createIncident(request);
        IncidentResponse fetched = incidentService.getIncidentById(created.getId());

        assertThat(fetched.getId()).isEqualTo(created.getId());
        assertThat(fetched.getTitle()).isEqualTo("Broken Streetlight");
    }

    @Test
    void shouldThrowExceptionWhenIncidentNotFound() {
        assertThatThrownBy(() -> incidentService.getIncidentById(88888L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident with ID 88888 not found");
    }
}
