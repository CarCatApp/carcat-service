package com.carland.carland_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * tr: Client event ingest hədləri. Rəqəmlər kodda sərt deyil.
 * en: Client event ingest limits. The numbers are not hardcoded in the flow.
 */
@Data
@Component
@ConfigurationProperties(prefix = "carland.client-events")
public class ClientEventProperties {

    private int ipPerMinute = 60;
    private int installEventsPerMinute = 300;
    private int maxBodyBytes = 524288;
    private int stormPerMinute = 20;
    private int retentionDays = 30;
    private int fingerprintVersion = 1;
    private int maxBatch = 50;
    private int messageMax = 500;
    private int stackMax = 8192;
    private int breadcrumbMax = 30;
}
