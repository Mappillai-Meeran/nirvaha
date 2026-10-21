package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findByIncidentId(Long incidentId);

    List<Evidence> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);

    Optional<Evidence> findTopByContentHash(String contentHash);

    List<Evidence> findByDemoRunId(String demoRunId);
}
