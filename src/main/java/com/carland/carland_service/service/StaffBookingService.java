package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingInboxResponse;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingItem;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * tr: Staff gelen istekler: inbox, kabul, ret. Flag'e bağlı değil.
 * en: Staff inbox / accept / reject. Not gated by the owner booking flag.
 */
@Service
@RequiredArgsConstructor
public class StaffBookingService {

    static final String DEFAULT_TZ = "Asia/Baku";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingStaffAccess bookingStaffAccess;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;

    @Transactional(readOnly = true)
    public BookingInboxResponse inbox(Long userId, boolean mustChangePassword, String status, Long branchId,
                                      Integer page, Integer pageSize, String timezone, String acceptLanguage) {
        BookingStaff staff = requireStaff(userId, mustChangePassword, acceptLanguage);
        String wanted = status == null || status.isBlank() ? BookingStatus.PENDING.apiValue() : status.trim().toLowerCase();
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        PageRequest pageable = PageRequest.of(safePage - 1, safeSize);
        Page<Booking> result;
        if (branchId != null) {
            bookingStaffAccess.requireWritableBranch(staff, branchId, acceptLanguage);
            result = bookingRepository.findByBranch_IdAndStatusOrderByCreatedAtDesc(branchId, wanted, pageable);
        } else if (BookingStaffRole.BRANCH_ADMIN.name().equals(staff.getRole())) {
            if (staff.getBranch() == null) {
                throw new ForbiddenException("branch required");
            }
            result = bookingRepository.findByBranch_IdAndStatusOrderByCreatedAtDesc(
                    staff.getBranch().getId(), wanted, pageable);
        } else {
            result = bookingRepository.findByBranch_Partner_IdAndStatusOrderByCreatedAtDesc(
                    staff.getPartner().getId(), wanted, pageable);
        }
        List<Booking> rows = result.getContent();
        Map<Long, List<String>> keys = keysByBooking(rows);
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone.trim();
        List<BookingView> items = new ArrayList<>();
        for (Booking booking : rows) {
            items.add(toView(booking, keys.getOrDefault(booking.getId(), List.of()), tz));
        }
        return BookingInboxResponse.builder()
                .page(safePage)
                .pageSize(safeSize)
                .items(items)
                .build();
    }

    @Transactional
    public BookingView accept(Long userId, boolean mustChangePassword, Long bookingId, String timezone,
                              String acceptLanguage) {
        return decide(userId, mustChangePassword, bookingId, BookingStatus.CONFIRMED.apiValue(), timezone, acceptLanguage);
    }

    @Transactional
    public BookingView reject(Long userId, boolean mustChangePassword, Long bookingId, String timezone,
                              String acceptLanguage) {
        return decide(userId, mustChangePassword, bookingId, BookingStatus.REJECTED.apiValue(), timezone, acceptLanguage);
    }

    private BookingView decide(Long userId, boolean mustChangePassword, Long bookingId, String nextStatus,
                               String timezone, String acceptLanguage) {
        BookingStaff staff = requireStaff(userId, mustChangePassword, acceptLanguage);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("booking not found"));
        bookingStaffAccess.requireWritableBranch(staff, booking.getBranch().getId(), acceptLanguage);
        if (!BookingStatus.PENDING.apiValue().equals(booking.getStatus())) {
            throw new ConflictException("booking is not pending");
        }
        booking.setStatus(nextStatus);
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone.trim();
        List<String> keys = bookingItemRepository.findByBooking_IdOrderByIdAsc(bookingId).stream()
                .map(BookingItem::getServiceKey)
                .toList();
        return toView(booking, keys, tz);
    }

    private BookingStaff requireStaff(Long userId, boolean mustChangePassword, String acceptLanguage) {
        if (userId == null) {
            throw new ForbiddenException("Staff token required");
        }
        if (mustChangePassword) {
            throw new ForbiddenException("Şifrəni dəyişdirməlisiniz");
        }
        String lang = acceptLanguage == null ? "az" : acceptLanguage;
        return bookingStaffAccess.requireActive(userId, lang);
    }

    private Map<Long, List<String>> keysByBooking(List<Booking> rows) {
        List<Long> ids = rows.stream().map(Booking::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return bookingItemRepository.findByBooking_IdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        item -> item.getBooking().getId(),
                        Collectors.mapping(BookingItem::getServiceKey, Collectors.toList())
                ));
    }

    private BookingView toView(Booking booking, List<String> keys, String timezone) {
        Range range = booking.getRange();
        Calendar calendar = range == null ? null : range.getCalendar();
        String mode = range == null || range.getBookingMode() == null || range.getBookingMode().isBlank()
                ? BookingMode.INSTANT.apiValue() : range.getBookingMode();
        return BookingView.builder()
                .bookingId(booking.getId())
                .ref(booking.getRef())
                .status(booking.getStatus())
                .bookingMode(mode)
                .branchId(booking.getBranch() == null ? null : booking.getBranch().getId())
                .slotId(range == null ? null : range.getRangeId())
                .day(calendar == null || calendar.getDay() == null ? null : calendar.getDay().toString())
                .start(clock(range == null ? null : range.getStart(), timezone))
                .end(clock(range == null ? null : range.getEnd(), timezone))
                .timezone(timezone)
                .vin(booking.getVin())
                .carId(booking.getCarId())
                .serviceKeys(keys)
                .priceMin(booking.getPriceMin())
                .priceMax(booking.getPriceMax())
                .currency(booking.getCurrency() == null ? "AZN" : booking.getCurrency())
                .unit(BookingCreateService.UNIT)
                .unreadCount(0)
                .build();
    }

    private static String clock(OffsetDateTime utc, String timezone) {
        if (utc == null) {
            return null;
        }
        return utc.atZoneSameInstant(ZoneId.of(timezone)).toLocalTime().format(CLOCK);
    }
}
