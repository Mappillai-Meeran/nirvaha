package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.CreateIncidentRequest;
import com.nirvaha.nirvaha_backend.dto.CreateUserRequest;
import com.nirvaha.nirvaha_backend.dto.IncidentResponse;
import com.nirvaha.nirvaha_backend.dto.PriorityScoreResponse;
import com.nirvaha.nirvaha_backend.dto.UserResponse;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentSeverity;
import com.nirvaha.nirvaha_backend.entity.PriorityLevel;
import com.nirvaha.nirvaha_backend.entity.UserRole;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.EvidenceRepository;
import com.nirvaha.nirvaha_backend.repository.IncidentRepository;
import com.nirvaha.nirvaha_backend.repository.UserRepository;
import com.nirvaha.nirvaha_backend.service.IncidentService;
import com.nirvaha.nirvaha_backend.service.PriorityScoringService;
import com.nirvaha.nirvaha_backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PriorityScoringServiceTest {

    @Autowired
    private PriorityScoringService priorityScoringService;

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
                "Priority Tester",
                "priority.tester@puducherry.gov.in",
                "pass123",
                UserRole.CITIZEN
        ));
        reporterId = reporter.getId();
    }

    @Test
    void shouldCalculateLowPriorityForIsolatedLowSeverityIncident() {
        IncidentResponse incident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Minor Street Cleaning", "Small litter on pavement",
                IncidentCategory.GARBAGE, IncidentSeverity.LOW,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "White Town"
        ));

        PriorityScoreResponse response = priorityScoringService.calculatePriority(incident.getId());

        assertThat(response.getIncidentId()).isEqualTo(incident.getId());
        assertThat(response.getFactors().getSeverityFactor()).isEqualTo(0.25);
        assertThat(response.getFactors().getReportVolumeFactor()).isEqualTo(0.00);
        assertThat(response.getFactors().getLocationImpactFactor()).isEqualTo(0.00);
        assertThat(response.getFactors().getRainfallFactor()).isEqualTo(0.00);
        // (0.25 * 0.40) * 100 = 10.0
        assertThat(response.getPriorityScore()).isEqualTo(10.0);
        assertThat(response.getPriorityLevel()).isEqualTo(PriorityLevel.LOW);
        assertThat(response.getReasons()).contains(
                "Incident severity is LOW",
                "Single report filed for this incident",
                "No nearby related incidents reported",
                "Rainfall data unavailable; no environmental contribution applied"
        );
    }

    @Test
    void shouldCalculateCorrectScoreForMediumSeverityIncident() {
        IncidentResponse incident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Streetlight Out", "Streetlight flickering",
                IncidentCategory.STREETLIGHT, IncidentSeverity.MEDIUM,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Mission St"
        ));

        PriorityScoreResponse response = priorityScoringService.calculatePriority(incident.getId());

        // (0.50 * 0.40) * 100 = 20.0 -> LOW level (0-24)
        assertThat(response.getFactors().getSeverityFactor()).isEqualTo(0.50);
        assertThat(response.getPriorityScore()).isEqualTo(20.0);
        assertThat(response.getPriorityLevel()).isEqualTo(PriorityLevel.LOW);
        assertThat(response.getReasons()).contains("Incident severity is MEDIUM");
    }

    @Test
    void shouldIncreaseScoreForHighSeverityWithMultipleReports() {
        // Main incident
        IncidentResponse mainIncident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Big Pothole A", "Deep hole in road",
                IncidentCategory.POTHOLE, IncidentSeverity.HIGH,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "ECR Road"
        ));

        // Duplicate report nearby (~15m away)
        incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Big Pothole B", "Dangerous pothole on ECR",
                IncidentCategory.POTHOLE, IncidentSeverity.HIGH,
                new BigDecimal("11.9417"), new BigDecimal("79.8084"), "ECR Road"
        ));

        PriorityScoreResponse response = priorityScoringService.calculatePriority(mainIncident.getId());

        // Severity HIGH = 0.75 (0.75 * 0.40 = 0.30)
        // Report volume = 2 reports -> 0.50 (0.50 * 0.30 = 0.15)
        // Location impact = 1 nearby same category -> 0.50 (0.50 * 0.20 = 0.10)
        // Rainfall = 0.00
        // Score = (0.30 + 0.15 + 0.10) * 100 = 55.0 -> HIGH level (50-74)
        assertThat(response.getFactors().getSeverityFactor()).isEqualTo(0.75);
        assertThat(response.getFactors().getReportVolumeFactor()).isEqualTo(0.50);
        assertThat(response.getFactors().getLocationImpactFactor()).isEqualTo(0.50);
        assertThat(response.getPriorityScore()).isEqualTo(55.0);
        assertThat(response.getPriorityLevel()).isEqualTo(PriorityLevel.HIGH);
        assertThat(response.getReasons()).contains(
                "Incident severity is HIGH",
                "2 reports indicate the same civic issue",
                "Nearby related incidents increase local impact"
        );
    }

    @Test
    void shouldReachCriticalPriorityForCriticalSeverityWithVolumeAndImpact() {
        IncidentResponse mainIncident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Severe Drain Burst A", "Major sewage burst",
                IncidentCategory.DRAINAGE, IncidentSeverity.CRITICAL,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Bussy St"
        ));

        // 2 duplicates nearby
        incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Severe Drain Burst B", "Major sewage overflow",
                IncidentCategory.DRAINAGE, IncidentSeverity.CRITICAL,
                new BigDecimal("11.9417"), new BigDecimal("79.8084"), "Bussy St"
        ));
        incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Severe Drain Burst C", "Sewage flooding road",
                IncidentCategory.DRAINAGE, IncidentSeverity.CRITICAL,
                new BigDecimal("11.9418"), new BigDecimal("79.8085"), "Bussy St"
        ));

        PriorityScoreResponse response = priorityScoringService.calculatePriority(mainIncident.getId());

        // Severity CRITICAL = 1.00 (0.40)
        // Report volume = 3 reports -> 1.00 (0.30)
        // Location impact = 2 nearby same category -> 0.50 (0.10)
        // Rainfall = 0.00
        // Score = (0.40 + 0.30 + 0.10) * 100 = 80.0 -> CRITICAL level (75-100)
        assertThat(response.getFactors().getSeverityFactor()).isEqualTo(1.00);
        assertThat(response.getFactors().getReportVolumeFactor()).isEqualTo(1.00);
        assertThat(response.getFactors().getLocationImpactFactor()).isEqualTo(0.50);
        assertThat(response.getPriorityScore()).isEqualTo(80.0);
        assertThat(response.getPriorityLevel()).isEqualTo(PriorityLevel.CRITICAL);
    }

    @Test
    void shouldExcludeUnrelatedCategoriesFromLocationImpact() {
        // Target: Pothole
        IncidentResponse mainIncident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Pothole Main", "Deep pothole",
                IncidentCategory.POTHOLE, IncidentSeverity.LOW,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Main St"
        ));

        // 3 nearby incidents of DIFFERENT categories (GARBAGE, STREETLIGHT, DRAINAGE)
        incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Garbage nearby", "Garbage bin overflowing",
                IncidentCategory.GARBAGE, IncidentSeverity.MEDIUM,
                new BigDecimal("11.9417"), new BigDecimal("79.8084"), "Main St"
        ));
        incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Streetlight nearby", "Light broken",
                IncidentCategory.STREETLIGHT, IncidentSeverity.LOW,
                new BigDecimal("11.9418"), new BigDecimal("79.8085"), "Main St"
        ));

        PriorityScoreResponse response = priorityScoringService.calculatePriority(mainIncident.getId());

        // Location impact must strictly consider SAME category -> 0.00
        assertThat(response.getFactors().getLocationImpactFactor()).isEqualTo(0.00);
        assertThat(response.getReasons()).contains("No nearby related incidents reported");
    }

    @Test
    void shouldIncorporateRainfallFactorWhenProvided() {
        IncidentResponse incident = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Waterlogging Risk", "Water accumulating",
                IncidentCategory.WATER_SUPPLY, IncidentSeverity.HIGH,
                new BigDecimal("11.9416"), new BigDecimal("79.8083"), "Beach Road"
        ));

        // Supply heavy rainfall (50mm -> factor 0.75)
        PriorityScoreResponse response = priorityScoringService.calculatePriority(incident.getId(), 50.0);

        // Severity HIGH = 0.75 (0.30)
        // Volume 1 = 0.00 (0.00)
        // Impact 0 = 0.00 (0.00)
        // Rainfall 50mm -> 0.75 (0.75 * 0.10 = 0.075 -> 7.5 points)
        // Score = (0.30 + 0.075) * 100 = 37.5 -> MEDIUM level
        assertThat(response.getFactors().getRainfallFactor()).isEqualTo(0.75);
        assertThat(response.getPriorityScore()).isEqualTo(37.5);
        assertThat(response.getPriorityLevel()).isEqualTo(PriorityLevel.MEDIUM);
        assertThat(response.getReasons()).contains("Heavy rainfall detected (50.0 mm)");
    }

    @Test
    void shouldVerifyThresholdBoundaries() {
        // Test boundary logic: <25 LOW, 25-49 MEDIUM, 50-74 HIGH, >=75 CRITICAL
        // 1. Score = 20.0 -> LOW
        IncidentResponse inc1 = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Inc 1", "Desc",
                IncidentCategory.OTHER, IncidentSeverity.MEDIUM,
                new BigDecimal("11.9000"), new BigDecimal("79.8000"), "Addr 1"
        ));
        assertThat(priorityScoringService.calculatePriority(inc1.getId()).getPriorityLevel()).isEqualTo(PriorityLevel.LOW);

        // 2. Score = 30.0 (Severity HIGH 0.75 -> 30.0) -> MEDIUM (25-49)
        IncidentResponse inc2 = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Inc 2", "Desc",
                IncidentCategory.OTHER, IncidentSeverity.HIGH,
                new BigDecimal("11.9100"), new BigDecimal("79.8100"), "Addr 2"
        ));
        assertThat(priorityScoringService.calculatePriority(inc2.getId()).getPriorityLevel()).isEqualTo(PriorityLevel.MEDIUM);

        // 3. Score = 55.0 (HIGH 30 + 2 reports 15 + 1 location 10) -> HIGH (50-74)
        IncidentResponse inc3 = incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Inc 3", "Desc",
                IncidentCategory.OTHER, IncidentSeverity.HIGH,
                new BigDecimal("11.9200"), new BigDecimal("79.8200"), "Addr 3"
        ));
        incidentService.createIncident(new CreateIncidentRequest(
                reporterId, "Inc 3 Dup", "Desc",
                IncidentCategory.OTHER, IncidentSeverity.HIGH,
                new BigDecimal("11.9201"), new BigDecimal("79.8201"), "Addr 3"
        ));
        assertThat(priorityScoringService.calculatePriority(inc3.getId()).getPriorityLevel()).isEqualTo(PriorityLevel.HIGH);
    }

    @Test
    void shouldThrowExceptionForNonExistentIncident() {
        assertThatThrownBy(() -> priorityScoringService.calculatePriority(999999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Incident with ID 999999 not found");
    }
}
