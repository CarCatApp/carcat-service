package com.carland.carland_service.digitalfinance;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * tr: Digital Finance prod çağrısı. Gövde ve status olduğu gibi servlet cevabına yazılır. ApiKey loglanmaz.
 * en: Digital Finance prod call. Body and status are written to the servlet response unchanged. ApiKey is never logged.
 */
@Slf4j
@Component
public class DigitalFinanceClient {

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    @Autowired
    public DigitalFinanceClient(
            @Value("${digital-finance.base-url}") String baseUrl,
            @Value("${digital-finance.api-key:}") String apiKey) {
        this(baseUrl, apiKey, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(15))
                .build());
    }

    DigitalFinanceClient(String baseUrl, String apiKey, HttpClient httpClient) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.httpClient = httpClient;
    }

    /**
     * tr: Upstream cevabı. Gövde olduğu gibi kalır. Timeout {@link java.io.IOException} olarak çıkar.
     * en: Upstream response. The body stays as received. A timeout surfaces as {@link java.io.IOException}.
     */
    public record Upstream(int statusCode, String contentType, byte[] body) {
    }

    /**
     * tr: Upstream cevabını bayt bayt yazar. JSON yeniden üretilmez.
     * en: Writes the upstream response byte for byte. JSON is not rebuilt.
     */
    public void forward(String method, String path, byte[] body, String requestIdentifier, HttpServletResponse response)
            throws IOException {
        forward(method, path, body, requestIdentifier, null, response);
    }

    /**
     * tr: Upstream cevabını bayt bayt yazar. RequestPurposeId doluysa header olarak gider.
     * en: Writes the upstream response byte for byte. RequestPurposeId is sent when present.
     */
    public void forward(String method, String path, byte[] body, String requestIdentifier,
                        String requestPurposeId, HttpServletResponse response) throws IOException {
        Upstream upstream = exchange(method, path, body, requestIdentifier, requestPurposeId);
        response.setStatus(upstream.statusCode());
        response.setContentType(upstream.contentType());
        response.getOutputStream().write(upstream.body());
        response.flushBuffer();
    }

    /**
     * tr: Digital Finance çağrısı. ApiKey loglanmaz. Kesilme IllegalStateException, timeout IOException.
     * en: Digital Finance call. ApiKey is never logged. Interrupt is IllegalStateException, timeout is IOException.
     */
    public Upstream exchange(String method, String path, byte[] body, String requestIdentifier,
                             String requestPurposeId) throws IOException {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("DIGITAL_FINANCE_API_KEY is empty");
        }
        String requestId = (requestIdentifier == null || requestIdentifier.isBlank())
                ? UUID.randomUUID().toString()
                : requestIdentifier.trim();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("ApiKey", apiKey)
                .header("RequestIdentifier", requestId);
        if (requestPurposeId != null && !requestPurposeId.isBlank()) {
            builder.header("RequestPurposeId", requestPurposeId.trim());
        }
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json");
            builder.POST(HttpRequest.BodyPublishers.ofByteArray(body == null ? new byte[0] : body));
        } else {
            builder.GET();
        }
        HttpResponse<byte[]> upstream;
        try {
            upstream = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Digital Finance call interrupted", ex);
        }
        log.info("Digital Finance {} {} status={}", method, path, upstream.statusCode());
        byte[] bytes = upstream.body() == null ? new byte[0] : upstream.body();
        String contentType = upstream.headers().firstValue("Content-Type").orElse("application/json;charset=UTF-8");
        return new Upstream(upstream.statusCode(), contentType, bytes);
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("digital-finance.base-url is empty");
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
