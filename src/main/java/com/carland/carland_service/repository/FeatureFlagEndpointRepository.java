package com.carland.carland_service.repository;

import com.carland.carland_service.entity.FeatureFlagEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * tr: Feature-flag endpoint (method + Spring path) kayıtları.
 * en: Feature-flag endpoint (method + Spring path) rows.
 */
@Repository
public interface FeatureFlagEndpointRepository extends JpaRepository<FeatureFlagEndpoint, Long> {

    Optional<FeatureFlagEndpoint> findByHttpMethodAndPathPattern(String httpMethod, String pathPattern);

    List<FeatureFlagEndpoint> findAllByOrderByPathPatternAscHttpMethodAsc();
}
