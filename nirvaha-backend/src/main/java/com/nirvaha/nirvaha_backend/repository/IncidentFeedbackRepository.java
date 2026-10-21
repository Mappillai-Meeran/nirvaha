package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.FeedbackType;
import com.nirvaha.nirvaha_backend.entity.IncidentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentFeedbackRepository extends JpaRepository<IncidentFeedback, Long> {

    List<IncidentFeedback> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);

    Optional<IncidentFeedback> findTopByIncidentIdOrderByCreatedAtDesc(Long incidentId);

    boolean existsByIncidentIdAndFeedbackType(Long incidentId, FeedbackType feedbackType);

    Optional<IncidentFeedback> findByResolutionId(Long resolutionId);

    boolean existsByResolutionId(Long resolutionId);

    List<IncidentFeedback> findByResolutionIdOrderByCreatedAtDesc(Long resolutionId);

    List<IncidentFeedback> findByDemoRunId(String demoRunId);
}
