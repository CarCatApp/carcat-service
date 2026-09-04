package com.carland.carland_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * tr: Uygulama genelinde kullanılan RestTemplate bean'ini tanımlar.
 * en: Defines the RestTemplate bean used across the application.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * tr: OpenAI görsel üretimi için ayrı client; 15–25 sn + pay. Global RestTemplate'e dokunmaz.
     * en: Dedicated client for OpenAI image generation; 15–25s plus headroom. Does not change the global RestTemplate.
     */
    @Bean("openaiRestTemplate")
    public RestTemplate openaiRestTemplate(
            @Value("${openai.image-timeout-ms:35000}") int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}
