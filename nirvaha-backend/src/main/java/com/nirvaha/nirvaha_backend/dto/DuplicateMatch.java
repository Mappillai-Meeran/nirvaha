package com.nirvaha.nirvaha_backend.dto;

import java.util.List;

public class DuplicateMatch {

    private Long incidentId;
    private double duplicateSimilarityScore;
    private double distanceMeters;
    private long timeDifferenceMinutes;
    private List<DuplicateMatchReason> reasons;

    public DuplicateMatch() {
    }

    public DuplicateMatch(Long incidentId, double duplicateSimilarityScore, double distanceMeters,
                          long timeDifferenceMinutes, List<DuplicateMatchReason> reasons) {
        this.incidentId = incidentId;
        this.duplicateSimilarityScore = duplicateSimilarityScore;
        this.distanceMeters = distanceMeters;
        this.timeDifferenceMinutes = timeDifferenceMinutes;
        this.reasons = reasons;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public double getDuplicateSimilarityScore() {
        return duplicateSimilarityScore;
    }

    public void setDuplicateSimilarityScore(double duplicateSimilarityScore) {
        this.duplicateSimilarityScore = duplicateSimilarityScore;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public long getTimeDifferenceMinutes() {
        return timeDifferenceMinutes;
    }

    public void setTimeDifferenceMinutes(long timeDifferenceMinutes) {
        this.timeDifferenceMinutes = timeDifferenceMinutes;
    }

    public List<DuplicateMatchReason> getReasons() {
        return reasons;
    }

    public void setReasons(List<DuplicateMatchReason> reasons) {
        this.reasons = reasons;
    }
}
