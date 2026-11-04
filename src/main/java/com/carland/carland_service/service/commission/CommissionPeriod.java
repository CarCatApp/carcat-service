package com.carland.carland_service.service.commission;

import java.time.LocalDate;

/**
 * tr: Rapor aralığı. Her iki gün dahil. preset boşsa bu ay.
 * en: Report range. Both days included. A blank preset is the current month.
 */
public record CommissionPeriod(LocalDate from, LocalDate to, String preset) {

    public static CommissionPeriod resolve(String preset, LocalDate from, LocalDate to, LocalDate today) {
        LocalDate day = today == null ? LocalDate.now(CommissionClock.BAKU) : today;
        if (from != null || to != null) {
            LocalDate start = from == null ? to : from;
            LocalDate end = to == null ? from : to;
            if (end.isBefore(start)) {
                LocalDate swap = start;
                start = end;
                end = swap;
            }
            return new CommissionPeriod(start, end, "custom");
        }
        String chosen = preset == null || preset.isBlank() ? "month" : preset;
        return switch (chosen) {
            case "prev" -> {
                LocalDate last = day.withDayOfMonth(1).minusDays(1);
                yield new CommissionPeriod(last.withDayOfMonth(1), last, "prev");
            }
            case "d7" -> new CommissionPeriod(day.minusDays(6), day, "d7");
            case "d30" -> new CommissionPeriod(day.minusDays(29), day, "d30");
            default -> {
                LocalDate first = day.withDayOfMonth(1);
                yield new CommissionPeriod(first, first.plusMonths(1).minusDays(1), "month");
            }
        };
    }
}
