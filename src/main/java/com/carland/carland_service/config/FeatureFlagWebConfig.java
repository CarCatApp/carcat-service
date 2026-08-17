package com.carland.carland_service.config;

import com.carland.carland_service.security.FeatureFlagInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * tr: Feature-flag interceptor'ını /api/** isteklerine kaydeder.
 * en: Registers the feature-flag interceptor for /api/** requests.
 */
@Configuration
@RequiredArgsConstructor
public class FeatureFlagWebConfig implements WebMvcConfigurer {

    private final FeatureFlagInterceptor featureFlagInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(featureFlagInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/feature-flags/**");
    }
}
