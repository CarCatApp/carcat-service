package com.carland.carland_service.messaging;

import com.carland.carland_service.config.CustomerEnsureRabbitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Enqueues ensure-customer work (first attempt or 15s retry / DLQ).
 * Publish failures are logged only — never break the HTTP request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerEnsurePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final CustomerEnsureRabbitProperties props;
    private final ObjectMapper objectMapper;

    public void publish(Long userId, String phoneNumber, String acceptLanguage) {
        if (userId == null || phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }
        publishMessage(new EnsureCustomerMessage(
                userId,
                phoneNumber.trim(),
                acceptLanguage == null || acceptLanguage.isBlank() ? "az" : acceptLanguage,
                0));
    }

    public void publishRetry(EnsureCustomerMessage message) {
        if (message == null) {
            return;
        }
        send(props.getRetryRoutingKey(), message.nextAttempt());
    }

    public void publishDlq(EnsureCustomerMessage message) {
        if (message == null) {
            return;
        }
        send(props.getDlqRoutingKey(), message);
    }

    private void publishMessage(EnsureCustomerMessage message) {
        send(props.getRoutingKey(), message);
    }

    private void send(String routingKey, EnsureCustomerMessage message) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(message);
            Message amqp = MessageBuilder.withBody(body)
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build();
            rabbitTemplate.send(props.getExchange(), routingKey, amqp);
            log.info("[customer-ensure] queued routingKey={} userId={} phone={} attempt={}",
                    routingKey, message.userId(), message.phoneNumber(), message.attempt());
        } catch (Exception e) {
            log.warn("[customer-ensure] queue publish failed userId={} routingKey={}: {}",
                    message.userId(), routingKey, e.toString());
        }
    }
}
