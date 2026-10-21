package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.controller.ProvenanceController;
import com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData;
import com.nirvaha.nirvaha_backend.dto.RoadImportSummary;
import com.nirvaha.nirvaha_backend.dto.RoadMatchContext;
import com.nirvaha.nirvaha_backend.exception.GlobalExceptionHandler;
import com.nirvaha.nirvaha_backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProvenanceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DataSourceService dataSourceService;

    @Mock
    private AdministrativeAreaService administrativeAreaService;

    @Mock
    private ImdWeatherAdapter imdWeatherAdapter;

    @Mock
    private ServiceRuleService serviceRuleService;

    @Mock
    private WeatherService weatherService;

    @Mock
    private RoadReferenceService roadReferenceService;

    @InjectMocks
    private ProvenanceController provenanceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(provenanceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. Returns historical precipitation telemetry with valid dates and provenance")
    void shouldReturnHistoricalPrecipitation() throws Exception {
        HistoricalPrecipitationData mockData = new HistoricalPrecipitationData();
        mockData.setAvailable(true);
        mockData.setTotalRainfallMm(18.6);
        mockData.setMaxHourlyRainfallMm(12.4);
        mockData.setProvider("Open-Meteo Historical Weather Archive");
        mockData.setLicense("Creative Commons Attribution 4.0 International (CC BY 4.0)");
        mockData.setProvenanceClass("OFFICIAL_EXTERNAL_DATA");
        mockData.setHourlyData(List.of(
                new HistoricalPrecipitationData.HourlyPrecipitationPoint("2024-11-20T00:00", 0.0),
                new HistoricalPrecipitationData.HourlyPrecipitationPoint("2024-11-20T01:00", 12.4)
        ));

        when(weatherService.fetchHistoricalPrecipitation(any(), any(), eq("2024-11-20"), eq("2024-11-21")))
                .thenReturn(mockData);

        mockMvc.perform(get("/api/provenance/weather/history")
                        .param("latitude", "11.9156")
                        .param("longitude", "79.8296")
                        .param("startDate", "2024-11-20")
                        .param("endDate", "2024-11-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.totalRainfallMm").value(18.6))
                .andExpect(jsonPath("$.maxHourlyRainfallMm").value(12.4))
                .andExpect(jsonPath("$.provider").value("Open-Meteo Historical Weather Archive"))
                .andExpect(jsonPath("$.license").value("Creative Commons Attribution 4.0 International (CC BY 4.0)"))
                .andExpect(jsonPath("$.provenanceClass").value("OFFICIAL_EXTERNAL_DATA"))
                .andExpect(jsonPath("$.hourlyData.length()").value(2));
    }

    @Test
    @DisplayName("2. Returns road reference layer summary with ODbL attribution")
    void shouldReturnRoadSummary() throws Exception {
        RoadImportSummary summary = new RoadImportSummary();
        summary.setAcceptedCount(221);
        summary.setTotalLengthKm(85.4);
        summary.setExtractVersion("OSM-PUDUCHERRY-2026.10-BOUNDED");
        summary.setStatus("SUCCESS");

        when(roadReferenceService.getLatestSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/provenance/roads/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedCount").value(221))
                .andExpect(jsonPath("$.totalLengthKm").value(85.4))
                .andExpect(jsonPath("$.extractVersion").value("OSM-PUDUCHERRY-2026.10-BOUNDED"))
                .andExpect(jsonPath("$.sourceAttribution").value("© OpenStreetMap contributors, Open Database License (ODbL) 1.0"));
    }

    @Test
    @DisplayName("3. Returns nearest road match context for coordinates")
    void shouldReturnNearestRoadMatch() throws Exception {
        RoadMatchContext match = RoadMatchContext.match(
                32762585L,
                "Bussy Street",
                "tertiary",
                "RC-3",
                14.2,
                0.95,
                "Snapped to OpenStreetMap tertiary road 'Bussy Street' [RC-3] (14.2m away)."
        );

        when(roadReferenceService.findNearestRoad(any(BigDecimal.class), any(BigDecimal.class), eq(300.0)))
                .thenReturn(match);

        mockMvc.perform(get("/api/provenance/roads/nearest")
                        .param("latitude", "11.9281")
                        .param("longitude", "79.8349")
                        .param("maxRadiusMeters", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roadFound").value(true))
                .andExpect(jsonPath("$.roadName").value("Bussy Street"))
                .andExpect(jsonPath("$.highwayType").value("tertiary"))
                .andExpect(jsonPath("$.roadReference").value("RC-3"))
                .andExpect(jsonPath("$.distanceMeters").value(14.2))
                .andExpect(jsonPath("$.confidence").value(0.95))
                .andExpect(jsonPath("$.source").value("OpenStreetMap Contributors"))
                .andExpect(jsonPath("$.provenanceClass").value("OFFICIAL_EXTERNAL_DATA"));
    }
}
