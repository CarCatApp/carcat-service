package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * tr: GEÇİCİ. Hyper ziyarətinin məbləğini rezervasiyaya yazır. Eyni gün və şöbədə ən erkən sətir siyahını daşıyır.
 *     Ekran yalnız tamamlanmış sətirdə oxuyur.
 * en: TEMPORARY. Writes a Hyper visit amount onto the booking. The earliest row of that day and branch carries the list.
 *     The screen reads it only on a completed row.
 */
@Service
@RequiredArgsConstructor
public class BookingPaidAmountService {

    private static final String PANEL = "panel";

    private final BookingRepository bookingRepository;
    private final VisitRepository visitRepository;

    /**
     * tr: Webhook ziyarəti yazılandan sonra həmin günü yenidən qurur.
     * en: Rebuilds that day after a webhook visit is saved.
     */
    @Transactional
    public void syncVisit(Visit visit) {
        if (visit == null || visit.getBookingId() != null || visit.getLastServiceDate() == null) {
            return;
        }
        String vin = visit.getCar() == null ? null : visit.getCar().getVin();
        syncHyperDay(vin, visit.getLastServiceDate(), ServicedBadgeIds.resolvedBranchId(visit));
    }

    /**
     * tr: Rezervasiya tamamlananda. Panel ziyarəti varsa onun məbləği, yoxsa həmin günün Hyper siyahısı.
     * en: When a booking is completed. The panel visit amount when one exists, otherwise that day's Hyper list.
     */
    @Transactional
    public void syncBooking(Booking booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }
        List<Visit> panel = visitRepository.findPanelByBookingId(booking.getId());
        if (!panel.isEmpty()) {
            booking.setPaidAmounts(amountsOf(panel));
            bookingRepository.save(booking);
            return;
        }
        Long branchId = booking.getBranch() == null ? null : booking.getBranch().getId();
        syncHyperDay(booking.getVin(), dayOf(booking), branchId);
    }

    @Transactional
    public void syncHyperDay(String vin, LocalDate day, Long branchId) {
        if (vin == null || vin.isBlank() || day == null || branchId == null) {
            return;
        }
        List<Booking> bookings = new ArrayList<>(bookingRepository.findByBranchVinAndDay(branchId, vin, day));
        bookings.removeIf(booking -> !visitRepository.findPanelByBookingId(booking.getId()).isEmpty());
        bookings.sort(BY_START);
        if (bookings.isEmpty()) {
            return;
        }
        List<BigDecimal> amounts = amountsOf(hyperVisits(vin, day, branchId));
        bookings.get(0).setPaidAmounts(amounts.isEmpty() ? null : amounts);
        for (int i = 1; i < bookings.size(); i++) {
            bookings.get(i).setPaidAmounts(null);
        }
        bookingRepository.saveAll(bookings);
    }

    private List<Visit> hyperVisits(String vin, LocalDate day, Long branchId) {
        List<Visit> matched = new ArrayList<>();
        for (Visit visit : visitRepository.findUnlinkedHyperByVinAndDay(vin, day)) {
            if (branchId.equals(ServicedBadgeIds.resolvedBranchId(visit))) {
                matched.add(visit);
            }
        }
        return matched;
    }

    static List<BigDecimal> amountsOf(List<Visit> visits) {
        List<BigDecimal> amounts = new ArrayList<>();
        if (visits == null) {
            return amounts;
        }
        for (Visit visit : visits) {
            BigDecimal amount = amountOf(visit);
            if (amount != null) {
                amounts.add(amount);
            }
        }
        return amounts;
    }

    static BigDecimal amountOf(Visit visit) {
        if (visit == null) {
            return null;
        }
        BigDecimal amount = visit.getFinalCostAmount() != null ? visit.getFinalCostAmount() : visit.getCostAmount();
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate dayOf(Booking booking) {
        Range range = booking.getRange();
        Calendar calendar = range == null ? null : range.getCalendar();
        return calendar == null ? null : calendar.getDay();
    }

    private static final Comparator<Booking> BY_START = (left, right) -> {
        OffsetDateTime leftStart = left.getRange() == null ? null : left.getRange().getStart();
        OffsetDateTime rightStart = right.getRange() == null ? null : right.getRange().getStart();
        if (leftStart == null && rightStart == null) {
            return Long.compare(idOf(left), idOf(right));
        }
        if (leftStart == null) {
            return 1;
        }
        if (rightStart == null) {
            return -1;
        }
        int compared = leftStart.compareTo(rightStart);
        return compared != 0 ? compared : Long.compare(idOf(left), idOf(right));
    };

    private static long idOf(Booking booking) {
        return booking.getId() == null ? Long.MAX_VALUE : booking.getId();
    }
}
