package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.TimeWindow;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TemporalAggregationService {

    public LocalDateTime calculateWindowStart(TimeWindow window, LocalDateTime now) {
        switch (window) {
            case H24:
                return now.minusHours(24);
            case D7:
                return now.minusDays(7);
            case D30:
                return now.minusDays(30);
            case D90:
                return now.minusDays(90);
            default:
                return now.minusDays(7);
        }
    }

    public LocalDateTime calculatePreviousWindowStart(TimeWindow window, LocalDateTime now) {
        switch (window) {
            case H24:
                return now.minusHours(48);
            case D7:
                return now.minusDays(14);
            case D30:
                return now.minusDays(60);
            case D90:
                return now.minusDays(180);
            default:
                return now.minusDays(14);
        }
    }

    public List<Incident> filterIncidentsInWindow(List<Incident> incidents, LocalDateTime start, LocalDateTime end) {
        if (incidents == null) return List.of();
        return incidents.stream()
                .filter(i -> i.getCreatedAt() != null &&
                        !i.getCreatedAt().isBefore(start) &&
                        !i.getCreatedAt().isAfter(end))
                .collect(Collectors.toList());
    }
}
