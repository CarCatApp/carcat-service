package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.DeviceToken;
import com.carland.carland_service.entity.Notification;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.DeviceTokenRepository;
import com.carland.carland_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
    @Mock NotificationRepository notificationRepository;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks BookingPushService service;

    @BeforeEach
    void inboxSaveRunsInItsOwnTransaction() {
        lenient().when(transactionManager.getTransaction(any()))
                .thenReturn(new SimpleTransactionStatus(true));
    }

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

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(saved.capture());
        List<Notification> rows = saved.getAllValues();
        for (int i = 0; i < rows.size(); i++) {
            assertInbox(rows.get(i), BookingPushService.TYPE_ACCEPTED, title.getAllValues().get(i), body.getAllValues().get(i));
        }
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

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(saved.capture());
        List<Notification> rows = saved.getAllValues();
        for (int i = 0; i < rows.size(); i++) {
            assertInbox(rows.get(i), BookingPushService.TYPE_REJECTED, title.getAllValues().get(i), body.getAllValues().get(i));
        }
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
    void missingTokenSkipsSendButKeepsInboxRow() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(null);
        when(customerRepository.findByUserId(5L)).thenReturn(customer("az"));

        assertDoesNotThrow(() -> service.accepted(sample(null)));

        verify(pushNotificationService, never()).send(any(), any(), any());
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(saved.capture());
        assertInbox(saved.getValue(), BookingPushService.TYPE_ACCEPTED,
                "Rezervasiya təsdiqləndi",
                "Nərimanov filialı 12 okt, 14:30 üçün rezervasiyanızı qəbul etdi. CC-104821");
    }

    @Test
    void missingCustomerSkipsSend() {
        Booking booking = sample(null);
        booking.setCustomerUserId(null);

        service.rejected(booking);

        verifyNoInteractions(pushNotificationService, deviceTokenRepository, notificationRepository);
    }

    @Test
    void fcmFailureDoesNotEscape() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("az"));
        doThrow(new RuntimeException("fcm")).when(pushNotificationService).send(any(), any(), any());

        assertDoesNotThrow(() -> service.accepted(sample(null)));
        assertDoesNotThrow(() -> service.rejected(sample("yer yoxdur")));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(saved.capture());
        assertEquals(BookingPushService.TYPE_ACCEPTED, saved.getAllValues().get(0).getType());
        assertEquals(BookingPushService.TYPE_REJECTED, saved.getAllValues().get(1).getType());
        assertEquals("ACTIVE", saved.getAllValues().get(0).getStatus());
        assertEquals("ACTIVE", saved.getAllValues().get(1).getStatus());
    }

    @Test
    void sendWaitsUntilAfterCommit() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("en"));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.accepted(sample(null));
            verify(pushNotificationService, never()).send(any(), any(), any());
            verify(notificationRepository, never()).save(any());

            doThrow(new RuntimeException("fcm")).when(pushNotificationService).send(any(), any(), any());
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                assertDoesNotThrow(sync::afterCommit);
            }
            verify(notificationRepository).save(any(Notification.class));
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

    private static void assertInbox(Notification row, String type, String title, String text) {
        assertNotNull(row.getCreatedAt());
        assertEquals(ZoneOffset.ofHours(4), row.getCreatedAt().getOffset());
        assertEquals(row.getCreatedAt().toLocalDate(), row.getCreated());
        assertTrue(Math.abs(Duration.between(row.getCreatedAt(), Notification.nowLocal()).toSeconds()) < 10);
        assertEquals(5L, row.getCustomerId());
        assertEquals(text, row.getNotificationText());
        assertEquals(title, row.getTitle());
        assertEquals("ACTIVE", row.getStatus());
        assertFalse(row.isRead());
        assertEquals(type, row.getType());
    }
}
