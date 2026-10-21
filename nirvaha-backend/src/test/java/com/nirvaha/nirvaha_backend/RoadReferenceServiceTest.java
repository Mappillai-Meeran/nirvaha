package com.nirvaha.nirvaha_backend;

import com.nirvaha.nirvaha_backend.dto.RoadImportSummary;
import com.nirvaha.nirvaha_backend.dto.RoadMatchContext;
import com.nirvaha.nirvaha_backend.entity.RoadSegment;
import com.nirvaha.nirvaha_backend.entity.ValidationStatus;
import com.nirvaha.nirvaha_backend.repository.RoadSegmentRepository;
import com.nirvaha.nirvaha_backend.service.RoadReferenceService;
import com.nirvaha.nirvaha_backend.util.GeospatialUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RoadReferenceServiceTest {

    @Autowired
    private RoadReferenceService roadReferenceService;

    @Autowired
    private RoadSegmentRepository roadSegmentRepository;

    @BeforeEach
    void setUp() {
        roadSegmentRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Successfully imports valid GeoJSON LineString road features")
    void testValidGeoJsonRoadImport() {
        String validGeoJson = """
        {
          "type": "FeatureCollection",
          "metadata": {
            "version": "TEST-OSM-2026.10",
            "provider": "OpenStreetMap Contributors"
          },
          "features": [
            {
              "type": "Feature",
              "id": 10001,
              "properties": {
                "name": "Anna Salai",
                "highway": "primary",
                "ref": "RC-1",
                "surface": "asphalt"
              },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [79.825000, 11.930000],
                  [79.830000, 11.935000]
                ]
              }
            }
          ]
        }
        """;

        RoadImportSummary summary = new RoadImportSummary();
        roadReferenceService.importFromInputStream(new ByteArrayInputStream(validGeoJson.getBytes(StandardCharsets.UTF_8)), summary);

        assertThat(summary.getStatus()).isEqualTo("SUCCESS");
        assertThat(summary.getAcceptedCount()).isEqualTo(1);
        assertThat(summary.getRejectedCount()).isEqualTo(0);

        List<RoadSegment> segments = roadSegmentRepository.findAll();
        assertThat(segments).hasSize(1);
        RoadSegment road = segments.get(0);
        assertThat(road.getName()).isEqualTo("Anna Salai");
        assertThat(road.getOsmFeatureId()).isEqualTo(10001L);
        assertThat(road.getHighwayType()).isEqualTo("primary");
        assertThat(road.getRoadReference()).isEqualTo("RC-1");
        assertThat(road.getValidationStatus()).isEqualTo(ValidationStatus.VERIFIED);
        assertThat(road.getSource()).isEqualTo("OpenStreetMap Contributors");
        assertThat(road.getLicense()).contains("ODbL");
        assertThat(road.getProvenanceClass()).isEqualTo("OFFICIAL_EXTERNAL_DATA");
        assertThat(road.getLengthMeters()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("2. Rejects malformed input, non-LineString geometries, and invalid coordinates")
    void testRejectsMalformedAndInvalidGeometries() {
        String malformedGeoJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "id": 20001,
              "properties": { "name": "Point Road" },
              "geometry": {
                "type": "Point",
                "coordinates": [79.825000, 11.930000]
              }
            },
            {
              "type": "Feature",
              "id": 20002,
              "properties": { "name": "Single Point Line" },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [79.825000, 11.930000]
                ]
              }
            },
            {
              "type": "Feature",
              "id": 20003,
              "properties": { "name": "Out Of Bounds Road (Delhi)" },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [77.209000, 28.613900],
                  [77.210000, 28.614000]
                ]
              }
            },
            {
              "type": "Feature",
              "properties": { "name": "Missing ID Road" },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [79.825000, 11.930000],
                  [79.826000, 11.931000]
                ]
              }
            }
          ]
        }
        """;

        RoadImportSummary summary = new RoadImportSummary();
        roadReferenceService.importFromInputStream(new ByteArrayInputStream(malformedGeoJson.getBytes(StandardCharsets.UTF_8)), summary);

        assertThat(summary.getAcceptedCount()).isEqualTo(0);
        assertThat(summary.getRejectedCount()).isEqualTo(4);
        assertThat(roadSegmentRepository.count()).isZero();
    }

    @Test
    @DisplayName("3. Repeat imports are idempotent and update duplicate OSM feature IDs deterministically")
    void testIdempotentRepeatImportAndDuplicateHandling() {
        String initialGeoJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "id": 30001,
              "properties": { "name": "Initial Bussy Street", "highway": "tertiary" },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [79.831000, 11.928000],
                  [79.833000, 11.928500]
                ]
              }
            }
          ]
        }
        """;

        String updatedGeoJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "id": 30001,
              "properties": { "name": "Updated Lal Bahadur Shastri Street", "highway": "tertiary" },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [79.831000, 11.928000],
                  [79.834000, 11.928600]
                ]
              }
            }
          ]
        }
        """;

        RoadImportSummary sum1 = new RoadImportSummary();
        roadReferenceService.importFromInputStream(new ByteArrayInputStream(initialGeoJson.getBytes(StandardCharsets.UTF_8)), sum1);
        assertThat(sum1.getAcceptedCount()).isEqualTo(1);
        assertThat(sum1.getDuplicateUpdatedCount()).isEqualTo(0);
        assertThat(roadSegmentRepository.count()).isEqualTo(1);

        // Re-import updated version with the SAME feature ID
        RoadImportSummary sum2 = new RoadImportSummary();
        roadReferenceService.importFromInputStream(new ByteArrayInputStream(updatedGeoJson.getBytes(StandardCharsets.UTF_8)), sum2);
        assertThat(sum2.getAcceptedCount()).isEqualTo(0);
        assertThat(sum2.getDuplicateUpdatedCount()).isEqualTo(1);
        assertThat(roadSegmentRepository.count()).isEqualTo(1);

        RoadSegment updated = roadSegmentRepository.findByOsmFeatureId(30001L).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Updated Lal Bahadur Shastri Street");
    }

    @Test
    @DisplayName("4. Accurately calculates distance and snaps coordinates to nearest road")
    void testNearestRoadDistanceAndSnapping() {
        String testGeoJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "id": 40001,
              "properties": { "name": "Kamaraj Salai", "highway": "secondary" },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [79.820000, 11.930000],
                  [79.840000, 11.930000]
                ]
              }
            }
          ]
        }
        """;

        roadReferenceService.importFromInputStream(new ByteArrayInputStream(testGeoJson.getBytes(StandardCharsets.UTF_8)), new RoadImportSummary());

        // Incident point ~55 meters North of Kamaraj Salai (Lat: 11.9305, Lon: 79.8300)
        BigDecimal incidentLat = new BigDecimal("11.930500");
        BigDecimal incidentLon = new BigDecimal("79.830000");

        RoadMatchContext match = roadReferenceService.findNearestRoad(incidentLat, incidentLon, 200.0);

        assertThat(match.getRoadFound()).isTrue();
        assertThat(match.getRoadName()).isEqualTo("Kamaraj Salai");
        assertThat(match.getDistanceMeters()).isBetween(50.0, 60.0);
        assertThat(match.getConfidence()).isGreaterThanOrEqualTo(0.70);
        assertThat(match.getQualityExplanation()).contains("Supplementary reference only");
        assertThat(match.getSource()).isEqualTo("OpenStreetMap Contributors");
        assertThat(match.getLicense()).contains("ODbL");
    }

    @Test
    @DisplayName("5. Returns empty match when no road is within search radius")
    void testNoRoadMatchOutsideSearchRadius() {
        String testGeoJson = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "id": 50001,
              "properties": { "name": "Goubert Avenue", "highway": "tertiary" },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [79.835000, 11.925000],
                  [79.835000, 11.935000]
                ]
              }
            }
          ]
        }
        """;

        roadReferenceService.importFromInputStream(new ByteArrayInputStream(testGeoJson.getBytes(StandardCharsets.UTF_8)), new RoadImportSummary());

        // Incident point far from Goubert Avenue (e.g. 500m away, while search radius is 100m)
        BigDecimal farLat = new BigDecimal("11.930000");
        BigDecimal farLon = new BigDecimal("79.825000");

        RoadMatchContext match = roadReferenceService.findNearestRoad(farLat, farLon, 100.0);

        assertThat(match.getRoadFound()).isFalse();
        assertThat(match.getRoadName()).isNull();
        assertThat(match.getQualityExplanation()).contains("No OpenStreetMap");
    }

    @Test
    @DisplayName("6. Gracefully handles null coordinates and missing artifact file")
    void testHandlesNullCoordinatesAndMissingArtifact() {
        RoadMatchContext matchNull = roadReferenceService.findNearestRoad(null, null);
        assertThat(matchNull.getRoadFound()).isFalse();
        assertThat(matchNull.getQualityExplanation()).contains("Missing incident coordinates");

        RoadImportSummary summary = roadReferenceService.importFromClasspath("non_existent_file.geojson");
        assertThat(summary.getStatus()).isEqualTo("MISSING_ARTIFACT");
        assertThat(summary.getRejectedCount()).isEqualTo(1);
    }
}
