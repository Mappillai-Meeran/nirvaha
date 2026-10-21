package com.nirvaha.nirvaha_backend.controller;

import com.nirvaha.nirvaha_backend.entity.AdministrativeArea;
import com.nirvaha.nirvaha_backend.entity.DataSource;
import com.nirvaha.nirvaha_backend.entity.FreshnessStatus;
import com.nirvaha.nirvaha_backend.entity.ServiceRule;
import com.nirvaha.nirvaha_backend.entity.WeatherObservation;
import com.nirvaha.nirvaha_backend.service.AdministrativeAreaService;
import com.nirvaha.nirvaha_backend.service.DataSourceService;
import com.nirvaha.nirvaha_backend.service.ImdWeatherAdapter;
import com.nirvaha.nirvaha_backend.service.ServiceRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/provenance")
public class ProvenanceController {

    private final DataSourceService dataSourceService;
    private final AdministrativeAreaService administrativeAreaService;
    private final ImdWeatherAdapter imdWeatherAdapter;
    private final ServiceRuleService serviceRuleService;
    private final com.nirvaha.nirvaha_backend.service.WeatherService weatherService;
    private final com.nirvaha.nirvaha_backend.service.RoadReferenceService roadReferenceService;

    public ProvenanceController(DataSourceService dataSourceService,
                                AdministrativeAreaService administrativeAreaService,
                                ImdWeatherAdapter imdWeatherAdapter,
                                ServiceRuleService serviceRuleService,
                                com.nirvaha.nirvaha_backend.service.WeatherService weatherService,
                                com.nirvaha.nirvaha_backend.service.RoadReferenceService roadReferenceService) {
        this.dataSourceService = dataSourceService;
        this.administrativeAreaService = administrativeAreaService;
        this.imdWeatherAdapter = imdWeatherAdapter;
        this.serviceRuleService = serviceRuleService;
        this.weatherService = weatherService;
        this.roadReferenceService = roadReferenceService;
    }

    @GetMapping("/sources")
    public ResponseEntity<List<Map<String, Object>>> getAllSources() {
        List<DataSource> sources = dataSourceService.getAllDataSources();
        List<Map<String, Object>> response = sources.stream().map(source -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", source.getId());
            map.put("name", source.getName());
            map.put("provider", source.getProvider());
            map.put("sourceType", source.getSourceType() != null ? source.getSourceType().name() : null);
            map.put("sourceUrl", source.getSourceUrl());
            map.put("datasetName", source.getDatasetName());
            map.put("license", source.getLicense());
            map.put("retrievalMethod", source.getRetrievalMethod());
            map.put("updateFrequency", source.getUpdateFrequency());
            map.put("retrievedAt", source.getRetrievedAt());
            map.put("publishedAt", source.getPublishedAt());
            map.put("version", source.getVersion());
            map.put("coverage", source.getCoverage());
            map.put("status", source.getStatus());
            map.put("freshness", dataSourceService.calculateFreshness(source).name());
            map.put("notes", source.getNotes());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sources/{id}")
    public ResponseEntity<Map<String, Object>> getSourceById(@PathVariable Long id) {
        return dataSourceService.getDataSourceById(id)
                .map(source -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", source.getId());
                    map.put("name", source.getName());
                    map.put("provider", source.getProvider());
                    map.put("sourceType", source.getSourceType() != null ? source.getSourceType().name() : null);
                    map.put("sourceUrl", source.getSourceUrl());
                    map.put("datasetName", source.getDatasetName());
                    map.put("license", source.getLicense());
                    map.put("retrievedAt", source.getRetrievedAt());
                    map.put("freshness", dataSourceService.calculateFreshness(source).name());
                    map.put("notes", source.getNotes());
                    return ResponseEntity.ok(map);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/areas")
    public ResponseEntity<List<AdministrativeArea>> getAllAdministrativeAreas() {
        return ResponseEntity.ok(administrativeAreaService.getAllAdministrativeAreas());
    }

    @GetMapping("/weather")
    public ResponseEntity<List<WeatherObservation>> getRecentWeather() {
        return ResponseEntity.ok(imdWeatherAdapter.getRecentObservations());
    }

    @GetMapping("/weather/live")
    public ResponseEntity<com.nirvaha.nirvaha_backend.dto.WeatherData> getLiveWeather(
            @RequestParam(required = false, defaultValue = "11.9156") java.math.BigDecimal latitude,
            @RequestParam(required = false, defaultValue = "79.8296") java.math.BigDecimal longitude) {
        return ResponseEntity.ok(weatherService.fetchWeatherData(latitude, longitude));
    }

    @GetMapping("/weather/history")
    public ResponseEntity<com.nirvaha.nirvaha_backend.dto.HistoricalPrecipitationData> getHistoricalPrecipitation(
            @RequestParam(required = false, defaultValue = "11.9156") java.math.BigDecimal latitude,
            @RequestParam(required = false, defaultValue = "79.8296") java.math.BigDecimal longitude,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // Default to the past 7 days if not specified
        if (endDate == null || endDate.isBlank()) {
            endDate = java.time.LocalDate.now().minusDays(1).toString();
        }
        if (startDate == null || startDate.isBlank()) {
            startDate = java.time.LocalDate.parse(endDate).minusDays(6).toString();
        }
        return ResponseEntity.ok(weatherService.fetchHistoricalPrecipitation(latitude, longitude, startDate, endDate));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<ServiceRule>> getAllServiceRules() {
        return ResponseEntity.ok(serviceRuleService.getAllServiceRules());
    }

    @GetMapping("/roads/summary")
    public ResponseEntity<com.nirvaha.nirvaha_backend.dto.RoadImportSummary> getRoadSummary() {
        return ResponseEntity.ok(roadReferenceService.getLatestSummary());
    }

    @GetMapping("/roads/nearest")
    public ResponseEntity<com.nirvaha.nirvaha_backend.dto.RoadMatchContext> getNearestRoad(
            @RequestParam java.math.BigDecimal latitude,
            @RequestParam java.math.BigDecimal longitude,
            @RequestParam(required = false, defaultValue = "300") Double maxRadiusMeters) {
        return ResponseEntity.ok(roadReferenceService.findNearestRoad(latitude, longitude, maxRadiusMeters != null ? maxRadiusMeters : 300.0));
    }
}
