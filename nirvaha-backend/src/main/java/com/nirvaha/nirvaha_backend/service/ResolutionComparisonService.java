package com.nirvaha.nirvaha_backend.service;

import com.nirvaha.nirvaha_backend.entity.Evidence;
import com.nirvaha.nirvaha_backend.util.HaversineDistance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service abstraction for evidence-to-resolution comparison.
 * <p>
 * MVP Baseline: Calculates a deterministic evidence consistency score between 0.0 and 1.0 based on
 * spatial proximity (Haversine distance), media format compatibility, and metadata alignment.
 * <p>
 * Future Extension: Designed to seamlessly delegate to a Python AI computer vision service
 * (e.g. POST /compare) without modifying domain or controller contracts.
 */
@Service
public class ResolutionComparisonService {

    public static final double WEIGHT_SPATIAL_PROXIMITY = 0.50;
    public static final double WEIGHT_MEDIA_COMPATIBILITY = 0.30;
    public static final double WEIGHT_METADATA_ALIGNMENT = 0.20;

    public static final double MAX_PROXIMITY_THRESHOLD_METERS = 500.0;

    public double compare(Evidence originalEvidence, Evidence resolutionEvidence) {
        if (originalEvidence == null || resolutionEvidence == null) {
            return 0.0;
        }

        double spatialScore = calculateSpatialProximityScore(originalEvidence, resolutionEvidence);
        double mediaScore = calculateMediaCompatibilityScore(originalEvidence, resolutionEvidence);
        double metadataScore = calculateMetadataAlignmentScore(originalEvidence, resolutionEvidence);

        double rawScore = (spatialScore * WEIGHT_SPATIAL_PROXIMITY)
                + (mediaScore * WEIGHT_MEDIA_COMPATIBILITY)
                + (metadataScore * WEIGHT_METADATA_ALIGNMENT);

        double clampedScore = Math.max(0.0, Math.min(1.0, rawScore));

        return BigDecimal.valueOf(clampedScore)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double calculateSpatialProximityScore(Evidence original, Evidence resolution) {
        if (original.getLatitude() == null || original.getLongitude() == null ||
                resolution.getLatitude() == null || resolution.getLongitude() == null) {
            // Default moderate spatial alignment if location coordinates omitted
            return 0.70;
        }

        double distanceMeters = HaversineDistance.calculateDistanceMeters(
                original.getLatitude(), original.getLongitude(),
                resolution.getLatitude(), resolution.getLongitude()
        );

        if (distanceMeters <= 50.0) {
            return 1.00;
        } else if (distanceMeters <= 100.0) {
            return 0.90;
        } else if (distanceMeters <= 200.0) {
            return 0.75;
        } else if (distanceMeters <= MAX_PROXIMITY_THRESHOLD_METERS) {
            return 0.50;
        } else {
            return 0.10;
        }
    }

    private double calculateMediaCompatibilityScore(Evidence original, Evidence resolution) {
        if (original.getEvidenceType() != null && original.getEvidenceType().equals(resolution.getEvidenceType())) {
            return 1.00;
        }
        return 0.50;
    }

    private double calculateMetadataAlignmentScore(Evidence original, Evidence resolution) {
        boolean originalValid = original.getFileUrl() != null && !original.getFileUrl().isBlank();
        boolean resolutionValid = resolution.getFileUrl() != null && !resolution.getFileUrl().isBlank();

        if (originalValid && resolutionValid) {
            return 1.00;
        } else if (originalValid || resolutionValid) {
            return 0.50;
        } else {
            return 0.00;
        }
    }
}
