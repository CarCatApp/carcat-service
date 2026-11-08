package com.carland.carland_service.service.commission;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.CommissionCustomerStatus;
import com.carland.carland_service.entity.CommissionStatusAudit;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CommissionCustomerStatusRepository;
import com.carland.carland_service.repository.CommissionStatusAuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommissionCustomerStatusServiceTest {

    private static final String VIN = "1HGBH41JXMN109186";

    @Mock
    private CommissionCustomerStatusRepository statusRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private HyperVinLookup hyperVinLookup;
    @Mock
    private ObjectProvider<CommissionCustomerStatusService> self;
    @Mock
    private CommissionStatusAuditRepository auditRepository;

    @InjectMocks
    private CommissionCustomerStatusService service;

    @Test
    void recordedNewDoesNotCallHyper() {
        CommissionCustomerStatus existing = CommissionCustomerStatus.builder()
                .vin(VIN)
                .status(CommissionStatus.NEW)
                .hyperOutcome(CommissionStatusRules.NOT_FOUND)
                .decidedAt(OffsetDateTime.parse("2026-01-01T00:00:00+04:00"))
                .build();
        when(statusRepository.findByVin(VIN)).thenReturn(Optional.of(existing));

        CommissionCustomerStatus result = service.ensure(booking(VIN));

        assertSame(existing, result);
        verify(hyperVinLookup, never()).lookup(any());
        ArgumentCaptor<CommissionStatusAudit> audit = ArgumentCaptor.forClass(CommissionStatusAudit.class);
        verify(auditRepository).save(audit.capture());
        assertEquals(CommissionStatusRules.ALREADY_NEW, audit.getValue().getReason());
        assertNull(audit.getValue().getRequestText());
    }

    @Test
    void oldVinBecomesNewAfterTwelveMonths() {
        CommissionCustomerStatus existing = CommissionCustomerStatus.builder()
                .vin(VIN)
                .status(CommissionStatus.OLD)
                .hyperOutcome(CommissionStatusRules.FOUND)
                .decidedAt(OffsetDateTime.parse("2026-01-01T00:00:00+04:00"))
                .build();
        when(statusRepository.findByVin(VIN)).thenReturn(Optional.of(existing));
        when(hyperVinLookup.lookup(VIN)).thenReturn(
                HyperVinSnapshot.found(List.of(LocalDate.of(2025, 10, 1)), "old", "{\"exists\":true}"));
        when(statusRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CommissionCustomerStatus result = service.ensure(booking(VIN));

        assertEquals(CommissionStatus.NEW, result.getStatus());
        assertEquals(CommissionStatusRules.INACTIVE_12M, result.getHyperOutcome());
        ArgumentCaptor<CommissionStatusAudit> audit = ArgumentCaptor.forClass(CommissionStatusAudit.class);
        verify(auditRepository).save(audit.capture());
        String request = audit.getValue().getRequestText();
        assertTrue(request.startsWith("GET /partner/v1/vehicles/by-vin/" + VIN + "\nrequest_datetime=2026-11-08T12:00"));
        assertFalse(request.contains("Bearer"));
        assertEquals("{\"exists\":true}", audit.getValue().getResponseText());
    }

    @Test
    void shortVinIsNotSentToHyper() {
        when(statusRepository.findByVin("ABC")).thenReturn(Optional.empty());

        assertNull(service.ensure(booking("ABC")));

        verify(hyperVinLookup, never()).lookup(any());
        verify(statusRepository, never()).saveAndFlush(any());
        ArgumentCaptor<CommissionStatusAudit> audit = ArgumentCaptor.forClass(CommissionStatusAudit.class);
        verify(auditRepository).save(audit.capture());
        assertEquals(CommissionStatusRules.INVALID_VIN, audit.getValue().getReason());
        assertEquals(CommissionStatus.NEW, audit.getValue().getStatus());
    }

    private static Booking booking(String vin) {
        return Booking.builder()
                .id(4L)
                .vin(vin)
                .createdAt(OffsetDateTime.parse("2026-11-08T12:00:00+04:00"))
                .build();
    }
}
