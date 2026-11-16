package com.carland.carland_service.service.commission;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.CommissionCustomerStatus;
import com.carland.carland_service.entity.CommissionStatusAudit;
import com.carland.carland_service.hyper.HyperVin;
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
 * tr: VIN Hyper loyalty. new / reactivated yapışır. old hər yoxlamada yenidən baxılır;
 *     yalnız reactivated-ə keçə bilər. Branch list və book eyni qaydanı istifadə edir.
 * en: VIN Hyper loyalty. new / reactivated stick. old is re-checked each time and may
 *     only move to reactivated. Branch list and book share the same rule.
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

    /**
     * tr: Branch list / loyalty oxuma. Sticky isə Hyper-ə getmir. old isə yenidən baxır.
     * en: Branch list / loyalty read. Sticky skips Hyper. old is re-checked.
     */
    @Transactional
    public String resolveStatus(String rawVin) {
        CommissionCustomerStatus row = resolve(rawVin, OffsetDateTime.now(CommissionClock.BAKU), null);
        return row == null ? null : row.getStatus();
    }

    @Transactional
    public CommissionCustomerStatus ensure(Booking booking) {
        if (booking == null) {
            return null;
        }
        OffsetDateTime bookingTime = booking.getCreatedAt() == null
                ? OffsetDateTime.now(CommissionClock.BAKU)
                : booking.getCreatedAt();
        return resolve(booking.getVin(), bookingTime, booking.getId());
    }

    @Transactional
    public CommissionCustomerStatus resolve(String rawVin, OffsetDateTime at, Long bookingId) {
        String vin = CommissionVin.normalize(rawVin);
        OffsetDateTime when = at == null ? OffsetDateTime.now(CommissionClock.BAKU) : at;
        Booking auditBooking = bookingId == null ? null : Booking.builder().id(bookingId).vin(vin).createdAt(when).build();
        if (vin == null) {
            audit(auditBooking, null, when, null, null, CommissionStatus.NEW, CommissionStatusRules.NO_VIN, false);
            return null;
        }
        CommissionCustomerStatus existing = statusRepository.findByVin(vin).orElse(null);
        if (existing != null && CommissionStatus.sticky(existing.getStatus())) {
            String reason = CommissionStatus.REACTIVATED.equals(existing.getStatus())
                    ? CommissionStatusRules.ALREADY_REACTIVATED
                    : CommissionStatusRules.ALREADY_NEW;
            audit(auditBooking, vin, when, existing.getEarliestServiceDate(), null,
                    existing.getStatus(), reason, false);
            return existing;
        }
        if (!CommissionVin.seventeen(vin)) {
            audit(auditBooking, vin, when, null, null, CommissionStatus.NEW, CommissionStatusRules.INVALID_VIN, false);
            return null;
        }
        HyperVinSnapshot snapshot = hyperVinLookup.lookup(vin);
        CommissionDecision decision = CommissionStatusRules.decide(when, snapshot);
        if (CommissionStatusRules.FUTURE_DATE.equals(decision.outcome())) {
            log.warn("commission future service date | vin={} bookingId={} date={}",
                    vin, bookingId, decision.serviceDate());
        }
        return save(auditBooking, vin, when, decision, snapshot, existing);
    }

    private CommissionCustomerStatus save(Booking booking, String vin, OffsetDateTime bookingTime,
                                          CommissionDecision decision, HyperVinSnapshot snapshot,
                                          CommissionCustomerStatus current) {
        if (current != null && CommissionStatus.sticky(current.getStatus())) {
            String reason = CommissionStatus.REACTIVATED.equals(current.getStatus())
                    ? CommissionStatusRules.ALREADY_REACTIVATED
                    : CommissionStatusRules.ALREADY_NEW;
            audit(booking, vin, bookingTime, current.getEarliestServiceDate(), null,
                    current.getStatus(), reason, false);
            return current;
        }
        OffsetDateTime now = OffsetDateTime.now(CommissionClock.BAKU);
        String nextStatus = decision.status();
        String reason = decision.outcome();
        if (current != null && CommissionStatus.OLD.equals(current.getStatus())) {
            if (CommissionStatus.NEW.equals(nextStatus)) {
                nextStatus = CommissionStatus.OLD;
                reason = CommissionStatusRules.KEEP_OLD;
            } else if (!CommissionStatus.OLD.equals(nextStatus)
                    && !CommissionStatus.REACTIVATED.equals(nextStatus)) {
                nextStatus = CommissionStatus.OLD;
                reason = CommissionStatusRules.KEEP_OLD;
            }
        }
        CommissionCustomerStatus row;
        if (current == null) {
            row = CommissionCustomerStatus.builder()
                    .vin(vin)
                    .status(nextStatus)
                    .decidedAt(now)
                    .firstBookingId(booking == null ? null : booking.getId())
                    .hyperOutcome(reason)
                    .earliestServiceDate(decision.serviceDate())
                    .snapshot(trim(decision.snapshot()))
                    .build();
            try {
                row = statusRepository.saveAndFlush(row);
            } catch (DataIntegrityViolationException ex) {
                return statusRepository.findByVin(vin).orElseThrow(() -> ex);
            }
        } else {
            current.setStatus(nextStatus);
            current.setDecidedAt(now);
            current.setHyperOutcome(reason);
            current.setEarliestServiceDate(decision.serviceDate());
            current.setSnapshot(trim(decision.snapshot()));
            row = statusRepository.saveAndFlush(current);
        }
        boolean review = CommissionStatusRules.FUTURE_DATE.equals(decision.outcome());
        audit(booking, vin, bookingTime, decision.serviceDate(), snapshot, nextStatus, reason, review);
        return row;
    }

    private void audit(Booking booking, String vin, OffsetDateTime bookingTime, LocalDate serviceDate,
                       HyperVinSnapshot snapshot, String status, String reason, boolean review) {
        String request = null;
        String response = null;
        if (snapshot != null && vin != null) {
            String hyperVin = HyperVin.forHyperApi(vin);
            request = "GET /partner/v1/vehicles/by-vin/" + (hyperVin != null ? hyperVin : vin)
                    + "\nrequest_datetime=" + bookingTime;
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
