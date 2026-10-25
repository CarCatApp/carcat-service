package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;

/**
 * tr: Katalog colorId fotoğraf promptunda hex (yoksa İngilizce ad) olur.
 *     colorId boşsa iddaColor Azerice ad olarak prompta eklenir.
 * en: A catalog colorId becomes the hex in the photo prompt (English name when hex is missing).
 *     When colorId is empty, iddaColor is added to the prompt as an Azerbaijani color name.
 */
public final class CarIddaColor {

    public static final int MAX_LENGTH = 80;

    private CarIddaColor() {
    }

    /**
     * tr: Add formundaki renk seçimi. colorId varsa iddaColor silinir.
     *     Yalnız iddaColor geldiyse eski colorId de silinir; prompt metni kullansın.
     * en: Color choice on add. iddaColor is cleared when colorId is set.
     *     iddaColor alone also clears a previous colorId so the prompt uses the text.
     */
    public static void apply(Car car, Long colorId, String iddaColor) {
        if (car == null) {
            return;
        }
        if (colorId != null) {
            car.setColorId(colorId);
            car.setIddaColor(null);
            return;
        }
        String stored = normalize(iddaColor);
        if (stored == null) {
            return;
        }
        car.setColorId(null);
        car.setIddaColor(stored);
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim().replace('\n', ' ').replace('\r', ' ').replace('"', ' ');
        text = text.replaceAll(" +", " ").trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > MAX_LENGTH) {
            text = text.substring(0, MAX_LENGTH).trim();
        }
        return text.isEmpty() ? null : text;
    }

    /** tr: Prompttaki kısa Azerice renk cümlesi. Metin yoksa boş. / en: Short Azerbaijani color clause. Empty when there is no text. */
    public static String azerbaijaniClause(String iddaColor) {
        String text = normalize(iddaColor);
        if (text == null) {
            return "";
        }
        return ", painted in the Azerbaijani color \"" + text + "\" (use the English paint color)";
    }
}
