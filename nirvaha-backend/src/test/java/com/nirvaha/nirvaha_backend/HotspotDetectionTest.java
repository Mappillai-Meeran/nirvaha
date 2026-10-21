package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.DataClassification;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.service.HotspotDetectionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HotspotDetectionTest {

    private final HotspotDetectionService hotspotService = new HotspotDetectionService();

    @Test
    void testDetectHotspotClusterWithin500m() {
        List<Incident> incidents = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 3 Drainage incidents within 100m of each other (Lat: 11.9400, Lng: 79.8000)
        for (int i = 0; i < 3; i++) {
            Incident inc = new Incident();
            inc.setId((long) (i + 1));
            inc.setCategory(IncidentCategory.DRAINAGE);
            inc.setStatus(IncidentStatus.REPORTED);
            inc.setClassificationTag(DataClassification.CITIZEN);
            inc.setCreatedAt(now);
            inc.setLatitude(BigDecimal.valueOf(11.9400 + (i * 0.0001)));
            inc.setLongitude(BigDecimal.valueOf(79.8000 + (i * 0.0001)));
            incidents.add(inc);
        }

        List<HotspotDetectionService.SpatialCluster> hotspots = hotspotService.detectHotspots(incidents);
        assertThat(hotspots).hasSize(1);
        assertThat(hotspots.get(0).category).isEqualTo(IncidentCategory.DRAINAGE);
        assertThat(hotspots.get(0).incidents).hasSize(3);
    }
}
