package com.carland.carland_service.filter;

import com.carland.carland_service.service.webhook.PartnerWebhookSignatureService;
import com.carland.carland_service.service.webhook.WebhookAuthResponseWriter;
import com.carland.carland_service.service.webhook.WebhookAuthValidationResult;
import com.carland.carland_service.util.InternalTokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final String PARTNER_PREFIX = "/webhook/partner/";
    private static final String TEST_PATH = "/webhook/partner/test";

    private final InternalTokenValidator internalTokenValidator;
    private final PartnerWebhookSignatureService partnerWebhookSignatureService;
    private final WebhookAuthResponseWriter webhookAuthResponseWriter;

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
            webhookAuthResponseWriter.writeUnauthorized(response, internalResult.getFailure());
            return;
        }

        if (TEST_PATH.equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
        byte[] body = wrapped.getCachedBody();

        WebhookAuthValidationResult partnerResult = partnerWebhookSignatureService.validate(wrapped, body);
        if (!partnerResult.isValid()) {
            log.warn(
                    "Webhook partner auth failed: path={}, partnerId={}, reason={}",
                    path,
                    partnerResult.getPartnerId(),
                    partnerResult.getFailure().getError()
            );
            webhookAuthResponseWriter.writeUnauthorized(response, partnerResult.getFailure());
            return;
        }

        filterChain.doFilter(wrapped, response);
    }
}
