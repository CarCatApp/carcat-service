package com.carland.carland_service.repository;

import com.carland.carland_service.entity.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * tr: App version (semver) kayıtları.
 * en: App version (semver) rows.
 */
@Repository
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

    Optional<AppVersion> findBySemver(String semver);

    Optional<AppVersion> findByCurrentTrue();

    List<AppVersion> findTop5ByOrderByCreatedAtDesc();

    List<AppVersion> findAllByOrderByCreatedAtDesc();
}
