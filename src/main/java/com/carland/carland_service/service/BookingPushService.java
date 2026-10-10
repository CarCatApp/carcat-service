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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * tr: Qəbul və rədddən sonra eyni title/body-ni bildiriş siyahısına yazır və müştərinin cihaz tokeninə FCM göndərir. Commitdən sonra işləyir; xəta statusu geri almır.
 * en: After accept or reject, writes the same title/body into the notification list and sends FCM to the customer's device token. Runs after commit; a failure does not undo the status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingPushService {

    static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private static final Map<String, String[]> MONTHS = Map.of(
            "az", new String[] {"yan", "fev", "mar", "apr", "may", "iyn", "iyl", "avq", "sen", "okt", "noy", "dek"},
            "en", new String[] {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"},
            "ru", new String[] {"янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"}
    );

    static final String TYPE_ACCEPTED = "BOOKING_ACCEPTED";
    static final String TYPE_REJECTED = "BOOKING_REJECTED";

    private final DeviceTokenRepository deviceTokenRepository;
    private final CustomerRepository customerRepository;
    private final PushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;

    /**
     * tr: Təsdiqlənmiş rezervasiya üçün push planlayır.
     * en: Schedules the push for a confirmed booking.
     */
    public void accepted(Booking booking) {
        schedule(booking, true);
    }

    /**
     * tr: Rədd edilmiş rezervasiya üçün push planlayır. Şube notu varsa body sonuna əlavə olunur.
     * en: Schedules the push for a rejected booking. A branch note is appended to the body when present.
     */
    public void rejected(Booking booking) {
        schedule(booking, false);
    }

    private void schedule(Booking booking, boolean accepted) {
        try {
            Notice notice = noticeOf(booking, accepted);
            if (notice == null) {
                return;
            }
            runAfterCommit(() -> deliver(notice));
        } catch (Exception ex) {
            log.warn("booking push schedule skipped id={}", booking == null ? null : booking.getId());
        }
    }

    private void deliver(Notice notice) {
        String lang = "az";
        try {
            lang = languageOf(notice.customerUserId());
        } catch (Exception ex) {
            log.warn("booking language lookup failed userId={} ref={} error={}",
                    notice.customerUserId(), notice.ref(), ex.getClass().getSimpleName());
        }
        String heading = title(lang, notice.accepted());
        String text = body(lang, notice);
        saveInbox(notice, heading, text);
        sendPush(notice, heading, text);
    }

    private void saveInbox(Notice notice, String heading, String text) {
        try {
            notificationRepository.save(Notification.builder()
                    .created(LocalDate.now())
                    .customerId(notice.customerUserId())
                    .notificationText(text)
                    .title(heading)
                    .status("ACTIVE")
                    .isRead(false)
                    .type(notice.accepted() ? TYPE_ACCEPTED : TYPE_REJECTED)
                    .build());
        } catch (Exception ex) {
            log.warn("booking inbox save failed userId={} ref={} error={}",
                    notice.customerUserId(), notice.ref(), ex.getClass().getSimpleName());
        }
    }

    private void sendPush(Notice notice, String heading, String text) {
        try {
            DeviceToken row = deviceTokenRepository.findByUserId(notice.customerUserId());
            if (row == null || row.getDeviceToken() == null || row.getDeviceToken().isBlank()) {
                log.info("booking push skipped, no device token userId={} ref={}",
                        notice.customerUserId(), notice.ref());
                return;
            }
            pushNotificationService.send(heading, text, row.getDeviceToken().trim());
        } catch (Exception ex) {
            log.warn("booking push failed userId={} ref={} error={}",
                    notice.customerUserId(), notice.ref(), ex.getClass().getSimpleName());
        }
    }

    private String languageOf(Long userId) {
        Customer customer = customerRepository.findByUserId(userId);
        if (customer == null) {
            return "az";
        }
        String lang = BookingMineService.langOf(customer.getNotificationLanguage());
        if ("en".equals(lang) || "ru".equals(lang)) {
            return lang;
        }
        return "az";
    }

    private static Notice noticeOf(Booking booking, boolean accepted) {
        if (booking == null || booking.getCustomerUserId() == null) {
            return null;
        }
        Branch branch = booking.getBranch();
        String branchName = branch == null || branch.getName() == null ? "" : branch.getName().trim();
        Range range = booking.getRange();
        OffsetDateTime start = range == null ? null : range.getStart();
        String ref = booking.getRef() == null ? "" : booking.getRef().trim();
        String note = accepted ? null : booking.getCancelNote();
        return new Notice(booking.getCustomerUserId(), branchName, start, ref, note, accepted);
    }

    private static String title(String lang, boolean accepted) {
        if (accepted) {
            return switch (lang) {
                case "en" -> "Booking confirmed";
                case "ru" -> "Запись подтверждена";
                default -> "Rezervasiya təsdiqləndi";
            };
        }
        return switch (lang) {
            case "en" -> "Booking declined";
            case "ru" -> "Запись отклонена";
            default -> "Rezervasiya rədd edildi";
        };
    }

    private static String body(String lang, Notice notice) {
        String when = whenText(notice.startUtc(), lang);
        String text = notice.accepted()
                ? acceptBody(lang, notice.branchName(), when, notice.ref())
                : rejectBody(lang, notice.branchName(), when);
        if (!notice.accepted()) {
            text = withReason(lang, text, notice.note());
        }
        return text;
    }

    private static String acceptBody(String lang, String branch, String when, String ref) {
        String sentence = switch (lang) {
            case "en" -> when == null
                    ? branch + " branch accepted your booking."
                    : branch + " branch accepted your booking for " + when + ".";
            case "ru" -> when == null
                    ? "Филиал " + branch + " принял вашу запись."
                    : "Филиал " + branch + " принял вашу запись на " + when + ".";
            default -> when == null
                    ? branch + " filialı rezervasiyanızı qəbul etdi."
                    : branch + " filialı " + when + " üçün rezervasiyanızı qəbul etdi.";
        };
        if (ref == null || ref.isBlank()) {
            return sentence;
        }
        return sentence + " " + ref;
    }

    private static String rejectBody(String lang, String branch, String when) {
        return switch (lang) {
            case "en" -> when == null
                    ? branch + " branch declined your booking."
                    : branch + " branch declined your booking for " + when + ".";
            case "ru" -> when == null
                    ? "Филиал " + branch + " отклонил вашу запись."
                    : "Филиал " + branch + " отклонил вашу запись на " + when + ".";
            default -> when == null
                    ? branch + " filialı rezervasiyanızı rədd etdi."
                    : branch + " filialı " + when + " rezervasiyanızı rədd etdi.";
        };
    }

    private static String withReason(String lang, String sentence, String note) {
        if (note == null || note.isBlank()) {
            return sentence;
        }
        String label = switch (lang) {
            case "en" -> "Reason";
            case "ru" -> "Причина";
            default -> "Səbəb";
        };
        return sentence + " " + label + ": " + note.trim();
    }

    private static String whenText(OffsetDateTime utc, String lang) {
        if (utc == null) {
            return null;
        }
        ZonedDateTime zoned = utc.atZoneSameInstant(BAKU);
        String[] months = MONTHS.getOrDefault(lang, MONTHS.get("az"));
        String month = months[zoned.getMonthValue() - 1];
        return zoned.getDayOfMonth() + " " + month + ", "
                + String.format("%02d:%02d", zoned.getHour(), zoned.getMinute());
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

    private record Notice(
            Long customerUserId,
            String branchName,
            OffsetDateTime startUtc,
            String ref,
            String note,
            boolean accepted
    ) {}
}
