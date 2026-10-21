package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.DataClassification;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.service.EmergingIssueService;
import com.nirvaha.nirvaha_backend.service.TrendAnalysisService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmergingIssueTest {

    private final EmergingIssueService emergingService = new EmergingIssueService();
    private final TrendAnalysisService trendService = new TrendAnalysisService();

    @Test
    void testDetectEmergingIssueOnSurgingActivity() {
        LocalDateTime now = LocalDateTime.now();
        List<Incident> current7d = new ArrayList<>();
        List<Incident> previous7d = new ArrayList<>();

        // 5 reports in current period
        for (int i = 0; i < 5; i++) {
            Incident inc = new Incident();
            inc.setCategory(IncidentCategory.WATER_SUPPLY);
            inc.setStatus(IncidentStatus.REPORTED);
            inc.setClassificationTag(DataClassification.CITIZEN);
            inc.setCreatedAt(now.minusDays(1));
            current7d.add(inc);
        }

        // 1 report in previous period (+400% surge)
        Incident prevInc = new Incident();
        prevInc.setCategory(IncidentCategory.WATER_SUPPLY);
        prevInc.setStatus(IncidentStatus.REPORTED);
        prevInc.setClassificationTag(DataClassification.CITIZEN);
        prevInc.setCreatedAt(now.minusDays(10));
        previous7d.add(prevInc);

        List<EmergingIssueService.EmergingCandidate> emerging = emergingService.detectEmergingIssues(current7d, previous7d, trendService);
        assertThat(emerging).hasSize(1);
        assertThat(emerging.get(0).category).isEqualTo(IncidentCategory.WATER_SUPPLY);
        assertThat(emerging.get(0).changePercentage).isGreaterThanOrEqualTo(50.0);
    }
}
