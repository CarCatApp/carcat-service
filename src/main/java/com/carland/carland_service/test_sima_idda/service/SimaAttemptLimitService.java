package com.carland.carland_service.test_sima_idda.service;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.SimaKycRecord;
import com.carland.carland_service.repository.SimaKycRecordRepository;
import com.carland.carland_service.test_sima_idda.config.SimaIddaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * SIMA attempt limits are counts of {@code sima_kyc_records}, not stored counters.
 * Admin reset sets a timestamp; rows created at or before that instant stay, but do not count.
 */
@Service
@RequiredArgsConstructor
public class SimaAttemptLimitService {

    public static final List<String> ATTEMPT_CHANNELS = List.of("CITIZEN", "FOREIGN");

    private final SimaKycRecordRepository simaKycRecordRepository;
    private final SimaIddaProperties simaIddaProperties;

    public int dailyLimit() {
        return simaIddaProperties.getKycDailyFailLimit();
    }

    public int totalLimit() {
        return simaIddaProperties.getKycTotalAttemptLimit();
    }

    public long countTotal(Customer customer) {
        LocalDateTime resetAt = customer.getSimaEverLimitResetAt();
        if (resetAt == null) {
            return simaKycRecordRepository.countByCustomerAndChannelIn(customer, ATTEMPT_CHANNELS);
        }
        return simaKycRecordRepository.countByCustomerAndChannelInAndCreatedAtAfter(
                customer, ATTEMPT_CHANNELS, resetAt);
    }

    public long countDailyFails(Customer customer) {
        LocalDateTime startOfToday = startOfToday();
        LocalDateTime resetAt = customer.getSimaDailyLimitResetAt();
        if (resetAt != null && resetAt.isAfter(startOfToday)) {
            return simaKycRecordRepository.countByCustomerAndChannelInAndVerifiedFalseAndCreatedAtAfter(
                    customer, ATTEMPT_CHANNELS, resetAt);
        }
        return simaKycRecordRepository.countByCustomerAndChannelInAndVerifiedFalseAndCreatedAtGreaterThanEqual(
                customer, ATTEMPT_CHANNELS, startOfToday);
    }

    public List<SimaKycRecord> listAttempts(Customer customer) {
        return simaKycRecordRepository.findByCustomerAndChannelInOrderByCreatedAtAsc(
                customer, ATTEMPT_CHANNELS);
    }

    @Transactional
    public void resetDaily(Customer customer) {
        customer.setSimaDailyLimitResetAt(now());
    }

    @Transactional
    public void resetEver(Customer customer) {
        customer.setSimaEverLimitResetAt(now());
    }

    public LocalDateTime now() {
        return LocalDateTime.now(zone());
    }

    private LocalDateTime startOfToday() {
        return LocalDate.now(zone()).atStartOfDay();
    }

    private ZoneId zone() {
        String zone = simaIddaProperties.getKycTimezone();
        return ZoneId.of(zone == null || zone.isBlank() ? "Asia/Baku" : zone);
    }
}
