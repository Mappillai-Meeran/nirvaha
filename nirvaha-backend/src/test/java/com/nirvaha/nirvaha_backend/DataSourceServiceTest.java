package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.entity.DataSource;
import com.nirvaha.nirvaha_backend.entity.FreshnessStatus;
import com.nirvaha.nirvaha_backend.entity.SourceType;
import com.nirvaha.nirvaha_backend.service.DataSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSourceServiceTest {

    @Autowired
    private DataSourceService dataSourceService;

    @Test
    void testSeededDataSources() {
        List<DataSource> sources = dataSourceService.getAllDataSources();
        assertThat(sources).isNotEmpty();
        assertThat(sources).anyMatch(s -> s.getName().contains("Puducherry Local Administration Department"));
        assertThat(sources).anyMatch(s -> s.getName().contains("India Meteorological Department"));
        assertThat(sources).anyMatch(s -> s.getName().contains("Puducherry Public Works Department"));
    }

    @Test
    void testCalculateFreshness() {
        DataSource source = new DataSource();
        source.setUpdateFrequency("DAILY");
        source.setRetrievedAt(LocalDateTime.now().minusHours(5));

        FreshnessStatus freshness = dataSourceService.calculateFreshness(source);
        assertThat(freshness).isEqualTo(FreshnessStatus.CURRENT);

        source.setRetrievedAt(LocalDateTime.now().minusDays(10));
        freshness = dataSourceService.calculateFreshness(source);
        assertThat(freshness).isEqualTo(FreshnessStatus.OUTDATED);
    }
}
