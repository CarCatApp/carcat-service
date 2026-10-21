package com.carland.carland_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AppAnnouncementProperties.class)
public class AppAnnouncementConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
