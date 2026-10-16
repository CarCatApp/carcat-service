package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.StaffSlotCapacityRequest;
import com.carland.carland_service.dto.request.StaffSlotDayHiddenRequest;
import com.carland.carland_service.dto.request.StaffSlotHiddenRequest;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.enums.RangeStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.AppointmentRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CalendarRepository;
import com.carland.carland_service.repository.RangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Panelden yer sayısı, saat gizleme ve seçili hizmetin gününü gizleme.
 * en: Place count, hour hide, and hiding one service's day from the panel.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffSlotEditService {

    static final int CAPACITY_MAX = 50;

    private final BookingStaffAccess bookingStaffAccess;
    private final RangeRepository rangeRepository;
    private final CalendarRepository calendarRepository;
    private final BookingRepository bookingRepository;
    private final BookingCapacityService bookingCapacityService;
    private final AppointmentRepository appointmentRepository;
    private final BookingPushService bookingPushService;

    /**
     * tr: Yer sayısını bir değiştirir. Dolu yerin altına inmez.
     * en: Changes the place count by one. It cannot fall below occupied places.
     */
    @Transactional
    public void capacity(Long userId, boolean mustChangePassword, Long slotId, StaffSlotCapacityRequest body,
                         String acceptLanguage) {
        String lang = lang(acceptLanguage);
        if (slotId == null || body == null || body.getDelta() == null) {
            throw new MissingFieldException(sentence(lang, "Yer sayısını göndərin.",
                    "Send the place change.", "Отправьте изменение числа мест."));
        }
        if (body.getDelta() != 1 && body.getDelta() != -1) {
            throw new ConflictException(sentence(lang, "Yer sayını bir-bir dəyişin.",
                    "Change the place count by one.", "Меняйте число мест по одному."));
        }
        Range range = locked(userId, mustChangePassword, slotId, lang);
        int occupying = occupying(range);
        int current = range.getWorkerCount() == null ? 0 : range.getWorkerCount();
        int next = current + body.getDelta();
        int floor = Math.max(1, occupying);
        if (next > CAPACITY_MAX || next < 1) {
            throw new ConflictException(sentence(lang, "Yer sayı 1 ilə 50 arasında olmalıdır.",
                    "Places must be between 1 and 50.", "Число мест должно быть от 1 до 50."));
        }
        if (next < floor) {
            throw new ConflictException(sentence(lang, "Yer sayı dolu rezervasiyadan az ola bilməz.",
                    "Places cannot be fewer than the accepted reservations.",
                    "Мест не может быть меньше принятых броней."));
        }
        range.setWorkerCount(next);
        bookingCapacityService.closePendingWhenFull(range);
    }

    /**
     * tr: Saati gizlər və ya açır. Günün gizlisi durur.
     * en: Hides or reopens the hour. The day's hide flag stays.
     */
    @Transactional
    public void hidden(Long userId, boolean mustChangePassword, Long slotId, StaffSlotHiddenRequest body,
                       String acceptLanguage) {
        String lang = lang(acceptLanguage);
        if (slotId == null || body == null || body.getHidden() == null) {
            throw new MissingFieldException(sentence(lang, "Saatın vəziyyətini göndərin.",
                    "Send the hour state.", "Отправьте состояние часа."));
        }
        Range range = locked(userId, mustChangePassword, slotId, lang);
        range.setStatus(Boolean.TRUE.equals(body.getHidden())
                ? RangeStatus.HIDDEN.name()
                : RangeStatus.AVAILABLE.name());
    }

    /**
     * tr: Seçili hizmetin o günkü saatlerini gizler. Saatların öz durumu durur.
     * en: Hides that service's hours on the day. Each hour's own status stays.
     */
    @Transactional
    public void dayHidden(Long userId, boolean mustChangePassword, StaffSlotDayHiddenRequest body,
                          String acceptLanguage) {
        String lang = lang(acceptLanguage);
        if (body == null || body.getHidden() == null || body.getDay() == null || body.getDay().isBlank()) {
            throw new MissingFieldException(sentence(lang, "Günü və vəziyyəti göndərin.",
                    "Send the day and the state.", "Отправьте день и состояние."));
        }
        int picked = (body.getPackageId() != null ? 1 : 0)
                + (body.getIndividualServiceId() != null ? 1 : 0)
                + (Boolean.TRUE.equals(body.getRepairInspection()) ? 1 : 0);
        if (picked != 1) {
            throw new MissingFieldException(sentence(lang, "Bir xidmət seçin.",
                    "Select one service.", "Выберите одну услугу."));
        }
        LocalDate day;
        try {
            day = LocalDate.parse(body.getDay().trim());
        } catch (Exception ex) {
            throw new MissingFieldException(sentence(lang, "Tarix seçin.", "Select a date.", "Выберите дату."));
        }
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, lang);
        Branch branch = branch(staff, body.getBranchId(), lang);
        List<Range> matched = new ArrayList<>();
        for (Calendar calendar : calendarRepository.findByBranchIdAndDayBetween(branch.getId(), day, day)) {
            if (calendar.getTimeRanges() == null) {
                continue;
            }
            for (Range range : calendar.getTimeRanges()) {
                if (!deleted(range) && sameTarget(range, body)) {
                    matched.add(range);
                }
            }
        }
        if (matched.isEmpty()) {
            throw new ResourceNotFoundException(sentence(lang, "Bu gün üçün slot yoxdur.",
                    "There is no slot for this day.", "На этот день слота нет."));
        }
        boolean hidden = Boolean.TRUE.equals(body.getHidden());
        for (Range range : matched) {
            range.setDayHidden(hidden);
        }
    }

    /**
     * tr: Günün takvimini siler. Aktiv rezervasiyalar "calendar_deleted" ilə rədd edilir. Rezervasiyası olan gün
     *     gizli silinir (saatlar DELETED, sətirlər durur ki köhnə rezervasiyaların vaxtı itməsin); heç rezervasiyası
     *     olmayan gün tam silinir.
     * en: Deletes the day's calendar. Active bookings are rejected with "calendar_deleted". A day with bookings is
     *     soft-deleted (hours DELETED, rows stay so old bookings keep their time); a day that never had bookings is
     *     removed completely.
     */
    @Transactional
    public void deleteCalendar(Long userId, boolean mustChangePassword, Long calendarId, String acceptLanguage) {
        String lang = lang(acceptLanguage);
        if (calendarId == null) {
            throw new MissingFieldException(sentence(lang, "Təqvimi seçin.",
                    "Select the calendar.", "Выберите календарь."));
        }
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, lang);
        Calendar calendar = calendarRepository.findById(calendarId)
                .filter(c -> c.getBranch() != null)
                .orElseThrow(() -> new ResourceNotFoundException(sentence(lang, "Təqvim tapılmadı.",
                        "Calendar was not found.", "Календарь не найден.")));
        bookingStaffAccess.requireWritableBranch(staff, calendar.getBranch().getId(), lang);
        List<Long> rangeIds = rangeRepository.findIdsByCalendarId(calendarId);
        if (rangeIds.isEmpty()) {
            calendarRepository.delete(calendar);
            return;
        }
        List<Range> ranges = new ArrayList<>();
        for (Long rangeId : rangeIds) {
            rangeRepository.lockByRangeId(rangeId).ifPresent(ranges::add);
        }
        List<Booking> active = bookingRepository.findByRange_RangeIdInAndStatusIn(
                rangeIds, BookingStatus.holdingCarOnRange());
        if (active != null) {
            for (Booking booking : active) {
                booking.setStatus(BookingStatus.REJECTED.apiValue());
                booking.setCancelReasonCode(BookingSystemNotes.CALENDAR_DELETED);
                booking.setCancelNote(BookingSystemNotes.text(BookingSystemNotes.CALENDAR_DELETED, "az"));
                notifyRejected(booking);
            }
        }
        if (!bookingRepository.existsByRange_RangeIdIn(rangeIds)
                && !appointmentRepository.existsByRange_RangeIdIn(rangeIds)) {
            calendarRepository.delete(calendar);
            return;
        }
        for (Range range : ranges) {
            range.setStatus(RangeStatus.DELETED.name());
        }
    }

    /**
     * tr: Push xətası təqvim silinəndə yazılan rəddi geri almır.
     * en: A push failure does not undo the rejection written when the calendar is deleted.
     */
    private void notifyRejected(Booking booking) {
        try {
            bookingPushService.rejected(booking);
        } catch (Exception ex) {
            log.warn("booking calendar-deleted push skipped id={}", booking == null ? null : booking.getId());
        }
    }

    private static boolean deleted(Range range) {
        return RangeStatus.DELETED.name().equals(range.getStatus());
    }

    private Range locked(Long userId, boolean mustChangePassword, Long slotId, String lang) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, lang);
        Range range = rangeRepository.lockByRangeId(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(sentence(lang, "Slot tapılmadı.",
                        "Slot was not found.", "Слот не найден.")));
        Calendar calendar = range.getCalendar();
        if (calendar == null || calendar.getBranch() == null) {
            throw new ResourceNotFoundException(sentence(lang, "Slot tapılmadı.",
                    "Slot was not found.", "Слот не найден."));
        }
        bookingStaffAccess.requireWritableBranch(staff, calendar.getBranch().getId(), lang);
        if (deleted(range)) {
            throw new ResourceNotFoundException(sentence(lang, "Slot tapılmadı.",
                    "Slot was not found.", "Слот не найден."));
        }
        return range;
    }

    private Branch branch(BookingStaff staff, Long branchId, String lang) {
        if (branchId != null) {
            return bookingStaffAccess.requireWritableBranch(staff, branchId, lang);
        }
        if (staff.getBranch() != null) {
            return bookingStaffAccess.requireWritableBranch(staff, staff.getBranch().getId(), lang);
        }
        throw new MissingFieldException(sentence(lang, "Şöbə seçin.", "Select a branch.", "Выберите филиал."));
    }

    private int occupying(Range range) {
        if (range.getRangeId() == null) {
            return 0;
        }
        return (int) bookingRepository.countByRange_RangeIdAndStatusIn(
                range.getRangeId(), BookingStatus.occupyingCapacity());
    }

    private static boolean sameTarget(Range range, StaffSlotDayHiddenRequest body) {
        if (body.getPackageId() != null) {
            return StaffSlotTargets.PACKAGE.equals(range.getSlotTarget())
                    && range.getCarePackage() != null
                    && body.getPackageId().equals(range.getCarePackage().getId());
        }
        if (body.getIndividualServiceId() != null) {
            return StaffSlotTargets.INDIVIDUAL.equals(range.getSlotTarget())
                    && range.getIndividualService() != null
                    && body.getIndividualServiceId().equals(range.getIndividualService().getId());
        }
        return Boolean.TRUE.equals(body.getRepairInspection())
                && StaffSlotTargets.REPAIR_INSPECTION.equals(range.getSlotTarget());
    }

    private static String lang(String header) {
        return StaffSlotGenerateService.lang(header);
    }

    private static String sentence(String lang, String az, String en, String ru) {
        if ("en".equals(lang)) {
            return en;
        }
        if ("ru".equals(lang)) {
            return ru;
        }
        return az;
    }
}
