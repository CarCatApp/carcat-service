package com.carland.carland_service.config;

import com.carland.carland_service.filter.WebhookSignatureFilter;
import com.carland.carland_service.service.webhook.PartnerWebhookSignatureService;
import com.carland.carland_service.service.webhook.WebhookAuthResponseWriter;
import com.carland.carland_service.util.InternalTokenValidator;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class WebhookFilterConfig {

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

    @Bean
    FilterRegistrationBean<WebhookSignatureFilter> webhookSignatureFilterRegistration(WebhookSignatureFilter filter) {
        FilterRegistrationBean<WebhookSignatureFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        registration.addUrlPatterns("/webhook/partner/*");
        return registration;
    }
}
