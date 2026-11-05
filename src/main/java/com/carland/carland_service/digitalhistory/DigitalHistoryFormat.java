package com.carland.carland_service.digitalhistory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * tr: PDF'te görünen sayılar ve tarihler. Gruplama boşluk, ondalık virgül, gün {@code dd.MM.yyyy}.
 * en: Numbers and dates printed on the PDF. Grouping space, decimal comma, day {@code dd.MM.yyyy}.
 */
final class DigitalHistoryFormat {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DigitalHistoryFormat() {
    }

    static String day(LocalDate date) {
        return date == null ? "" : DAY.format(date);
    }

    static String km(Number mileage) {
        if (mileage == null) {
            return "";
        }
        return grouped(mileage.longValue()) + " km";
    }

    static String volume(Integer cm3) {
        if (cm3 == null) {
            return "";
        }
        return grouped(cm3.longValue()) + " sm³";
    }

    static String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return "";
        }
        String unit = currency == null || currency.isBlank() ? "AZN" : currency.trim();
        return decimal(amount) + " " + unit;
    }

    static String total(BigDecimal amount, String currency, boolean mixed) {
        if (amount == null) {
            return "";
        }
        if (mixed) {
            return decimal(amount);
        }
        return money(amount, currency);
    }

    static String qty(BigDecimal qty) {
        if (qty == null) {
            return "";
        }
        DecimalFormat format = new DecimalFormat("#,##0.##", symbols());
        return format.format(qty);
    }

    static String text(String value) {
        return value == null ? "" : value.trim();
    }

    static String joinSpace(String left, String right) {
        String a = text(left);
        String b = text(right);
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return a + " " + b;
    }

    private static String grouped(long value) {
        return new DecimalFormat("#,##0", symbols()).format(value);
    }

    private static String decimal(BigDecimal amount) {
        return new DecimalFormat("#,##0.00", symbols()).format(amount);
    }

    private static DecimalFormatSymbols symbols() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return symbols;
    }
}
