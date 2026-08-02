package com.carland.carland_service.security;

import com.carland.carland_service.dto.response.WebhookAuthErrorResponse;
import com.carland.carland_service.exceptions.WebhookAuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * tr: Webhook kimlik doğrulaması başarısız olduğunda 401 JSON cevabını üreten bileşendir;
 *     hata kodunu X-Webhook-Auth-Error header'ına koyar, serileştirme hatasında fallback JSON yazar.
 * en: Component producing the 401 JSON response when webhook authentication fails;
 *     puts the error code into the X-Webhook-Auth-Error header and writes a fallback JSON on
 *     serialization failure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookAuthResponseWriter {

    public static final String AUTH_ERROR_HEADER = "X-Webhook-Auth-Error";

    private final ObjectMapper objectMapper;

    /**
     * tr: Doğrulama sonucundaki başarısızlık bilgisinden hata gövdesini kurup 401 olarak yazar.
     * en: Builds the error body from the validation result's failure info and writes it as 401.
     */
    public void writeUnauthorized(HttpServletResponse response, WebhookAuthValidationResult result) throws IOException {
        WebhookAuthFailure failure = result.getFailure();
        WebhookAuthException exception = new WebhookAuthException(failure, result.getPartnerId());
        WebhookAuthErrorResponse body = WebhookAuthErrorResponse.builder()
                .error(failure.getError())
                .message(exception.getMessage())
                .partnerId(result.getPartnerId())
                .timeStamp(LocalDateTime.now())
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .build();
        writeJson(response, failure.name(), body);
    }

    /**
     * tr: Sadece failure bilgisiyle (partnerId'siz) 401 cevabı yazar.
     * en: Writes a 401 response from the failure info alone (without partnerId).
     */
    public void writeUnauthorized(HttpServletResponse response, WebhookAuthFailure failure) throws IOException {
        writeUnauthorized(response, WebhookAuthValidationResult.failure(failure));
    }

    /**
     * tr: Hata gövdesini JSON'a çevirip cevaba yazar; response zaten commit edilmişse sadece loglar,
     *     serileştirme başarısızsa elle kurulmuş fallback JSON kullanır.
     * en: Serializes the error body to JSON and writes it to the response; only logs when the response is
     *     already committed, uses a hand-built fallback JSON when serialization fails.
     */
    private void writeJson(HttpServletResponse response, String errorCode, WebhookAuthErrorResponse body) throws IOException {
        if (response.isCommitted()) {
            log.error("Cannot write webhook auth JSON: response already committed, errorCode={}", errorCode);
            return;
        }

        byte[] jsonBytes;
        try {
            jsonBytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception ex) {
            log.error("Failed to serialize webhook auth error as JSON, using fallback", ex);
            jsonBytes = fallbackJson(body).getBytes(StandardCharsets.UTF_8);
        }

        String json = new String(jsonBytes, StandardCharsets.UTF_8);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(AUTH_ERROR_HEADER, errorCode);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    /**
     * tr: Jackson başarısız olursa kullanılacak basit JSON string'ini elle kurar.
     * en: Hand-builds the simple JSON string used when Jackson fails.
     */
    private static String fallbackJson(WebhookAuthErrorResponse body) {
        return "{\"error\":\"" + escape(body.getError())
                + "\",\"message\":\"" + escape(body.getMessage())
                + "\",\"partnerId\":" + (body.getPartnerId() != null ? body.getPartnerId() : "null")
                + ",\"status\":401}";
    }

    /**
     * tr: JSON içine gömülecek değerlerdeki ters bölü ve tırnak karakterlerini kaçışlar.
     * en: Escapes backslash and quote characters in values embedded into the JSON.
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
