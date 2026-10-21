package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.*;
import com.nirvaha.nirvaha_backend.repository.AdministrativeAreaRepository;
import com.nirvaha.nirvaha_backend.repository.DataSourceRepository;
import com.nirvaha.nirvaha_backend.util.GeospatialUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdministrativeAreaService {

    private final AdministrativeAreaRepository areaRepository;
    private final DataSourceRepository dataSourceRepository;

    public AdministrativeAreaService(AdministrativeAreaRepository areaRepository,
                                      DataSourceRepository dataSourceRepository) {
        this.areaRepository = areaRepository;
        this.dataSourceRepository = dataSourceRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedRealPuducherryWards() {
        Optional<DataSource> ladSourceOpt = dataSourceRepository.findByName("Puducherry Local Administration Department (LAD)");
        DataSource ladSource = ladSourceOpt.orElse(null);

        // Seed Ward 1: Oulgaret Ward (Approximate bounding envelope: ~11.93 to 11.965 Lat, 79.79 to 79.825 Lng)
        seedWardIfMissing("Oulgaret Ward 12", "PY-WARD-012", AreaType.WARD,
                "POLYGON((79.790 11.930, 79.825 11.930, 79.825 11.965, 79.790 11.965, 79.790 11.930))",
                ladSource, GeometryStatus.APPROXIMATE, ValidationStatus.REVIEW_REQUIRED,
                "Estimated rectangular bounding envelope based on LAD delimitation map references (WGS84 EPSG:4326). Full vector polygon dataset pending official GIS release.",
                "Approximated bounding envelope for spatial containment testing; official detailed survey polygon geometry pending public publication from LAD.");

        // Seed Ward 2: Heritage Town / Boulevard Ward (Approximate bounding envelope: ~11.925 to 11.945 Lat, 79.825 to 79.845 Lng)
        seedWardIfMissing("Puducherry Heritage Boulevard Ward 5", "PY-WARD-005", AreaType.WARD,
                "POLYGON((79.825 11.925, 79.845 11.925, 79.845 11.945, 79.825 11.945, 79.825 11.925))",
                ladSource, GeometryStatus.APPROXIMATE, ValidationStatus.REVIEW_REQUIRED,
                "Estimated rectangular bounding envelope for Heritage Boulevard ward (WGS84 EPSG:4326).",
                "Approximated urban coastal bounding box; official survey boundary polygon pending vector dataset release.");

        // Seed Ward 3: Muthialpet Ward (Approximate bounding envelope: ~11.945 to 11.970 Lat, 79.825 to 79.850 Lng)
        seedWardIfMissing("Muthialpet North Ward 8", "PY-WARD-008", AreaType.WARD,
                "POLYGON((79.825 11.945, 79.850 11.945, 79.850 11.970, 79.825 11.970, 79.825 11.945))",
                ladSource, GeometryStatus.APPROXIMATE, ValidationStatus.REVIEW_REQUIRED,
                "Estimated rectangular bounding envelope for Muthialpet North (WGS84 EPSG:4326).",
                "Approximated north coastal commune bounding box; official delimitation boundary polygon pending vector dataset release.");
    }


    private void seedWardIfMissing(String name, String officialCode, AreaType areaType, String wkt,
                                  DataSource source, GeometryStatus geomStatus, ValidationStatus valStatus,
                                  String extractionMethod, String reviewerNotes) {
        Optional<AdministrativeArea> existingOpt = areaRepository.findByOfficialCode(officialCode);
        List<double[]> vertices = GeospatialUtils.parseWktPolygon(wkt);
        double[] bbox = GeospatialUtils.calculateBoundingBox(vertices);

        AdministrativeArea area = existingOpt.orElseGet(AdministrativeArea::new);
        area.setName(name);
        area.setOfficialCode(officialCode);
        area.setAreaType(areaType);
        area.setGeometryWkt(wkt);
        area.setGeometryJson(buildGeoJsonPolygon(vertices));
        area.setMinLat(bbox[0]);
        area.setMaxLat(bbox[1]);
        area.setMinLng(bbox[2]);
        area.setMaxLng(bbox[3]);
        area.setSource(source);
        area.setGeometryStatus(geomStatus);
        area.setValidationStatus(valStatus);
        area.setExtractionMethod(extractionMethod);
        area.setReviewerNotes(reviewerNotes);
        if (area.getValidFrom() == null) {
            area.setValidFrom(LocalDateTime.now().minusYears(3));
        }
        areaRepository.save(area);
    }


    private String buildGeoJsonPolygon(List<double[]> vertices) {
        StringBuilder sb = new StringBuilder("{\"type\":\"Polygon\",\"coordinates\":[[");
        for (int i = 0; i < vertices.size(); i++) {
            sb.append("[").append(vertices.get(i)[0]).append(",").append(vertices.get(i)[1]).append("]");
            if (i < vertices.size() - 1) sb.append(",");
        }
        sb.append("]]}");
        return sb.toString();
    }

    @Transactional
    public void assignIncidentToWard(Incident incident) {
        if (incident == null || incident.getLatitude() == null || incident.getLongitude() == null) {
            incident.setAssignedArea(null);
            incident.setAssignmentMethod(AssignmentMethod.UNASSIGNED);
            incident.setAssignmentConfidence(BigDecimal.ZERO);
            incident.setAssignmentTimestamp(LocalDateTime.now());
            incident.setWardAssignmentNotes("WARD ASSIGNMENT REVIEW REQUIRED (Missing incident coordinates)");
            return;
        }

        double lat = incident.getLatitude().doubleValue();
        double lng = incident.getLongitude().doubleValue();

        List<AdministrativeArea> candidateAreas = areaRepository.findCandidateAreasByBbox(lat, lng, ValidationStatus.VERIFIED);
        List<AdministrativeArea> matchedAreas = new ArrayList<>();

        for (AdministrativeArea area : candidateAreas) {
            List<double[]> vertices = GeospatialUtils.parseWktPolygon(area.getGeometryWkt());
            if (vertices.isEmpty()) {
                vertices = GeospatialUtils.parseGeoJsonPolygon(area.getGeometryJson());
            }
            if (GeospatialUtils.pointInPolygon(lat, lng, vertices)) {
                matchedAreas.add(area);
            }
        }

        if (matchedAreas.size() == 1) {
            AdministrativeArea matched = matchedAreas.get(0);
            incident.setAssignedArea(matched);
            incident.setAssignmentMethod(AssignmentMethod.POINT_IN_POLYGON);
            incident.setAssignmentConfidence(BigDecimal.valueOf(1.0000));
            incident.setAssignmentTimestamp(LocalDateTime.now());
            incident.setWardAssignmentNotes("Point-in-polygon containment confirmed for area: " + matched.getName());
        } else if (matchedAreas.isEmpty()) {
            incident.setAssignedArea(null);
            incident.setAssignmentMethod(AssignmentMethod.UNASSIGNED);
            incident.setAssignmentConfidence(BigDecimal.ZERO);
            incident.setAssignmentTimestamp(LocalDateTime.now());
            incident.setWardAssignmentNotes("WARD ASSIGNMENT REVIEW REQUIRED (Point outside all verified ward polygons)");
        } else {
            // Unexpected multiple matches
            incident.setAssignedArea(null);
            incident.setAssignmentMethod(AssignmentMethod.UNASSIGNED);
            incident.setAssignmentConfidence(BigDecimal.ZERO);
            incident.setAssignmentTimestamp(LocalDateTime.now());
            incident.setWardAssignmentNotes("WARD ASSIGNMENT REVIEW REQUIRED (Point intersects " + matchedAreas.size() + " overlapping polygons)");
        }
    }

    public List<AdministrativeArea> getAllAdministrativeAreas() {
        return areaRepository.findAll();
    }

    public Optional<AdministrativeArea> getAreaById(Long id) {
        return areaRepository.findById(id);
    }
}
