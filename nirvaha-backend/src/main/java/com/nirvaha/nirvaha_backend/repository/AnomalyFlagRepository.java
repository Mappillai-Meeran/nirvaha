package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.AnomalyFlag;
import com.nirvaha.nirvaha_backend.entity.AnomalyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnomalyFlagRepository extends JpaRepository<AnomalyFlag, Long> {

    List<AnomalyFlag> findByIncidentId(Long incidentId);

    boolean existsByIncidentIdAndAnomalyType(Long incidentId, AnomalyType anomalyType);

    Optional<AnomalyFlag> findByIncidentIdAndAnomalyType(Long incidentId, AnomalyType anomalyType);

    List<AnomalyFlag> findByStatus(com.nirvaha.nirvaha_backend.entity.AnomalyStatus status);
}
