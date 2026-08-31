package com.carland.carland_service.repository;

import com.carland.carland_service.entity.AppVersion;
import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagRoleState;
import com.carland.carland_service.enums.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRoleStateRepository extends JpaRepository<FeatureFlagRoleState, Long> {

    Optional<FeatureFlagRoleState> findByFlagAndVersionAndRole(FeatureFlag flag, AppVersion version, UserRoles role);

    Optional<FeatureFlagRoleState> findFirstByFlagAndRole(FeatureFlag flag, UserRoles role);

    @Query("select s from FeatureFlagRoleState s join fetch s.flag where s.version = :version")
    List<FeatureFlagRoleState> findByVersionFetchFlag(AppVersion version);

    @Query("select s from FeatureFlagRoleState s join fetch s.flag")
    List<FeatureFlagRoleState> findAllFetchFlag();

    boolean existsByFlagAndVersion(FeatureFlag flag, AppVersion version);

    boolean existsByFlag(FeatureFlag flag);
}
