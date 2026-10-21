package com.nirvaha.nirvaha_backend.dto;

import java.util.List;

public class IncidentAnomaliesResponse {

    private Long incidentId;
    private List<AnomalyFlagResponse> anomalies;

    public IncidentAnomaliesResponse() {
    }

    public IncidentAnomaliesResponse(Long incidentId, List<AnomalyFlagResponse> anomalies) {
        this.incidentId = incidentId;
        this.anomalies = anomalies;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public List<AnomalyFlagResponse> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(List<AnomalyFlagResponse> anomalies) {
        this.anomalies = anomalies;
    }
}
