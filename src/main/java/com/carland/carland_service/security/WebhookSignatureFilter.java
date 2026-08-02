package com.carland.carland_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * tr: /webhook/partner/* istekleri için kimlik doğrulama filtresidir; önce X-Internal-Token'ı doğrular,
 *     test endpoint'i hariç tüm isteklerde partner HMAC imzasını (X-Signature) kontrol eder,
 *     başarısızlıkta 401 JSON cevabı yazar.
 * en: Authentication filter for /webhook/partner/* requests; validates the X-Internal-Token first,
 *     then checks the partner HMAC signature (X-Signature) for all requests except the test endpoint,
 *     writing a 401 JSON response on failure.
 */
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final String PARTNER_PREFIX = "/webhook/partner/";
    private static final String TEST_PATH = "/webhook/partner/test";

    private final InternalTokenValidator internalTokenValidator;
    private final PartnerWebhookSignatureService partnerWebhookSignatureService;
    private final WebhookAuthResponseWriter webhookAuthResponseWriter;

    /**
     * tr: Partner webhook path'i değilse zinciri devam ettirir; internal token ve HMAC imza doğrulamalarını
     *     sırayla çalıştırır, herhangi biri başarısızsa 401 yazar ve isteği durdurur.
     * en: Continues the chain for non partner-webhook paths; runs internal token and HMAC signature
     *     validations in order, writes 401 and stops the request when either fails.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith(PARTNER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        WebhookAuthValidationResult internalResult = internalTokenValidator.validate(request);
        if (!internalResult.isValid()) {
            log.warn("Webhook internal auth failed: path={}, reason={}", path, internalResult.getFailure().getError());
            webhookAuthResponseWriter.writeUnauthorized(response, internalResult);
            return;
        }

        if (TEST_PATH.equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        byte[] body = resolveBodyBytes(request, wrapped);

        WebhookAuthValidationResult partnerResult = partnerWebhookSignatureService.validate(wrapped, body);
        if (!partnerResult.isValid()) {
            log.warn(
                    "Webhook partner auth failed: path={}, partnerId={}, reason={}, bodyBytes={}",
                    path,
                    partnerResult.getPartnerId(),
                    partnerResult.getFailure().getError(),
                    body.length
            );
            webhookAuthResponseWriter.writeUnauthorized(response, partnerResult);
            return;
        }

        filterChain.doFilter(wrapped, response);
    }

    /**
     * tr: İmza doğrulamasında kullanılacak ham body byte'larını çözer: önce cache'lenmiş body'ye,
     *     sonra ContentCachingRequestWrapper'a bakar, en son stream'den okur.
     * en: Resolves the raw body bytes for signature validation: checks the cached body first,
     *     then the ContentCachingRequestWrapper, finally reads from the stream.
     */
    private byte[] resolveBodyBytes(HttpServletRequest request, CachedBodyHttpServletRequest wrapped) throws IOException {
        byte[] body = wrapped.getCachedBody();
        if (body.length > 0) {
            return body;
        }
        if (request instanceof ContentCachingRequestWrapper caching) {
            byte[] cached = caching.getContentAsByteArray();
            if (cached.length > 0) {
                return cached;
            }
        }
        return StreamUtils.copyToByteArray(request.getInputStream());
    }
}
