package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Hangi app version'ın is_current olacağını seçer.
 * en: Selects which app version is is_current.
 */
@Data
public class FeatureFlagVersionCurrentRequest {

    String semver;
}
