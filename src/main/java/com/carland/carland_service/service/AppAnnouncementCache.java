package com.carland.carland_service.service;

import com.carland.carland_service.config.AppAnnouncementProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * tr: Aday liste cache'i. Key {@code app-announcements:candidates}, TTL config (varsayılan 60 sn).
 *     Yazanlar: create, update, setActive, copy → {@link #evictAfterCommit}.
 *     Süre penceresi cache'ten sonra seçimde uygulanır. Redis hatası DB'ye düşer. HIT logu yok.
 * en: Candidate-list cache. Key {@code app-announcements:candidates}, TTL from config (default 60s).
 *     Writers: create, update, setActive, copy → {@link #evictAfterCommit}.
 *     The time window is applied after the cache read. Redis errors fall through to the DB. No HIT log.
 */
@Slf4j
@Component
public class AppAnnouncementCache {

    static final String KEY = "app-announcements:candidates";

    private static final TypeReference<List<AppAnnouncementCandidate>> TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AppAnnouncementProperties properties;

    public AppAnnouncementCache(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            AppAnnouncementProperties properties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public List<AppAnnouncementCandidate> getOrLoad(Supplier<List<AppAnnouncementCandidate>> loader) {
        try {
            String json = stringRedisTemplate.opsForValue().get(KEY);
            if (json != null && !json.isBlank()) {
                return objectMapper.readValue(json, TYPE);
            }
        } catch (Exception ex) {
            log.warn("REDIS_SKIP app-announcements | {}", ex.getMessage());
        }
        List<AppAnnouncementCandidate> loaded = loader.get();
        List<AppAnnouncementCandidate> safe = loaded == null ? List.of() : loaded;
        try {
            int ttl = Math.max(properties.getCacheTtlSeconds(), 1);
            stringRedisTemplate.opsForValue().set(KEY, objectMapper.writeValueAsString(safe), Duration.ofSeconds(ttl));
        } catch (Exception ex) {
            log.warn("REDIS_PUT_FAIL app-announcements | {}", ex.getMessage());
        }
        return safe;
    }

    public void evictAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict();
                }
            });
            return;
        }
        evict();
    }

    private void evict() {
        try {
            stringRedisTemplate.delete(KEY);
        } catch (Exception ex) {
            log.warn("REDIS_EVICT_FAIL app-announcements | {}", ex.getMessage());
        }
    }
}
