package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.RoadSegment;
import com.nirvaha.nirvaha_backend.entity.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoadSegmentRepository extends JpaRepository<RoadSegment, Long> {

    Optional<RoadSegment> findByOsmFeatureId(Long osmFeatureId);

    List<RoadSegment> findByValidationStatus(ValidationStatus validationStatus);

    long countByValidationStatus(ValidationStatus validationStatus);

    @Query("SELECT r FROM RoadSegment r WHERE r.validationStatus = :status " +
           "AND r.minLat <= :maxLat AND r.maxLat >= :minLat " +
           "AND r.minLng <= :maxLng AND r.maxLng >= :minLng")
    List<RoadSegment> findCandidateRoadsByBbox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("status") ValidationStatus status);

    @Query("SELECT COALESCE(SUM(r.lengthMeters), 0.0) FROM RoadSegment r WHERE r.validationStatus = 'VERIFIED'")
    Double sumVerifiedLengthMeters();
}
