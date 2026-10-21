package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.Resolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResolutionRepository extends JpaRepository<Resolution, Long> {

    List<Resolution> findByIncidentId(Long incidentId);

    Optional<Resolution> findTopByIncidentIdOrderByCreatedAtDesc(Long incidentId);

    List<Resolution> findByDemoRunId(String demoRunId);
}
