package com.nirvaha.nirvaha_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * RoadSegment
 *
 * Represents an isolated, read-only road reference segment imported from
 * OpenStreetMap (OSM) under the Open Database License (ODbL) 1.0.
 *
 * This entity is strictly an external reference layer. It does NOT represent
 * official government public works infrastructure, and matching against this
 * layer does NOT alter incident ownership, SLA rules, or accountability metrics.
 */
@Entity
@Table(name = "road_segments", indexes = {
    @Index(name = "idx_road_osm_feature_id", columnList = "osm_feature_id", unique = true),
    @Index(name = "idx_road_bbox", columnList = "min_lat, max_lat, min_lng, max_lng")
})
public class RoadSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "osm_feature_id", nullable = false, unique = true)
    private Long osmFeatureId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "highway_type", nullable = false, length = 50)
    private String highwayType;

    @Column(name = "road_reference", length = 50)
    private String roadReference;

    @Column(length = 50)
    private String surface;

    @Column(name = "is_oneway")
    private Boolean oneway;

    @Column(name = "max_speed", length = 20)
    private String maxSpeed;

    @Column(name = "geometry_json", columnDefinition = "TEXT", nullable = false)
    private String geometryJson;

    @Column(name = "geometry_wkt", columnDefinition = "TEXT")
    private String geometryWkt;

    @Column(name = "min_lat", nullable = false)
    private Double minLat;

    @Column(name = "max_lat", nullable = false)
    private Double maxLat;

    @Column(name = "min_lng", nullable = false)
    private Double minLng;

    @Column(name = "max_lng", nullable = false)
    private Double maxLng;

    @Column(name = "length_meters")
    private Double lengthMeters;

    @Column(nullable = false, length = 100)
    private String source = "OpenStreetMap Contributors";

    @Column(nullable = false, length = 150)
    private String license = "Open Database License (ODbL) 1.0";

    @Column(name = "provenance_class", nullable = false, length = 50)
    private String provenanceClass = "OFFICIAL_EXTERNAL_DATA";

    @Column(name = "extract_version", length = 50)
    private String extractVersion = "OSM-PUDUCHERRY-2026.10-BOUNDED";

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false)
    private ValidationStatus validationStatus = ValidationStatus.VERIFIED;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private LocalDateTime importedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RoadSegment() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.importedAt == null) {
            this.importedAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.validationStatus == null) {
            this.validationStatus = ValidationStatus.VERIFIED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOsmFeatureId() {
        return osmFeatureId;
    }

    public void setOsmFeatureId(Long osmFeatureId) {
        this.osmFeatureId = osmFeatureId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHighwayType() {
        return highwayType;
    }

    public void setHighwayType(String highwayType) {
        this.highwayType = highwayType;
    }

    public String getRoadReference() {
        return roadReference;
    }

    public void setRoadReference(String roadReference) {
        this.roadReference = roadReference;
    }

    public String getSurface() {
        return surface;
    }

    public void setSurface(String surface) {
        this.surface = surface;
    }

    public Boolean getOneway() {
        return oneway;
    }

    public void setOneway(Boolean oneway) {
        this.oneway = oneway;
    }

    public String getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(String maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public String getGeometryJson() {
        return geometryJson;
    }

    public void setGeometryJson(String geometryJson) {
        this.geometryJson = geometryJson;
    }

    public String getGeometryWkt() {
        return geometryWkt;
    }

    public void setGeometryWkt(String geometryWkt) {
        this.geometryWkt = geometryWkt;
    }

    public Double getMinLat() {
        return minLat;
    }

    public void setMinLat(Double minLat) {
        this.minLat = minLat;
    }

    public Double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(Double maxLat) {
        this.maxLat = maxLat;
    }

    public Double getMinLng() {
        return minLng;
    }

    public void setMinLng(Double minLng) {
        this.minLng = minLng;
    }

    public Double getMaxLng() {
        return maxLng;
    }

    public void setMaxLng(Double maxLng) {
        this.maxLng = maxLng;
    }

    public Double getLengthMeters() {
        return lengthMeters;
    }

    public void setLengthMeters(Double lengthMeters) {
        this.lengthMeters = lengthMeters;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getProvenanceClass() {
        return provenanceClass;
    }

    public void setProvenanceClass(String provenanceClass) {
        this.provenanceClass = provenanceClass;
    }

    public String getExtractVersion() {
        return extractVersion;
    }

    public void setExtractVersion(String extractVersion) {
        this.extractVersion = extractVersion;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
