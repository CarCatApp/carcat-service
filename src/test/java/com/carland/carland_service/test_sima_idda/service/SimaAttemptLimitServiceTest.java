package com.carland.carland_service.test_sima_idda.service;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.repository.SimaKycRecordRepository;
import com.carland.carland_service.test_sima_idda.config.SimaIddaProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimaAttemptLimitServiceTest {

    @Mock SimaKycRecordRepository repository;
    @Mock SimaIddaProperties properties;

    @Test
    void resetTimestamps_countOnlyRowsAfterCutoff() {
        when(properties.getKycTimezone()).thenReturn("Asia/Baku");
        Customer customer = Customer.builder()
                .userId(1L)
                .simaDailyLimitResetAt(LocalDateTime.now().plusMinutes(1))
                .simaEverLimitResetAt(LocalDateTime.of(2026, 9, 1, 0, 0))
                .build();
        SimaAttemptLimitService service = new SimaAttemptLimitService(repository, properties);
        when(repository.countByCustomerAndChannelInAndCreatedAtAfter(eq(customer), eq(List.of("CITIZEN", "FOREIGN")), eq(customer.getSimaEverLimitResetAt())))
                .thenReturn(1L);
        when(repository.countByCustomerAndChannelInAndVerifiedFalseAndCreatedAtAfter(eq(customer), eq(List.of("CITIZEN", "FOREIGN")), eq(customer.getSimaDailyLimitResetAt())))
                .thenReturn(0L);

        service.countTotal(customer);
        service.countDailyFails(customer);

        verify(repository).countByCustomerAndChannelInAndCreatedAtAfter(customer, List.of("CITIZEN", "FOREIGN"), customer.getSimaEverLimitResetAt());
        verify(repository).countByCustomerAndChannelInAndVerifiedFalseAndCreatedAtAfter(customer, List.of("CITIZEN", "FOREIGN"), customer.getSimaDailyLimitResetAt());
    }
}
