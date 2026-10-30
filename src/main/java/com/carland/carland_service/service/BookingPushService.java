package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.DeviceToken;
import com.carland.carland_service.entity.Notification;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStatus;
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
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * tr: Qəbul, rədd, ləğv, gəlmədi və tamamlamadan sonra eyni title/body-ni bildiriş siyahısına yazır və müştərinin cihaz tokeninə FCM göndərir. Commitdən sonra işləyir; xəta statusu geri almır.
 * en: After accept, reject, cancel, no-show, or complete, writes the same title/body into the notification list and sends FCM to the customer's device token. Runs after commit; a failure does not undo the status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingPushService {

    static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final int REASON_LIMIT = 80;

    static final String TYPE_ACCEPTED = "BOOKING_ACCEPTED";
    static final String TYPE_REJECTED = "BOOKING_REJECTED";
    static final String TYPE_NO_SHOW = "BOOKING_NO_SHOW";
    static final String TYPE_CANCELLED = "BOOKING_CANCELLED";
    static final String TYPE_COMPLETED = "BOOKING_COMPLETED";
    static final String TYPE_REMIND_24H = "BOOKING_REMINDER_24H";
    static final String TYPE_REMIND_3H = "BOOKING_REMINDER_3H";

    private final DeviceTokenRepository deviceTokenRepository;
    private final CustomerRepository customerRepository;
    private final PushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * tr: Təsdiqlənmiş və ya ani qəbul olunmuş rezervasiya üçün push planlayır.
     * en: Schedules the push for a confirmed booking or an instant auto-accepted booking.
     */
    public void accepted(Booking booking) {
        schedule(booking, Kind.ACCEPTED);
    }

    /**
     * tr: Rədd edilmiş rezervasiya üçün push planlayır. Şube notu varsa body-yə səbəb kimi düşür.
     * en: Schedules the push for a rejected booking. A branch note is included as the reason when present.
     */
    public void rejected(Booking booking) {
        schedule(booking, Kind.REJECTED);
    }

    /** Staff marked the customer as not arrived. */
    public void noShow(Booking booking) {
        schedule(booking, Kind.NO_SHOW);
    }

    /**
     * tr: Müştəri ləğvi. Gözləyən sorğu bir mətn, təsdiqlənmiş və ani növbə başqa mətn alır. Status artıq cancelled ola bilər; əvvəlki status ayrıca gəlir.
     * en: Customer cancel. A waiting request uses one sentence, a confirmed or instant booking another. The row may already be cancelled; the previous status is passed in.
     */
    public void cancelled(Booking booking, String statusBefore) {
        String previous = statusBefore == null ? "" : statusBefore.trim().toLowerCase(Locale.ROOT);
        Kind kind = BookingStatus.PENDING.apiValue().equals(previous)
                ? Kind.CANCELLED_REQUEST
                : Kind.CANCELLED_BOOKING;
        schedule(booking, kind);
    }

    /** Staff marked the booking completed. */
    public void completed(Booking booking) {
        schedule(booking, Kind.COMPLETED);
    }

    /**
     * tr: Növbədən təxminən 24 saat əvvəl. Mətn CRCT-409 cədvəlindəki X1-dir.
     * en: About 24 hours before the booking. Copy is X1 from the CRCT-409 table.
     */
    public void remindDayBefore(Booking booking) {
        if (!hasStart(booking)) {
            return;
        }
        schedule(booking, Kind.REMIND_24H);
    }

    /**
     * tr: Növbədən təxminən 3 saat əvvəl. "təxminən" elastik pəncərə üçündür.
     * en: About 3 hours before the booking. "About" matches the elastic window.
     */
    public void remindSoon(Booking booking) {
        if (!hasStart(booking)) {
            return;
        }
        schedule(booking, Kind.REMIND_3H);
    }

    private static boolean hasStart(Booking booking) {
        return booking != null && booking.getRange() != null && booking.getRange().getStart() != null;
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
            case CANCELLED_REQUEST, CANCELLED_BOOKING -> TYPE_CANCELLED;
            case COMPLETED -> TYPE_COMPLETED;
            case REMIND_24H -> TYPE_REMIND_24H;
            case REMIND_3H -> TYPE_REMIND_3H;
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
            pushNotificationService.send(heading, text, row.getDeviceToken().trim(), idsOf(notice));
        } catch (Exception ex) {
            log.warn("booking push failed userId={} ref={} error={}",
                    notice.customerUserId(), notice.ref(), ex.getClass().getSimpleName());
        }
    }

    private static Map<String, String> idsOf(Notice notice) {
        Map<String, String> data = new LinkedHashMap<>();
        if (notice.bookingId() != null) {
            data.put("bookingId", Long.toString(notice.bookingId()));
        }
        if (notice.branchId() != null) {
            data.put("branchId", Long.toString(notice.branchId()));
        }
        return data;
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
        Long branchId = branch == null ? null : branch.getId();
        String branchName = branch == null || branch.getName() == null ? "" : branch.getName().trim();
        Range range = booking.getRange();
        OffsetDateTime start = range == null ? null : range.getStart();
        String ref = booking.getRef() == null ? "" : booking.getRef().trim();
        boolean withReason = kind == Kind.REJECTED || kind == Kind.NO_SHOW;
        String note = withReason ? booking.getCancelNote() : null;
        String reasonCode = withReason ? booking.getCancelReasonCode() : null;
        return new Notice(booking.getId(), booking.getCustomerUserId(), branchId, branchName, start, ref, note, reasonCode, kind);
    }

    private static String title(String lang, Kind kind) {
        return switch (kind) {
            case ACCEPTED -> switch (lang) {
                case "en" -> "Booking confirmed";
                case "ru" -> "Запись подтверждена";
                default -> "Növbə təsdiqləndi";
            };
            case REJECTED -> switch (lang) {
                case "en" -> "Service declined your request";
                case "ru" -> "Сервис отклонил заявку";
                default -> "Servis sorğunu qəbul etmədi";
            };
            case NO_SHOW -> switch (lang) {
                case "en" -> "Service cancelled your booking";
                case "ru" -> "Сервис отменил запись";
                default -> "Servis növbəni ləğv etdi";
            };
            case CANCELLED_REQUEST -> switch (lang) {
                case "en" -> "You cancelled your request";
                case "ru" -> "Вы отменили заявку";
                default -> "Sorğunu ləğv etdiniz";
            };
            case CANCELLED_BOOKING -> switch (lang) {
                case "en" -> "You cancelled your booking";
                case "ru" -> "Вы отменили запись";
                default -> "Növbəni ləğv etdiniz";
            };
            case COMPLETED -> switch (lang) {
                case "en" -> "Service completed";
                case "ru" -> "Услуга выполнена";
                default -> "Xidmət tamamlandı";
            };
            case REMIND_24H, REMIND_3H -> switch (lang) {
                case "en" -> "Reminder";
                case "ru" -> "Напоминание";
                default -> "Xatırlatma";
            };
        };
    }

    private static String body(String lang, Notice notice) {
        String when = whenText(notice.startUtc());
        String reason = BookingSystemNotes.resolve(notice.reasonCode(), notice.note(), lang);
        return switch (notice.kind()) {
            case ACCEPTED -> acceptBody(lang, notice.branchName(), when);
            case REJECTED -> withReason(rejectSentence(lang, notice.branchName()), lang, reason);
            case NO_SHOW -> withReason(noShowSentence(lang, notice.branchName(), when), lang, reason);
            case CANCELLED_REQUEST -> cancelRequestBody(lang, notice.branchName());
            case CANCELLED_BOOKING -> cancelBookingBody(lang, notice.branchName(), when);
            case COMPLETED -> completeBody(lang, notice.branchName());
            case REMIND_24H -> remindDayBody(lang, notice.branchName(), notice.startUtc());
            case REMIND_3H -> remindSoonBody(lang, notice.branchName(), notice.startUtc());
        };
    }

    private static String remindDayBody(String lang, String serviceName, OffsetDateTime start) {
        String date = dateText(start);
        String time = timeText(start);
        return switch (lang) {
            case "en" -> serviceName + ": You have a booking tomorrow. Time: " + date + ", " + time + ". See you soon!";
            case "ru" -> serviceName + ": Завтра у вас запись. Время: " + date + ", " + time + ". Ждём вас!";
            default -> serviceName + ": Sabah növbəniz var. Vaxt: " + date + ", " + time + ". Sizi gözləyirik!";
        };
    }

    private static String remindSoonBody(String lang, String serviceName, OffsetDateTime start) {
        String time = timeText(start);
        return switch (lang) {
            case "en" -> serviceName + ": Your booking is in about 3 hours. Time: today, " + time + ". See you soon!";
            case "ru" -> serviceName + ": До вашей записи осталось примерно 3 часа. Время: сегодня, " + time + ". Ждём вас!";
            default -> serviceName + ": Növbənizə təxminən 3 saat qalıb. Vaxt: bu gün, " + time + ". Sizi gözləyirik!";
        };
    }

    private static String acceptBody(String lang, String branch, String when) {
        String clock = when == null ? "" : switch (lang) {
            case "en" -> " Time: " + when + ".";
            case "ru" -> " Время: " + when + ".";
            default -> " Vaxt: " + when + ".";
        };
        return switch (lang) {
            case "en" -> branch + ": Your booking is confirmed." + clock + " See you soon!";
            case "ru" -> branch + ": Ваша запись подтверждена." + clock + " Ждём вас!";
            default -> branch + ": Növbəniz təsdiqləndi." + clock + " Sizi gözləyirik!";
        };
    }

    private static String rejectSentence(String lang, String serviceName) {
        return switch (lang) {
            case "en" -> serviceName + ": The service declined your booking request.";
            case "ru" -> serviceName + ": Сервис отклонил вашу заявку на запись.";
            default -> serviceName + ": Növbə sorğunuz servis tərəfindən qəbul edilmədi.";
        };
    }

    private static String noShowSentence(String lang, String serviceName, String when) {
        if (when == null) {
            return switch (lang) {
                case "en" -> serviceName + ": Your booking was cancelled by the service.";
                case "ru" -> serviceName + ": Ваша запись отменена сервисом.";
                default -> serviceName + ": Növbəniz servis tərəfindən ləğv edildi.";
            };
        }
        return switch (lang) {
            case "en" -> serviceName + ": Your booking (" + when + ") was cancelled by the service.";
            case "ru" -> serviceName + ": Ваша запись (" + when + ") отменена сервисом.";
            default -> serviceName + ": Növbəniz (" + when + ") servis tərəfindən ləğv edildi.";
        };
    }

    private static String completeBody(String lang, String serviceName) {
        return switch (lang) {
            case "en" -> serviceName + ": Your service is complete. Thank you for choosing us! You can see the work done in your service history.";
            case "ru" -> serviceName + ": Услуга выполнена. Спасибо, что выбрали нас! Выполненные работы можно посмотреть в истории обслуживания.";
            default -> serviceName + ": Xidmət tamamlandı. Bizi seçdiyiniz üçün təşəkkür edirik! Görülən işlərə servis tarixçəsində baxa bilərsiniz.";
        };
    }

    private static String cancelRequestBody(String lang, String serviceName) {
        return switch (lang) {
            case "en" -> serviceName + ": You cancelled your booking request. You can book again at any time.";
            case "ru" -> serviceName + ": Вы отменили заявку на запись. Вы можете записаться снова в любое время.";
            default -> serviceName + ": Növbə sorğunuz sizin tərəfinizdən ləğv edildi. İstədiyiniz vaxt yenidən növbə götürə bilərsiniz.";
        };
    }

    private static String cancelBookingBody(String lang, String serviceName, String when) {
        if (when == null) {
            return switch (lang) {
                case "en" -> serviceName + ": You cancelled your booking. If needed, you can choose a new time.";
                case "ru" -> serviceName + ": Вы отменили запись. При необходимости вы можете выбрать новое время.";
                default -> serviceName + ": Növbəniz sizin tərəfinizdən ləğv edildi. Ehtiyac olarsa, yeni vaxt seçə bilərsiniz.";
            };
        }
        return switch (lang) {
            case "en" -> serviceName + ": You cancelled your booking (" + when + "). If needed, you can choose a new time.";
            case "ru" -> serviceName + ": Вы отменили запись (" + when + "). При необходимости вы можете выбрать новое время.";
            default -> serviceName + ": Növbəniz (" + when + ") sizin tərəfinizdən ləğv edildi. Ehtiyac olarsa, yeni vaxt seçə bilərsiniz.";
        };
    }

    private static String withReason(String sentence, String lang, String reason) {
        String clipped = clipReason(reason);
        if (clipped == null) {
            return sentence;
        }
        String label = switch (lang) {
            case "en" -> "Reason";
            case "ru" -> "Причина";
            default -> "Səbəb";
        };
        return sentence + " " + label + ": " + clipped + ".";
    }

    static String clipReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int count = trimmed.codePointCount(0, trimmed.length());
        if (count <= REASON_LIMIT) {
            return trimmed;
        }
        int end = trimmed.offsetByCodePoints(0, REASON_LIMIT - 3);
        return trimmed.substring(0, end) + "...";
    }

    private static String whenText(OffsetDateTime utc) {
        if (utc == null) {
            return null;
        }
        return dateText(utc) + ", " + timeText(utc);
    }

    private static String dateText(OffsetDateTime utc) {
        if (utc == null) {
            return "";
        }
        return DATE.format(utc.atZoneSameInstant(BAKU));
    }

    private static String timeText(OffsetDateTime utc) {
        if (utc == null) {
            return "";
        }
        return CLOCK.format(utc.atZoneSameInstant(BAKU));
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
        ACCEPTED, REJECTED, NO_SHOW, CANCELLED_REQUEST, CANCELLED_BOOKING, COMPLETED, REMIND_24H, REMIND_3H
    }

    private record Notice(
            Long bookingId,
            Long customerUserId,
            Long branchId,
            String branchName,
            OffsetDateTime startUtc,
            String ref,
            String note,
            String reasonCode,
            Kind kind
    ) {}
}
