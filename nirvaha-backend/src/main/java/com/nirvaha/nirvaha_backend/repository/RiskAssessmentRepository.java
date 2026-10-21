package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    List<RiskAssessment> findByWardIdOrderByAssessmentTimeDesc(Long wardId);

    List<RiskAssessment> findByWardIdAndCategoryOrderByAssessmentTimeDesc(Long wardId, IncidentCategory category);

    Optional<RiskAssessment> findTopByWardIdAndCategoryOrderByAssessmentTimeDesc(Long wardId, IncidentCategory category);

    Optional<RiskAssessment> findTopByWardIdOrderByAssessmentTimeDesc(Long wardId);
}
