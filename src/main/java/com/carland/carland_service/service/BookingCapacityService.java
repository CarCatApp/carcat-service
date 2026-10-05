package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.enums.BookingStatus;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * tr: Yeri yalnız qəbul edilmiş rezervasiya tutur. Yer dolunca gözləyənlər rədd olunur.
 * en: Only an accepted booking takes a place. Waiting requests are rejected once the places are full.
 */
@Service
@RequiredArgsConstructor
public class BookingCapacityService {

    public static final String PLACES_FULL = "places_full";

    private final BookingRepository bookingRepository;

    /**
     * tr: Qəbul sayı yerə çatıbsa yeni qəbul olmaz.
     * en: A new acceptance is refused once accepted bookings fill the range.
     */
    public void requirePlace(Range range, String acceptLanguage) {
        if (accepted(range) >= capacity(range)) {
            throw new ConflictException(note(acceptLanguage));
        }
    }

    /**
     * tr: Qəbul sayı yerə çatıbsa eyni range-dəki gözləyənləri rədd edir.
     * en: Rejects the other waiting bookings on this range once accepted bookings fill it.
     */
    public void closePendingWhenFull(Range range) {
        if (range == null || range.getRangeId() == null || accepted(range) < capacity(range)) {
            return;
        }
        List<Booking> waiting = bookingRepository.findByRange_RangeIdAndStatus(
                range.getRangeId(), BookingStatus.PENDING.apiValue());
        if (waiting == null) {
            return;
        }
        for (Booking row : waiting) {
            row.setStatus(BookingStatus.REJECTED.apiValue());
            row.setCancelReasonCode(PLACES_FULL);
            row.setCancelNote(note("az"));
        }
    }

    public static String note(String acceptLanguage) {
        String lang = acceptLanguage == null ? "" : acceptLanguage.toLowerCase(Locale.ROOT);
        if (lang.startsWith("en")) {
            return "We were unable to accept your reservation because all places are taken.";
        }
        if (lang.startsWith("ru")) {
            return "Мы не смогли принять вашу бронь, поскольку все места заняты.";
        }
        return "Yerlər dolduğu üçün rezervasiyanızı qəbul edə bilmədik.";
    }

    private long accepted(Range range) {
        if (range == null || range.getRangeId() == null) {
            return 0;
        }
        return bookingRepository.countByRange_RangeIdAndStatusIn(
                range.getRangeId(), BookingStatus.occupyingCapacity());
    }

    private static int capacity(Range range) {
        if (range == null || range.getWorkerCount() == null) {
            return 0;
        }
        return range.getWorkerCount();
    }
}
