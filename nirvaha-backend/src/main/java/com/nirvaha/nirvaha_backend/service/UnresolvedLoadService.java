package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UnresolvedLoadService {

    public static class UnresolvedLoadSummary {
        public int totalIncidents;
        public int unresolvedCount;
        public double avgAgeDays;
        public LocalDateTime oldestUnresolvedDate;

        public UnresolvedLoadSummary(int totalIncidents, int unresolvedCount, double avgAgeDays, LocalDateTime oldestUnresolvedDate) {
            this.totalIncidents = totalIncidents;
            this.unresolvedCount = unresolvedCount;
            this.avgAgeDays = avgAgeDays;
            this.oldestUnresolvedDate = oldestUnresolvedDate;
        }
    }

    public UnresolvedLoadSummary calculateUnresolvedLoad(List<Incident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return new UnresolvedLoadSummary(0, 0, 0.0, null);
        }

        List<Incident> unresolved = incidents.stream()
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED)
                .collect(Collectors.toList());

        if (unresolved.isEmpty()) {
            return new UnresolvedLoadSummary(incidents.size(), 0, 0.0, null);
        }

        LocalDateTime now = LocalDateTime.now();
        double totalDays = 0.0;
        LocalDateTime oldest = null;

        for (Incident inc : unresolved) {
            LocalDateTime created = inc.getCreatedAt() != null ? inc.getCreatedAt() : now;
            long days = Duration.between(created, now).toDays();
            totalDays += days;

            if (oldest == null || created.isBefore(oldest)) {
                oldest = created;
            }
        }

        double avgDays = totalDays / unresolved.size();
        return new UnresolvedLoadSummary(incidents.size(), unresolved.size(), avgDays, oldest);
    }
}
