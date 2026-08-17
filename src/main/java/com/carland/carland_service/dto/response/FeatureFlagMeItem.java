package com.carland.carland_service.dto.response;

import com.carland.carland_service.enums.FeatureFlagState;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * tr: Flutter /me cevabı; çağıran kullanıcının rolü + o role ait API state map'i.
 * en: Flutter /me response; caller role plus that role's API state map.
 */
@Value
@Builder
public class FeatureFlagMeItem {

    String role;
    String appVersion;
    String evaluatedAt;
    Map<String, FeatureFlagState> flags;
}
