package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecurringProblemService {

    public static class RecurringCandidate {
        public IncidentCategory category;
        public int activePeriodsCount;
        public int totalIncidents;
        public List<Incident> incidents;

        public RecurringCandidate(IncidentCategory category, int activePeriodsCount, int totalIncidents, List<Incident> incidents) {
            this.category = category;
            this.activePeriodsCount = activePeriodsCount;
            this.totalIncidents = totalIncidents;
            this.incidents = incidents;
        }
    }

    public List<RecurringCandidate> detectRecurringProblems(List<Incident> last30DaysIncidents) {
        List<RecurringCandidate> candidates = new ArrayList<>();
        if (last30DaysIncidents == null || last30DaysIncidents.isEmpty()) {
            return candidates;
        }

        Map<IncidentCategory, List<Incident>> byCat = last30DaysIncidents.stream()
                .collect(Collectors.groupingBy(Incident::getCategory));

        for (Map.Entry<IncidentCategory, List<Incident>> entry : byCat.entrySet()) {
            IncidentCategory category = entry.getKey();
            List<Incident> list = entry.getValue();

            // Count distinct 7-day windows in which reports occurred
            long distinctPeriods = list.stream()
                    .map(inc -> inc.getCreatedAt() != null ? (inc.getCreatedAt().getDayOfYear() / 7) : 0)
                    .distinct()
                    .count();

            if (distinctPeriods >= 2 && list.size() >= 4) {
                candidates.add(new RecurringCandidate(category, (int) distinctPeriods, list.size(), list));
            }
        }

        return candidates;
    }
}
