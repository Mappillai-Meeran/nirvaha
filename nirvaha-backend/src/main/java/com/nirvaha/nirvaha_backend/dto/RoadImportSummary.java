package com.nirvaha.nirvaha_backend.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class RoadImportSummary {

    private int totalRead = 0;
    private int acceptedCount = 0;
    private int rejectedCount = 0;
    private int duplicateUpdatedCount = 0;
    private double totalLengthKm = 0.0;
    private String extractVersion = "OSM-PUDUCHERRY-2026.10-BOUNDED";
    private Map<String, Integer> rejectionReasons = new HashMap<>();
    private LocalDateTime importTimestamp = LocalDateTime.now();
    private String status = "INITIALIZED";
    private String sourceAttribution = "© OpenStreetMap contributors, Open Database License (ODbL) 1.0";

    public RoadImportSummary() {
    }

    public void incrementRejected(String reason) {
        rejectedCount++;
        rejectionReasons.put(reason, rejectionReasons.getOrDefault(reason, 0) + 1);
    }

    public void incrementAccepted() {
        acceptedCount++;
    }

    public void incrementDuplicateUpdated() {
        duplicateUpdatedCount++;
    }

    // Getters and Setters

    public int getTotalRead() {
        return totalRead;
    }

    public void setTotalRead(int totalRead) {
        this.totalRead = totalRead;
    }

    public int getAcceptedCount() {
        return acceptedCount;
    }

    public void setAcceptedCount(int acceptedCount) {
        this.acceptedCount = acceptedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(int rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public int getDuplicateUpdatedCount() {
        return duplicateUpdatedCount;
    }

    public void setDuplicateUpdatedCount(int duplicateUpdatedCount) {
        this.duplicateUpdatedCount = duplicateUpdatedCount;
    }

    public double getTotalLengthKm() {
        return totalLengthKm;
    }

    public void setTotalLengthKm(double totalLengthKm) {
        this.totalLengthKm = totalLengthKm;
    }

    public String getExtractVersion() {
        return extractVersion;
    }

    public void setExtractVersion(String extractVersion) {
        this.extractVersion = extractVersion;
    }

    public Map<String, Integer> getRejectionReasons() {
        return rejectionReasons;
    }

    public void setRejectionReasons(Map<String, Integer> rejectionReasons) {
        this.rejectionReasons = rejectionReasons;
    }

    public LocalDateTime getImportTimestamp() {
        return importTimestamp;
    }

    public void setImportTimestamp(LocalDateTime importTimestamp) {
        this.importTimestamp = importTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceAttribution() {
        return sourceAttribution;
    }

    public void setSourceAttribution(String sourceAttribution) {
        this.sourceAttribution = sourceAttribution;
    }
}
