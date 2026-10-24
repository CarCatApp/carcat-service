package com.carland.carland_service.idda;

import com.carland.carland_service.entity.Brand;
import com.carland.carland_service.entity.Model;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * tr: IDDA Mark alanı tek stringdir (marka + model + tip). İlk boşluktan bölünmez.
 *     Sondaki Type.Description düşer, en uzun katalog markası eşlenir.
 *     Katalogda marka yoksa kalan metin olduğu gibi markaya yazılır.
 * en: IDDA Mark is one string (brand + model + type). It is not split on the first space.
 *     A trailing Type.Description is removed, then the longest catalog brand is matched.
 *     When no brand matches, the remaining text is stored as the brand.
 */
public final class IddaMarkSplitter {

    private static final Set<String> FUEL_WORDS = Set.of(
            "HIBRID", "DIZEL", "BENZIN", "QAZ", "ELEKTRIK", "PLUGIN"
    );

    private IddaMarkSplitter() {
    }

    public static IddaMarkSplit split(String mark, String typeDescription, List<Brand> brands, List<Model> models) {
        String core = stripTrailingType(mark == null ? "" : mark.trim(), typeDescription);
        core = core.trim();
        if (core.isEmpty()) {
            return new IddaMarkSplit(null, null);
        }
        Brand brand = longestBrandPrefix(core, brands);
        if (brand == null) {
            String whole = stripFuelParen(core);
            return new IddaMarkSplit(whole.isBlank() ? null : whole, null);
        }
        String foldedBrand = fold(brand.getBrandName());
        String rest = core.substring(Math.min(foldedBrand.length(), core.length())).trim();
        rest = stripFuelParen(rest);
        String modelName = canonicalModel(rest, brand.getBrandId(), models);
        return new IddaMarkSplit(brand.getBrandName(), modelName);
    }

    static String fold(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String upper = raw.toUpperCase(Locale.forLanguageTag("az"));
        StringBuilder sb = new StringBuilder(upper.length());
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            sb.append(switch (c) {
                case 'İ', 'I', 'ı' -> 'I';
                case 'Ş' -> 'S';
                case 'Ğ' -> 'G';
                case 'Ü' -> 'U';
                case 'Ö' -> 'O';
                case 'Ç' -> 'C';
                case 'Ə' -> 'E';
                default -> c;
            });
        }
        return sb.toString();
    }

    private static String stripTrailingType(String mark, String typeDescription) {
        if (mark.isEmpty() || typeDescription == null || typeDescription.isBlank()) {
            return mark;
        }
        String foldedMark = fold(mark);
        String foldedType = fold(typeDescription.trim());
        if (foldedType.isEmpty() || foldedMark.length() < foldedType.length() || !foldedMark.endsWith(foldedType)) {
            return mark;
        }
        int cut = mark.length() - foldedType.length();
        return mark.substring(0, cut).trim();
    }

    private static Brand longestBrandPrefix(String core, List<Brand> brands) {
        if (brands == null || brands.isEmpty()) {
            return null;
        }
        String foldedCore = fold(core);
        Brand best = null;
        int bestLen = -1;
        for (Brand brand : brands) {
            if (brand == null || brand.getBrandName() == null || brand.getBrandName().isBlank()) {
                continue;
            }
            String foldedBrand = fold(brand.getBrandName().trim());
            if (foldedBrand.isEmpty() || foldedBrand.length() > foldedCore.length()) {
                continue;
            }
            if (!foldedCore.startsWith(foldedBrand)) {
                continue;
            }
            if (foldedCore.length() > foldedBrand.length()
                    && Character.isLetterOrDigit(foldedCore.charAt(foldedBrand.length()))) {
                continue;
            }
            if (foldedBrand.length() > bestLen) {
                best = brand;
                bestLen = foldedBrand.length();
            }
        }
        return best;
    }

    private static String stripFuelParen(String value) {
        String trimmed = value == null ? "" : value.trim();
        int open = trimmed.lastIndexOf('(');
        int close = trimmed.endsWith(")") ? trimmed.length() - 1 : -1;
        if (open < 0 || close < open) {
            return trimmed;
        }
        String inside = fold(trimmed.substring(open + 1, close)).replace("-", "").replace(" ", "");
        if (!FUEL_WORDS.contains(inside)) {
            return trimmed;
        }
        return trimmed.substring(0, open).trim();
    }

    /**
     * tr: Kalan metin katalog modeliyle birebir örtüşürse katalog adı yazılır.
     *     "520 D" gibi artan harf varsa tahmin yok, metin olduğu gibi kalır.
     * en: A catalog model name is used only when the remainder matches it fully.
     *     A leftover such as "520 D" is kept as written.
     */
    private static String canonicalModel(String rest, Long brandId, List<Model> models) {
        if (rest == null || rest.isBlank()) {
            return null;
        }
        if (models == null || brandId == null) {
            return rest;
        }
        String foldedRest = fold(rest);
        Model exact = null;
        int exactLen = -1;
        for (Model model : models) {
            if (model == null || model.getModelName() == null || !brandId.equals(model.getBrandId())) {
                continue;
            }
            String foldedModel = fold(model.getModelName().trim());
            if (foldedModel.isEmpty() || !foldedRest.equals(foldedModel)) {
                continue;
            }
            if (foldedModel.length() > exactLen) {
                exact = model;
                exactLen = foldedModel.length();
            }
        }
        return exact == null ? rest : exact.getModelName();
    }
}
