package com.nirvaha.nirvaha_backend.dto;

public class RoadMatchContext {

    private Boolean roadFound = false;
    private Long osmFeatureId;
    private String roadName;
    private String highwayType;
    private String roadReference;
    private Double distanceMeters;
    private Double confidence;
    private String qualityExplanation;
    private String source = "OpenStreetMap Contributors";
    private String license = "Open Database License (ODbL) 1.0 - © OpenStreetMap contributors";
    private String provenanceClass = "OFFICIAL_EXTERNAL_DATA";
    private String extractVersion = "OSM-PUDUCHERRY-2026.10-BOUNDED";

    public RoadMatchContext() {
    }

    public static RoadMatchContext empty(String reason) {
        RoadMatchContext ctx = new RoadMatchContext();
        ctx.setRoadFound(false);
        ctx.setQualityExplanation(reason != null ? reason : "No matching OpenStreetMap road segment found within search radius.");
        ctx.setConfidence(0.0);
        return ctx;
    }

    public static RoadMatchContext match(Long osmFeatureId, String roadName, String highwayType,
                                        String roadReference, Double distanceMeters, Double confidence,
                                        String qualityExplanation) {
        RoadMatchContext ctx = new RoadMatchContext();
        ctx.setRoadFound(true);
        ctx.setOsmFeatureId(osmFeatureId);
        ctx.setRoadName(roadName);
        ctx.setHighwayType(highwayType);
        ctx.setRoadReference(roadReference);
        ctx.setDistanceMeters(distanceMeters);
        ctx.setConfidence(confidence);
        ctx.setQualityExplanation(qualityExplanation);
        return ctx;
    }

    // Getters and Setters

    public Boolean getRoadFound() {
        return roadFound;
    }

    public void setRoadFound(Boolean roadFound) {
        this.roadFound = roadFound;
    }

    public Long getOsmFeatureId() {
        return osmFeatureId;
    }

    public void setOsmFeatureId(Long osmFeatureId) {
        this.osmFeatureId = osmFeatureId;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
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

    public Double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getQualityExplanation() {
        return qualityExplanation;
    }

    public void setQualityExplanation(String qualityExplanation) {
        this.qualityExplanation = qualityExplanation;
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
}
