package com.carland.carland_service.service.commission;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.CommissionCustomerStatus;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CommissionCustomerStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;

/**
 * tr: İlk CarCat randevusunda VIN'e Hyper sorar ve durumu bir kez yazar.
 * en: Asks Hyper on the first CarCat booking and writes the status once.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionCustomerStatusService {

    private final CommissionCustomerStatusRepository statusRepository;
    private final BookingRepository bookingRepository;
    private final HyperVinLookup hyperVinLookup;
    private final ObjectProvider<CommissionCustomerStatusService> self;

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

    @Transactional
    public CommissionCustomerStatus ensure(Booking booking) {
        if (booking == null) {
            return null;
        }
        String vin = CommissionVin.normalize(booking.getVin());
        if (vin == null) {
            return null;
        }
        CommissionCustomerStatus existing = statusRepository.findByVin(vin).orElse(null);
        if (existing != null) {
            return existing;
        }
        CommissionDecision decision = CommissionStatusRules.decide(
                CommissionClock.bakuDate(booking.getCreatedAt()),
                hyperVinLookup.lookup(vin));
        CommissionCustomerStatus row = CommissionCustomerStatus.builder()
                .vin(vin)
                .status(decision.status())
                .decidedAt(OffsetDateTime.now(CommissionClock.BAKU))
                .firstBookingId(booking.getId())
                .hyperOutcome(decision.outcome())
                .earliestServiceDate(decision.earliestServiceDate())
                .snapshot(trim(decision.snapshot()))
                .build();
        try {
            return statusRepository.saveAndFlush(row);
        } catch (DataIntegrityViolationException ex) {
            return statusRepository.findByVin(vin).orElseThrow(() -> ex);
        }
    }

    private static String trim(String snapshot) {
        if (snapshot == null) {
            return null;
        }
        return snapshot.length() <= 2000 ? snapshot : snapshot.substring(0, 2000);
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
