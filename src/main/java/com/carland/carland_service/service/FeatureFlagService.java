package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.FeatureFlagStateUpdateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCreateRequest;
import com.carland.carland_service.dto.response.FeatureFlagMeItem;
import com.carland.carland_service.entity.AppVersion;
import com.carland.carland_service.entity.FeatureFlagAudit;
import com.carland.carland_service.entity.FeatureFlagEndpoint;
import com.carland.carland_service.enums.FeatureFlagState;

import java.util.List;
import java.util.Map;

/**
 * tr: Feature-flag okuma, admin grid, audit ve interceptor lookup.
 * en: Feature-flag reads, admin grid, audit and interceptor lookup.
 */
public interface FeatureFlagService {

    List<FeatureFlagMeItem> me(String appVersionHeader);

    Map<String, Object> grid(String semver);

    FeatureFlagAudit updateState(FeatureFlagStateUpdateRequest request, String actor);

    AppVersion createVersion(FeatureFlagVersionCreateRequest request);

    List<AppVersion> latestVersions();

    List<FeatureFlagAudit> recentAudit(String semver);

    FeatureFlagState resolve(String httpMethod, String requestPath, String role, String appVersionHeader);

    boolean isNeverGuard(String requestPath);

    FeatureFlagEndpoint upsertEndpoint(String httpMethod, String pathPattern, boolean neverGuard);

    void ensureRoleStatesForCurrentVersion(FeatureFlagEndpoint endpoint);

    AppVersion ensureCurrentVersion();

    void reloadCache();
}
