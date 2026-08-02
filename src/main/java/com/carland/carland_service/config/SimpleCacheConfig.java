package com.carland.carland_service.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * tr: Basit in-memory cache konfigürasyonu; "hyper" adlı cache için ConcurrentMap tabanlı bir CacheManager tanımlar.
 * en: Simple in-memory cache configuration; defines a ConcurrentMap-based CacheManager for the cache named "hyper".
 */
@Configuration
public class SimpleCacheConfig {

    /**
     * tr: "hyper" cache'ini yöneten ConcurrentMapCacheManager bean'ini üretir.
     * en: Produces the ConcurrentMapCacheManager bean managing the "hyper" cache.
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("hyper");
    }
}
