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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * tr: Rezervasyon sohbeti canlı hattı (SSE). new_message / delivery / read / typing / presence.
 *     presence: bu rezervasyonun sohbet hattı. Bağlanınca her iki rolün anlık durumu gider;
 *     son dinleyici düşünce karşı tarafa online=false gider. İstemci periyodik istek atmaz.
 * en: Booking chat live line (SSE). new_message / delivery / read / typing / presence.
 *     presence is this booking's chat line. On connect both roles' current state is sent;
 *     when the last listener of a role drops, peers get online=false. Clients do not poll.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingChatLiveService {

    private static final String FLUSH = "x".repeat(32 * 1024);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<Listener>> open = new ConcurrentHashMap<>();

    public SseEmitter listen(Long bookingId, String viewerRole) {
        SseEmitter emitter = new SseEmitter(0L);
        Listener listener = new Listener(emitter, viewerRole == null ? "" : viewerRole);
        open.computeIfAbsent(bookingId, id -> new CopyOnWriteArrayList<>()).add(listener);
        emitter.onCompletion(() -> remove(bookingId, listener));
        emitter.onTimeout(() -> remove(bookingId, listener));
        emitter.onError(error -> remove(bookingId, listener));
        try {
            emitter.send(SseEmitter.event().comment("open"));
            push(emitter);
            sendSnapshot(bookingId, listener);
            listener.announced = true;
            if (hasRole(bookingId, listener.role)) {
                publishPresence(bookingId, listener.role, true, listener);
            }
        } catch (Exception ex) {
            remove(bookingId, listener);
        }
        return emitter;
    }

    /** Karşı taraf (farklı rol) canlı dinliyor mu? */
    public boolean hasPeerListener(Long bookingId, String myRole) {
        List<Listener> emitters = open.get(bookingId);
        if (emitters == null || emitters.isEmpty() || myRole == null) {
            return false;
        }
        for (Listener listener : emitters) {
            if (listener.role != null && !listener.role.isBlank() && !myRole.equals(listener.role)) {
                return true;
            }
        }
        return false;
    }

    public void publishTimeProposalAfterCommit(Long bookingId, Object messageView) {
        if (bookingId == null || messageView == null) {
            return;
        }
        afterCommit(() -> publish(bookingId, "time_proposal", Map.of(
                "event", "time_proposal",
                "reservationId", String.valueOf(bookingId),
                "message", messageView)));
    }

    public void publishNewMessageAfterCommit(Long bookingId, Object messageView) {
        if (bookingId == null || messageView == null) {
            return;
        }
        afterCommit(() -> publish(bookingId, "new_message", Map.of(
                "event", "new_message",
                "reservationId", String.valueOf(bookingId),
                "message", messageView)));
    }

    public void publishDelivery(Long bookingId, String clientMessageId, String serverId, String delivery) {
        if (bookingId == null || clientMessageId == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "message_delivery");
        payload.put("reservationId", String.valueOf(bookingId));
        payload.put("clientMessageId", clientMessageId);
        payload.put("serverId", serverId);
        payload.put("delivery", delivery);
        publishNowOrAfterCommit(bookingId, "message_delivery", payload);
    }

    public void publishRead(Long bookingId, List<String> clientMessageIds) {
        if (bookingId == null || clientMessageIds == null || clientMessageIds.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "message_read");
        payload.put("reservationId", String.valueOf(bookingId));
        payload.put("clientMessageIds", new ArrayList<>(clientMessageIds));
        payload.put("delivery", "seen");
        publishNowOrAfterCommit(bookingId, "message_read", payload);
    }

    public void publishTyping(Long bookingId, String authorId, String fromRole, boolean isTyping) {
        if (bookingId == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "typing");
        payload.put("reservationId", String.valueOf(bookingId));
        payload.put("authorId", authorId);
        payload.put("fromRole", fromRole);
        payload.put("isTyping", isTyping);
        publish(bookingId, "typing", payload);
    }

    private void publishNowOrAfterCommit(Long bookingId, String eventName, Map<String, Object> payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            afterCommit(() -> publish(bookingId, eventName, payload));
        } else {
            publish(bookingId, eventName, payload);
        }
    }

    private void sendSnapshot(Long bookingId, Listener listener) throws IOException {
        sendOne(bookingId, listener, presencePayload(bookingId, "customer", hasRole(bookingId, "customer")));
        sendOne(bookingId, listener, presencePayload(bookingId, "branch", hasRole(bookingId, "branch")));
    }

    private void publishPresence(Long bookingId, String role, boolean online, Listener skip) {
        if (bookingId == null || role == null || role.isBlank()) {
            return;
        }
        publish(bookingId, "presence", presencePayload(bookingId, role, online), skip);
    }

    private Map<String, Object> presencePayload(Long bookingId, String role, boolean online) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "presence");
        payload.put("reservationId", String.valueOf(bookingId));
        payload.put("role", role);
        payload.put("online", online);
        return payload;
    }

    private void sendOne(Long bookingId, Listener listener, Map<String, Object> payload) throws IOException {
        try {
            String json = objectMapper.writeValueAsString(payload);
            listener.emitter.send(SseEmitter.event().name("presence").data(json));
            push(listener.emitter);
        } catch (Exception ex) {
            remove(bookingId, listener);
            if (ex instanceof IOException io) {
                throw io;
            }
            throw new IOException(ex);
        }
    }

    private boolean hasRole(Long bookingId, String role) {
        List<Listener> emitters = open.get(bookingId);
        if (emitters == null || emitters.isEmpty() || role == null) {
            return false;
        }
        for (Listener listener : emitters) {
            if (role.equals(listener.role)) {
                return true;
            }
        }
        return false;
    }

    private void publish(Long bookingId, String eventName, Map<String, Object> payload) {
        publish(bookingId, eventName, payload, null);
    }

    private void publish(Long bookingId, String eventName, Map<String, Object> payload, Listener skip) {
        List<Listener> emitters = open.get(bookingId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("CHAT_LIVE_SKIP | {}", ex.getMessage());
            return;
        }
        for (Listener listener : emitters) {
            if (listener == skip) {
                continue;
            }
            try {
                listener.emitter.send(SseEmitter.event().name(eventName).data(json));
                push(listener.emitter);
            } catch (Exception ex) {
                remove(bookingId, listener);
            }
        }
    }

    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        open.forEach((bookingId, emitters) -> {
            for (Listener listener : emitters) {
                try {
                    listener.emitter.send(SseEmitter.event().comment("ping"));
                    push(listener.emitter);
                } catch (Exception ex) {
                    remove(bookingId, listener);
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

    private void remove(Long bookingId, Listener listener) {
        CopyOnWriteArrayList<Listener> emitters = open.get(bookingId);
        if (emitters == null) {
            return;
        }
        boolean removed = emitters.remove(listener);
        if (emitters.isEmpty()) {
            open.remove(bookingId, emitters);
        }
        if (!removed || !listener.announced || hasRole(bookingId, listener.role)) {
            return;
        }
        publishPresence(bookingId, listener.role, false, null);
    }

    private static final class Listener {
        private final SseEmitter emitter;
        private final String role;
        private boolean announced;

        private Listener(SseEmitter emitter, String role) {
            this.emitter = emitter;
            this.role = role;
        }
    }
}
