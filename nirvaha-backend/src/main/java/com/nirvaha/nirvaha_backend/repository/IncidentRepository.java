package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.DataClassification;
import com.nirvaha.nirvaha_backend.entity.Incident;
import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByCategory(IncidentCategory category);

    List<Incident> findByReporterId(Long reporterId);

    List<Incident> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    List<Incident> findByClassificationTag(DataClassification classificationTag);

    List<Incident> findByDemoRunId(String demoRunId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i WHERE (i.intakeSource != com.nirvaha.nirvaha_backend.entity.IncidentSource.SIMULATED OR i.intakeSource IS NULL) AND (i.classificationTag != com.nirvaha.nirvaha_backend.entity.DataClassification.SIMULATED OR i.classificationTag IS NULL) ORDER BY i.createdAt DESC")
    List<Incident> findOfficialIncidents();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i WHERE i.id = :id")
    java.util.Optional<Incident> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i WHERE i.status != com.nirvaha.nirvaha_backend.entity.IncidentStatus.RESOLVED AND (i.intakeSource != com.nirvaha.nirvaha_backend.entity.IncidentSource.SIMULATED OR i.intakeSource IS NULL) AND (i.classificationTag != com.nirvaha.nirvaha_backend.entity.DataClassification.SIMULATED OR i.classificationTag IS NULL)")
    List<Incident> findOpenNonSimulatedIncidents();
}
