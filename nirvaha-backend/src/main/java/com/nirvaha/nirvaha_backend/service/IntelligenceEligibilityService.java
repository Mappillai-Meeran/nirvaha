package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.DataClassification;
import com.nirvaha.nirvaha_backend.entity.Incident;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IntelligenceEligibilityService {

    /**
     * Determines whether an incident record is eligible to participate in Phase 13 civic intelligence calculations.
     * Rules:
     * 1. Must NOT be DataClassification.SIMULATED (production isolation rule).
     * 2. Must have valid non-null creation timestamp, category, and status.
     */
    public boolean isEligibleForIntelligence(Incident incident) {
        if (incident == null) {
            return false;
        }
        if (incident.getClassificationTag() == DataClassification.SIMULATED ||
            incident.getIntakeSource() == com.nirvaha.nirvaha_backend.entity.IncidentSource.SIMULATED) {
            return false;
        }
        return incident.getCreatedAt() != null &&
               incident.getCategory() != null &&
               incident.getStatus() != null;
    }

    /**
     * Determines whether an incident is eligible for spatial hotspot or geospatial clustering intelligence.
     * Additionally requires non-null coordinates.
     */
    public boolean isEligibleForGeospatialIntelligence(Incident incident) {
        if (!isEligibleForIntelligence(incident)) {
            return false;
        }
        return incident.getLatitude() != null && incident.getLongitude() != null;
    }

    /**
     * Filters a collection of incidents to include only records eligible for civic intelligence.
     */
    public List<Incident> filterEligibleIncidents(List<Incident> incidents) {
        if (incidents == null) {
            return List.of();
        }
        return incidents.stream()
                .filter(this::isEligibleForIntelligence)
                .collect(Collectors.toList());
    }

    /**
     * Filters a collection of incidents to include only records eligible for spatial/geospatial intelligence.
     */
    public List<Incident> filterEligibleGeospatialIncidents(List<Incident> incidents) {
        if (incidents == null) {
            return List.of();
        }
        return incidents.stream()
                .filter(this::isEligibleForGeospatialIntelligence)
                .collect(Collectors.toList());
    }
}
