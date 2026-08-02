package com.carland.carland_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * tr: Swagger UI login/refresh JS'inin kullanacağı auth URL yapılandırmasını döner.
 * en: Returns auth URL configuration used by the Swagger UI login/refresh JavaScript.
 */
@RestController
public class SwaggerAuthConfigController {

    @Value("${carland.swagger.auth-login-url:https://digital-innovation.agency/auth/server/api/v1/users/login}")
    private String loginUrl;

    @Value("${carland.swagger.auth-refresh-url:https://digital-innovation.agency/auth/server/api/v1/users/refresh}")
    private String refreshUrl;

    @GetMapping("/swagger-auth-config")
    public Map<String, Object> config() {
        return Map.of(
                "loginUrl", loginUrl,
                "refreshUrl", refreshUrl,
                "acceptLanguage", "az",
                "accessTtlSeconds", 900
        );
    }
}
