package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.config.ClientEventProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * tr: Ingest sayğacları. Açarlar client-events:rl:* və client-events:storm:*.
 *     TTL 60 saniyə. İstifadəçi keşi deyil, evict yoxdur — müddət bitəndə düşür.
 *     Redis yoxdursa ingest açıq qalır (hadisə itməsin), fırtına bayrağı yazılmır.
 * en: Ingest counters. Keys are client-events:rl:* and client-events:storm:*.
 *     TTL is 60 seconds. Not user cache, so there is no evict — the TTL drops them.
 *     If Redis is down, ingest stays open and the storm flag is skipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientEventQuota {

    static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redis;
    private final ClientEventProperties properties;

    public boolean allowIp(String ip) {
        return allow("client-events:rl:ip:" + part(ip), 1, properties.getIpPerMinute());
    }

    public boolean allowInstall(String installId, int events) {
        if (events <= 0) {
            return true;
        }
        return allow("client-events:rl:install:" + part(installId), events, properties.getInstallEventsPerMinute());
    }

    /** tr: Bu dəqiqədə eyni barmaq izi həddi keçibsə true. Hadisə yenə də yazılır. */
    public boolean storm(String fingerprint) {
        if (properties.getStormPerMinute() <= 0) {
            return false;
        }
        Long count = increment("client-events:storm:" + part(fingerprint), 1);
        return count != null && count > properties.getStormPerMinute();
    }

    private boolean allow(String key, int amount, int max) {
        if (max <= 0) {
            return true;
        }
        Long count = increment(key, amount);
        if (count == null) {
            return true;
        }
        return count <= max;
    }

    private Long increment(String key, int amount) {
        try {
            Long count = amount == 1
                    ? redis.opsForValue().increment(key)
                    : redis.opsForValue().increment(key, amount);
            Long ttl = redis.getExpire(key);
            if (ttl == null || ttl < 0) {
                redis.expire(key, WINDOW);
            }
            return count;
        } catch (RuntimeException ex) {
            log.warn("client-events redis skip: {}", ex.getClass().getSimpleName());
            return null;
        }
    }

    private static String part(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        String value = raw.trim().replaceAll("[\\s]", "");
        if (value.length() > 80) {
            return value.substring(0, 80);
        }
        return value;
    }
}
