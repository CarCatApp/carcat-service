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
import java.util.Map;

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

    private static final Map<String, String> PUSH_IDS = Map.of("bookingId", "4", "branchId", "1");

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
        verify(pushNotificationService, times(3)).send(title.capture(), body.capture(), eq("tok-1"), eq(PUSH_IDS));
        assertEquals(List.of(
                "Növbə təsdiqləndi",
                "Booking confirmed",
                "Запись подтверждена"), title.getAllValues());
        assertEquals(List.of(
                "Nərimanov: Növbəniz təsdiqləndi. Vaxt: 12.10.2026, 14:30. Sizi gözləyirik!",
                "Nərimanov: Your booking is confirmed. Time: 12.10.2026, 14:30. See you soon!",
                "Nərimanov: Ваша запись подтверждена. Время: 12.10.2026, 14:30. Ждём вас!"), body.getAllValues());

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
        verify(pushNotificationService, times(3)).send(title.capture(), body.capture(), eq("tok-1"), eq(PUSH_IDS));
        assertEquals(List.of(
                "Servis sorğunu qəbul etmədi",
                "Service declined your request",
                "Сервис отклонил заявку"), title.getAllValues());
        assertEquals(List.of(
                "Nərimanov: Növbə sorğunuz servis tərəfindən qəbul edilmədi. Səbəb: Bu gün yer yoxdur.",
                "Nərimanov: The service declined your booking request. Reason: Bu gün yer yoxdur.",
                "Nərimanov: Сервис отклонил вашу заявку на запись."), body.getAllValues());

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(saved.capture());
        List<Notification> rows = saved.getAllValues();
        for (int i = 0; i < rows.size(); i++) {
            assertInbox(rows.get(i), BookingPushService.TYPE_REJECTED, title.getAllValues().get(i), body.getAllValues().get(i));
        }
    }

    @Test
    void noShowAndCancelCopyFollowCustomerLanguage() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("az"), customer("en"), customer("ru"));

        service.noShow(sample("Müştəri təyin olunmuş vaxtda gəlmədi"));
        service.cancelled(sample(null), "pending");
        service.cancelled(confirmed(), "auto_accepted");

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(pushNotificationService, times(3)).send(title.capture(), body.capture(), eq("tok-1"), eq(PUSH_IDS));
        assertEquals(List.of(
                "Servis növbəni ləğv etdi",
                "You cancelled your request",
                "Вы отменили запись"), title.getAllValues());
        assertEquals(List.of(
                "Nərimanov: Növbəniz (12.10.2026, 14:30) servis tərəfindən ləğv edildi. Səbəb: Müştəri təyin olunmuş vaxtda gəlmədi.",
                "Nərimanov: You cancelled your booking request. You can book again at any time.",
                "Nərimanov: Вы отменили запись (12.10.2026, 14:30). При необходимости вы можете выбрать новое время."
        ), body.getAllValues());

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(saved.capture());
        assertEquals(BookingPushService.TYPE_NO_SHOW, saved.getAllValues().get(0).getType());
        assertEquals(BookingPushService.TYPE_CANCELLED, saved.getAllValues().get(1).getType());
        assertEquals(BookingPushService.TYPE_CANCELLED, saved.getAllValues().get(2).getType());
    }

    @Test
    void longReasonIsCutAtEightyCharacters() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("az"));
        String reason = "a".repeat(81);

        service.rejected(sample(reason));

        String shown = "a".repeat(77) + "...";
        verify(pushNotificationService).send(
                eq("Servis sorğunu qəbul etmədi"),
                eq("Nərimanov: Növbə sorğunuz servis tərəfindən qəbul edilmədi. Səbəb: " + shown + "."),
                eq("tok-1"), eq(PUSH_IDS));
    }

    @Test
    void completedCopyFollowsCustomerLanguage() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("az"), customer("en"), customer("ru"));

        service.completed(sample(null));
        service.completed(sample(null));
        service.completed(sample("ignored"));

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(pushNotificationService, times(3)).send(title.capture(), body.capture(), eq("tok-1"), eq(PUSH_IDS));
        assertEquals(List.of(
                "Xidmət tamamlandı",
                "Service completed",
                "Услуга выполнена"), title.getAllValues());
        assertEquals(List.of(
                "Nərimanov: Xidmət tamamlandı. Bizi seçdiyiniz üçün təşəkkür edirik! Görülən işlərə servis tarixçəsində baxa bilərsiniz.",
                "Nərimanov: Your service is complete. Thank you for choosing us! You can see the work done in your service history.",
                "Nərimanov: Услуга выполнена. Спасибо, что выбрали нас! Выполненные работы можно посмотреть в истории обслуживания."), body.getAllValues());

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(saved.capture());
        List<Notification> rows = saved.getAllValues();
        for (int i = 0; i < rows.size(); i++) {
            assertInbox(rows.get(i), BookingPushService.TYPE_COMPLETED, title.getAllValues().get(i), body.getAllValues().get(i));
        }
    }

    @Test
    void unknownLanguageFallsBackToAz() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("de"));

        service.accepted(sample(null));

        verify(pushNotificationService).send(
                eq("Növbə təsdiqləndi"),
                eq("Nərimanov: Növbəniz təsdiqləndi. Vaxt: 12.10.2026, 14:30. Sizi gözləyirik!"),
                eq("tok-1"), eq(PUSH_IDS));
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
                "Növbə təsdiqləndi",
                "Nərimanov: Növbəniz təsdiqləndi. Vaxt: 12.10.2026, 14:30. Sizi gözləyirik!");
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
        doThrow(new RuntimeException("fcm")).when(pushNotificationService).send(any(), any(), any(), any());

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
    void reminderCopyFollowsCustomerLanguage() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(
                customer("az"), customer("en"), customer("ru"),
                customer("az"), customer("en"), customer("ru"));

        service.remindDayBefore(confirmed());
        service.remindDayBefore(confirmed());
        service.remindDayBefore(confirmed());
        service.remindSoon(confirmed());
        service.remindSoon(confirmed());
        service.remindSoon(confirmed());

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(pushNotificationService, times(6)).send(title.capture(), body.capture(), eq("tok-1"), eq(PUSH_IDS));
        assertEquals(List.of(
                "Xatırlatma", "Reminder", "Напоминание",
                "Xatırlatma", "Reminder", "Напоминание"), title.getAllValues());
        assertEquals(List.of(
                "Nərimanov: Sabah növbəniz var. Vaxt: 12.10.2026, 14:30. Sizi gözləyirik!",
                "Nərimanov: You have a booking tomorrow. Time: 12.10.2026, 14:30. See you soon!",
                "Nərimanov: Завтра у вас запись. Время: 12.10.2026, 14:30. Ждём вас!",
                "Nərimanov: Növbənizə təxminən 3 saat qalıb. Vaxt: bu gün, 14:30. Sizi gözləyirik!",
                "Nərimanov: Your booking is in about 3 hours. Time: today, 14:30. See you soon!",
                "Nərimanov: До вашей записи осталось примерно 3 часа. Время: сегодня, 14:30. Ждём вас!"),
                body.getAllValues());

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(6)).save(saved.capture());
        List<Notification> rows = saved.getAllValues();
        assertInbox(rows.get(0), BookingPushService.TYPE_REMIND_24H, title.getAllValues().get(0), body.getAllValues().get(0));
        assertInbox(rows.get(3), BookingPushService.TYPE_REMIND_3H, title.getAllValues().get(3), body.getAllValues().get(3));
    }

    @Test
    void reminderWithoutStartDoesNotSend() {
        Booking booking = confirmed();
        booking.setRange(null);
        service.remindDayBefore(booking);
        service.remindSoon(booking);
        verifyNoInteractions(pushNotificationService);
    }

    @Test
    void sendWaitsUntilAfterCommit() {
        when(deviceTokenRepository.findByUserId(5L)).thenReturn(token());
        when(customerRepository.findByUserId(5L)).thenReturn(customer("en"));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.accepted(sample(null));
            verify(pushNotificationService, never()).send(any(), any(), any(), any());
            verify(notificationRepository, never()).save(any());

            doThrow(new RuntimeException("fcm")).when(pushNotificationService).send(any(), any(), any(), any());
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                assertDoesNotThrow(sync::afterCommit);
            }
            verify(notificationRepository).save(any(Notification.class));
            verify(pushNotificationService).send(
                    eq("Booking confirmed"),
                    eq("Nərimanov: Your booking is confirmed. Time: 12.10.2026, 14:30. See you soon!"),
                    eq("tok-1"), eq(PUSH_IDS));
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

    private static Booking confirmed() {
        Booking booking = sample(null);
        booking.setStatus("auto_accepted");
        return booking;
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
