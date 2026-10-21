package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import com.nirvaha.nirvaha_backend.util.HaversineDistance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HotspotDetectionService {

    public static final double HOTSPOT_RADIUS_METERS = 500.0;
    public static final int MIN_HOTSPOT_CLUSTER_SIZE = 3;

    public static class SpatialCluster {
        public IncidentCategory category;
        public double centroidLat;
        public double centroidLng;
        public double radiusMeters;
        public List<Incident> incidents;
        public int unresolvedCount;

        public SpatialCluster(IncidentCategory category, double centroidLat, double centroidLng, double radiusMeters, List<Incident> incidents, int unresolvedCount) {
            this.category = category;
            this.centroidLat = centroidLat;
            this.centroidLng = centroidLng;
            this.radiusMeters = radiusMeters;
            this.incidents = incidents;
            this.unresolvedCount = unresolvedCount;
        }
    }

    public List<SpatialCluster> detectHotspots(List<Incident> eligibleGeospatialIncidents) {
        List<SpatialCluster> hotspots = new ArrayList<>();
        if (eligibleGeospatialIncidents == null || eligibleGeospatialIncidents.isEmpty()) {
            return hotspots;
        }

        // Group incidents by category to ensure category-specific hotspots
        Map<IncidentCategory, List<Incident>> byCategory = eligibleGeospatialIncidents.stream()
                .collect(Collectors.groupingBy(Incident::getCategory));

        for (Map.Entry<IncidentCategory, List<Incident>> entry : byCategory.entrySet()) {
            IncidentCategory category = entry.getKey();
            List<Incident> catIncidents = entry.getValue();

            boolean[] visited = new boolean[catIncidents.size()];

            for (int i = 0; i < catIncidents.size(); i++) {
                if (visited[i]) continue;
                Incident seed = catIncidents.get(i);
                List<Incident> cluster = new ArrayList<>();
                cluster.add(seed);

                double sumLat = seed.getLatitude().doubleValue();
                double sumLng = seed.getLongitude().doubleValue();

                for (int j = 0; j < catIncidents.size(); j++) {
                    if (i == j || visited[j]) continue;
                    Incident cand = catIncidents.get(j);
                    double dist = HaversineDistance.calculateDistanceMeters(
                            seed.getLatitude(), seed.getLongitude(),
                            cand.getLatitude(), cand.getLongitude()
                    );
                    if (dist <= HOTSPOT_RADIUS_METERS) {
                        cluster.add(cand);
                        sumLat += cand.getLatitude().doubleValue();
                        sumLng += cand.getLongitude().doubleValue();
                        visited[j] = true;
                    }
                }

                if (cluster.size() >= MIN_HOTSPOT_CLUSTER_SIZE) {
                    visited[i] = true;
                    double cLat = sumLat / cluster.size();
                    double cLng = sumLng / cluster.size();
                    int unresolved = (int) cluster.stream().filter(inc -> inc.getStatus() != IncidentStatus.RESOLVED).count();

                    hotspots.add(new SpatialCluster(category, cLat, cLng, HOTSPOT_RADIUS_METERS, cluster, unresolved));
                }
            }
        }

        return hotspots;
    }
}
