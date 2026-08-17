package com.carland.carland_service.repository;

import com.carland.carland_service.entity.AppVersion;
import com.carland.carland_service.entity.FeatureFlagEndpoint;
import com.carland.carland_service.entity.FeatureFlagRoleState;
import com.carland.carland_service.enums.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * tr: Version + endpoint + rol görünürlük satırları.
 * en: Version + endpoint + role visibility rows.
 */
@Repository
public interface FeatureFlagRoleStateRepository extends JpaRepository<FeatureFlagRoleState, Long> {

    Optional<FeatureFlagRoleState> findByEndpointAndVersionAndRole(
            FeatureFlagEndpoint endpoint, AppVersion version, UserRoles role);

    List<FeatureFlagRoleState> findByVersion(AppVersion version);

    @Query("select s from FeatureFlagRoleState s join fetch s.endpoint where s.version = :version")
    List<FeatureFlagRoleState> findByVersionFetchEndpoint(AppVersion version);

    boolean existsByEndpointAndVersion(FeatureFlagEndpoint endpoint, AppVersion version);
}
