package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.FeatureFlagMeItem;
import com.carland.carland_service.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * tr: Mobil feature-flag toplu okuma; her rol için ayrı flags map döner.
 * en: Mobile bulk feature-flag read; returns a flags map per role.
 */
@RestController
@RequestMapping("/api/v1/feature-flags")
@RequiredArgsConstructor
public class FeatureFlagMeController {

    private final FeatureFlagService featureFlagService;

    @GetMapping("/me")
    public List<FeatureFlagMeItem> me(
            @RequestHeader(value = "X-App-Version", required = false) String appVersion
    ) {
        return featureFlagService.me(appVersion);
    }
}
