package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.IncidentCategory;
import com.nirvaha.nirvaha_backend.entity.ServiceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRuleRepository extends JpaRepository<ServiceRule, Long> {

    @Query("SELECT r FROM ServiceRule r WHERE r.serviceCategory = :category ORDER BY r.isOfficialSla DESC, r.id DESC")
    List<ServiceRule> findByServiceCategory(@Param("category") IncidentCategory category);

    @Query("SELECT r FROM ServiceRule r WHERE r.serviceCategory = :category AND r.department = :department AND r.jurisdiction = :jurisdiction")
    List<ServiceRule> findApplicableRules(
            @Param("category") IncidentCategory category,
            @Param("department") String department,
            @Param("jurisdiction") String jurisdiction);
}
