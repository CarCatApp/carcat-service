package com.carland.carland_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * tr: Hizmet kategorisi title/description JSON'unu ObjectMapper ile okur ve yazar.
 * en: Reads and writes service-category title/description JSON with ObjectMapper.
 */
@Component
@RequiredArgsConstructor
public class ServiceCategoryJson {

    private static final TypeReference<Map<String, String>> MAP = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    /**
     * tr: Boş veya bozuk JSON'da boş map döner.
     * en: Returns an empty map for blank or invalid JSON.
     */
    public Map<String, String> read(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(json, MAP);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            return Map.of("az", json);
        }
    }

    /**
     * tr: az/en/ru map'ini JSON string yapar. Üç anahtar da yazılır.
     * en: Writes an az/en/ru map as a JSON string. All three keys are present.
     */
    public String write(String az, String en, String ru) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("az", az == null ? "" : az);
        map.put("en", en == null ? "" : en);
        map.put("ru", ru == null ? "" : ru);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("service category json", ex);
        }
    }

    /**
     * tr: Üç dil de boşsa null. Aksi halde write.
     * en: Null when all three languages are blank. Otherwise write.
     */
    public String writeOrNull(String az, String en, String ru) {
        if (isBlank(az) && isBlank(en) && isBlank(ru)) {
            return null;
        }
        return write(az, en, ru);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
