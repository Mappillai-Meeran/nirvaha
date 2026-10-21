package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.AdministrativeArea;
import com.nirvaha.nirvaha_backend.entity.AreaType;
import com.nirvaha.nirvaha_backend.entity.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdministrativeAreaRepository extends JpaRepository<AdministrativeArea, Long> {
    Optional<AdministrativeArea> findByOfficialCode(String officialCode);
    List<AdministrativeArea> findByAreaType(AreaType areaType);
    List<AdministrativeArea> findByValidationStatus(ValidationStatus status);

    @Query("SELECT a FROM AdministrativeArea a WHERE a.validationStatus = :status AND " +
           ":lat >= a.minLat AND :lat <= a.maxLat AND :lng >= a.minLng AND :lng <= a.maxLng")
    List<AdministrativeArea> findCandidateAreasByBbox(@Param("lat") Double lat, @Param("lng") Double lng, @Param("status") ValidationStatus status);
}
