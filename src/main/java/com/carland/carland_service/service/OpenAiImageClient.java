package com.carland.carland_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * tr: OpenAI Images API (images/generations) — sunucu-sunucu; key loglanmaz.
 * en: OpenAI Images API (images/generations) — server-to-server; key is never logged.
 */
@Slf4j
@Service
public class OpenAiImageClient {

    private static final String GENERATIONS_URL = "https://api.openai.com/v1/images/generations";

    private final RestTemplate openaiRestTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String size;
    private final String quality;

    public OpenAiImageClient(@Qualifier("openaiRestTemplate") RestTemplate openaiRestTemplate,
                             ObjectMapper objectMapper,
                             @Value("${openai.api-key}") String apiKey,
                             @Value("${openai.image-model}") String model,
                             @Value("${openai.image-size}") String size,
                             @Value("${openai.image-quality}") String quality) {
        this.openaiRestTemplate = openaiRestTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.size = size;
        this.quality = quality;
    }

    /**
     * tr: Prompt'tan webp byte üretir. API key loglanmaz; PO için b64_json decode öncesi yazılır.
     * en: Produces webp bytes from the prompt. API key is never logged; b64_json is logged before decode for PO.
     */
    public byte[] generateWebp(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey == null ? "" : apiKey.trim());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("size", size);
        body.put("quality", quality);
        body.put("output_format", "webp");
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception ex) {
            jsonBody = String.valueOf(body);
        }
        log.info("OpenAI image full request method=POST url={} contentType=application/json authorization=Bearer *** body={}",
                GENERATIONS_URL, jsonBody);

        ResponseEntity<String> response = openaiRestTemplate.postForEntity(
                GENERATIONS_URL, new HttpEntity<>(body, headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("OpenAI images HTTP " + response.getStatusCode().value());
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode usage = root.get("usage");
            if (usage != null && !usage.isNull()) {
                log.info("OpenAI image usage={}", usage);
            }
            String b64 = root.path("data").path(0).path("b64_json").asText(null);
            if (b64 == null || b64.isBlank()) {
                throw new IllegalStateException("OpenAI images empty b64_json");
            }
            b64 = b64.replaceAll("\\s", "");
            int comma = b64.indexOf(',');
            if (comma >= 0) {
                b64 = b64.substring(comma + 1);
            }
            log.info("OpenAI image b64_json length={} value={}", b64.length(), b64);
            return Base64.getDecoder().decode(b64);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("OpenAI images parse failed", ex);
        }
    }
}
