package com.carland.carland_service.service;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.exceptions.MissingFieldException;

/**
 * tr: Həftə içi, şənbə və bazar aralığı {@code HH:mm-HH:mm}. Köhnə həftə sonu sütunu şənbə və bazar eyni olanda dolu qalır.
 * en: Weekday, Saturday and Sunday ranges as {@code HH:mm-HH:mm}. The legacy weekend column stays set only when Saturday and Sunday match.
 */
public final class BranchWorkingHours {

    private BranchWorkingHours() {
    }

    public static void write(Branch branch,
                             String weekdayStart, String weekdayEnd,
                             String saturdayStart, String saturdayEnd,
                             String sundayStart, String sundayEnd) {
        apply(branch,
                range(weekdayStart, weekdayEnd, "weekday"),
                range(saturdayStart, saturdayEnd, "saturday"),
                range(sundayStart, sundayEnd, "sunday"));
    }

    public static void apply(Branch branch, String weekday, String saturday, String sunday) {
        branch.setWorkingHours(weekday);
        branch.setWorkingHoursWeekday(weekday);
        branch.setWorkingHoursSaturday(saturday);
        branch.setWorkingHoursSunday(sunday);
        branch.setWorkingHoursWeekend(same(saturday, sunday) ? saturday : null);
    }

    public static String weekdayOf(Branch branch) {
        if (branch == null) {
            return null;
        }
        String weekday = blankToNull(branch.getWorkingHoursWeekday());
        return weekday != null ? weekday : blankToNull(branch.getWorkingHours());
    }

    /** Şənbə sütunu boşdursa köhnə həftə sonu sətri. */
    public static String saturdayOf(Branch branch) {
        if (branch == null) {
            return null;
        }
        String saturday = blankToNull(branch.getWorkingHoursSaturday());
        return saturday != null ? saturday : blankToNull(branch.getWorkingHoursWeekend());
    }

    /** Bazar sütunu boşdursa köhnə həftə sonu sətri. */
    public static String sundayOf(Branch branch) {
        if (branch == null) {
            return null;
        }
        String sunday = blankToNull(branch.getWorkingHoursSunday());
        return sunday != null ? sunday : blankToNull(branch.getWorkingHoursWeekend());
    }

    public static String range(String start, String end, String label) {
        boolean hasStart = start != null && !start.isBlank();
        boolean hasEnd = end != null && !end.isBlank();
        if (!hasStart && !hasEnd) {
            return null;
        }
        if (!hasStart) {
            throw MissingFieldException.required(label + "Start");
        }
        if (!hasEnd) {
            throw MissingFieldException.required(label + "End");
        }
        Integer from = minutes(start);
        Integer to = minutes(end);
        if (from == null) {
            throw new MissingFieldException(label + "Start is invalid");
        }
        if (to == null) {
            throw new MissingFieldException(label + "End is invalid");
        }
        if (to <= from) {
            throw new MissingFieldException(label + "End must be after " + label + "Start");
        }
        return clock(from) + "-" + clock(to);
    }

    private static boolean same(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Integer minutes(String raw) {
        String value = raw.trim();
        if (value.length() < 5 || value.charAt(2) != ':') {
            return null;
        }
        try {
            int hour = Integer.parseInt(value.substring(0, 2));
            int minute = Integer.parseInt(value.substring(3, 5));
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return hour * 60 + minute;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String clock(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
