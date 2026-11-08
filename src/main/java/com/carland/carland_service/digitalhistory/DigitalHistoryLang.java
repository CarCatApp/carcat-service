package com.carland.carland_service.digitalhistory;

import com.carland.carland_service.service.BookingMineService;

/**
 * tr: Paylaşım anındakı Accept-Language. az, en, ru deyilsə az.
 * en: Accept-Language at share time. Anything other than az, en or ru becomes az.
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
