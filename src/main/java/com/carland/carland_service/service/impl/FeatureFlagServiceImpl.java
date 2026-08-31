package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.FeatureFlagAttachRequest;
import com.carland.carland_service.dto.request.FeatureFlagEndpointWriteRequest;
import com.carland.carland_service.dto.request.FeatureFlagStateUpdateRequest;
import com.carland.carland_service.dto.request.FeatureFlagWriteRequest;
import com.carland.carland_service.util.SemVer;
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
import com.carland.carland_service.service.FeatureFlagAdminSupport;
import com.carland.carland_service.service.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
        String clientVersion = blankToNull(appVersionHeader);
        List<FeatureFlag> flags = flagRepository.findByDeletedAtIsNullOrderByNameAsc();
        Map<String, FeatureFlagState> stateByFlag = new LinkedHashMap<>();
        for (FeatureFlagRoleState row : roleStateRepository.findAllFetchFlag()) {
            if (row.getRole() == role && row.getFlag() != null && row.getFlag().getDeletedAt() == null) {
                stateByFlag.put(row.getFlag().getName(), row.getState());
            }
        }
        Map<String, FeatureFlagMeGroup> out = new LinkedHashMap<>();
        for (FeatureFlag flag : flags) {
            if (!SemVer.isAtLeast(clientVersion, minOf(flag))) {
                continue;
            }
            FeatureFlagState state = stateByFlag.get(flag.getName());
            if (state == null) {
                continue;
            }
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
                .appVersion(clientVersion)
                .evaluatedAt(Instant.now().toString())
                .flags(out)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> adminSnapshot() {
        List<FeatureFlag> flags = flagRepository.findByDeletedAtIsNullOrderByNameAsc();
        Map<String, FeatureFlagState> stateIndex = new LinkedHashMap<>();
        for (FeatureFlagRoleState row : roleStateRepository.findAllFetchFlag()) {
            if (row.getFlag() != null) {
                stateIndex.put(row.getFlag().getId() + "|" + row.getRole().name(), row.getState());
            }
        }
        List<Map<String, Object>> flagDtos = new ArrayList<>();
        for (FeatureFlag flag : flags) {
            Map<String, String> states = new LinkedHashMap<>();
            for (UserRoles role : GRID_ROLES) {
                FeatureFlagState st = stateIndex.get(flag.getId() + "|" + role.name());
                states.put(role.name(), (st != null ? st : FeatureFlagState.HIDDEN).name());
            }
            List<Map<String, Object>> eps = new ArrayList<>();
            for (FeatureFlagEndpoint ep : endpointRepository.findByFlag_Id(flag.getId())) {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("id", ep.getId());
                e.put("method", ep.getHttpMethod());
                e.put("path", ep.getPathPattern());
                eps.add(e);
            }
            Map<String, Object> dto = flagSummary(flag);
            dto.put("states", states);
            dto.put("endpoints", eps);
            flagDtos.add(dto);
        }
        List<Map<String, Object>> catalog = new ArrayList<>();
        for (FeatureFlagEndpoint ep : endpointRepository.findAllByOrderByPathPatternAscHttpMethodAsc()) {
            catalog.add(FeatureFlagAdminSupport.endpointDto(ep));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("roles", List.of("USER", "ADMIN", "SUPER_ADMIN", "BOSS"));
        body.put("flags", flagDtos);
        body.put("catalog", catalog);
        body.put("availableEndpoints", catalog.stream().filter(e -> Boolean.FALSE.equals(e.get("claimed")) && Boolean.FALSE.equals(e.get("neverGuard"))).toList());
        body.put("audit", auditRepository.findAllByOrderByCreatedAtDesc(
                        FeatureFlagAdminSupport.pageRequest(0, 10)).getContent().stream()
                .map(FeatureFlagAdminSupport::auditDto).toList());
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listFlags() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (FeatureFlag flag : flagRepository.findByDeletedAtIsNullOrderByNameAsc()) {
            out.add(flagSummary(flag));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> flagDetail(Long id) {
        FeatureFlag flag = liveFlag(id);
        Map<String, FeatureFlagState> stateIndex = new LinkedHashMap<>();
        for (FeatureFlagRoleState row : roleStateRepository.findAllFetchFlag()) {
            if (row.getFlag() != null && row.getFlag().getId().equals(id)) {
                stateIndex.put(row.getRole().name(), row.getState());
            }
        }
        Map<String, String> states = new LinkedHashMap<>();
        for (UserRoles role : GRID_ROLES) {
            FeatureFlagState st = stateIndex.get(role.name());
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
        Map<String, Object> dto = flagSummary(flag);
        dto.put("states", states);
        dto.put("endpoints", eps);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> availableEndpoints(int page, int size) {
        var pageable = FeatureFlagAdminSupport.pageRequest(page, size);
        var result = endpointRepository.findByNeverGuardFalseAndFlagIsNullOrderByPathPatternAscHttpMethodAsc(pageable);
        return FeatureFlagAdminSupport.envelope(result, result.getContent().stream()
                .map(FeatureFlagAdminSupport::endpointDto)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listEndpoints(int page, int size) {
        var pageable = FeatureFlagAdminSupport.pageRequest(page, size);
        var result = endpointRepository.findAllByOrderByPathPatternAscHttpMethodAsc(pageable);
        return FeatureFlagAdminSupport.envelope(result, result.getContent().stream()
                .map(FeatureFlagAdminSupport::endpointDto)
                .toList());
    }

    @Override
    @Transactional
    public Map<String, Object> updateEndpoint(Long id, FeatureFlagEndpointWriteRequest request, String actor) {
        FeatureFlagEndpoint ep = endpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint id"));
        if (ep.getFlag() != null) {
            throw new IllegalStateException("ENDPOINT_CLAIMED");
        }
        String method = request.getMethod() != null ? normalizeMethod(request.getMethod()) : ep.getHttpMethod();
        String path = request.getPath() != null ? normalizePath(request.getPath()) : ep.getPathPattern();
        endpointRepository.findByHttpMethodAndPathPattern(method, path)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalStateException("ENDPOINT_EXISTS");
                });
        ep.setHttpMethod(method);
        ep.setPathPattern(path);
        if (request.getNeverGuard() != null) {
            ep.setNeverGuard(request.getNeverGuard());
        }
        endpointRepository.save(ep);
        audit(actor, "UPDATE", method, path, null, null, currentSemverCache.get(), ep.getFlag(), UserRoles.ADMIN);
        reloadCache();
        return FeatureFlagAdminSupport.endpointDto(ep);
    }

    @Override
    @Transactional
    public void deleteEndpoint(Long id, String actor) {
        FeatureFlagEndpoint ep = endpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown endpoint id"));
        FeatureFlag attached = ep.getFlag();
        if (attached != null) {
            ep.setFlag(null);
            endpointRepository.save(ep);
            audit(actor, "DETACH", ep.getHttpMethod(), ep.getPathPattern(), null, null,
                    currentSemverCache.get(), attached, UserRoles.ADMIN);
        }
        audit(actor, "DELETE", ep.getHttpMethod(), ep.getPathPattern(), null, null,
                currentSemverCache.get(), attached, UserRoles.ADMIN);
        endpointRepository.delete(ep);
        reloadCache();
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method required");
        }
        return method.trim().toUpperCase();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path required");
        }
        String p = path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> auditForFlag(Long id, int page, int size) {
        FeatureFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found"));
        var pageable = FeatureFlagAdminSupport.pageRequest(page, size);
        var result = auditRepository.findForFlag(flag.getId(), flag.getName(), pageable);
        return FeatureFlagAdminSupport.envelope(result, result.getContent().stream()
                .map(FeatureFlagAdminSupport::auditDto)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listAudit(int page, int size, String flagName) {
        var pageable = FeatureFlagAdminSupport.pageRequest(page, size);
        var result = (flagName != null && !flagName.isBlank())
                ? auditRepository.findByFlagNameIgnoreCaseOrderByCreatedAtDesc(flagName.trim(), pageable)
                : auditRepository.findAllByOrderByCreatedAtDesc(pageable);
        return FeatureFlagAdminSupport.envelope(result, result.getContent().stream()
                .map(FeatureFlagAdminSupport::auditDto)
                .toList());
    }

    private Map<String, Object> flagSummary(FeatureFlag flag) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", flag.getId());
        dto.put("name", flag.getName());
        dto.put("description", flag.getDescription());
        dto.put("defaultState", flag.getDefaultState() != null ? flag.getDefaultState().name() : null);
        dto.put("minAvailableVersion", flag.getMinAvailableVersion());
        dto.put("createdAt", flag.getCreatedAt() != null ? flag.getCreatedAt().toString() : null);
        dto.put("updatedAt", flag.getUpdatedAt() != null ? flag.getUpdatedAt().toString() : null);
        return dto;
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
        String minVersion = requireMinVersion(request.getMinAvailableVersion());
        FeatureFlag flag = flagRepository.save(FeatureFlag.builder()
                .name(name)
                .description(request.getDescription())
                .defaultState(defaultState)
                .minAvailableVersion(minVersion)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        seedRoleStates(flag, defaultState);
        audit(actor, "CREATE", "FLAG", name, null, defaultState, minVersion, flag, UserRoles.ADMIN);
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
        if (request.getMinAvailableVersion() != null && !request.getMinAvailableVersion().isBlank()) {
            flag.setMinAvailableVersion(requireMinVersion(request.getMinAvailableVersion()));
        }
        flag.setUpdatedAt(LocalDateTime.now());
        flagRepository.save(flag);
        audit(actor, "UPDATE", "FLAG", flag.getName(), oldDefault, flag.getDefaultState(),
                flag.getMinAvailableVersion(), flag, UserRoles.ADMIN);
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
        audit(actor, "DELETE", "FLAG", flag.getName(), flag.getDefaultState(), null, currentSemverCache.get(), flag, UserRoles.ADMIN);
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
            audit(actor, "ATTACH", ep.getHttpMethod(), ep.getPathPattern(), null, null, currentSemverCache.get(), flag, UserRoles.ADMIN);
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
        audit(actor, "DETACH", ep.getHttpMethod(), ep.getPathPattern(), null, null, currentSemverCache.get(), flag, UserRoles.ADMIN);
        reloadCache();
    }

    @Override
    @Transactional
    public FeatureFlagAudit updateState(FeatureFlagStateUpdateRequest request, String actor) {
        if (request.getFlagId() == null || request.getRole() == null || request.getState() == null) {
            throw new IllegalArgumentException("flagId, role, state required");
        }
        FeatureFlag flag = liveFlag(request.getFlagId());
        FeatureFlagRoleState row = roleStateRepository
                .findFirstByFlagAndRole(flag, request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("FLAG_STATE_NOT_FOUND"));
        FeatureFlagState oldState = row.getState();
        row.setState(request.getState());
        roleStateRepository.save(row);
        FeatureFlagAudit saved = audit(actor, "STATE", flag.getName(), request.getRole().name(),
                oldState, request.getState(), flag.getMinAvailableVersion(), flag, request.getRole());
        reloadCache();
        return saved;
    }

    @Override
    public FeatureFlagState resolve(String httpMethod, String requestPath, String role, String appVersionHeader) {
        FeatureFlagEndpoint matched = matchEndpoint(httpMethod, requestPath);
        if (matched == null || matched.isNeverGuard() || matched.getFlag() == null
                || matched.getFlag().getDeletedAt() != null) {
            return FeatureFlagState.ENABLED;
        }
        FeatureFlag flag = matched.getFlag();
        if (!SemVer.isAtLeast(blankToNull(appVersionHeader), minOf(flag))) {
            return FeatureFlagState.ENABLED;
        }
        UserRoles parsedRole = parseRole(role);
        if (parsedRole == null) {
            return FeatureFlagState.HIDDEN;
        }
        FeatureFlagState cached = stateCache.get().get(cacheKey(flag.getId(), parsedRole));
        if (cached != null) {
            return cached;
        }
        return FeatureFlagState.HIDDEN;
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
    public int syncScannedEndpoints(List<FeatureFlagEndpoint> scanned) {
        Map<String, FeatureFlagEndpoint> existing = new HashMap<>();
        for (FeatureFlagEndpoint ep : endpointRepository.findAll()) {
            existing.put(ep.getHttpMethod() + " " + ep.getPathPattern(), ep);
        }
        int inserted = 0;
        for (FeatureFlagEndpoint incoming : scanned) {
            FeatureFlagEndpoint current = existing.get(incoming.getHttpMethod() + " " + incoming.getPathPattern());
            if (current == null) {
                endpointRepository.save(incoming);
                inserted++;
                continue;
            }
            if (current.isNeverGuard() != incoming.isNeverGuard()) {
                current.setNeverGuard(incoming.isNeverGuard());
                endpointRepository.save(current);
            }
        }
        return inserted;
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
                ep.getFlag().getMinAvailableVersion();
            }
        }
        endpointCache.set(List.copyOf(endpoints));
        Map<String, FeatureFlagState> states = new LinkedHashMap<>();
        Map<Long, FeatureFlagState> defaults = new LinkedHashMap<>();
        for (FeatureFlag flag : flagRepository.findByDeletedAtIsNullOrderByNameAsc()) {
            defaults.put(flag.getId(), flag.getDefaultState());
        }
        for (FeatureFlagRoleState row : roleStateRepository.findAllFetchFlag()) {
            if (row.getFlag() != null) {
                states.put(cacheKey(row.getFlag().getId(), row.getRole()), row.getState());
            }
        }
        stateCache.set(Map.copyOf(states));
        flagDefaultCache.set(Map.copyOf(defaults));
        appVersionRepository.findByCurrentTrue()
                .ifPresent(v -> currentSemverCache.set(v.getSemver()));
        log.info("Feature-flag cache reloaded: {} endpoints, {} flag-states", endpoints.size(), states.size());
    }

    /**
     * Seeds one ENABLED/DISABLED/HIDDEN row per role. Internally still ties to current AppVersion
     * row for the leftover FK; resolution does not use that catalog.
     */
    private void seedRoleStates(FeatureFlag flag, FeatureFlagState defaultState) {
        if (roleStateRepository.existsByFlag(flag)) {
            return;
        }
        AppVersion version = ensureCurrentVersion();
        for (UserRoles role : GRID_ROLES) {
            roleStateRepository.save(FeatureFlagRoleState.builder()
                    .flag(flag)
                    .version(version)
                    .role(role)
                    .state(defaultState)
                    .build());
        }
    }

    private FeatureFlag liveFlag(Long id) {
        return flagRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found"));
    }

    private FeatureFlagAudit audit(String actor, String action, String methodOrName, String pathOrRole,
                                   FeatureFlagState oldState, FeatureFlagState newState, String version,
                                   FeatureFlag flag, UserRoles role) {
        FeatureFlagAudit row = FeatureFlagAudit.builder()
                .createdAt(LocalDateTime.now())
                .flagId(flag != null ? flag.getId() : null)
                .flagName(flag != null ? flag.getName() : null)
                .actor(actor != null ? actor : "unknown")
                .httpMethod(action.length() > 8 ? action.substring(0, 8) : action)
                .pathPattern((methodOrName != null ? methodOrName : "") + (pathOrRole != null ? " " + pathOrRole : ""))
                .role(role != null ? role : UserRoles.ADMIN)
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

    private String requireMinVersion(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("minAvailableVersion required");
        }
        SemVer parsed = SemVer.parse(raw.trim());
        return parsed.toString();
    }

    private static String minOf(FeatureFlag flag) {
        String min = flag.getMinAvailableVersion();
        if (min == null || min.isBlank()) {
            return "0.0.0";
        }
        return min;
    }

    private static String blankToNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
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

    private String cacheKey(Long flagId, UserRoles role) {
        return flagId + "|" + role.name();
    }

}
