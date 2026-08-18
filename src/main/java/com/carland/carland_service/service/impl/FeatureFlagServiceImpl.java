package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.FeatureFlagAttachRequest;
import com.carland.carland_service.dto.request.FeatureFlagStateUpdateRequest;
import com.carland.carland_service.dto.request.FeatureFlagVersionCreateRequest;
import com.carland.carland_service.dto.request.FeatureFlagWriteRequest;
import com.carland.carland_service.dto.response.FeatureFlagEndpointView;
import com.carland.carland_service.dto.response.FeatureFlagMeGroup;
import com.carland.carland_service.dto.response.FeatureFlagMeItem;
import com.carland.carland_service.entity.AppVersion;
import com.carland.carland_service.entity.FeatureFlag;
import com.carland.carland_service.entity.FeatureFlagAudit;
import com.carland.carland_service.entity.FeatureFlagEndpoint;
import com.carland.carland_service.entity.FeatureFlagRoleState;
import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.repository.AppVersionRepository;
import com.carland.carland_service.repository.FeatureFlagAuditRepository;
import com.carland.carland_service.repository.FeatureFlagEndpointRepository;
import com.carland.carland_service.repository.FeatureFlagRepository;
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
    private final FeatureFlagRepository flagRepository;
    private final FeatureFlagEndpointRepository endpointRepository;
    private final FeatureFlagRoleStateRepository roleStateRepository;
    private final FeatureFlagAuditRepository auditRepository;

    private final AtomicReference<List<FeatureFlagEndpoint>> endpointCache = new AtomicReference<>(List.of());
    private final AtomicReference<Map<String, FeatureFlagState>> stateCache = new AtomicReference<>(Map.of());
    private final AtomicReference<Map<Long, FeatureFlagState>> flagDefaultCache = new AtomicReference<>(Map.of());
    private final AtomicReference<String> currentSemverCache = new AtomicReference<>(DEFAULT_VERSION);

    @Override
    @Transactional(readOnly = true)
    public FeatureFlagMeItem me(String roleHeader, String appVersionHeader) {
        UserRoles role = parseRole(roleHeader);
        if (role == null) {
            throw new IllegalArgumentException("role header required (USER|ADMIN|SUPER_ADMIN|BOSS)");
        }
        AppVersion version = resolveVersion(appVersionHeader);
        List<FeatureFlag> flags = flagRepository.findByDeletedAtIsNullOrderByNameAsc();
        Map<String, FeatureFlagState> stateByFlag = new LinkedHashMap<>();
        for (FeatureFlagRoleState row : roleStateRepository.findByVersionFetchFlag(version)) {
            if (row.getRole() == role && row.getFlag() != null && row.getFlag().getDeletedAt() == null) {
                stateByFlag.put(row.getFlag().getName(), row.getState());
            }
        }
        Map<String, FeatureFlagMeGroup> out = new LinkedHashMap<>();
        for (FeatureFlag flag : flags) {
            FeatureFlagState state = stateByFlag.getOrDefault(flag.getName(), flag.getDefaultState());
            List<FeatureFlagEndpoint> attached = endpointRepository.findByFlag_Id(flag.getId());
            List<FeatureFlagEndpointView> views = new ArrayList<>();
            for (FeatureFlagEndpoint ep : attached) {
                views.add(FeatureFlagEndpointView.builder()
                        .id(ep.getId())
                        .method(ep.getHttpMethod())
                        .path(ep.getPathPattern())
                        .build());
            }
            out.put(flag.getName(), FeatureFlagMeGroup.builder().state(state).endpoints(views).build());
        }
        return FeatureFlagMeItem.builder()
                .role(role.name())
                .appVersion(version.getSemver())
                .evaluatedAt(Instant.now().toString())
                .flags(out)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> adminSnapshot(String semver) {
        AppVersion version = resolveVersion(semver);
        List<FeatureFlag> flags = flagRepository.findByDeletedAtIsNullOrderByNameAsc();
        Map<String, FeatureFlagState> stateIndex = new LinkedHashMap<>();
        for (FeatureFlagRoleState row : roleStateRepository.findByVersionFetchFlag(version)) {
            if (row.getFlag() != null) {
                stateIndex.put(row.getFlag().getId() + "|" + row.getRole().name(), row.getState());
            }
        }
        List<Map<String, Object>> flagDtos = new ArrayList<>();
        for (FeatureFlag flag : flags) {
            Map<String, String> states = new LinkedHashMap<>();
            for (UserRoles role : GRID_ROLES) {
                FeatureFlagState st = stateIndex.get(flag.getId() + "|" + role.name());
                states.put(role.name(), (st != null ? st : flag.getDefaultState()).name());
            }
            List<Map<String, Object>> eps = new ArrayList<>();
            for (FeatureFlagEndpoint ep : endpointRepository.findByFlag_Id(flag.getId())) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("id", ep.getId());
                e.put("method", ep.getHttpMethod());
                e.put("path", ep.getPathPattern());
                eps.add(e);
            }
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", flag.getId());
            dto.put("name", flag.getName());
            dto.put("description", flag.getDescription());
            dto.put("defaultState", flag.getDefaultState().name());
            dto.put("states", states);
            dto.put("endpoints", eps);
            flagDtos.add(dto);
        }
        List<Map<String, Object>> catalog = new ArrayList<>();
        for (FeatureFlagEndpoint ep : endpointRepository.findAllByOrderByPathPatternAscHttpMethodAsc()) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("id", ep.getId());
            e.put("method", ep.getHttpMethod());
            e.put("path", ep.getPathPattern());
            e.put("neverGuard", ep.isNeverGuard());
            boolean claimed = ep.getFlag() != null;
            e.put("claimed", claimed);
            e.put("inFlag", claimed);
            catalog.add(e);
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
        body.put("flags", flagDtos);
        body.put("catalog", catalog);
        body.put("availableEndpoints", catalog.stream().filter(e -> Boolean.FALSE.equals(e.get("claimed")) && Boolean.FALSE.equals(e.get("neverGuard"))).toList());
        body.put("audit", toAuditDtos(auditRepository.findTop50ByAppVersionOrderByCreatedAtDesc(version.getSemver())));
        return body;
    }

    @Override
    @Transactional
    public FeatureFlag createFlag(FeatureFlagWriteRequest request, String actor) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("name required");
        }
        String name = request.getName().trim().toUpperCase().replace(' ', '_');
        if (flagRepository.existsByName(name)) {
            throw new IllegalStateException("FLAG_EXISTS");
        }
        FeatureFlagState defaultState = request.getDefaultState() != null
                ? request.getDefaultState() : FeatureFlagState.HIDDEN;
        FeatureFlag flag = flagRepository.save(FeatureFlag.builder()
                .name(name)
                .description(request.getDescription())
                .defaultState(defaultState)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        seedRoleStates(flag, defaultState);
        audit(actor, "CREATE", "FLAG", name, null, defaultState, currentSemverCache.get());
        reloadCache();
        return flag;
    }

    @Override
    @Transactional
    public FeatureFlag updateFlag(Long id, FeatureFlagWriteRequest request, String actor) {
        FeatureFlag flag = liveFlag(id);
        FeatureFlagState oldDefault = flag.getDefaultState();
        if (request.getDescription() != null) {
            flag.setDescription(request.getDescription());
        }
        if (request.getDefaultState() != null) {
            flag.setDefaultState(request.getDefaultState());
        }
        flag.setUpdatedAt(LocalDateTime.now());
        flagRepository.save(flag);
        audit(actor, "UPDATE", "FLAG", flag.getName(), oldDefault, flag.getDefaultState(), currentSemverCache.get());
        reloadCache();
        return flag;
    }

    @Override
    @Transactional
    public void deleteFlag(Long id, String actor) {
        FeatureFlag flag = liveFlag(id);
        if (!endpointRepository.findByFlag_Id(id).isEmpty()) {
            throw new IllegalStateException("HAS_ENDPOINTS");
        }
        flag.setDeletedAt(LocalDateTime.now());
        flagRepository.save(flag);
        audit(actor, "DELETE", "FLAG", flag.getName(), flag.getDefaultState(), null, currentSemverCache.get());
        reloadCache();
    }

    @Override
    @Transactional
    public void attachEndpoints(Long flagId, FeatureFlagAttachRequest request, String actor) {
        FeatureFlag flag = liveFlag(flagId);
        if (request.getEndpointIds() == null || request.getEndpointIds().isEmpty()) {
            throw new IllegalArgumentException("endpointIds required");
        }
        List<FeatureFlagEndpoint> batch = new ArrayList<>();
        for (Long endpointId : request.getEndpointIds()) {
            FeatureFlagEndpoint ep = endpointRepository.findById(endpointId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint id " + endpointId));
            if (ep.isNeverGuard()) {
                throw new IllegalArgumentException("Endpoint is never-guardable: " + ep.getId());
            }
            if (ep.getFlag() != null && !ep.getFlag().getId().equals(flagId)) {
                throw new IllegalStateException("ENDPOINT_CLAIMED");
            }
            batch.add(ep);
        }
        for (FeatureFlagEndpoint ep : batch) {
            ep.setFlag(flag);
            endpointRepository.save(ep);
            audit(actor, "ATTACH", ep.getHttpMethod(), ep.getPathPattern(), null, null, currentSemverCache.get());
        }
        reloadCache();
    }

    @Override
    @Transactional
    public void detachEndpoint(Long flagId, Long endpointId, String actor) {
        FeatureFlag flag = liveFlag(flagId);
        FeatureFlagEndpoint ep = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint id"));
        if (ep.getFlag() == null || !ep.getFlag().getId().equals(flag.getId())) {
            throw new IllegalStateException("NOT_ATTACHED");
        }
        ep.setFlag(null);
        endpointRepository.save(ep);
        audit(actor, "DETACH", ep.getHttpMethod(), ep.getPathPattern(), null, null, currentSemverCache.get());
        reloadCache();
    }

    @Override
    @Transactional
    public FeatureFlagAudit updateState(FeatureFlagStateUpdateRequest request, String actor) {
        if (request.getFlagId() == null || request.getRole() == null || request.getState() == null) {
            throw new IllegalArgumentException("flagId, role, state required");
        }
        AppVersion version = resolveVersion(request.getVersion());
        FeatureFlag flag = liveFlag(request.getFlagId());
        FeatureFlagRoleState row = roleStateRepository
                .findByFlagAndVersionAndRole(flag, version, request.getRole())
                .orElseGet(() -> FeatureFlagRoleState.builder()
                        .flag(flag)
                        .version(version)
                        .role(request.getRole())
                        .state(flag.getDefaultState())
                        .build());
        FeatureFlagState oldState = row.getState();
        row.setState(request.getState());
        roleStateRepository.save(row);
        FeatureFlagAudit saved = audit(actor, "STATE", flag.getName(), request.getRole().name(),
                oldState, request.getState(), version.getSemver());
        reloadCache();
        return saved;
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
        boolean makeCurrent = request.isMakeCurrent();
        if (makeCurrent) {
            appVersionRepository.findByCurrentTrue().ifPresent(current -> {
                current.setCurrent(false);
                appVersionRepository.save(current);
            });
        }
        AppVersion created = appVersionRepository.save(AppVersion.builder()
                .semver(semver)
                .current(makeCurrent)
                .createdAt(LocalDateTime.now())
                .build());
        for (FeatureFlagRoleState src : roleStateRepository.findByVersionFetchFlag(source)) {
            roleStateRepository.save(FeatureFlagRoleState.builder()
                    .flag(src.getFlag())
                    .version(created)
                    .role(src.getRole())
                    .state(src.getState())
                    .build());
        }
        reloadCache();
        return created;
    }

    @Override
    @Transactional
    public AppVersion setCurrentVersion(String semver) {
        if (semver == null || semver.isBlank()) {
            throw new IllegalArgumentException("semver required");
        }
        AppVersion target = appVersionRepository.findBySemver(semver.trim())
                .orElseThrow(() -> new IllegalArgumentException("Unknown version " + semver));
        if (!target.isCurrent()) {
            appVersionRepository.findByCurrentTrue().ifPresent(current -> {
                current.setCurrent(false);
                appVersionRepository.save(current);
            });
            target.setCurrent(true);
            appVersionRepository.save(target);
            reloadCache();
        }
        return target;
    }

    @Override
    public FeatureFlagState resolve(String httpMethod, String requestPath, String role, String appVersionHeader) {
        FeatureFlagEndpoint matched = matchEndpoint(httpMethod, requestPath);
        if (matched == null || matched.isNeverGuard() || matched.getFlag() == null
                || matched.getFlag().getDeletedAt() != null) {
            return FeatureFlagState.ENABLED;
        }
        UserRoles parsedRole = parseRole(role);
        if (parsedRole == null) {
            return FeatureFlagState.HIDDEN;
        }
        String version = resolveVersionSemver(appVersionHeader);
        FeatureFlagState cached = stateCache.get()
                .get(cacheKey(version, matched.getFlag().getId(), parsedRole));
        if (cached != null) {
            return cached;
        }
        FeatureFlagState def = flagDefaultCache.get().get(matched.getFlag().getId());
        return def != null ? def : FeatureFlagState.HIDDEN;
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
    @Transactional(readOnly = true)
    public void reloadCache() {
        List<FeatureFlagEndpoint> endpoints = endpointRepository.findAllWithFlag();
        for (FeatureFlagEndpoint ep : endpoints) {
            if (ep.getFlag() != null) {
                ep.getFlag().getName();
                ep.getFlag().getDeletedAt();
                ep.getFlag().getDefaultState();
            }
        }
        endpointCache.set(List.copyOf(endpoints));
        Map<String, FeatureFlagState> states = new LinkedHashMap<>();
        Map<Long, FeatureFlagState> defaults = new LinkedHashMap<>();
        for (FeatureFlag flag : flagRepository.findByDeletedAtIsNullOrderByNameAsc()) {
            defaults.put(flag.getId(), flag.getDefaultState());
        }
        for (AppVersion version : appVersionRepository.findAll()) {
            for (FeatureFlagRoleState row : roleStateRepository.findByVersionFetchFlag(version)) {
                if (row.getFlag() != null) {
                    states.put(cacheKey(version.getSemver(), row.getFlag().getId(), row.getRole()), row.getState());
                }
            }
        }
        stateCache.set(Map.copyOf(states));
        flagDefaultCache.set(Map.copyOf(defaults));
        appVersionRepository.findByCurrentTrue()
                .ifPresent(v -> currentSemverCache.set(v.getSemver()));
        log.info("Feature-flag cache reloaded: {} endpoints, {} flag-states", endpoints.size(), states.size());
    }

    private void seedRoleStates(FeatureFlag flag, FeatureFlagState defaultState) {
        for (AppVersion version : appVersionRepository.findAll()) {
            if (roleStateRepository.existsByFlagAndVersion(flag, version)) {
                continue;
            }
            for (UserRoles role : GRID_ROLES) {
                roleStateRepository.save(FeatureFlagRoleState.builder()
                        .flag(flag)
                        .version(version)
                        .role(role)
                        .state(defaultState)
                        .build());
            }
        }
    }

    private FeatureFlag liveFlag(Long id) {
        return flagRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found"));
    }

    private FeatureFlagAudit audit(String actor, String action, String methodOrName, String pathOrRole,
                                   FeatureFlagState oldState, FeatureFlagState newState, String version) {
        FeatureFlagAudit row = FeatureFlagAudit.builder()
                .createdAt(LocalDateTime.now())
                .actor(actor != null ? actor : "unknown")
                .httpMethod(action.length() > 8 ? action.substring(0, 8) : action)
                .pathPattern((methodOrName != null ? methodOrName : "") + (pathOrRole != null ? " " + pathOrRole : ""))
                .role(UserRoles.ADMIN)
                .oldState(oldState)
                .newState(newState != null ? newState : FeatureFlagState.HIDDEN)
                .appVersion(version != null ? version : currentSemverCache.get())
                .build();
        return auditRepository.save(row);
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

    private String cacheKey(String version, Long flagId, UserRoles role) {
        return version + "|" + flagId + "|" + role.name();
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
