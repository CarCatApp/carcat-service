package com.carland.carland_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * tr: Uygulama genelinde kullanılan RestTemplate bean'ini tanımlar.
 * en: Defines the RestTemplate bean used across the application.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
