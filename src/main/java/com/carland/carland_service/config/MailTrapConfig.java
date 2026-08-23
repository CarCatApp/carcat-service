package com.carland.carland_service.config;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * tr: Mailtrap istemcisini tanımlayan konfigürasyondur; spring.mail.password değerini API token olarak
 *     kullanır. (Not: aktif mail gönderimi Brevo üzerinden yapılıyor; bu bean şu an kullanılmıyor olabilir.)
 * en: Configuration defining the Mailtrap client; uses spring.mail.password as the API token.
 *     (Note: active mail delivery goes through Brevo; this bean may currently be unused.)
 */
@Configuration
public class MailTrapConfig {

    @Value("${spring.mail.password}")
    private String apiToken;

    /**
     * tr: Konfigüre edilmiş MailtrapClient bean'ini üretir.
     * en: Produces the configured MailtrapClient bean.
     */
    @Bean
    public MailtrapClient mailtrapClient() {
        MailtrapConfig config = new MailtrapConfig.Builder()
                .token(apiToken)
                .build();
        return MailtrapClientFactory.createMailtrapClient(config);
    }


}
