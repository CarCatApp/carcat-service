package com.carland.carland_service.repository;

import com.carland.carland_service.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {

    Optional<FeatureFlag> findByName(String name);

    Optional<FeatureFlag> findByIdAndDeletedAtIsNull(Long id);

    List<FeatureFlag> findByDeletedAtIsNullOrderByNameAsc();

    boolean existsByName(String name);
}
