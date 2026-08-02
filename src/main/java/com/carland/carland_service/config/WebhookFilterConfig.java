package com.carland.carland_service.config;

import com.carland.carland_service.security.WebhookSignatureFilter;
import com.carland.carland_service.security.PartnerWebhookSignatureService;
import com.carland.carland_service.security.WebhookAuthResponseWriter;
import com.carland.carland_service.security.InternalTokenValidator;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * tr: Partner webhook güvenlik filtresinin konfigürasyonu; WebhookSignatureFilter bean'ini oluşturur ve /webhook/partner/* URL desenine yüksek öncelikle kaydeder.
 * en: Configuration for the partner webhook security filter; creates the WebhookSignatureFilter bean and registers it for the /webhook/partner/* URL pattern with high precedence.
 */
@Configuration
public class WebhookFilterConfig {

    /**
     * tr: Internal token doğrulayıcı, partner imza servisi ve hata yazıcısını bir araya getirerek WebhookSignatureFilter bean'ini üretir.
     * en: Produces the WebhookSignatureFilter bean by wiring together the internal token validator, partner signature service, and auth failure response writer.
     */
    @Bean
    WebhookSignatureFilter webhookSignatureFilter(
            InternalTokenValidator internalTokenValidator,
            PartnerWebhookSignatureService partnerWebhookSignatureService,
            WebhookAuthResponseWriter webhookAuthResponseWriter
    ) {
        return new WebhookSignatureFilter(
                internalTokenValidator,
                partnerWebhookSignatureService,
                webhookAuthResponseWriter
        );
    }

    /**
     * tr: Filtreyi /webhook/partner/* URL desenine, en yüksek önceliğe yakın bir sırayla (HIGHEST_PRECEDENCE + 5) kaydeder.
     * en: Registers the filter for the /webhook/partner/* URL pattern with near-highest precedence (HIGHEST_PRECEDENCE + 5).
     */
    @Bean
    FilterRegistrationBean<WebhookSignatureFilter> webhookSignatureFilterRegistration(WebhookSignatureFilter filter) {
        FilterRegistrationBean<WebhookSignatureFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
        registration.addUrlPatterns("/webhook/partner/*");
        return registration;
    }
}
