package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.WeatherObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, Long> {

    @Query("SELECT w FROM WeatherObservation w WHERE w.provider = :provider AND w.stationOrDistrictCode = :code AND w.observationType = :type AND w.observedAt = :observedAt")
    Optional<WeatherObservation> findObservation(
            @Param("provider") String provider,
            @Param("code") String code,
            @Param("type") String type,
            @Param("observedAt") LocalDateTime observedAt);

    Optional<WeatherObservation> findByRawPayloadHash(String rawPayloadHash);

    @Query("SELECT w FROM WeatherObservation w WHERE w.observedAt >= :startTime AND w.observedAt <= :endTime ORDER BY w.observedAt DESC")
    List<WeatherObservation> findObservationsInWindow(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT w FROM WeatherObservation w ORDER BY w.observedAt DESC")
    List<WeatherObservation> findRecentObservations();
}
