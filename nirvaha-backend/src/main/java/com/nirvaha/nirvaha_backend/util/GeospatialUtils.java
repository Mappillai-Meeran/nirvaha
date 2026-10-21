package com.nirvaha.nirvaha_backend.util;

import java.util.ArrayList;
import java.util.List;

public class GeospatialUtils {

    /**
     * Ray-casting point-in-polygon algorithm.
     * @param lat Incident latitude
     * @param lng Incident longitude
     * @param polygonVertices List of [longitude, latitude] coordinate pairs
     * @return true if point is inside polygon, false otherwise
     */
    public static boolean pointInPolygon(double lat, double lng, List<double[]> polygonVertices) {
        if (polygonVertices == null || polygonVertices.size() < 3) {
            return false;
        }
        boolean inside = false;
        int n = polygonVertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygonVertices.get(i)[0]; // lng
            double yi = polygonVertices.get(i)[1]; // lat
            double xj = polygonVertices.get(j)[0]; // lng
            double yj = polygonVertices.get(j)[1]; // lat

            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * Parses WKT POLYGON string into list of [lng, lat] coordinate pairs.
     * Example WKT: POLYGON((79.80 11.90, 79.85 11.90, 79.85 11.95, 79.80 11.95, 79.80 11.90))
     */
    public static List<double[]> parseWktPolygon(String wkt) {
        List<double[]> vertices = new ArrayList<>();
        if (wkt == null || !wkt.toUpperCase().contains("POLYGON")) {
            return vertices;
        }
        try {
            int startIdx = wkt.indexOf("((");
            int endIdx = wkt.lastIndexOf("))");
            if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
                return vertices;
            }
            String coordsStr = wkt.substring(startIdx + 2, endIdx).trim();
            String[] pairs = coordsStr.split(",");
            for (String pair : pairs) {
                String[] parts = pair.trim().split("\\s+");
                if (parts.length >= 2) {
                    double lng = Double.parseDouble(parts[0]);
                    double lat = Double.parseDouble(parts[1]);
                    vertices.add(new double[]{lng, lat});
                }
            }
        } catch (Exception e) {
            // Ignore parse errors, return parsed vertices
        }
        return vertices;
    }

    /**
     * Parses GeoJSON string containing coordinates into list of [lng, lat] pairs.
     */
    public static List<double[]> parseGeoJsonPolygon(String geoJson) {
        List<double[]> vertices = new ArrayList<>();
        if (geoJson == null || geoJson.isBlank()) {
            return vertices;
        }
        try {
            int startIdx = geoJson.indexOf("[[[");
            int endIdx = geoJson.indexOf("]]]");
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                String inner = geoJson.substring(startIdx + 3, endIdx);
                // Split into coordinate arrays e.g. [79.8, 11.9], [79.85, 11.9]
                String[] points = inner.split("\\]\\s*,\\s*\\[");
                for (String pt : points) {
                    String clean = pt.replace("[", "").replace("]", "").trim();
                    String[] parts = clean.split(",");
                    if (parts.length >= 2) {
                        double lng = Double.parseDouble(parts[0].trim());
                        double lat = Double.parseDouble(parts[1].trim());
                        vertices.add(new double[]{lng, lat});
                    }
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return vertices;
    }

    /**
     * Calculates bounding box [minLat, maxLat, minLng, maxLng] for vertices.
     */
    public static double[] calculateBoundingBox(List<double[]> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return new double[]{0.0, 0.0, 0.0, 0.0};
        }
        double minLng = Double.MAX_VALUE;
        double maxLng = -Double.MAX_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;

        for (double[] pt : vertices) {
            double lng = pt[0];
            double lat = pt[1];
            if (lng < minLng) minLng = lng;
            if (lng > maxLng) maxLng = lng;
            if (lat < minLat) minLat = lat;
            if (lat > maxLat) maxLat = lat;
        }
        return new double[]{minLat, maxLat, minLng, maxLng};
    }

    /**
     * Parses GeoJSON LineString coordinates into a list of [lon, lat] points.
     */
    public static List<double[]> parseGeoJsonLineString(String geoJson) {
        List<double[]> points = new ArrayList<>();
        if (geoJson == null || geoJson.isBlank()) {
            return points;
        }
        try {
            int startIdx = geoJson.indexOf("[[");
            int endIdx = geoJson.lastIndexOf("]]");
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                String inner = geoJson.substring(startIdx + 2, endIdx);
                String[] rawPoints = inner.split("\\]\\s*,\\s*\\[");
                for (String raw : rawPoints) {
                    String clean = raw.replace("[", "").replace("]", "").trim();
                    String[] parts = clean.split(",");
                    if (parts.length >= 2) {
                        double lon = Double.parseDouble(parts[0].trim());
                        double lat = Double.parseDouble(parts[1].trim());
                        points.add(new double[]{lon, lat});
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return points;
    }

    /**
     * Calculates distance in meters from point (pLat, pLon) to nearest segment of polyline.
     */
    public static double pointToPolylineDistanceMeters(double pLat, double pLon, List<double[]> polyline) {
        if (polyline == null || polyline.size() < 2) {
            return Double.MAX_VALUE;
        }
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < polyline.size() - 1; i++) {
            double lonA = polyline.get(i)[0];
            double latA = polyline.get(i)[1];
            double lonB = polyline.get(i + 1)[0];
            double latB = polyline.get(i + 1)[1];

            double dist = pointToSegmentDistanceMeters(pLat, pLon, latA, lonA, latB, lonB);
            if (dist < minDistance) {
                minDistance = dist;
            }
        }
        return minDistance;
    }

    /**
     * Calculates distance from point (pLat, pLon) to segment between A(latA, lonA) and B(latB, lonB).
     */
    public static double pointToSegmentDistanceMeters(double pLat, double pLon, double latA, double lonA, double latB, double lonB) {
        double avgLatRad = Math.toRadians((latA + latB + pLat) / 3.0);
        double cosLat = Math.cos(avgLatRad);

        double mPerDegLat = 111132.954;
        double mPerDegLon = 111412.84 * cosLat;

        double dx = (lonB - lonA) * mPerDegLon;
        double dy = (latB - latA) * mPerDegLat;

        double px = (pLon - lonA) * mPerDegLon;
        double py = (pLat - latA) * mPerDegLat;

        double segLenSq = dx * dx + dy * dy;
        if (segLenSq < 1e-6) {
            return Math.sqrt(px * px + py * py);
        }

        double t = (px * dx + py * dy) / segLenSq;
        if (t < 0.0) t = 0.0;
        else if (t > 1.0) t = 1.0;

        double nearestX = t * dx;
        double nearestY = t * dy;

        double distX = px - nearestX;
        double distY = py - nearestY;

        return Math.sqrt(distX * distX + distY * distY);
    }

    /**
     * Calculates total length in meters of a polyline.
     */
    public static double calculatePolylineLengthMeters(List<double[]> polyline) {
        if (polyline == null || polyline.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 0; i < polyline.size() - 1; i++) {
            double lonA = polyline.get(i)[0];
            double latA = polyline.get(i)[1];
            double lonB = polyline.get(i + 1)[0];
            double latB = polyline.get(i + 1)[1];

            total += HaversineDistance.calculateDistanceMeters(
                    java.math.BigDecimal.valueOf(latA),
                    java.math.BigDecimal.valueOf(lonA),
                    java.math.BigDecimal.valueOf(latB),
                    java.math.BigDecimal.valueOf(lonB)
            );
        }
        return total;
    }

    public static boolean isValidWgs84Coordinate(double lat, double lon) {
        return !Double.isNaN(lat) && !Double.isNaN(lon) &&
                lat >= -90.0 && lat <= 90.0 &&
                lon >= -180.0 && lon <= 180.0;
    }

    public static boolean isInPuducherryRegion(double lat, double lon) {
        return isValidWgs84Coordinate(lat, lon) &&
                lat >= 11.0 && lat <= 13.0 &&
                lon >= 79.0 && lon <= 81.0;
    }
}
