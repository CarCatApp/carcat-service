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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * tr: Qəbul, rədd, ləğv və tamamlamadan sonra eyni title/body-ni bildiriş siyahısına yazır və müştərinin cihaz tokeninə FCM göndərir. Commitdən sonra işləyir; xəta statusu geri almır.
 * en: After accept, reject, cancel, or complete, writes the same title/body into the notification list and sends FCM to the customer's device token. Runs after commit; a failure does not undo the status.
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
    static final String TYPE_NO_SHOW = "BOOKING_NO_SHOW";
    static final String TYPE_CANCELLED = "BOOKING_CANCELLED";
    static final String TYPE_COMPLETED = "BOOKING_COMPLETED";

    private final DeviceTokenRepository deviceTokenRepository;
    private final CustomerRepository customerRepository;
    private final PushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * tr: Təsdiqlənmiş rezervasiya üçün push planlayır.
     * en: Schedules the push for a confirmed booking.
     */
    public void accepted(Booking booking) {
        schedule(booking, Kind.ACCEPTED);
    }

    /**
     * tr: Rədd edilmiş rezervasiya üçün push planlayır. Şube notu varsa body sonuna əlavə olunur.
     * en: Schedules the push for a rejected booking. A branch note is appended to the body when present.
     */
    public void rejected(Booking booking) {
        schedule(booking, Kind.REJECTED);
    }

    /** Staff marked the customer as not arrived. */
    public void noShow(Booking booking) {
        schedule(booking, Kind.NO_SHOW);
    }

    /** Customer cancelled their own booking. */
    public void cancelled(Booking booking) {
        schedule(booking, Kind.CANCELLED);
    }

    /** Staff marked the booking completed. */
    public void completed(Booking booking) {
        schedule(booking, Kind.COMPLETED);
    }

    private void schedule(Booking booking, Kind kind) {
        try {
            Notice notice = noticeOf(booking, kind);
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
        String heading = title(lang, notice.kind());
        String text = body(lang, notice);
        saveInbox(notice, heading, text);
        sendPush(notice, heading, text);
    }

    private void saveInbox(Notice notice, String heading, String text) {
        String type = switch (notice.kind()) {
            case ACCEPTED -> TYPE_ACCEPTED;
            case REJECTED -> TYPE_REJECTED;
            case NO_SHOW -> TYPE_NO_SHOW;
            case CANCELLED -> TYPE_CANCELLED;
            case COMPLETED -> TYPE_COMPLETED;
        };
        OffsetDateTime createdAt = Notification.nowLocal();
        try {
            TransactionTemplate write = new TransactionTemplate(transactionManager);
            write.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            write.executeWithoutResult(status -> notificationRepository.save(Notification.builder()
                    .created(createdAt.toLocalDate())
                    .createdAt(createdAt)
                    .customerId(notice.customerUserId())
                    .notificationText(text)
                    .title(heading)
                    .status("ACTIVE")
                    .isRead(false)
                    .type(type)
                    .build()));
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

    private static Notice noticeOf(Booking booking, Kind kind) {
        if (booking == null || booking.getCustomerUserId() == null) {
            return null;
        }
        Branch branch = booking.getBranch();
        String branchName = branch == null || branch.getName() == null ? "" : branch.getName().trim();
        Range range = booking.getRange();
        OffsetDateTime start = range == null ? null : range.getStart();
        String ref = booking.getRef() == null ? "" : booking.getRef().trim();
        String note = kind == Kind.REJECTED ? booking.getCancelNote() : null;
        return new Notice(booking.getCustomerUserId(), branchName, start, ref, note, kind);
    }

    private static String title(String lang, Kind kind) {
        return switch (kind) {
            case ACCEPTED -> switch (lang) {
                case "en" -> "Booking confirmed";
                case "ru" -> "Запись подтверждена";
                default -> "Rezervasiya təsdiqləndi";
            };
            case REJECTED -> switch (lang) {
                case "en" -> "Appointment request cancelled";
                case "ru" -> "Запрос на запись отменён";
                default -> "Növbə sorğusu ləğv edildi";
            };
            case NO_SHOW, CANCELLED -> switch (lang) {
                case "en" -> "Appointment cancelled";
                case "ru" -> "Запись отменена";
                default -> "Növbə ləğv edildi";
            };
            case COMPLETED -> switch (lang) {
                case "en" -> "Appointment completed";
                case "ru" -> "Запись завершена";
                default -> "Növbə tamamlandı";
            };
        };
    }

    private static String body(String lang, Notice notice) {
        String when = whenText(notice.startUtc(), lang);
        return switch (notice.kind()) {
            case ACCEPTED -> acceptBody(lang, notice.branchName(), when, notice.ref());
            case REJECTED -> rejectBody(lang, notice.branchName(), notice.note());
            case NO_SHOW -> noShowBody(lang, notice.branchName());
            case CANCELLED -> cancelBody(lang, notice.branchName());
            case COMPLETED -> completeBody(lang, notice.branchName());
        };
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

    /** {service_name}: Növbə sorğunuz ləğv edildi. Səbəb: {reason}. */
    private static String rejectBody(String lang, String serviceName, String reason) {
        String sentence = switch (lang) {
            case "en" -> serviceName + ": Your appointment request was cancelled.";
            case "ru" -> serviceName + ": Ваш запрос на запись отменён.";
            default -> serviceName + ": Növbə sorğunuz ləğv edildi.";
        };
        if (reason == null || reason.isBlank()) {
            return sentence;
        }
        String label = switch (lang) {
            case "en" -> "Reason";
            case "ru" -> "Причина";
            default -> "Səbəb";
        };
        return sentence + " " + label + ": " + reason.trim() + ".";
    }

    /** {service_name}: Təyin edilmiş vaxtda yaxınlaşmadığınız üçün növbəniz ləğv edildi (No-show). */
    private static String noShowBody(String lang, String serviceName) {
        return switch (lang) {
            case "en" -> serviceName + ": Your appointment was cancelled because you did not arrive at the scheduled time (No-show).";
            case "ru" -> serviceName + ": Ваша запись отменена, так как вы не пришли в назначенное время (No-show).";
            default -> serviceName + ": Təyin edilmiş vaxtda yaxınlaşmadığınız üçün növbəniz ləğv edildi (No-show).";
        };
    }

    /** {service_name}: Növbəniz uğurla tamamlandı. */
    private static String completeBody(String lang, String serviceName) {
        return switch (lang) {
            case "en" -> serviceName + ": Your appointment was completed.";
            case "ru" -> serviceName + ": Ваша запись успешно завершена.";
            default -> serviceName + ": Növbəniz uğurla tamamlandı.";
        };
    }

    /** {service_name}: Növbəniz uğurla ləğv edildi. İstədiyiniz vaxt yenidən növbə götürə bilərsiniz. */
    private static String cancelBody(String lang, String serviceName) {
        return switch (lang) {
            case "en" -> serviceName + ": Your appointment was cancelled. You can book again whenever you like.";
            case "ru" -> serviceName + ": Ваша запись успешно отменена. Вы можете записаться снова в любое удобное время.";
            default -> serviceName + ": Növbəniz uğurla ləğv edildi. İstədiyiniz vaxt yenidən növbə götürə bilərsiniz.";
        };
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

    private enum Kind {
        ACCEPTED, REJECTED, NO_SHOW, CANCELLED, COMPLETED
    }

    private record Notice(
            Long customerUserId,
            String branchName,
            OffsetDateTime startUtc,
            String ref,
            String note,
            Kind kind
    ) {}
}
