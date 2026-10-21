package com.nirvaha.nirvaha_backend.dto;

import java.util.List;

public class DuplicateDetectionResponse {

    private Long incidentId;
    private List<DuplicateMatch> matches;

    public DuplicateDetectionResponse() {
    }

    public DuplicateDetectionResponse(Long incidentId, List<DuplicateMatch> matches) {
        this.incidentId = incidentId;
        this.matches = matches;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public List<DuplicateMatch> getMatches() {
        return matches;
    }

    public void setMatches(List<DuplicateMatch> matches) {
        this.matches = matches;
    }
}
