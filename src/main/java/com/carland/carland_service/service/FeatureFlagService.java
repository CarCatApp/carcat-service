package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.FeatureFlagAttachRequest;
import com.carland.carland_service.dto.request.FeatureFlagStateUpdateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCreateRequest;
import com.carland.carland_service.dto.request.FeatureFlagWriteRequest;
import com.carland.carland_service.dto.response.FeatureFlagMeItem;
import com.carland.carland_service.entity.AppVersion;
import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagAudit;
import com.carland.carland_service.entity.FeatureFlagEndpoint;
import com.carland.carland_service.enums.FeatureFlagState;

import java.util.List;
import java.util.Map;

public interface FeatureFlagService {

    FeatureFlagMeItem me(String roleHeader, String appVersionHeader);

    Map<String, Object> adminSnapshot(String semver);

    FeatureFlag createFlag(FeatureFlagWriteRequest request, String actor);

    FeatureFlag updateFlag(Long id, FeatureFlagWriteRequest request, String actor);

    void deleteFlag(Long id, String actor);

    void attachEndpoints(Long flagId, FeatureFlagAttachRequest request, String actor);

    void detachEndpoint(Long flagId, Long endpointId, String actor);

    FeatureFlagAudit updateState(FeatureFlagStateUpdateRequest request, String actor);

    AppVersion createVersion(FeatureFlagVersionCreateRequest request);

    AppVersion setCurrentVersion(String semver);

    FeatureFlagState resolve(String httpMethod, String requestPath, String role, String appVersionHeader);

    boolean isNeverGuard(String requestPath);

    FeatureFlagEndpoint upsertEndpoint(String httpMethod, String pathPattern, boolean neverGuard);

    AppVersion ensureCurrentVersion();

    void reloadCache();
}
