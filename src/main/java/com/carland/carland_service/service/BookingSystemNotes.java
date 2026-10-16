package com.carland.carland_service.service;

import java.util.Map;

/**
 * tr: Sistemin özü yazdığı rədd notları. Bazada kod + az mətn durur; cavabda Accept-Language dilində qaytarılır.
 * en: Rejection notes written by the system itself. The row keeps the code + az text; responses use the Accept-Language text.
 */
public final class BookingSystemNotes {

    public static final String PLACES_FULL = "places_full";
    public static final String CALENDAR_DELETED = "calendar_deleted";

    private static final Map<String, Map<String, String>> TEXTS = Map.of(
            PLACES_FULL, Map.of(
                    "az", "Yerlər dolduğu üçün rezervasiyanızı qəbul edə bilmədik.",
                    "en", "We were unable to accept your reservation because all places are taken.",
                    "ru", "Мы не смогли принять вашу бронь, поскольку все места заняты."),
            CALENDAR_DELETED, Map.of(
                    "az", "Xidmət təqvimi silindi",
                    "en", "The service calendar was deleted",
                    "ru", "Календарь услуг был удалён")
    );

    private BookingSystemNotes() {
    }

    /**
     * tr: Bilinən kodun mətni (dil yoxdursa az). Bilinməyən kod null.
     * en: Text of a known code (az when the language is missing). Unknown code is null.
     */
    public static String text(String code, String acceptLanguage) {
        Map<String, String> texts = code == null ? null : TEXTS.get(code);
        if (texts == null) {
            return null;
        }
        String found = texts.get(BookingMineService.langOf(acceptLanguage));
        return found == null ? texts.get("az") : found;
    }

    /**
     * tr: Sistem kodudursa dilə uyğun mətn, deyilsə bazadakı not olduğu kimi.
     * en: The localized text for a system code, otherwise the stored note as is.
     */
    public static String resolve(String code, String storedNote, String acceptLanguage) {
        String text = text(code, acceptLanguage);
        return text == null ? storedNote : text;
    }
}
