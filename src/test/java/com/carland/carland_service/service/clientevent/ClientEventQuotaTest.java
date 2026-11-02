package com.carland.carland_service.service.clientevent;

import com.carland.carland_service.config.ClientEventProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientEventQuotaTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;

    private ClientEventQuota quota;

    @BeforeEach
    void setUp() {
        quota = new ClientEventQuota(redis, new ClientEventProperties());
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void ipAllowsUnderTheCap() {
        when(values.increment(anyString())).thenReturn(60L);
        when(redis.getExpire(anyString())).thenReturn(10L);
        assertTrue(quota.allowIp("10.0.0.1"));
    }

    @Test
    void ipBlocksOverTheCap() {
        when(values.increment(anyString())).thenReturn(61L);
        when(redis.getExpire(anyString())).thenReturn(10L);
        assertFalse(quota.allowIp("10.0.0.1"));
    }

    @Test
    void redisDownLeavesIngestOpen() {
        when(values.increment(anyString())).thenThrow(new IllegalStateException("down"));
        assertTrue(quota.allowIp("10.0.0.1"));
        assertFalse(quota.storm("abc"));
    }

    @Test
    void installCountsEvents() {
        when(values.increment(anyString(), anyLong())).thenReturn(300L);
        when(redis.getExpire(anyString())).thenReturn(-1L);
        when(redis.expire(anyString(), any(Duration.class))).thenReturn(true);
        assertTrue(quota.allowInstall("install", 300));
    }

    @Test
    void stormStartsAboveTheThreshold() {
        when(values.increment(anyString())).thenReturn(21L);
        when(redis.getExpire(anyString())).thenReturn(5L);
        assertTrue(quota.storm("fingerprint"));
    }
}
