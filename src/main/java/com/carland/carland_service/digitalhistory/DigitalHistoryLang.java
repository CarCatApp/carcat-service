package com.carland.carland_service.digitalhistory;

import com.carland.carland_service.service.BookingMineService;

/**
 * tr: Hesabat dili {@code customers.notificationLanguage}. Boş və ya az/en/ru deyilsə az.
 * en: Report language is {@code customers.notificationLanguage}. Blank or anything other than az, en, ru becomes az.
 */
public final class DigitalHistoryLang {

    private DigitalHistoryLang() {
    }

    public static String normalize(String acceptLanguage) {
        String raw = BookingMineService.langOf(acceptLanguage);
        if (raw.startsWith("en")) {
            return "en";
        }
        if (raw.startsWith("ru")) {
            return "ru";
        }
        if (raw.startsWith("az")) {
            return "az";
        }
        return "az";
    }
}
