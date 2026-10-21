package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.IncidentServiceTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentServiceTrackingRepository extends JpaRepository<IncidentServiceTracking, Long> {
    Optional<IncidentServiceTracking> findByIncidentId(Long incidentId);
    List<IncidentServiceTracking> findBySlaStatus(com.nirvaha.nirvaha_backend.entity.SlaStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM IncidentServiceTracking t JOIN FETCH t.serviceRule JOIN FETCH t.incident WHERE t.slaStatus IN :statuses")
    List<IncidentServiceTracking> findBySlaStatusIn(@org.springframework.data.repository.query.Param("statuses") List<com.nirvaha.nirvaha_backend.entity.SlaStatus> statuses);

    @org.springframework.data.jpa.repository.Query("SELECT t.slaStatus, COUNT(t) FROM IncidentServiceTracking t WHERE (t.incident.intakeSource != com.nirvaha.nirvaha_backend.entity.IncidentSource.SIMULATED OR t.incident.intakeSource IS NULL) AND (t.incident.classificationTag != com.nirvaha.nirvaha_backend.entity.DataClassification.SIMULATED OR t.incident.classificationTag IS NULL) GROUP BY t.slaStatus")
    List<Object[]> countBySlaStatusGrouped();

    @org.springframework.data.jpa.repository.Query("SELECT t FROM IncidentServiceTracking t JOIN FETCH t.serviceRule sr JOIN FETCH t.incident i WHERE t.slaStatus = com.nirvaha.nirvaha_backend.entity.SlaStatus.OVERDUE AND sr.isOfficialSla = true AND (i.intakeSource != com.nirvaha.nirvaha_backend.entity.IncidentSource.SIMULATED OR i.intakeSource IS NULL) AND (i.classificationTag != com.nirvaha.nirvaha_backend.entity.DataClassification.SIMULATED OR i.classificationTag IS NULL)")
    List<IncidentServiceTracking> findOfficialOverdue();
}
