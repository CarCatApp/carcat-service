package com.carland.carland_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * tr: Rezervasyon sohbeti canlı hattı (SSE). Şube inbox live'ından ayrı; booking başına dinleyici.
 * en: Booking chat live line (SSE). Separate from branch inbox live; listeners per booking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingChatLiveService {

    private static final String FLUSH = "x".repeat(32 * 1024);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> open = new ConcurrentHashMap<>();

    public SseEmitter listen(Long bookingId) {
        SseEmitter emitter = new SseEmitter(0L);
        open.computeIfAbsent(bookingId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(bookingId, emitter));
        emitter.onTimeout(() -> remove(bookingId, emitter));
        emitter.onError(error -> remove(bookingId, emitter));
        try {
            emitter.send(SseEmitter.event().comment("open"));
            push(emitter);
        } catch (Exception ex) {
            remove(bookingId, emitter);
        }
        return emitter;
    }

    public void publishNewMessageAfterCommit(Long bookingId, Object messageView) {
        if (bookingId == null || messageView == null) {
            return;
        }
        afterCommit(() -> deliver(bookingId, "new_message", messageView));
    }

    private void deliver(Long bookingId, String eventName, Object messageView) {
        List<SseEmitter> emitters = open.get(bookingId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", eventName);
        payload.put("reservationId", String.valueOf(bookingId));
        payload.put("message", messageView);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("CHAT_LIVE_SKIP | {}", ex.getMessage());
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(json));
                push(emitter);
            } catch (Exception ex) {
                remove(bookingId, emitter);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        open.forEach((bookingId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                    push(emitter);
                } catch (Exception ex) {
                    remove(bookingId, emitter);
                }
            }
        });
    }

    private static void afterCommit(Runnable send) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    private static void push(SseEmitter emitter) throws IOException {
        emitter.send(SseEmitter.event().comment(FLUSH));
    }

    private void remove(Long bookingId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = open.get(bookingId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            open.remove(bookingId, emitters);
        }
    }
}
