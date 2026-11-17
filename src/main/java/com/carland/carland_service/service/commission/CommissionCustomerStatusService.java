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
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * tr: VIN Hyper loyalty. new / reactivated yapışır. old hər yoxlamada yenidən baxılır;
 *     yalnız reactivated-ə keçə bilər. Hyper uğursuz → failed (sticky deyil). VIN yox → —.
 *     Hyper çağırışı DB transaction xaricindədir.
 * en: VIN Hyper loyalty. new / reactivated stick. old is re-checked and may become reactivated.
 *     Hyper failure → failed (not sticky). No VIN → —. Hyper call is outside the DB transaction.
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
                log.error("commission status failed | bookingId={} err={}", bookingId, ex.toString());
                try {
                    self.getObject().markEnsureFailed(bookingId, ex);
                } catch (RuntimeException markEx) {
                    log.error("commission status mark-failed also failed | bookingId={} err={}",
                            bookingId, markEx.toString());
                }
            }
        });
    }

    /**
     * tr: Booking oxu (qısa TX) → Hyper TX xaricində → qərarı ayrı TX-də yaz.
     * en: Load booking (short TX) → Hyper outside TX → persist decision in its own TX.
     */
    public void ensureById(Long bookingId) {
        Booking booking = self.getObject().loadBooking(bookingId);
        if (booking == null) {
            return;
        }
        self.getObject().ensure(booking);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Booking loadBooking(Long bookingId) {
        if (bookingId == null) {
            return null;
        }
        return bookingRepository.findById(bookingId).orElse(null);
    }

    public CommissionCustomerStatus find(String vin) {
        String key = CommissionVin.normalize(vin);
        if (key == null) {
            return null;
        }
        return statusRepository.findByVin(key).orElse(null);
    }

    /**
     * tr: Branch list / loyalty oxuma. Sticky isə Hyper-ə getmir. old / failed / — yenidən baxır.
     * en: Branch list / loyalty read. Sticky skips Hyper. old / failed / — are re-checked.
     */
    public String resolveStatus(String rawVin) {
        CommissionCustomerStatus row = ensure(rawVin, OffsetDateTime.now(CommissionClock.BAKU), null);
        return row == null ? null : row.getStatus();
    }

    public CommissionCustomerStatus ensure(Booking booking) {
        if (booking == null) {
            return null;
        }
        OffsetDateTime bookingTime = booking.getCreatedAt() == null
                ? OffsetDateTime.now(CommissionClock.BAKU)
                : booking.getCreatedAt();
        return ensure(booking.getVin(), bookingTime, booking.getId());
    }

    /**
     * tr: Sticky qısa oxu TX-də; Hyper çöldə; yazı ayrı REQUIRES_NEW TX.
     * en: Sticky short read TX; Hyper outside; write in a separate REQUIRES_NEW TX.
     */
    public CommissionCustomerStatus ensure(String rawVin, OffsetDateTime at, Long bookingId) {
        OffsetDateTime when = at == null ? OffsetDateTime.now(CommissionClock.BAKU) : at;
        String vin = CommissionVin.normalize(rawVin);
        Booking auditBooking = bookingId == null ? null : Booking.builder().id(bookingId).vin(vin).createdAt(when).build();

        if (vin == null) {
            return self.getObject().persistDecision(auditBooking, null, when,
                    new CommissionDecision(CommissionStatus.DASH, CommissionStatusRules.NO_VIN, null, "no_vin"),
                    null, null);
        }

        CommissionCustomerStatus existing = self.getObject().findInTx(vin);
        if (existing != null && CommissionStatus.sticky(existing.getStatus())) {
            self.getObject().auditOnly(auditBooking, vin, when, existing.getEarliestServiceDate(), null,
                    existing.getStatus(),
                    CommissionStatus.REACTIVATED.equals(existing.getStatus())
                            ? CommissionStatusRules.ALREADY_REACTIVATED
                            : CommissionStatusRules.ALREADY_NEW,
                    false);
            return existing;
        }

        if (!CommissionVin.seventeen(vin)) {
            return self.getObject().persistDecision(auditBooking, vin, when,
                    new CommissionDecision(CommissionStatus.DASH, CommissionStatusRules.INVALID_VIN, null, "invalid_vin"),
                    null, existing);
        }

        HyperVinSnapshot snapshot;
        try {
            snapshot = hyperVinLookup.lookup(vin);
        } catch (RuntimeException ex) {
            log.warn("commission hyper lookup threw | vin={} err={}", vin, ex.getClass().getSimpleName());
            snapshot = HyperVinSnapshot.failed(ex.getClass().getSimpleName());
        }

        CommissionDecision decision = CommissionStatusRules.decide(when, snapshot);
        if (CommissionStatusRules.FUTURE_DATE.equals(decision.outcome())) {
            log.warn("commission future service date | vin={} bookingId={} date={}",
                    vin, bookingId, decision.serviceDate());
        }
        return self.getObject().persistDecision(auditBooking, vin, when, decision, snapshot, existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public CommissionCustomerStatus findInTx(String vin) {
        return statusRepository.findByVin(vin).orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markEnsureFailed(Long bookingId, RuntimeException cause) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        String vin = booking == null ? null : CommissionVin.normalize(booking.getVin());
        OffsetDateTime when = booking == null || booking.getCreatedAt() == null
                ? OffsetDateTime.now(CommissionClock.BAKU)
                : booking.getCreatedAt();
        String reason = cause instanceof InvalidDataAccessApiUsageException
                ? CommissionStatusRules.NO_TX
                : CommissionStatusRules.CALCULATE_FAILED;
        String detail = cause == null ? reason : cause.getClass().getSimpleName();
        CommissionCustomerStatus existing = vin == null ? null : statusRepository.findByVin(vin).orElse(null);
        if (existing != null && CommissionStatus.sticky(existing.getStatus())) {
            audit(booking, vin, when, existing.getEarliestServiceDate(), null,
                    existing.getStatus(),
                    CommissionStatus.REACTIVATED.equals(existing.getStatus())
                            ? CommissionStatusRules.ALREADY_REACTIVATED
                            : CommissionStatusRules.ALREADY_NEW,
                    false);
            return;
        }
        if (existing != null && CommissionStatus.OLD.equals(existing.getStatus())) {
            existing.setHyperOutcome(reason);
            existing.setDecidedAt(OffsetDateTime.now(CommissionClock.BAKU));
            existing.setSnapshot(trim(detail));
            statusRepository.saveAndFlush(existing);
            audit(booking, vin, when, existing.getEarliestServiceDate(), null,
                    CommissionStatus.OLD, CommissionStatusRules.KEEP_OLD, false);
            return;
        }
        persistRow(booking, vin, when,
                new CommissionDecision(CommissionStatus.FAILED, reason, null, detail),
                null, existing);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommissionCustomerStatus persistDecision(Booking booking, String vin, OffsetDateTime bookingTime,
                                                    CommissionDecision decision, HyperVinSnapshot snapshot,
                                                    CommissionCustomerStatus current) {
        return persistRow(booking, vin, bookingTime, decision, snapshot, current);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditOnly(Booking booking, String vin, OffsetDateTime bookingTime, LocalDate serviceDate,
                          HyperVinSnapshot snapshot, String status, String reason, boolean review) {
        audit(booking, vin, bookingTime, serviceDate, snapshot, status, reason, review);
    }

    private CommissionCustomerStatus persistRow(Booking booking, String vin, OffsetDateTime bookingTime,
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
            if (CommissionStatus.NEW.equals(nextStatus)
                    || CommissionStatus.FAILED.equals(nextStatus)
                    || CommissionStatus.DASH.equals(nextStatus)) {
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
            if (vin == null) {
                audit(booking, null, bookingTime, decision.serviceDate(), snapshot, nextStatus, reason, false);
                return null;
            }
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
        boolean review = CommissionStatusRules.FUTURE_DATE.equals(decision.outcome())
                || CommissionStatus.FAILED.equals(nextStatus)
                || CommissionStatus.DASH.equals(nextStatus);
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
