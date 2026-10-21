package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.DataClassification;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.service.IntelligenceEligibilityService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IntelligenceEligibilityTest {

    private final IntelligenceEligibilityService eligibilityService = new IntelligenceEligibilityService();

    @Test
    void shouldExcludeSimulatedDataFromProductionIntelligence() {
        Incident simulated = new Incident();
        simulated.setClassificationTag(DataClassification.SIMULATED);
        simulated.setCreatedAt(LocalDateTime.now());
        simulated.setCategory(IncidentCategory.DRAINAGE);
        simulated.setStatus(IncidentStatus.REPORTED);

        boolean eligible = eligibilityService.isEligibleForIntelligence(simulated);
        assertThat(eligible).isFalse();
    }

    @Test
    void shouldIncludeValidCitizenIncidents() {
        Incident citizenInc = new Incident();
        citizenInc.setClassificationTag(DataClassification.CITIZEN);
        citizenInc.setCreatedAt(LocalDateTime.now());
        citizenInc.setCategory(IncidentCategory.DRAINAGE);
        citizenInc.setStatus(IncidentStatus.REPORTED);

        boolean eligible = eligibilityService.isEligibleForIntelligence(citizenInc);
        assertThat(eligible).isTrue();
    }

    @Test
    void shouldRequireValidCoordinatesForGeospatialIntelligence() {
        Incident incNoCoords = new Incident();
        incNoCoords.setClassificationTag(DataClassification.CITIZEN);
        incNoCoords.setCreatedAt(LocalDateTime.now());
        incNoCoords.setCategory(IncidentCategory.POTHOLE);
        incNoCoords.setStatus(IncidentStatus.REPORTED);
        incNoCoords.setLatitude(null);

        assertThat(eligibilityService.isEligibleForGeospatialIntelligence(incNoCoords)).isFalse();

        incNoCoords.setLatitude(BigDecimal.valueOf(11.9400));
        incNoCoords.setLongitude(BigDecimal.valueOf(79.8000));
        assertThat(eligibilityService.isEligibleForGeospatialIntelligence(incNoCoords)).isTrue();
    }
}
