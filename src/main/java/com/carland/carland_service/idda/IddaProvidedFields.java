package com.carland.carland_service.idda;

import com.carland.carland_service.service.CarIddaColor;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: IDDA'dan gelen ve müşterinin değiştiremeyeceği alan adları. Yakıt, ban ve yürüyüş bu listede yoktur.
 *     iddaColor yalnız IDDA renk metni doluysa eklenir; boşsa katalog rengi seçilebilir.
 * en: Field names that come from IDDA and the customer cannot change. Fuel, body and mileage are not in this list.
 *     iddaColor is added only when IDDA sent a color name; an empty color can still be picked from the catalog.
 */
public final class IddaProvidedFields {

    public static final String IDDA_COLOR = "iddaColor";

    public static final List<String> NAMES = List.of(
            "plateNumber",
            "vin",
            "brand",
            "model",
            "modelYear",
            "engineVolume"
    );

    private IddaProvidedFields() {
    }

    public static List<String> names(String iddaColor) {
        if (CarIddaColor.normalize(iddaColor) == null) {
            return NAMES;
        }
        List<String> names = new ArrayList<>(NAMES);
        names.add(IDDA_COLOR);
        return List.copyOf(names);
    }

    /**
     * tr: Liste rengi. iddaColor kilitliyse IDDA metni katalog adının önüne geçer.
     * en: List color. A locked iddaColor replaces the catalog name.
     */
    public static String listColor(List<String> provided, String iddaColor, String catalogName) {
        if (showsIddaColor(provided, iddaColor)) {
            return CarIddaColor.normalize(iddaColor);
        }
        if (catalogName != null && !catalogName.isBlank()) {
            return catalogName;
        }
        String text = CarIddaColor.normalize(iddaColor);
        return text == null ? "unknown" : text;
    }

    public static boolean showsIddaColor(List<String> provided, String iddaColor) {
        return provided != null
                && provided.contains(IDDA_COLOR)
                && CarIddaColor.normalize(iddaColor) != null;
    }
}
