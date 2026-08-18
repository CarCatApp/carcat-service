package com.carland.carland_service.dto.response;

import com.carland.carland_service.enums.FeatureFlagState;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * tr: Flutter /me — çağıran rol için named flag grupları (içinde API listesi).
 * en: Flutter /me — named flag groups (with API list) for the caller role.
 */
@Value
@Builder
public class FeatureFlagMeItem {

    String role;
    String appVersion;
    String evaluatedAt;
    Map<String, FeatureFlagMeGroup> flags;
}
