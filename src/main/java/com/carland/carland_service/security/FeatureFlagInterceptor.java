package com.carland.carland_service.security;

import com.carland.carland_service.enums.FeatureFlagState;
import com.carland.carland_service.service.FeatureFlagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * tr: Guard edilen API: client min version altındaysa flag yok sayılır (geçer).
 *     Aksi halde rol state DISABLED→403, HIDDEN→404.
 * en: For guarded APIs: if the flag's minAvailableVersion is above the client, the flag
 *     does not apply (pass). Otherwise DISABLED→403 and HIDDEN→404 from role state.
 */
@Component
@RequiredArgsConstructor
public class FeatureFlagInterceptor implements HandlerInterceptor {

    private final FeatureFlagService featureFlagService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        if (featureFlagService.isNeverGuard(path)) {
            return true;
        }

        String role = request.getHeader("role");
        String appVersion = request.getHeader("X-App-Version");
        FeatureFlagState state = featureFlagService.resolve(request.getMethod(), path, role, appVersion);

        if (state == FeatureFlagState.DISABLED) {
            write(response, HttpServletResponse.SC_FORBIDDEN, "FEATURE_DISABLED");
            return false;
        }
        if (state == FeatureFlagState.HIDDEN) {
            write(response, HttpServletResponse.SC_NOT_FOUND, "FEATURE_HIDDEN");
            return false;
        }
        return true;
    }

    private void write(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + code + "\"}");
    }
}
