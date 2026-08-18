package com.carland.carland_service.service;

import com.carland.carland_service.entity.FeatureFlagEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * tr: Boot'ta Spring route tablosunu tarar; yalnızca feature_flag_endpoint kataloğunu günceller (flag attach admin'de).
 * en: Scans Spring's route table at boot; upserts the endpoint catalog only (flag attach is admin-driven).
 */
@Slf4j
@Component
public class FeatureFlagEndpointScanner implements ApplicationRunner {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final FeatureFlagService featureFlagService;

    public FeatureFlagEndpointScanner(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping requestMappingHandlerMapping,
            FeatureFlagService featureFlagService
    ) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.featureFlagService = featureFlagService;
    }

    @Override
    public void run(ApplicationArguments args) {
        featureFlagService.ensureCurrentVersion();

        List<FeatureFlagEndpoint> scanned = new ArrayList<>();
        for (RequestMappingInfo info : requestMappingHandlerMapping.getHandlerMethods().keySet()) {
            Set<String> paths = pathsOf(info);
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                continue;
            }
            for (String path : paths) {
                if (shouldSkip(path)) {
                    continue;
                }
                boolean neverGuard = isNeverGuardPath(path);
                for (RequestMethod method : methods) {
                    scanned.add(FeatureFlagEndpoint.builder()
                            .httpMethod(method.name())
                            .pathPattern(path)
                            .neverGuard(neverGuard)
                            .build());
                }
            }
        }
        int inserted = featureFlagService.syncScannedEndpoints(scanned);
        featureFlagService.reloadCache();
        log.info("Feature-flag scan finished: {} Spring pairs, {} new catalog rows", scanned.size(), inserted);
    }

    private Set<String> pathsOf(RequestMappingInfo info) {
        Set<String> paths = new LinkedHashSet<>();
        if (info.getPathPatternsCondition() != null) {
            paths.addAll(info.getPathPatternsCondition().getPatternValues());
        }
        if (info.getPatternsCondition() != null) {
            paths.addAll(info.getPatternsCondition().getPatterns());
        }
        return paths;
    }

    private boolean shouldSkip(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        return path.startsWith("/admin")
                || path.contains("swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webhook")
                || path.equals("/error")
                || path.startsWith("/actuator")
                || path.equals("/swagger-auth-config")
                || path.equals("/swagger-custom.js")
                || path.equals("/swagger-definitions-config");
    }

    private boolean isNeverGuardPath(String path) {
        return path.startsWith("/legal")
                || path.startsWith("/api/v1/feature-flags")
                || path.startsWith("/api/v1/user/customer-cars")
                || path.equals("/api/v1/group/by/get/brand/list/with/models");
    }
}
