package com.carland.carland_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * tr: Tüm isteklerde çalışan güvenlik filtresidir; webhook ve public path'leri (Swagger, admin UI, legal vb.)
 *     JWT kontrolünden muaf tutar, diğer isteklerde Authorization: Bearer header'ı yoksa 401 döner.
 *     Gerçek JWT doğrulaması Kong gateway'de yapılır, burada sadece header varlığı kontrol edilir.
 * en: Security filter running on every request; exempts webhook and public paths (Swagger, admin UI, legal etc.)
 *     from the JWT check and returns 401 when the Authorization: Bearer header is missing on other requests.
 *     Actual JWT validation happens at the Kong gateway; only header presence is checked here.
 */
@Component
@RequiredArgsConstructor
public class CustomFilter extends OncePerRequestFilter {

    /**
     * tr: İstek path'ine bakar: webhook ve public path'leri doğrudan geçirir, korumalı path'lerde Bearer token
     *     yoksa 401 JSON hatası yazar, varsa SecurityContext'e basit bir authentication koyup zinciri devam ettirir.
     * en: Inspects the request path: lets webhook and public paths through, writes a 401 JSON error when a
     *     protected path has no Bearer token, otherwise puts a simple authentication into the SecurityContext
     *     and continues the chain.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        System.out.println("========== REQUEST ==========");
        System.out.println("URI: " + path);
        System.out.println("Context: " + request.getContextPath());
        System.out.println("Servlet: " + request.getServletPath());
        if (path.startsWith("/webhook/partner/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/webhook/")) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isPublic =
                path.contains("/swagger-ui")
                        || path.contains("/v3/api-docs")
                        || path.contains("/swagger-resources")
                        || path.contains("/webjars/")
                        || path.equals("/swagger-custom.js")
                        || path.equals("/swagger-auth-config")
                        || path.startsWith("/legal/")
                        ||        path.equals("/admin")
                        || path.startsWith("/admin/")
                        || path.contains("/test/hyper")
                        || path.startsWith("/auth/")
                        || path.contains("/test/get")
                        || path.startsWith("/api/v1/user/customer-cars")
                        || path.equals("/api/v1/group/by/get/brand/list/with/models");

        if (isPublic) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"JWT token required\"}");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user", null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
