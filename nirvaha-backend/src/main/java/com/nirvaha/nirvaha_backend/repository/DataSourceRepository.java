package com.nirvaha.nirvaha_backend.repository;

import com.nirvaha.nirvaha_backend.entity.DataSource;
import com.nirvaha.nirvaha_backend.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
    Optional<DataSource> findByName(String name);
    List<DataSource> findBySourceType(SourceType sourceType);
    List<DataSource> findByStatus(String status);
}
