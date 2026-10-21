package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.IncidentAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentActionRepository extends JpaRepository<IncidentAction, Long> {

    List<IncidentAction> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);

    List<IncidentAction> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);

    List<IncidentAction> findByOfficerIdAndActionTypeOrderByCreatedAtDesc(Long officerId, com.nirvaha.nirvaha_backend.entity.ActionType actionType);

    List<IncidentAction> findByDemoRunId(String demoRunId);
}
