package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffBookingArrival;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * tr: Şube başına açık ekran bağlantıları. Cron yok. Rezervasyon commit olunca o şubeye bir satır gider.
 * en: Open screen connections per branch. No cron. One line goes to that branch after the booking commits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffBookingLiveService {

    /** Kong/nginx küçük satırı tamponda tutar. Bu dolgu tamponu doldurup satırı tarayıcıya iter. */
    private static final String FLUSH = "x".repeat(32 * 1024);

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> open = new ConcurrentHashMap<>();

    public SseEmitter listen(Long branchId) {
        SseEmitter emitter = new SseEmitter(0L);
        open.computeIfAbsent(branchId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(branchId, emitter));
        emitter.onTimeout(() -> remove(branchId, emitter));
        emitter.onError(error -> remove(branchId, emitter));
        try {
            emitter.send(SseEmitter.event().comment("open"));
            push(emitter);
        } catch (Exception ex) {
            remove(branchId, emitter);
        }
        return emitter;
    }

    public void publishAfterCommit(StaffBookingArrival arrival) {
        if (arrival == null || arrival.getBranchId() == null || arrival.getBookingId() == null) {
            return;
        }
        Runnable send = () -> deliver(arrival);
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

    @Scheduled(fixedRate = 15000)
    public void heartbeat() {
        open.forEach((branchId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                    push(emitter);
                } catch (Exception ex) {
                    remove(branchId, emitter);
                }
            }
        });
    }

    private void deliver(StaffBookingArrival arrival) {
        List<SseEmitter> emitters = open.get(arrival.getBranchId());
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(arrival);
        } catch (Exception ex) {
            log.warn("BOOKING_LIVE_SKIP | {}", ex.getMessage());
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("arrival").data(json));
                push(emitter);
            } catch (Exception ex) {
                remove(arrival.getBranchId(), emitter);
            }
        }
    }

    private static void push(SseEmitter emitter) throws IOException {
        emitter.send(SseEmitter.event().comment(FLUSH));
    }

    private void remove(Long branchId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = open.get(branchId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            open.remove(branchId, emitters);
        }
    }
}
