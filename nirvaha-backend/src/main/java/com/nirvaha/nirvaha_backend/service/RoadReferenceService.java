package com.nirvaha.nirvaha_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirvaha.nirvaha_backend.dto.RoadImportSummary;
import com.nirvaha.nirvaha_backend.dto.RoadMatchContext;
import com.nirvaha.nirvaha_backend.entity.RoadSegment;
import com.nirvaha.nirvaha_backend.entity.ValidationStatus;
import com.nirvaha.nirvaha_backend.repository.RoadSegmentRepository;
import com.nirvaha.nirvaha_backend.util.GeospatialUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RoadReferenceService {

    private static final Logger log = LoggerFactory.getLogger(RoadReferenceService.class);
    public static final double DEFAULT_MAX_SEARCH_RADIUS_METERS = 300.0;
    private static final String DEFAULT_GEOJSON_PATH = "data/osm_puducherry_arterial_roads.geojson";

    private final RoadSegmentRepository roadSegmentRepository;
    private final ObjectMapper objectMapper;
    private RoadImportSummary latestSummary = new RoadImportSummary();

    public RoadReferenceService(RoadSegmentRepository roadSegmentRepository) {
        this.roadSegmentRepository = roadSegmentRepository;
        this.objectMapper = new ObjectMapper();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Checking OpenStreetMap Puducherry road reference dataset...");
        long count = roadSegmentRepository.countByValidationStatus(ValidationStatus.VERIFIED);
        if (count == 0) {
            log.info("No verified road segments found in database. Initiating seed import from classpath: {}", DEFAULT_GEOJSON_PATH);
            importFromClasspath(DEFAULT_GEOJSON_PATH);
        } else {
            log.info("OpenStreetMap road reference layer already initialized with {} verified segments.", count);
            refreshSummaryFromDatabase();
        }
    }

    public synchronized RoadImportSummary importFromClasspath(String classpathResource) {
        RoadImportSummary summary = new RoadImportSummary();
        summary.setStatus("IN_PROGRESS");
        try {
            ClassPathResource resource = new ClassPathResource(classpathResource);
            if (!resource.exists()) {
                log.warn("Classpath resource '{}' not found. Skipping road reference import.", classpathResource);
                summary.setStatus("MISSING_ARTIFACT");
                summary.incrementRejected("Missing classpath resource artifact: " + classpathResource);
                this.latestSummary = summary;
                return summary;
            }
            try (InputStream in = resource.getInputStream()) {
                return importFromInputStream(in, summary);
            }
        } catch (Exception e) {
            log.error("Failed to import road reference dataset from {}: {}", classpathResource, e.getMessage(), e);
            summary.setStatus("ERROR");
            summary.incrementRejected("IO or parsing exception: " + e.getMessage());
            this.latestSummary = summary;
            return summary;
        }
    }

    @Transactional
    public RoadImportSummary importFromInputStream(InputStream in, RoadImportSummary summary) {
        try {
            JsonNode root = objectMapper.readTree(in);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) {
                summary.setStatus("EMPTY_DATASET");
                summary.incrementRejected("FeatureCollection contains no features array or is empty.");
                this.latestSummary = summary;
                return summary;
            }

            JsonNode metadata = root.path("metadata");
            String extractVersion = metadata.hasNonNull("version") ? metadata.get("version").asText() : "OSM-PUDUCHERRY-2026.10-BOUNDED";
            summary.setExtractVersion(extractVersion);
            summary.setTotalRead(features.size());

            for (JsonNode feature : features) {
                processFeature(feature, extractVersion, summary);
            }

            summary.setStatus("SUCCESS");
            summary.setImportTimestamp(LocalDateTime.now());
            double totalMeters = roadSegmentRepository.sumVerifiedLengthMeters();
            summary.setTotalLengthKm(BigDecimal.valueOf(totalMeters / 1000.0).setScale(2, RoundingMode.HALF_UP).doubleValue());

            log.info("OSM Road Import completed: {} read, {} accepted, {} duplicates updated, {} rejected. Total road length: {} km",
                    summary.getTotalRead(), summary.getAcceptedCount(), summary.getDuplicateUpdatedCount(),
                    summary.getRejectedCount(), summary.getTotalLengthKm());

            this.latestSummary = summary;
            return summary;
        } catch (Exception e) {
            log.error("Unexpected error during road import: {}", e.getMessage(), e);
            summary.setStatus("FAILED");
            summary.incrementRejected("Parser error: " + e.getMessage());
            this.latestSummary = summary;
            return summary;
        }
    }

    private void processFeature(JsonNode feature, String extractVersion, RoadImportSummary summary) {
        Long osmId = feature.hasNonNull("id") ? feature.get("id").asLong() : null;
        if (osmId == null && feature.path("properties").hasNonNull("osmId")) {
            osmId = feature.path("properties").get("osmId").asLong();
        }

        if (osmId == null) {
            summary.incrementRejected("Feature missing valid OSM feature ID.");
            return;
        }

        JsonNode geom = feature.path("geometry");
        String geomType = geom.path("type").asText("");
        if (!"LineString".equalsIgnoreCase(geomType)) {
            summary.incrementRejected("Unsupported geometry type: '" + geomType + "'. Only LineString is supported.");
            return;
        }

        JsonNode coords = geom.path("coordinates");
        if (!coords.isArray() || coords.size() < 2) {
            summary.incrementRejected("Invalid geometry: coordinates must contain at least 2 points.");
            return;
        }

        List<double[]> points = new ArrayList<>();
        boolean hasInvalidCoords = false;
        boolean hasOutOfBoundsCoords = false;

        for (JsonNode pt : coords) {
            if (!pt.isArray() || pt.size() < 2) {
                hasInvalidCoords = true;
                break;
            }
            double lon = pt.get(0).asDouble();
            double lat = pt.get(1).asDouble();

            if (!GeospatialUtils.isValidWgs84Coordinate(lat, lon)) {
                hasInvalidCoords = true;
                break;
            }
            if (!GeospatialUtils.isInPuducherryRegion(lat, lon)) {
                hasOutOfBoundsCoords = true;
                break;
            }
            points.add(new double[]{lon, lat});
        }

        if (hasInvalidCoords) {
            summary.incrementRejected("Malformed coordinates in LineString for OSM ID " + osmId);
            return;
        }
        if (hasOutOfBoundsCoords) {
            summary.incrementRejected("Coordinates outside Puducherry regional bounds (11-13 N, 79-81 E) for OSM ID " + osmId);
            return;
        }

        JsonNode props = feature.path("properties");
        String name = props.hasNonNull("name") ? props.get("name").asText().trim() : "Unnamed Arterial Road";
        String highway = props.hasNonNull("highway") ? props.get("highway").asText().trim() : "road";
        String ref = props.hasNonNull("ref") ? props.get("ref").asText().trim() : null;
        String surface = props.hasNonNull("surface") ? props.get("surface").asText().trim() : null;
        Boolean oneway = props.hasNonNull("oneway") && ("yes".equalsIgnoreCase(props.get("oneway").asText()) || "1".equals(props.get("oneway").asText()));
        String maxspeed = props.hasNonNull("maxspeed") ? props.get("maxspeed").asText().trim() : null;

        double[] bbox = GeospatialUtils.calculateBoundingBox(points);
        double lengthMeters = GeospatialUtils.calculatePolylineLengthMeters(points);

        Optional<RoadSegment> existingOpt = roadSegmentRepository.findByOsmFeatureId(osmId);
        boolean isDuplicate = existingOpt.isPresent();

        RoadSegment segment = existingOpt.orElseGet(RoadSegment::new);
        segment.setOsmFeatureId(osmId);
        segment.setName(name);
        segment.setHighwayType(highway);
        segment.setRoadReference(ref);
        segment.setSurface(surface);
        segment.setOneway(oneway);
        segment.setMaxSpeed(maxspeed);
        segment.setGeometryJson(geom.toString());
        segment.setMinLat(bbox[0]);
        segment.setMaxLat(bbox[1]);
        segment.setMinLng(bbox[2]);
        segment.setMaxLng(bbox[3]);
        segment.setLengthMeters(lengthMeters);
        segment.setExtractVersion(extractVersion);
        segment.setValidationStatus(ValidationStatus.VERIFIED);
        segment.setRejectionReason(null);

        roadSegmentRepository.save(segment);

        if (isDuplicate) {
            summary.incrementDuplicateUpdated();
        } else {
            summary.incrementAccepted();
        }
    }

    @Transactional(readOnly = true)
    public RoadMatchContext findNearestRoad(BigDecimal latitude, BigDecimal longitude, double maxSearchRadiusMeters) {
        if (latitude == null || longitude == null) {
            return RoadMatchContext.empty("Missing incident coordinates for road reference lookup.");
        }

        double lat = latitude.doubleValue();
        double lon = longitude.doubleValue();

        if (!GeospatialUtils.isValidWgs84Coordinate(lat, lon)) {
            return RoadMatchContext.empty("Invalid incident WGS84 coordinates: " + lat + ", " + lon);
        }

        if (!GeospatialUtils.isInPuducherryRegion(lat, lon)) {
            return RoadMatchContext.empty("Incident coordinates outside Puducherry regional bounds.");
        }

        // Bounding box buffer for candidate query
        double deltaLat = maxSearchRadiusMeters / 111132.954;
        double deltaLon = maxSearchRadiusMeters / (111412.84 * Math.cos(Math.toRadians(lat)));

        double queryMinLat = lat - deltaLat;
        double queryMaxLat = lat + deltaLat;
        double queryMinLng = lon - deltaLon;
        double queryMaxLng = lon + deltaLon;

        List<RoadSegment> candidates = roadSegmentRepository.findCandidateRoadsByBbox(
                queryMinLat, queryMaxLat, queryMinLng, queryMaxLng, ValidationStatus.VERIFIED);

        if (candidates.isEmpty()) {
            return RoadMatchContext.empty(String.format("No OpenStreetMap arterial road within %.0fm search radius.", maxSearchRadiusMeters));
        }

        RoadSegment closestRoad = null;
        double minDistanceMeters = Double.MAX_VALUE;

        for (RoadSegment road : candidates) {
            List<double[]> polyline = GeospatialUtils.parseGeoJsonLineString(road.getGeometryJson());
            if (polyline.size() < 2) continue;

            double dist = GeospatialUtils.pointToPolylineDistanceMeters(lat, lon, polyline);
            if (dist < minDistanceMeters) {
                minDistanceMeters = dist;
                closestRoad = road;
            }
        }

        if (closestRoad == null || minDistanceMeters > maxSearchRadiusMeters) {
            return RoadMatchContext.empty(String.format("No OpenStreetMap road within %.0fm search radius (nearest candidate was %.1fm).",
                    maxSearchRadiusMeters, minDistanceMeters));
        }

        // Calculate confidence score based on proximity
        double confidence;
        if (minDistanceMeters <= 25.0) {
            confidence = 0.95;
        } else if (minDistanceMeters <= 50.0) {
            confidence = 0.85;
        } else if (minDistanceMeters <= 100.0) {
            confidence = 0.70;
        } else if (minDistanceMeters <= 200.0) {
            confidence = 0.50;
        } else {
            confidence = 0.35;
        }

        double roundedDistance = BigDecimal.valueOf(minDistanceMeters).setScale(1, RoundingMode.HALF_UP).doubleValue();
        String explanation = String.format(
                "Snapped to OpenStreetMap %s '%s'%s (%.1fm away). Supplementary reference only; not officially verified government asset.",
                closestRoad.getHighwayType(),
                closestRoad.getName(),
                closestRoad.getRoadReference() != null ? " [" + closestRoad.getRoadReference() + "]" : "",
                roundedDistance
        );

        return RoadMatchContext.match(
                closestRoad.getOsmFeatureId(),
                closestRoad.getName(),
                closestRoad.getHighwayType(),
                closestRoad.getRoadReference(),
                roundedDistance,
                confidence,
                explanation
        );
    }

    public RoadMatchContext findNearestRoad(BigDecimal latitude, BigDecimal longitude) {
        return findNearestRoad(latitude, longitude, DEFAULT_MAX_SEARCH_RADIUS_METERS);
    }

    public RoadImportSummary getLatestSummary() {
        return latestSummary;
    }

    private void refreshSummaryFromDatabase() {
        long verifiedCount = roadSegmentRepository.countByValidationStatus(ValidationStatus.VERIFIED);
        Double totalMeters = roadSegmentRepository.sumVerifiedLengthMeters();
        latestSummary.setAcceptedCount((int) verifiedCount);
        latestSummary.setTotalRead((int) verifiedCount);
        latestSummary.setTotalLengthKm(BigDecimal.valueOf(totalMeters / 1000.0).setScale(2, RoundingMode.HALF_UP).doubleValue());
        latestSummary.setStatus("SUCCESS");
        latestSummary.setImportTimestamp(LocalDateTime.now());
    }
}
