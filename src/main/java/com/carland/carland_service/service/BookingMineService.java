package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingMineResponse;
import com.carland.carland_service.dto.booking.BookingView;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingItem;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingMode;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.BookingItemRepository;
import com.carland.carland_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * tr: Owner rezervasyon listesi (CRCT-285 mine). unread=0 ta ki 286.
 * en: Owner booking list (CRCT-285 mine). unread=0 until 286.
 */
@Service
@RequiredArgsConstructor
public class BookingMineService {

    static final String DEFAULT_TZ = "Asia/Baku";
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter STARTS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;

    @Transactional(readOnly = true)
    public BookingMineResponse mine(Long customerUserId, String statusCsv, Long carId,
                                    Integer page, Integer pageSize, Integer limit, String timezoneHeader) {
        if (customerUserId == null) {
            throw MissingFieldException.required("X-User-Id");
        }
        String timezone = timezoneHeader == null || timezoneHeader.isBlank() ? DEFAULT_TZ : timezoneHeader.trim();
        int safePage = page == null || page < 1 ? 1 : page;
        int size = limit != null ? limit : (pageSize == null ? 20 : pageSize);
        if (size < 1) {
            size = 20;
        }
        size = Math.min(size, 50);
        PageRequest pageable = PageRequest.of(safePage - 1, size, Sort.by("range.start").ascending());
        List<String> statuses = parseStatuses(statusCsv);
        Page<Booking> result = loadPage(customerUserId, carId, statuses, pageable);
        Map<Long, List<String>> keys = keysByBooking(result.getContent());
        List<BookingView> items = new ArrayList<>();
        for (Booking booking : result.getContent()) {
            items.add(toView(booking, keys.getOrDefault(booking.getId(), List.of()), timezone));
        }
        return BookingMineResponse.builder()
                .counts(countsOf(customerUserId, carId))
                .page(safePage)
                .pageSize(size)
                .total(result.getTotalElements())
                .items(items)
                .build();
    }

    private Page<Booking> loadPage(Long userId, Long carId, List<String> statuses, PageRequest pageable) {
        boolean filterStatus = !statuses.isEmpty();
        if (carId != null && filterStatus) {
            return bookingRepository.findByCustomerUserIdAndCarIdAndStatusIn(userId, carId, statuses, pageable);
        }
        if (carId != null) {
            return bookingRepository.findByCustomerUserIdAndCarId(userId, carId, pageable);
        }
        if (filterStatus) {
            return bookingRepository.findByCustomerUserIdAndStatusIn(userId, statuses, pageable);
        }
        return bookingRepository.findByCustomerUserId(userId, pageable);
    }

    private Map<String, Long> countsOf(Long userId, Long carId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            counts.put(status.apiValue(), 0L);
        }
        List<Object[]> rows = carId == null
                ? bookingRepository.countGroupByStatus(userId)
                : bookingRepository.countGroupByStatusAndCarId(userId, carId);
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String status = String.valueOf(row[0]);
            long n = row[1] instanceof Number number ? number.longValue() : 0L;
            counts.put(status, n);
        }
        return counts;
    }

    static List<String> parseStatuses(String statusCsv) {
        if (statusCsv == null || statusCsv.isBlank()) {
            return List.of();
        }
        Set<String> wanted = new LinkedHashSet<>();
        for (String raw : statusCsv.split(",")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String value = raw.trim().toLowerCase();
            if ("canceled".equals(value)) {
                value = BookingStatus.CANCELLED.apiValue();
            }
            wanted.add(value);
        }
        if (wanted.contains(BookingStatus.CONFIRMED.apiValue())) {
            wanted.add(BookingStatus.AUTO_ACCEPTED.apiValue());
        }
        return List.copyOf(wanted);
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
        Branch branch = booking.getBranch();
        Partner partner = branch == null ? null : branch.getPartner();
        String mode = range == null || range.getBookingMode() == null || range.getBookingMode().isBlank()
                ? BookingMode.INSTANT.apiValue() : range.getBookingMode();
        return BookingView.builder()
                .bookingId(booking.getId())
                .ref(booking.getRef())
                .status(booking.getStatus())
                .bookingMode(mode)
                .branchId(branch == null ? null : branch.getId())
                .branchName(branch == null ? null : branch.getName())
                .partnerName(partner == null ? null : partner.getName())
                .slotId(range == null ? null : range.getRangeId())
                .day(calendar == null || calendar.getDay() == null ? null : calendar.getDay().toString())
                .start(clock(range == null ? null : range.getStart(), timezone))
                .end(clock(range == null ? null : range.getEnd(), timezone))
                .startsAt(startsAt(range == null ? null : range.getStart(), timezone))
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

    private static String startsAt(OffsetDateTime utc, String timezone) {
        if (utc == null) {
            return null;
        }
        return utc.atZoneSameInstant(ZoneId.of(timezone)).format(STARTS);
    }
}
