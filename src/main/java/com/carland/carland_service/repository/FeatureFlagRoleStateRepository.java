package com.carland.carland_service.repository;

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

    Optional<FeatureFlagRoleState> findFirstByFlagAndRole(FeatureFlag flag, UserRoles role);

    @Query("select s from FeatureFlagRoleState s join fetch s.flag")
    List<FeatureFlagRoleState> findAllFetchFlag();

    boolean existsByFlag(FeatureFlag flag);
}
