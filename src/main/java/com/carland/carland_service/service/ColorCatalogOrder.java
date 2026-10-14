package com.carland.carland_service.service;

import com.carland.carland_service.entity.Color;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * tr: Renk sırası. az / en / ru ayrı numara. Numara yoksa eski sabit liste.
 * en: Color order. Separate numbers for az / en / ru. Missing numbers use the old fixed list.
 */
public final class ColorCatalogOrder {

    private static final List<String> AZ = List.of(
            "Qara", "Yaş asfalt", "Boz", "Gümüşü", "Ağ", "Bej", "Tünd qırmızı", "Qırmızı",
            "Çəhrayı", "Narıncı", "Qızılı", "Sarı", "Xaki", "Tünd yaşıl", "Yaşıl", "Açıq yaşıl",
            "Mavi", "Göy", "Bənövşəyi", "Qəhvəyi", "Bordo", "Mat qara", "Metalik gümüş",
            "Tünd mavi", "İncə ağ", "Digər"
    );
    private static final List<String> EN = List.of(
            "Black", "Wet Asphalt", "Gray / Grey", "Gray", "Silver", "White", "Beige", "Dark Red",
            "Red", "Pink", "Orange", "Gold", "Yellow", "Khaki", "Dark Green", "Green", "Light Green",
            "Light Blue", "Blue", "Purple", "Brown", "Maroon", "Matte Black", "Metallic silver",
            "Navy blue", "Pearl white", "Other"
    );
    private static final List<String> RU = List.of(
            "Чёрный", "Мокрый асфальт", "Серый", "Серебристый", "Белый", "Бежевый", "Тёмно-красный",
            "Красный", "Розовый", "Оранжевый", "Золотой", "Жёлтый", "Хаки", "Тёмно-зелёный", "Зелёный",
            "Светло-зелёный", "Голубой", "Синий", "Фиолетовый", "Коричневый", "Тёмно-бордовый",
            "Матовый чёрный", "Серебристый металлик", "Тёмно-синий", "Жемчужно-белый", "Другой"
    );

    private ColorCatalogOrder() {
    }

    /**
     * tr: Accept-Language → az, en veya ru. en/ru ile başlayanlar o dil, kalanı az.
     * en: Accept-Language → az, en, or ru. Values starting with en/ru use that language; the rest are az.
     */
    public static String langKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "az";
        }
        String key = raw.toLowerCase(Locale.ROOT).trim();
        if (key.startsWith("en")) {
            return "en";
        }
        if (key.startsWith("ru")) {
            return "ru";
        }
        return "az";
    }

    public static Integer read(Color color, String lang) {
        if (color == null) {
            return null;
        }
        String key = langKey(lang);
        if ("en".equals(key)) {
            return color.getSortOrderEn();
        }
        if ("ru".equals(key)) {
            return color.getSortOrderRu();
        }
        return color.getSortOrderAz();
    }

    public static void write(Color color, String lang, Integer value) {
        String key = langKey(lang);
        if ("en".equals(key)) {
            color.setSortOrderEn(value);
            return;
        }
        if ("ru".equals(key)) {
            color.setSortOrderRu(value);
            return;
        }
        color.setSortOrderAz(value);
    }

    public static Comparator<Color> comparator(String lang) {
        String key = langKey(lang);
        return Comparator
                .comparing((Color color) -> read(color, key), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(color -> legacyIndex(key, color))
                .thenComparing(color -> display(key, color), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(color -> color.getColorId() == null ? Long.MAX_VALUE : color.getColorId());
    }

    public static String swatch(String hex) {
        if (hex == null) {
            return "";
        }
        String body = hex.trim();
        if (body.startsWith("#")) {
            body = body.substring(1);
        }
        if (!body.matches("(?i)[0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8}")) {
            return "";
        }
        return "#" + body;
    }

    private static int legacyIndex(String key, Color color) {
        String name = color.nameForLang(key);
        if (name == null) {
            return Integer.MAX_VALUE;
        }
        int index = names(key).indexOf(name);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static String display(String key, Color color) {
        String name = color.nameForLang(key);
        return name == null ? "" : name;
    }

    private static List<String> names(String key) {
        if ("en".equals(key)) {
            return EN;
        }
        if ("ru".equals(key)) {
            return RU;
        }
        return AZ;
    }
}
