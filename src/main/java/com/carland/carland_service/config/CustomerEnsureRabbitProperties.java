package com.carland.carland_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "carland.customer-ensure")
public class CustomerEnsureRabbitProperties {
    private String exchange = "customer.ensure";
    private String queue = "customer.ensure.work";
    private String routingKey = "ensure";
    private String retryQueue = "customer.ensure.retry";
    private String retryRoutingKey = "ensure.retry";
    private String dlq = "customer.ensure.dlq";
    private String dlqRoutingKey = "ensure.dlq";
    /** Delay before re-attempting a failed ensure (default 15s). */
    private long retryDelayMs = 15_000L;
    /** Cap retries so a poison message lands in DLQ (40 * 15s ≈ 10 min). */
    private int maxAttempts = 40;
}
