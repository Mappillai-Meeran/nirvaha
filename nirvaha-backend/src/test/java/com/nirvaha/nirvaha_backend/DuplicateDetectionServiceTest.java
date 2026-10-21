package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.EvidenceRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.DuplicateDetectionService;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DuplicateDetectionServiceTest {

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

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

    private Long reporterId;

    @BeforeEach
    void setUp() {
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();

        UserResponse reporter = userService.createUser(new CreateUserRequest(
                "Duplicate Tester",
                "duplicate.tester@puducherry.gov.in",
                "pass123",
                UserRole.CITIZEN
        ));
        reporterId = reporter.getId();
    }

    @Test
    void shouldDetectHighSimilarityForSameCategoryNearbyRecentIncident() {
        // Base Incident
        IncidentResponse baseIncident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Drainage Leak A", "Drainage overflow on Main St",
                IncidentCategory.DRAINAGE, IncidentSeverity.HIGH,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Main St"
        ));

        // Nearby Incident (approx 15 meters away) reported right after
        IncidentResponse nearbyIncident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Drainage Leak B", "Drainage overflow on Main St",
                IncidentCategory.DRAINAGE, IncidentSeverity.HIGH,
                new BigDecimal("11.9417"), new BigDecimal("79.8084"), "Main St"
        ));

        DuplicateDetectionResponse response = duplicateDetectionService.findDuplicatesForIncident(baseIncident.getId());

        assertThat(response.getIncidentId()).isEqualTo(baseIncident.getId());
        assertThat(response.getMatches()).hasSize(1);

        DuplicateMatch match = response.getMatches().get(0);
        assertThat(match.getIncidentId()).isEqualTo(nearbyIncident.getId());
        assertThat(match.getDuplicateSimilarityScore()).isEqualTo(1.0);
        assertThat(match.getReasons()).containsExactlyInAnyOrder(
                DuplicateMatchReason.SAME_CATEGORY,
                DuplicateMatchReason.LOCATION_WITHIN_THRESHOLD,
                DuplicateMatchReason.TIME_WITHIN_THRESHOLD
        );
    }

    @Test
    void shouldExcludeDifferentCategoryIncidents() {
        IncidentResponse baseIncident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Drainage Problem", "Drainage issue",
                IncidentCategory.DRAINAGE, IncidentSeverity.HIGH,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Main St"
        ));

        // Same location, but category is POTHOLE
        incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Pothole Problem", "Road pothole",
                IncidentCategory.POTHOLE, IncidentSeverity.HIGH,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Main St"
        ));

        DuplicateDetectionResponse response = duplicateDetectionService.findDuplicatesForIncident(baseIncident.getId());

        assertThat(response.getMatches()).isEmpty();
    }

    @Test
    void shouldExcludeFarAwayIncidents() {
        IncidentResponse baseIncident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Drainage White Town", "Drainage issue",
                IncidentCategory.DRAINAGE, IncidentSeverity.HIGH,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "White Town"
        ));

        // Same category, but far away (> 5km away: 11.9800, 79.8500)
        incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Drainage Lawspet", "Far away drainage issue",
                IncidentCategory.DRAINAGE, IncidentSeverity.HIGH,
                new BigDecimal("11.9800"), new BigDecimal("79.8500"), "Lawspet"
        ));

        DuplicateDetectionResponse response = duplicateDetectionService.findDuplicatesForIncident(baseIncident.getId());

        assertThat(response.getMatches()).isEmpty();
    }

    @Test
    void shouldNeverCompareIncidentWithItself() {
        IncidentResponse baseIncident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Single Incident", "Only one report",
                IncidentCategory.GARBAGE, IncidentSeverity.LOW,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Heritage Town"
        ));

        DuplicateDetectionResponse response = duplicateDetectionService.findDuplicatesForIncident(baseIncident.getId());

        assertThat(response.getMatches()).isEmpty();
    }

    @Test
    void shouldOrderMultipleMatchesByScoreDescendingThenDistanceAscending() {
        IncidentResponse baseIncident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Garbage Dump Main", "Garbage pile",
                IncidentCategory.GARBAGE, IncidentSeverity.MEDIUM,
                new BigDecimal("11.9400"), new BigDecimal("79.8000"), "Location 0"
        ));

        // Candidate 1: ~15 meters away
        IncidentResponse cand1 = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Garbage Dump Close", "Garbage pile close",
                IncidentCategory.GARBAGE, IncidentSeverity.MEDIUM,
                new BigDecimal("11.9401"), new BigDecimal("79.8001"), "Location 1"
        ));

        // Candidate 2: ~150 meters away
        IncidentResponse cand2 = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Garbage Dump Further", "Garbage pile further",
                IncidentCategory.GARBAGE, IncidentSeverity.MEDIUM,
                new BigDecimal("11.9410"), new BigDecimal("79.8010"), "Location 2"
        ));

        DuplicateDetectionResponse response = duplicateDetectionService.findDuplicatesForIncident(baseIncident.getId());

        assertThat(response.getMatches()).hasSize(2);
        // Candidate 1 should be first because it is closer
        assertThat(response.getMatches().get(0).getIncidentId()).isEqualTo(cand1.getId());
        assertThat(response.getMatches().get(1).getIncidentId()).isEqualTo(cand2.getId());
    }

    @Test
    void shouldThrowExceptionForInvalidIncidentId() {
        assertThatThrownBy(() -> duplicateDetectionService.findDuplicatesForIncident(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident with ID 99999 not found");
    }
}
