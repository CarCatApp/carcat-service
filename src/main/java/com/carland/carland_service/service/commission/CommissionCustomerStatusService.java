package com.carland.carland_service.service.commission;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.CommissionCustomerStatus;
import com.carland.carland_service.entity.CommissionStatusAudit;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CommissionCustomerStatusRepository;
import com.carland.carland_service.repository.CommissionStatusAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * tr: Növbə yarananda VIN-i Hyper-a soruşur. Yeni yapışır. Köhnə hər yeni növbədə yenidən baxılır.
 * en: Asks Hyper when a booking is created. New sticks. Old is checked again on every new booking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionCustomerStatusService {

    private final CommissionCustomerStatusRepository statusRepository;
    private final BookingRepository bookingRepository;
    private final HyperVinLookup hyperVinLookup;
    private final ObjectProvider<CommissionCustomerStatusService> self;
    private final CommissionStatusAuditRepository auditRepository;

    public void schedule(Long bookingId) {
        if (bookingId == null) {
            return;
        }
        runAfterCommit(() -> {
            try {
                self.getObject().ensureById(bookingId);
            } catch (RuntimeException ex) {
                log.warn("commission status failed | bookingId={} err={}", bookingId, ex.toString());
            }
        });
    }

    @Transactional
    public void ensureById(Long bookingId) {
        bookingRepository.findById(bookingId).ifPresent(this::ensure);
    }

    public CommissionCustomerStatus find(String vin) {
        String key = CommissionVin.normalize(vin);
        if (key == null) {
            return null;
        }
        return statusRepository.findByVin(key).orElse(null);
    }

    @Transactional
    public CommissionCustomerStatus ensure(Booking booking) {
        if (booking == null) {
            return null;
        }
        String vin = CommissionVin.normalize(booking.getVin());
        OffsetDateTime bookingTime = booking.getCreatedAt() == null
                ? OffsetDateTime.now(CommissionClock.BAKU)
                : booking.getCreatedAt();
        if (vin == null) {
            audit(booking, null, bookingTime, null, null, CommissionStatus.NEW, CommissionStatusRules.NO_VIN, false);
            return null;
        }
        CommissionCustomerStatus existing = statusRepository.findByVin(vin).orElse(null);
        if (existing != null && CommissionStatus.NEW.equals(existing.getStatus())) {
            audit(booking, vin, bookingTime, existing.getEarliestServiceDate(), null,
                    CommissionStatus.NEW, CommissionStatusRules.ALREADY_NEW, false);
            return existing;
        }
        if (!CommissionVin.seventeen(vin)) {
            audit(booking, vin, bookingTime, null, null, CommissionStatus.NEW, CommissionStatusRules.INVALID_VIN, false);
            return null;
        }
        HyperVinSnapshot snapshot = hyperVinLookup.lookup(vin);
        CommissionDecision decision = CommissionStatusRules.decide(bookingTime, snapshot);
        if (CommissionStatusRules.FUTURE_DATE.equals(decision.outcome())) {
            log.warn("commission future service date | vin={} bookingId={} date={}",
                    vin, booking.getId(), decision.serviceDate());
        }
        return save(booking, vin, bookingTime, decision, snapshot);
    }

    private CommissionCustomerStatus save(Booking booking, String vin, OffsetDateTime bookingTime,
                                          CommissionDecision decision, HyperVinSnapshot snapshot) {
        CommissionCustomerStatus current = statusRepository.findByVin(vin).orElse(null);
        if (current != null && CommissionStatus.NEW.equals(current.getStatus())) {
            audit(booking, vin, bookingTime, current.getEarliestServiceDate(), null,
                    CommissionStatus.NEW, CommissionStatusRules.ALREADY_NEW, false);
            return current;
        }
        OffsetDateTime now = OffsetDateTime.now(CommissionClock.BAKU);
        CommissionCustomerStatus row;
        if (current == null) {
            row = CommissionCustomerStatus.builder()
                    .vin(vin)
                    .status(decision.status())
                    .decidedAt(now)
                    .firstBookingId(booking.getId())
                    .hyperOutcome(decision.outcome())
                    .earliestServiceDate(decision.serviceDate())
                    .snapshot(trim(decision.snapshot()))
                    .build();
            try {
                row = statusRepository.saveAndFlush(row);
            } catch (DataIntegrityViolationException ex) {
                return statusRepository.findByVin(vin).orElseThrow(() -> ex);
            }
        } else {
            current.setStatus(decision.status());
            current.setDecidedAt(now);
            current.setHyperOutcome(decision.outcome());
            current.setEarliestServiceDate(decision.serviceDate());
            current.setSnapshot(trim(decision.snapshot()));
            row = statusRepository.saveAndFlush(current);
        }
        boolean review = CommissionStatusRules.FUTURE_DATE.equals(decision.outcome());
        audit(booking, vin, bookingTime, decision.serviceDate(), snapshot, decision.status(), decision.outcome(), review);
        return row;
    }

    private void audit(Booking booking, String vin, OffsetDateTime bookingTime, LocalDate serviceDate,
                       HyperVinSnapshot snapshot, String status, String reason, boolean review) {
        String request = null;
        String response = null;
        if (snapshot != null && vin != null) {
            request = "GET /partner/v1/vehicles/by-vin/" + vin + "\nrequest_datetime=" + bookingTime;
            response = snapshot.raw();
        }
        auditRepository.save(CommissionStatusAudit.builder()
                .vin(vin)
                .bookingId(booking == null ? null : booking.getId())
                .bookingTime(bookingTime)
                .requestText(clip(request))
                .responseText(clip(response))
                .lastServiceDate(serviceDate)
                .status(status)
                .reason(reason)
                .reviewRequired(review)
                .build());
    }

    private static String trim(String snapshot) {
        return clip(snapshot, 2000);
    }

    private static String clip(String value) {
        return clip(value, 4000);
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
