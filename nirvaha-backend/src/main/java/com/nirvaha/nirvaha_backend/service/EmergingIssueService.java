package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmergingIssueService {

    public static final double EMERGING_SURGE_THRESHOLD_PCT = 50.0;
    public static final int MIN_EMERGING_INCIDENTS = 3;

    public static class EmergingCandidate {
        public IncidentCategory category;
        public int currentCount;
        public int previousCount;
        public double changePercentage;
        public List<Incident> incidents;

        public EmergingCandidate(IncidentCategory category, int currentCount, int previousCount, double changePercentage, List<Incident> incidents) {
            this.category = category;
            this.currentCount = currentCount;
            this.previousCount = previousCount;
            this.changePercentage = changePercentage;
            this.incidents = incidents;
        }
    }

    public List<EmergingCandidate> detectEmergingIssues(List<Incident> currentPeriodIncidents, List<Incident> previousPeriodIncidents, TrendAnalysisService trendService) {
        List<EmergingCandidate> candidates = new ArrayList<>();
        if (currentPeriodIncidents == null || currentPeriodIncidents.isEmpty()) {
            return candidates;
        }

        Map<IncidentCategory, List<Incident>> currentByCat = currentPeriodIncidents.stream()
                .collect(Collectors.groupingBy(Incident::getCategory));
        Map<IncidentCategory, List<Incident>> prevByCat = previousPeriodIncidents != null ?
                previousPeriodIncidents.stream().collect(Collectors.groupingBy(Incident::getCategory)) : Map.of();

        for (Map.Entry<IncidentCategory, List<Incident>> entry : currentByCat.entrySet()) {
            IncidentCategory category = entry.getKey();
            List<Incident> curList = entry.getValue();
            List<Incident> prevList = prevByCat.getOrDefault(category, List.of());

            int curCount = curList.size();
            int prevCount = prevList.size();
            double changePct = trendService.calculateChangePercentage(curCount, prevCount);

            if (curCount >= MIN_EMERGING_INCIDENTS && changePct >= EMERGING_SURGE_THRESHOLD_PCT) {
                candidates.add(new EmergingCandidate(category, curCount, prevCount, changePct, curList));
            }
        }

        return candidates;
    }
}
