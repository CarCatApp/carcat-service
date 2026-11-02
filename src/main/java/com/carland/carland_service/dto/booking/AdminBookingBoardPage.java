package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Admin tahtası. Sayaclar bütün aktiv şubelerin gerçek toplamıdır. items bir sayfadır.
 * en: Admin board. Counters are real totals across active branches. items is one page.
 */
@Data
@Builder
public class AdminBookingBoardPage {
    long pending;
    long ready;
    long completed;
    long rejected;
    int page;
    int pageSize;
    long total;
    int totalPages;
    List<BookingView> items;
}
