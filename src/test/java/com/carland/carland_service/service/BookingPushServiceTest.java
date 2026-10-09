package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.DeviceToken;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.DeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPushServiceTest {

    @Mock DeviceTokenRepository deviceTokenRepository;
    @Mock CustomerRepository customerRepository;
    @Mock PushNotificationService pushNotificationService;
    @InjectMocks BookingPushService service;

    @Test
    void acceptedCopyFollowsCustomerLanguage() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(
                customer("az"), customer("en"), customer("ru-RU"));

        service.accepted(sample(null));
        service.accepted(sample(null));
        service.accepted(sample(null));

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(pushNotificationService, times(3)).send(title.capture(), body.capture(), eq("tok-1"));
        assertEquals(List.of(
                "Rezervasiya təsdiqləndi",
                "Booking confirmed",
                "Запись подтверждена"), title.getAllValues());
        assertEquals(List.of(
                "Nərimanov filialı 12 okt, 14:30 üçün rezervasiyanızı qəbul etdi. CC-104821",
                "Nərimanov branch accepted your booking for 12 Oct, 14:30. CC-104821",
                "Филиал Nərimanov принял вашу запись на 12 окт, 14:30. CC-104821"), body.getAllValues());
    }

    @Test
    void rejectedCopyFollowsCustomerLanguageAndKeepsTheBranchNote() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(
                customer("az"), customer("en"), customer("ru"));

        service.rejected(sample("Bu gün yer yoxdur"));
        service.rejected(sample("Bu gün yer yoxdur"));
        service.rejected(sample(null));

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(pushNotificationService, times(3)).send(title.capture(), body.capture(), eq("tok-1"));
        assertEquals(List.of(
                "Rezervasiya rədd edildi",
                "Booking declined",
                "Запись отклонена"), title.getAllValues());
        assertEquals(List.of(
                "Nərimanov filialı 12 okt, 14:30 rezervasiyanızı rədd etdi. Səbəb: Bu gün yer yoxdur",
                "Nərimanov branch declined your booking for 12 Oct, 14:30. Reason: Bu gün yer yoxdur",
                "Филиал Nərimanov отклонил вашу запись на 12 окт, 14:30."), body.getAllValues());
    }

    @Test
    void unknownLanguageFallsBackToAz() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("de"));

        service.accepted(sample(null));

        verify(pushNotificationService).send(
                eq("Rezervasiya təsdiqləndi"),
                eq("Nərimanov filialı 12 okt, 14:30 üçün rezervasiyanızı qəbul etdi. CC-104821"),
                eq("tok-1"));
    }

    @Test
    void missingTokenSkipsSend() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(null);

        assertDoesNotThrow(() -> service.accepted(sample(null)));

        verify(pushNotificationService, never()).send(any(), any(), any());
        verify(customerRepository, never()).findByUserId(any());
    }

    @Test
    void missingCustomerSkipsSend() {
        Booking booking = sample(null);
        booking.setCustomerUserId(null);

        service.rejected(booking);

        verifyNoInteractions(pushNotificationService, deviceTokenRepository);
    }

    @Test
    void fcmFailureDoesNotEscape() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("az"));
        doThrow(new RuntimeException("fcm")).when(pushNotificationService).send(any(), any(), any());

        assertDoesNotThrow(() -> service.accepted(sample(null)));
        assertDoesNotThrow(() -> service.rejected(sample("yer yoxdur")));
    }

    @Test
    void sendWaitsUntilAfterCommit() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("en"));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.accepted(sample(null));
            verify(pushNotificationService, never()).send(any(), any(), any());

            doThrow(new RuntimeException("fcm")).when(pushNotificationService).send(any(), any(), any());
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                assertDoesNotThrow(sync::afterCommit);
            }
            verify(pushNotificationService).send(
                    eq("Booking confirmed"),
                    eq("Nərimanov branch accepted your booking for 12 Oct, 14:30. CC-104821"),
                    eq("tok-1"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static Booking sample(String note) {
        Branch branch = Branch.builder().id(1L).name("Nərimanov").build();
        Range range = Range.builder()
                .rangeId(1L)
                .start(OffsetDateTime.parse("2026-10-12T10:30:00Z"))
                .build();
        return Booking.builder()
                .id(4L)
                .ref("CC-104821")
                .customerUserId(5L)
                .branch(branch)
                .range(range)
                .cancelNote(note)
                .status("pending")
                .build();
    }

    private static Customer customer(String language) {
        return Customer.builder().userId(5L).notificationLanguage(language).build();
    }

    private static DeviceToken token() {
        return DeviceToken.builder().userId(5L).deviceToken("tok-1").platform("android").build();
    }
}
