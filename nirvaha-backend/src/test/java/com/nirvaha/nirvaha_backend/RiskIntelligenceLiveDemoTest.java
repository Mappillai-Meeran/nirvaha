package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.*;
import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.*;
import com.nirvaha.nirvaha_backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RiskIntelligenceLiveDemoTest {

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
    private Ward wardBoulevard;
    private Ward wardMudaliarpet;

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

        UserResponse citizen = userService.createUser(new CreateUserRequest("Command Center Demo Citizen", "citizen.riskdemo@nirvaha.gov", "password", UserRole.CITIZEN));
        citizenId = citizen.getId();

        wardBoulevard = wardRepository.save(new Ward(null, 1, "Boulevard North", new BigDecimal("11.933000"), new BigDecimal("79.830000"), LocalDateTime.now()));
        wardMudaliarpet = wardRepository.save(new Ward(null, 2, "Mudaliarpet", new BigDecimal("11.915000"), new BigDecimal("79.815000"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("Live Demo: Government Command Center Ward Risk Intelligence Overview")
    void liveDemo_WardRiskOverview() {
        // Create 6 drainage incidents in Boulevard Ward (high risk density)
        for (int i = 1; i <= 6; i++) {
            incidentService.createIncident(new CreateIncidentRequest(
                    citizenId, "Severe Drainage Overflow " + i, "Storm drain blocked near street " + i,
                    IncidentCategory.DRAINAGE, IncidentSeverity.HIGH,
                    new BigDecimal("11.933" + i), new BigDecimal("79.830000"), "Boulevard Street " + i
            ));
        }

        // Create 1 minor streetlight issue in Mudaliarpet Ward (low risk)
        incidentService.createIncident(new CreateIncidentRequest(
                citizenId, "Dark Lamp Post", "Streetlight flickering",
                IncidentCategory.STREETLIGHT, IncidentSeverity.LOW,
                new BigDecimal("11.915100"), new BigDecimal("79.815100"), "Mudaliarpet Main"
        ));

        // Evaluate Ward 1
        WardRiskResponse boulevardRisk = riskIntelligenceService.evaluateWardRisk(wardBoulevard.getId(), IncidentCategory.DRAINAGE);

        System.out.println("\n=======================================================");
        System.out.println("  NIRVAHA GOVERNMENT COMMAND CENTER RISK INTELLIGENCE  ");
        System.out.println("=======================================================");
        System.out.println("Ward ID: " + boulevardRisk.getWardId());
        System.out.println("Ward Name: " + boulevardRisk.getWardName() + " (Ward #" + boulevardRisk.getWardNumber() + ")");
        System.out.println("Top Category Risk: " + boulevardRisk.getTopRiskCategory());
        System.out.println("Overall Risk Score: " + boulevardRisk.getRiskScore() + " (" + boulevardRisk.getRiskScore100() + "/100)");
        System.out.println("Risk Level: " + boulevardRisk.getRiskLevel());
        System.out.println("Active Incidents: " + boulevardRisk.getActiveIncidentCount());
        System.out.println("Unresolved Incidents: " + boulevardRisk.getUnresolvedIncidentCount());
        System.out.println("Weather Context: " + boulevardRisk.getWeatherContext());
        System.out.println("Explanation: " + boulevardRisk.getExplanation());
        System.out.println("Recommended Action: " + boulevardRisk.getRecommendedAction());
        System.out.println("-------------------------------------------------------");
        System.out.println("CONTRIBUTING SIGNAL FACTOR BREAKDOWN:");
        boulevardRisk.getFactors().forEach(f ->
                System.out.printf("  - %-30s | Value: %.2f | Contribution: %.3f | %s%n",
                        f.getName(), f.getValue(), f.getContribution(), f.getDescription())
        );
        System.out.println("=======================================================\n");

        assertThat(boulevardRisk.getRiskScore()).isGreaterThanOrEqualTo(0.50);
        assertThat(boulevardRisk.getRiskLevel()).isIn(RiskLevel.HIGH, RiskLevel.CRITICAL);
        assertThat(boulevardRisk.getExplanation()).contains("Boulevard North");
        assertThat(boulevardRisk.getRecommendedAction()).contains("Inspect drainage hotspots");
        assertThat(riskAssessmentRepository.count()).isGreaterThanOrEqualTo(1);
    }
}
