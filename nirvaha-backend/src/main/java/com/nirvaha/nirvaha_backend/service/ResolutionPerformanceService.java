package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResolutionPerformanceService {

    public static class SystemResolutionMetrics {
        public int totalReports;
        public int resolvedCount;
        public double resolutionRatePct;
        public double avgResolutionTimeHours;

        public SystemResolutionMetrics(int totalReports, int resolvedCount, double resolutionRatePct, double avgResolutionTimeHours) {
            this.totalReports = totalReports;
            this.resolvedCount = resolvedCount;
            this.resolutionRatePct = resolutionRatePct;
            this.avgResolutionTimeHours = avgResolutionTimeHours;
        }
    }

    public SystemResolutionMetrics calculateSystemMetrics(List<Incident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return new SystemResolutionMetrics(0, 0, 0.0, 0.0);
        }

        int total = incidents.size();
        List<Incident> resolved = incidents.stream()
                .filter(i -> i.getStatus() == IncidentStatus.RESOLVED)
                .toList();

        int resCount = resolved.size();
        double ratePct = ((double) resCount / total) * 100.0;

        double totalHours = 0.0;
        for (Incident inc : resolved) {
            LocalDateTime start = inc.getCreatedAt();
            LocalDateTime end = inc.getUpdatedAt() != null ? inc.getUpdatedAt() : LocalDateTime.now();
            if (start != null && end != null && !end.isBefore(start)) {
                totalHours += Duration.between(start, end).toHours();
            }
        }

        double avgHours = resCount > 0 ? (totalHours / resCount) : 0.0;
        return new SystemResolutionMetrics(total, resCount, ratePct, avgHours);
    }
}
