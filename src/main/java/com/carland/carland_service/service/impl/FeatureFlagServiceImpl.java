package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.FeatureFlagStateUpdateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCreateRequest;
import com.carland.carland_service.dto.response.FeatureFlagMeItem;
import com.carland.carland_service.entity.AppVersion;
import com.carland.carland_service.entity.FeatureFlagAudit;
import com.carland.carland_service.entity.FeatureFlagEndpoint;
import com.carland.carland_service.entity.FeatureFlagRoleState;
import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.repository.AppVersionRepository;
import com.carland.carland_service.repository.FeatureFlagAuditRepository;
import com.carland.carland_service.repository.FeatureFlagEndpointRepository;
import com.carland.carland_service.repository.FeatureFlagRoleStateRepository;
import com.carland.carland_service.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * tr: Feature-flag iş kuralları: version snapshot, rol state, /me listesi, interceptor eşleme, in-memory cache.
 * en: Feature-flag rules: version snapshot, role state, /me list, interceptor match, in-memory cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagServiceImpl implements FeatureFlagService {

    private static final String DEFAULT_VERSION = "2.1.0";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final UserRoles[] GRID_ROLES = {
            UserRoles.USER, UserRoles.ADMIN, UserRoles.SUPER_ADMIN, UserRoles.BOSS
    };

    private final AppVersionRepository appVersionRepository;
    private final FeatureFlagEndpointRepository endpointRepository;
    private final FeatureFlagRoleStateRepository roleStateRepository;
    private final FeatureFlagAuditRepository auditRepository;

    private final AtomicReference<List<FeatureFlagEndpoint>> endpointCache = new AtomicReference<>(List.of());
    private final AtomicReference<Map<String, FeatureFlagState>> stateCache = new AtomicReference<>(Map.of());
    private final AtomicReference<String> currentSemverCache = new AtomicReference<>(DEFAULT_VERSION);

    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlagMeItem> me(String appVersionHeader) {
        AppVersion version = resolveVersion(appVersionHeader);
        List<FeatureFlagRoleState> rows = roleStateRepository.findByVersionFetchEndpoint(version);
        String evaluatedAt = Instant.now().toString();

        Map<UserRoles, Map<String, FeatureFlagState>> byRole = new LinkedHashMap<>();
        for (UserRoles role : GRID_ROLES) {
            byRole.put(role, new LinkedHashMap<>());
        }
        for (FeatureFlagRoleState row : rows) {
            FeatureFlagEndpoint ep = row.getEndpoint();
            if (ep.isNeverGuard()) {
                continue;
            }
            String key = ep.getHttpMethod() + " " + ep.getPathPattern();
            byRole.get(row.getRole()).put(key, row.getState());
        }

        List<FeatureFlagMeItem> out = new ArrayList<>();
        for (UserRoles role : GRID_ROLES) {
            out.add(FeatureFlagMeItem.builder()
                    .role(role.name())
                    .appVersion(version.getSemver())
                    .evaluatedAt(evaluatedAt)
                    .flags(byRole.get(role))
                    .build());
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> grid(String semver) {
        AppVersion version = resolveVersion(semver);
        List<FeatureFlagEndpoint> endpoints = endpointRepository.findAllByOrderByPathPatternAscHttpMethodAsc();
        List<FeatureFlagRoleState> rows = roleStateRepository.findByVersionFetchEndpoint(version);

        Map<String, Map<String, String>> stateByKey = new LinkedHashMap<>();
        for (FeatureFlagRoleState row : rows) {
            String key = row.getEndpoint().getId() + "|" + row.getRole().name();
            stateByKey.computeIfAbsent(key, k -> new LinkedHashMap<>());
            stateByKey.get(key).put("state", row.getState().name());
        }

        List<Map<String, Object>> endpointDtos = new ArrayList<>();
        for (FeatureFlagEndpoint ep : endpoints) {
            Map<String, String> states = new LinkedHashMap<>();
            for (UserRoles role : GRID_ROLES) {
                String key = ep.getId() + "|" + role.name();
                Map<String, String> found = stateByKey.get(key);
                states.put(role.name(), found != null ? found.get("state") : FeatureFlagState.ENABLED.name());
            }
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("method", ep.getHttpMethod());
            dto.put("path", ep.getPathPattern());
            dto.put("neverGuard", ep.isNeverGuard());
            dto.put("states", states);
            endpointDtos.add(dto);
        }

        List<Map<String, Object>> versions = new ArrayList<>();
        for (AppVersion v : appVersionRepository.findAllByOrderByCreatedAtDesc()) {
            Map<String, Object> vd = new LinkedHashMap<>();
            vd.put("semver", v.getSemver());
            vd.put("current", v.isCurrent());
            versions.add(vd);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", version.getSemver());
        body.put("current", version.isCurrent());
        body.put("roles", List.of("USER", "ADMIN", "SUPER_ADMIN", "BOSS"));
        body.put("versions", versions);
        body.put("endpoints", endpointDtos);
        body.put("audit", toAuditDtos(auditRepository.findTop50ByAppVersionOrderByCreatedAtDesc(version.getSemver())));
        return body;
    }

    @Override
    @Transactional
    public FeatureFlagAudit updateState(FeatureFlagStateUpdateRequest request, String actor) {
        if (request.getVersion() == null || request.getMethod() == null || request.getPath() == null
                || request.getRole() == null || request.getState() == null) {
            throw new IllegalArgumentException("version, method, path, role, state required");
        }
        AppVersion version = appVersionRepository.findBySemver(request.getVersion())
                .orElseThrow(() -> new IllegalArgumentException("Unknown version " + request.getVersion()));
        FeatureFlagEndpoint endpoint = endpointRepository
                .findByHttpMethodAndPathPattern(request.getMethod().toUpperCase(), request.getPath())
                .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint"));

        FeatureFlagRoleState row = roleStateRepository
                .findByEndpointAndVersionAndRole(endpoint, version, request.getRole())
                .orElseGet(() -> FeatureFlagRoleState.builder()
                        .endpoint(endpoint)
                        .version(version)
                        .role(request.getRole())
                        .state(FeatureFlagState.ENABLED)
                        .build());

        FeatureFlagState oldState = row.getState();
        row.setState(request.getState());
        roleStateRepository.save(row);

        FeatureFlagAudit audit = FeatureFlagAudit.builder()
                .createdAt(LocalDateTime.now())
                .actor(actor != null ? actor : "unknown")
                .httpMethod(endpoint.getHttpMethod())
                .pathPattern(endpoint.getPathPattern())
                .role(request.getRole())
                .oldState(oldState)
                .newState(request.getState())
                .appVersion(version.getSemver())
                .build();
        auditRepository.save(audit);
        reloadCache();
        return audit;
    }

    @Override
    @Transactional
    public AppVersion createVersion(FeatureFlagVersionCreateRequest request) {
        String semver = request.getSemver() == null ? "" : request.getSemver().trim();
        if (semver.isBlank()) {
            throw new IllegalArgumentException("semver required");
        }
        if (appVersionRepository.findBySemver(semver).isPresent()) {
            throw new IllegalArgumentException("Version already exists");
        }
        AppVersion source = request.getCopyFrom() != null && !request.getCopyFrom().isBlank()
                ? appVersionRepository.findBySemver(request.getCopyFrom())
                .orElseThrow(() -> new IllegalArgumentException("copyFrom not found"))
                : appVersionRepository.findByCurrentTrue()
                .orElseThrow(() -> new IllegalArgumentException("No current version"));

        appVersionRepository.findByCurrentTrue().ifPresent(current -> {
            current.setCurrent(false);
            appVersionRepository.save(current);
        });

        AppVersion created = appVersionRepository.save(AppVersion.builder()
                .semver(semver)
                .current(true)
                .createdAt(LocalDateTime.now())
                .build());

        List<FeatureFlagRoleState> sourceRows = roleStateRepository.findByVersionFetchEndpoint(source);
        for (FeatureFlagRoleState src : sourceRows) {
            roleStateRepository.save(FeatureFlagRoleState.builder()
                    .endpoint(src.getEndpoint())
                    .version(created)
                    .role(src.getRole())
                    .state(src.getState())
                    .build());
        }
        reloadCache();
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppVersion> latestVersions() {
        return appVersionRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureFlagAudit> recentAudit(String semver) {
        if (semver == null || semver.isBlank()) {
            return auditRepository.findTop100ByOrderByCreatedAtDesc();
        }
        return auditRepository.findTop50ByAppVersionOrderByCreatedAtDesc(semver);
    }

    @Override
    public FeatureFlagState resolve(String httpMethod, String requestPath, String role, String appVersionHeader) {
        FeatureFlagEndpoint matched = matchEndpoint(httpMethod, requestPath);
        if (matched == null || matched.isNeverGuard()) {
            return FeatureFlagState.ENABLED;
        }
        UserRoles parsedRole = parseRole(role);
        if (parsedRole == null) {
            return FeatureFlagState.ENABLED;
        }
        String version = resolveVersionSemver(appVersionHeader);
        FeatureFlagState cached = stateCache.get().get(cacheKey(version, matched.getHttpMethod(), matched.getPathPattern(), parsedRole));
        return cached != null ? cached : FeatureFlagState.ENABLED;
    }

    @Override
    public boolean isNeverGuard(String requestPath) {
        if (requestPath == null) {
            return true;
        }
        return requestPath.startsWith("/admin")
                || requestPath.startsWith("/swagger")
                || requestPath.contains("/swagger-ui")
                || requestPath.startsWith("/v3/api-docs")
                || requestPath.startsWith("/webhook")
                || requestPath.startsWith("/legal")
                || requestPath.equals("/error")
                || requestPath.startsWith("/actuator")
                || requestPath.equals("/api/v1/feature-flags/me")
                || requestPath.startsWith("/api/v1/feature-flags");
    }

    @Override
    @Transactional
    public FeatureFlagEndpoint upsertEndpoint(String httpMethod, String pathPattern, boolean neverGuard) {
        FeatureFlagEndpoint endpoint = endpointRepository
                .findByHttpMethodAndPathPattern(httpMethod, pathPattern)
                .orElseGet(() -> FeatureFlagEndpoint.builder()
                        .httpMethod(httpMethod)
                        .pathPattern(pathPattern)
                        .neverGuard(neverGuard)
                        .build());
        endpoint.setNeverGuard(neverGuard);
        return endpointRepository.save(endpoint);
    }

    @Override
    @Transactional
    public void ensureRoleStatesForCurrentVersion(FeatureFlagEndpoint endpoint) {
        AppVersion current = ensureCurrentVersion();
        if (roleStateRepository.existsByEndpointAndVersion(endpoint, current)) {
            return;
        }
        for (UserRoles role : GRID_ROLES) {
            roleStateRepository.save(FeatureFlagRoleState.builder()
                    .endpoint(endpoint)
                    .version(current)
                    .role(role)
                    .state(FeatureFlagState.ENABLED)
                    .build());
        }
    }

    @Override
    @Transactional
    public AppVersion ensureCurrentVersion() {
        return appVersionRepository.findByCurrentTrue().orElseGet(() ->
                appVersionRepository.findBySemver(DEFAULT_VERSION).orElseGet(() ->
                        appVersionRepository.save(AppVersion.builder()
                                .semver(DEFAULT_VERSION)
                                .current(true)
                                .createdAt(LocalDateTime.now())
                                .build())));
    }

    @Override
    public void reloadCache() {
        List<FeatureFlagEndpoint> endpoints = endpointRepository.findAll();
        endpointCache.set(List.copyOf(endpoints));
        Map<String, FeatureFlagState> states = new LinkedHashMap<>();
        for (AppVersion version : appVersionRepository.findAll()) {
            for (FeatureFlagRoleState row : roleStateRepository.findByVersionFetchEndpoint(version)) {
                FeatureFlagEndpoint ep = row.getEndpoint();
                states.put(cacheKey(version.getSemver(), ep.getHttpMethod(), ep.getPathPattern(), row.getRole()),
                        row.getState());
            }
        }
        stateCache.set(Map.copyOf(states));
        appVersionRepository.findByCurrentTrue()
                .ifPresent(v -> currentSemverCache.set(v.getSemver()));
        log.info("Feature-flag cache reloaded: {} endpoints, {} states", endpoints.size(), states.size());
    }

    private FeatureFlagEndpoint matchEndpoint(String httpMethod, String requestPath) {
        if (httpMethod == null || requestPath == null) {
            return null;
        }
        String method = httpMethod.toUpperCase();
        for (FeatureFlagEndpoint ep : endpointCache.get()) {
            if (!method.equals(ep.getHttpMethod())) {
                continue;
            }
            if (PATH_MATCHER.match(ep.getPathPattern(), requestPath)) {
                return ep;
            }
        }
        return null;
    }

    private AppVersion resolveVersion(String semver) {
        if (semver != null && !semver.isBlank()) {
            return appVersionRepository.findBySemver(semver.trim())
                    .orElseGet(this::ensureCurrentVersion);
        }
        return ensureCurrentVersion();
    }

    private String resolveVersionSemver(String header) {
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return currentSemverCache.get();
    }

    private UserRoles parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return UserRoles.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String cacheKey(String version, String method, String path, UserRoles role) {
        return version + "|" + method + "|" + path + "|" + role.name();
    }

    private List<Map<String, Object>> toAuditDtos(List<FeatureFlagAudit> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FeatureFlagAudit row : rows) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("at", row.getCreatedAt() != null ? row.getCreatedAt().toString() : null);
            dto.put("actor", row.getActor());
            dto.put("method", row.getHttpMethod());
            dto.put("path", row.getPathPattern());
            dto.put("role", row.getRole() != null ? row.getRole().name() : null);
            dto.put("oldState", row.getOldState() != null ? row.getOldState().name() : null);
            dto.put("newState", row.getNewState() != null ? row.getNewState().name() : null);
            dto.put("appVersion", row.getAppVersion());
            out.add(dto);
        }
        return out;
    }
}
