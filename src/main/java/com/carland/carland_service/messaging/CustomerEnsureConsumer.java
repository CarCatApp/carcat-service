package com.carland.carland_service.messaging;

import com.carland.carland_service.config.CustomerEnsureRabbitProperties;
import com.carland.carland_service.service.CustomerEnsureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Runs ensureCustomer; on miss/failure requeues with 15s TTL delay until maxAttempts, then DLQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEnsureConsumer {

    private final ObjectMapper objectMapper;
    private final CustomerEnsureService customerEnsureService;
    private final CustomerEnsurePublisher publisher;
    private final CustomerEnsureRabbitProperties props;

    @RabbitListener(queues = "${carland.customer-ensure.queue}")
    public void onMessage(Message message) {
        EnsureCustomerMessage payload;
        try {
            payload = objectMapper.readValue(message.getBody(), EnsureCustomerMessage.class);
        } catch (Exception e) {
            log.error("[customer-ensure] bad payload, dropping: {}", e.toString());
            return;
        }
        if (payload == null || payload.userId() == null
                || payload.phoneNumber() == null || payload.phoneNumber().isBlank()) {
            log.error("[customer-ensure] incomplete payload, dropping");
            return;
        }

        try {
            customerEnsureService.ensure(
                    payload.userId(),
                    payload.phoneNumber(),
                    null,
                    null,
                    payload.acceptLanguage());
            if (customerEnsureService.hasActiveCustomer(payload.userId(), payload.phoneNumber())) {
                log.info("[customer-ensure] done userId={} attempt={}", payload.userId(), payload.attempt());
                return;
            }
            log.warn("[customer-ensure] still missing after ensure userId={} attempt={}",
                    payload.userId(), payload.attempt());
            scheduleRetryOrDlq(payload);
        } catch (Exception e) {
            log.warn("[customer-ensure] ensure failed userId={} attempt={}: {}",
                    payload.userId(), payload.attempt(), e.toString());
            scheduleRetryOrDlq(payload);
        }
    }

    private void scheduleRetryOrDlq(EnsureCustomerMessage payload) {
        int nextAttempt = payload.attempt() + 1;
        if (nextAttempt >= props.getMaxAttempts()) {
            log.error("[customer-ensure] max attempts reached, DLQ userId={} phone={} attempts={}",
                    payload.userId(), payload.phoneNumber(), nextAttempt);
            publisher.publishDlq(payload.nextAttempt());
            return;
        }
        publisher.publishRetry(payload);
    }
}
