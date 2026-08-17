package com.carland.carland_service.dto.response;

import com.carland.carland_service.enums.FeatureFlagState;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * tr: Flutter /me cevabında bir rolün flag map'i.
 * en: One role's flag map in the Flutter /me response.
 */
@Value
@Builder
public class FeatureFlagMeItem {

    String role;
    String appVersion;
    String evaluatedAt;
    Map<String, FeatureFlagState> flags;
}
