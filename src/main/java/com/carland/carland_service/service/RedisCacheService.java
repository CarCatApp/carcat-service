package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.CarResponse;
import com.carland.carland_service.dto.response.CarVinServiceHistoryResponse;
import com.carland.carland_service.dto.response.VisitHistoryResponse;
import com.carland.carland_service.entity.BodyType;
import com.carland.carland_service.entity.Brand;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.Model;
import com.carland.carland_service.entity.ModelYear;
import com.carland.carland_service.entity.TransmissionType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Redis read-through cache. Miss or Redis down → DB. HIT is logged with log.info.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private static final Duration PHOTO_TTL = Duration.ofHours(1);
    private static final Duration CATALOG_TTL = Duration.ofDays(30);

    private final StringRedisTemplate stringRedisTemplate;
    @Qualifier("redisBytesTemplate")
    private final RedisTemplate<String, byte[]> redisBytesTemplate;
    private final ObjectMapper objectMapper;

    public List<CarResponse> getOrLoadCarList(String userId, String lang, Supplier<List<CarResponse>> loader) {
        String key = "carlist:" + userId + ":" + langKey(lang);
        List<CarResponse> hit = getJson(key, new TypeReference<>() {}, "carlist userId=" + userId);
        if (hit != null) {
            return hit;
        }
        List<CarResponse> loaded = loader.get();
        putJson(key, loaded, null);
        return loaded;
    }

    public void evictCarList(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        deleteByPattern("carlist:" + userId + ":*");
    }

    public ResponseEntity<byte[]> getCarPhoto(Long carId) {
        return getPhoto("photo:car:" + carId, "car-photo carId=" + carId);
    }

    public void putCarPhoto(Long carId, MediaType mediaType, byte[] bytes) {
        putPhoto("photo:car:" + carId, mediaType, bytes);
    }

    public void evictCarPhoto(Long carId) {
        deletePhoto("photo:car:" + carId);
    }

    public ResponseEntity<byte[]> getUserPhoto(String userId) {
        return getPhoto("photo:user:" + userId, "user-photo userId=" + userId);
    }

    public void putUserPhoto(String userId, MediaType mediaType, byte[] bytes) {
        putPhoto("photo:user:" + userId, mediaType, bytes);
    }

    public void evictUserPhoto(String userId) {
        deletePhoto("photo:user:" + userId);
    }

    public VisitHistoryResponse getOrLoadHistoryV2(String vin, String lang, Supplier<VisitHistoryResponse> loader) {
        String key = "history:v2:" + vinKey(vin) + ":" + langKey(lang);
        VisitHistoryResponse hit = getJson(key, new TypeReference<>() {}, "history-v2 vin=" + vin);
        if (hit != null) {
            return hit;
        }
        VisitHistoryResponse loaded = loader.get();
        putJson(key, loaded, null);
        return loaded;
    }

    public CarVinServiceHistoryResponse getOrLoadHistoryV1(String vin, String lang,
                                                           Supplier<CarVinServiceHistoryResponse> loader) {
        String key = "history:v1:" + vinKey(vin) + ":" + langKey(lang);
        CarVinServiceHistoryResponse hit = getJson(key, new TypeReference<>() {}, "history-v1 vin=" + vin);
        if (hit != null) {
            return hit;
        }
        CarVinServiceHistoryResponse loaded = loader.get();
        putJson(key, loaded, null);
        return loaded;
    }

    public void evictHistory(String vin) {
        String norm = vinKey(vin);
        deleteByPattern("history:v1:" + norm + ":*");
        deleteByPattern("history:v2:" + norm + ":*");
    }

    public void evictCarAndHistory(String userId, String vin) {
        evictCarList(userId);
        evictHistory(vin);
    }

    public void evictCarListAfterCommit(String userId) {
        runAfterCommit(() -> evictCarList(userId));
    }

    public void evictCarAndHistoryAfterCommit(Car car) {
        String userId = ownerUserId(car);
        String vin = vinOf(car);
        runAfterCommit(() -> evictCarAndHistory(userId, vin));
    }

    public String ownerUserId(Car car) {
        try {
            if (car == null || car.getCustomer() == null || car.getCustomer().getUserId() == null) {
                return null;
            }
            return String.valueOf(car.getCustomer().getUserId());
        } catch (Exception ex) {
            log.warn("REDIS_EVICT_SKIP owner | {}", ex.getMessage());
            return null;
        }
    }

    private static String vinOf(Car car) {
        return car == null ? null : car.getVin();
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    public List<Brand> getOrLoadBrands(Supplier<List<Brand>> loader) {
        return getOrLoadCatalog("catalog:brands", new TypeReference<>() {}, loader, "catalog brands");
    }

    public List<Brand> getOrLoadBrandsWithModels(Supplier<List<Brand>> loader) {
        return getOrLoadCatalog("catalog:brands-with-models", new TypeReference<>() {}, loader, "catalog brands+models");
    }

    public List<Model> getOrLoadModels(Long brandId, Supplier<List<Model>> loader) {
        return getOrLoadCatalog("catalog:models:" + brandId, new TypeReference<>() {}, loader, "catalog models brandId=" + brandId);
    }

    public List<BodyType> getOrLoadBodyTypes(String lang, Supplier<List<BodyType>> loader) {
        return getOrLoadCatalog("catalog:body:" + langKey(lang), new TypeReference<>() {}, loader, "catalog body");
    }

    public List<TransmissionType> getOrLoadTransmissions(Supplier<List<TransmissionType>> loader) {
        return getOrLoadCatalog("catalog:transmission", new TypeReference<>() {}, loader, "catalog transmission");
    }

    public List<EngineType> getOrLoadEngineTypes(String lang, Supplier<List<EngineType>> loader) {
        return getOrLoadCatalog("catalog:engine:" + langKey(lang), new TypeReference<>() {}, loader, "catalog engine");
    }

    public List<ModelYear> getOrLoadYears(Supplier<List<ModelYear>> loader) {
        return getOrLoadCatalog("catalog:years", new TypeReference<>() {}, loader, "catalog years");
    }

    public List<Color> getOrLoadColors(String lang, Supplier<List<Color>> loader) {
        return getOrLoadCatalog("catalog:colors:" + langKey(lang), new TypeReference<>() {}, loader, "catalog colors");
    }

    public void evictCatalogColors() {
        deleteByPattern("catalog:colors:*");
    }

    private <T> List<T> getOrLoadCatalog(String key, TypeReference<List<T>> type, Supplier<List<T>> loader, String hitLabel) {
        List<T> hit = getJson(key, type, hitLabel);
        if (hit != null) {
            return hit;
        }
        List<T> loaded = loader.get();
        putJson(key, loaded, CATALOG_TTL);
        return loaded;
    }

    private ResponseEntity<byte[]> getPhoto(String key, String hitLabel) {
        try {
            byte[] bytes = redisBytesTemplate.opsForValue().get(key + ":bytes");
            String type = stringRedisTemplate.opsForValue().get(key + ":type");
            if (bytes == null || type == null) {
                return null;
            }
            log.info("REDIS_HIT {}", hitLabel);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(bytes);
        } catch (Exception ex) {
            log.warn("REDIS_SKIP {} | {}", hitLabel, ex.getMessage());
            return null;
        }
    }

    private void putPhoto(String key, MediaType mediaType, byte[] bytes) {
        if (bytes == null || mediaType == null) {
            return;
        }
        try {
            redisBytesTemplate.opsForValue().set(key + ":bytes", bytes, PHOTO_TTL);
            stringRedisTemplate.opsForValue().set(key + ":type", mediaType.toString(), PHOTO_TTL);
        } catch (Exception ex) {
            log.warn("REDIS_PUT_FAIL photo | {}", ex.getMessage());
        }
    }

    private void deletePhoto(String key) {
        try {
            stringRedisTemplate.delete(List.of(key + ":bytes", key + ":type"));
            redisBytesTemplate.delete(key + ":bytes");
        } catch (Exception ex) {
            log.warn("REDIS_EVICT_FAIL photo | {}", ex.getMessage());
        }
    }

    private <T> T getJson(String key, TypeReference<T> type, String hitLabel) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            T value = objectMapper.readValue(json, type);
            log.info("REDIS_HIT {}", hitLabel);
            return value;
        } catch (Exception ex) {
            log.warn("REDIS_SKIP {} | {}", hitLabel, ex.getMessage());
            return null;
        }
    }

    private void putJson(String key, Object value, Duration ttl) {
        if (value == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            if (ttl == null) {
                stringRedisTemplate.opsForValue().set(key, json);
            } else {
                stringRedisTemplate.opsForValue().set(key, json, ttl);
            }
        } catch (Exception ex) {
            log.warn("REDIS_PUT_FAIL {} | {}", key, ex.getMessage());
        }
    }

    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("REDIS_EVICT_FAIL {} | {}", pattern, ex.getMessage());
        }
    }

    private static String langKey(String lang) {
        if (lang == null || lang.isBlank()) {
            return "az";
        }
        return lang.toLowerCase();
    }

    private static String vinKey(String vin) {
        return vin == null ? "" : vin.trim().toUpperCase();
    }
}
