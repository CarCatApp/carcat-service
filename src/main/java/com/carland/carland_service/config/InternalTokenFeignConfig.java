package com.carland.carland_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class InternalTokenFeignConfig {

    @Bean
    public RequestInterceptor internalTokenInterceptor(
            @Value("${carland.internal-token:}") String internalToken
    ) {
        return template -> {
            if (internalToken != null && !internalToken.isBlank()) {
                template.header("X-Internal-Token", internalToken);
            }
        };
    }
}
