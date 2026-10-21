package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.exception.ResourceNotFoundException;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RiskIntelligenceServiceTest {

    @Autowired
    private RiskIntelligenceService riskIntelligenceService;

    @Autowired
    private UserService userService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RiskAssessmentRepository riskAssessmentRepository;

    @Autowired
    private AnomalyFlagRepository anomalyFlagRepository;

    @Autowired
    private ResolutionRepository resolutionRepository;

    @Autowired
    private IncidentActionRepository incidentActionRepository;

    @Autowired
    private EvidenceRepository evidenceRepository;

    private Long citizenId;
    private Ward ward1;

    @BeforeEach
    void setUp() {
        riskAssessmentRepository.deleteAll();
        anomalyFlagRepository.deleteAll();
        resolutionRepository.deleteAll();
        incidentActionRepository.deleteAll();
        evidenceRepository.deleteAll();
        incidentRepository.deleteAll();
        wardRepository.deleteAll();
        userRepository.deleteAll();

        UserResponse citizen = userService.createUser(new CreateUserRequest("Citizen RiskTest", "risk.citizen@nirvaha.gov", "password", UserRole.CITIZEN));
        citizenId = citizen.getId();

        ward1 = wardRepository.save(new Ward(null, 1, "Boulevard North", new BigDecimal("11.933000"), new BigDecimal("79.830000"), LocalDateTime.now()));
    }

    @Test
    void testRiskLevelBoundaries() {
        assertThat(riskIntelligenceService.determineRiskLevel(0.24)).isEqualTo(RiskLevel.LOW);
        assertThat(riskIntelligenceService.determineRiskLevel(0.25)).isEqualTo(RiskLevel.MODERATE);
        assertThat(riskIntelligenceService.determineRiskLevel(0.49)).isEqualTo(RiskLevel.MODERATE);
        assertThat(riskIntelligenceService.determineRiskLevel(0.50)).isEqualTo(RiskLevel.HIGH);
        assertThat(riskIntelligenceService.determineRiskLevel(0.74)).isEqualTo(RiskLevel.HIGH);
        assertThat(riskIntelligenceService.determineRiskLevel(0.75)).isEqualTo(RiskLevel.CRITICAL);
        assertThat(riskIntelligenceService.determineRiskLevel(1.00)).isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void testEvaluateWardRisk_NoIncidents_LowRisk() {
        WardRiskResponse response = riskIntelligenceService.evaluateWardRisk(ward1.getId(), IncidentCategory.DRAINAGE);

        assertThat(response.getWardId()).isEqualTo(ward1.getId());
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(response.getRiskScore()).isLessThanOrEqualTo(0.24);
        assertThat(response.getWeatherContext()).isNotNull();
        assertThat(response.getRecommendedAction()).contains("Routine monitoring");
    }

    @Test
    void testEvaluateWardRisk_IncreasingIncidents_IncreasesRisk() {
        for (int i = 1; i <= 5; i++) {
            incidentService.createIncident(new CreateIncidentRequest(
                    citizenId, "Drainage issue " + i, "Overflow on street " + i,
                    IncidentCategory.DRAINAGE, IncidentSeverity.HIGH,
                    new BigDecimal("11.933" + i), new BigDecimal("79.830000"), "Street " + i
            ));
        }

        WardRiskResponse response = riskIntelligenceService.evaluateWardRisk(ward1.getId(), IncidentCategory.DRAINAGE);

        assertThat(response.getRiskScore()).isGreaterThan(0.25);
        assertThat(response.getFactors()).hasSize(6);
        assertThat(response.getExplanation()).contains("Boulevard North");
        assertThat(response.getRecommendedAction()).contains("drainage hotspots");
    }

    @Test
    void testTimeDecay_RecentIncidentVsOlderIncident() {
        // Create an older incident (25 days old)
        Incident oldInc = new Incident(null, userRepository.findById(citizenId).orElseThrow(), "Old Drainage", "Desc",
                IncidentCategory.DRAINAGE, IncidentSeverity.MEDIUM, IncidentStatus.REPORTED,
                new BigDecimal("11.933000"), new BigDecimal("79.830000"), "Address",
                LocalDateTime.now().minusDays(25), LocalDateTime.now().minusDays(25));
        incidentRepository.save(oldInc);

        WardRiskResponse oldResponse = riskIntelligenceService.evaluateWardRisk(ward1.getId(), IncidentCategory.DRAINAGE);
        Double oldRecentFactor = oldResponse.getFactors().stream()
                .filter(f -> f.getName().contains("Recent Activity"))
                .map(RiskFactorContribution::getValue)
                .findFirst().orElse(0.0);

        // Reset and create a brand new incident (today)
        incidentRepository.deleteAll();
        incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "New Drainage", "Desc", IncidentCategory.DRAINAGE,
                IncidentSeverity.MEDIUM, new BigDecimal("11.933000"), new BigDecimal("79.830000"), "Address"
        ));

        WardRiskResponse newResponse = riskIntelligenceService.evaluateWardRisk(ward1.getId(), IncidentCategory.DRAINAGE);
        Double newRecentFactor = newResponse.getFactors().stream()
                .filter(f -> f.getName().contains("Recent Activity"))
                .map(RiskFactorContribution::getValue)
                .findFirst().orElse(0.0);

        assertThat(newRecentFactor).isGreaterThan(oldRecentFactor);
    }

    @Test
    void testSpatialConcentration_IncidentsWithin500mIncreaseRisk() {
        // Create 4 incidents within 100 meters
        for (int i = 1; i <= 4; i++) {
            incidentService.createIncident(new CreateIncidentRequest(
                    citizenId, "Clustered Pothole " + i, "Desc " + i,
                    IncidentCategory.POTHOLE, IncidentSeverity.HIGH,
                    new BigDecimal("11.93300" + i), new BigDecimal("79.830000"), "Close Location"
            ));
        }

        WardRiskResponse response = riskIntelligenceService.evaluateWardRisk(ward1.getId(), IncidentCategory.POTHOLE);

        Double spatialVal = response.getFactors().stream()
                .filter(f -> f.getName().contains("Spatial Concentration"))
                .map(RiskFactorContribution::getValue)
                .findFirst().orElse(0.0);

        assertThat(spatialVal).isGreaterThan(0.5);
    }

    @Test
    void testWeatherFallback_WhenWeatherServiceReturnsFallback() {
        // Test fallback WeatherData behavior
        WeatherData fallback = WeatherData.fallback(null);
        assertThat(fallback.getAvailable()).isFalse();
        assertThat(fallback.getSourceMessage()).contains("Weather data unavailable; risk calculated from civic history and spatial signals.");
    }

    @Test
    void testValidation_MissingWard_Throws404() {
        assertThatThrownBy(() -> riskIntelligenceService.evaluateWardRisk(999L, IncidentCategory.DRAINAGE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ward with ID 999 not found");
    }
}
