package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IntelligenceSignal;
import com.nirvaha.nirvaha_backend.entity.SignalType;
import com.nirvaha.nirvaha_backend.entity.TimeWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntelligenceSignalRepository extends JpaRepository<IntelligenceSignal, Long> {

    List<IntelligenceSignal> findBySignalType(SignalType signalType);

    List<IntelligenceSignal> findByCategory(IncidentCategory category);

    List<IntelligenceSignal> findByStatus(String status);

    @Query("SELECT s FROM IntelligenceSignal s WHERE s.status = :status AND " +
           "(:type IS NULL OR s.signalType = :type) AND " +
           "(:category IS NULL OR s.category = :category) AND " +
           "(:window IS NULL OR s.timeWindow = :window) " +
           "ORDER BY s.generatedAt DESC")
    List<IntelligenceSignal> findFilteredSignals(
            @Param("status") String status,
            @Param("type") SignalType type,
            @Param("category") IncidentCategory category,
            @Param("window") TimeWindow window);
}
