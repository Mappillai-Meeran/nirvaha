package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.WeatherObservation;
import com.nirvaha.nirvaha_backend.service.ImdWeatherAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ImdWeatherAdapterTest {

    @Autowired
    private ImdWeatherAdapter imdWeatherAdapter;

    @Test
    void testRecordObservationAndDeduplication() {
        LocalDateTime now = LocalDateTime.now();
        WeatherObservation obs1 = imdWeatherAdapter.recordObservationIfMissing(
                "IMD", "TEST_STATION_01", "DAILY_RAINFALL", now, now.minusDays(1), now, 55.0, 10.0, 450.0, "HEAVY", null, "REC-01"
        );
        assertThat(obs1).isNotNull();

        // Attempting to insert identical observation returns existing record
        WeatherObservation obs2 = imdWeatherAdapter.recordObservationIfMissing(
                "IMD", "TEST_STATION_01", "DAILY_RAINFALL", now, now.minusDays(1), now, 55.0, 10.0, 450.0, "HEAVY", null, "REC-01"
        );
        assertThat(obs2.getId()).isEqualTo(obs1.getId());
    }

    @Test
    void testGenerateRainfallContextForIncident() {
        Incident incident = new Incident();
        incident.setCategory(IncidentCategory.DRAINAGE);
        incident.setCreatedAt(LocalDateTime.now());

        String context = imdWeatherAdapter.generateRainfallContextForIncident(incident);
        assertThat(context).isNotNull();
        assertThat(context).contains("IMD");
    }
}
