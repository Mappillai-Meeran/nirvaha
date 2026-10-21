package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.OfficerDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfficerDecisionRepository extends JpaRepository<OfficerDecision, Long> {

    List<OfficerDecision> findByIncidentIdOrderByDecidedAtDesc(Long incidentId);

    List<OfficerDecision> findByDecidedByIdOrderByDecidedAtDesc(Long officerId);
}
