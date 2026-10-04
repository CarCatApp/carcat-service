package com.carland.carland_service.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Bir günün çalışma aralığını tam dilimlere böler. Öğle arası ve günün geçmiş saatleri düşer.
 * en: Splits one day's working window into whole slices. Lunch and hours already past are dropped.
 */
public final class StaffSlotWindows {

    public static final ZoneId ZONE = ZoneId.of("Asia/Baku");

    private StaffSlotWindows() {
    }

    public record Slice(OffsetDateTime start, OffsetDateTime end) {
    }

    public static List<Slice> slices(LocalDate day, LocalTime start, LocalTime end, int durationMin,
                                     LocalTime breakStart, LocalTime breakEnd, OffsetDateTime now) {
        List<Slice> out = new ArrayList<>();
        if (durationMin <= 0 || start == null || end == null || !start.isBefore(end)) {
            return out;
        }
        int cursor = minutes(start);
        int limit = minutes(end);
        Integer breakFrom = breakStart == null || breakEnd == null || !breakStart.isBefore(breakEnd)
                ? null : minutes(breakStart);
        Integer breakTo = breakFrom == null ? null : minutes(breakEnd);
        while (cursor + durationMin <= limit) {
            int sliceEnd = cursor + durationMin;
            if (breakFrom != null && overlaps(cursor, sliceEnd, breakFrom, breakTo)) {
                cursor = sliceEnd;
                continue;
            }
            OffsetDateTime sliceStart = at(day, cursor);
            OffsetDateTime sliceFinish = at(day, sliceEnd);
            if (now == null || sliceStart.isAfter(now)) {
                out.add(new Slice(sliceStart, sliceFinish));
            }
            cursor = sliceEnd;
        }
        return out;
    }

    private static boolean overlaps(int start, int end, int otherStart, int otherEnd) {
        return start < otherEnd && otherStart < end;
    }

    private static int minutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    private static OffsetDateTime at(LocalDate day, int minuteOfDay) {
        LocalTime time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60);
        return ZonedDateTime.of(day, time, ZONE).toOffsetDateTime();
    }
}
