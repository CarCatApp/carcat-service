package com.carland.carland_service.idda;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * tr: IDDA Mark alanı tek stringdir. Sondaki Type.Description ve parantez içindeki yakıt düşer.
 *     Listedeki iki kelimelik ad markadır, kalan metin modeldir. Liste yazımı kaydedilir.
 *     Mercedes-Benz ve Rolls-Royce tireli ya da ayrı yazılsa da aynı markadır.
 *     Diğerlerinde ilk kelime marka, kalan metin modeldir. O yazım IDDA metninden kalır.
 * en: IDDA Mark is one string. A trailing Type.Description and a parenthetical fuel word are removed.
 *     A listed two-word name is the brand and the rest is the model. The list spelling is stored.
 *     Mercedes-Benz and Rolls-Royce match with a hyphen or a space.
 *     Otherwise the first word is the brand and the rest is the model, kept as IDDA wrote them.
 */
public final class IddaMarkSplitter {

    private static final Set<String> FUEL_WORDS = Set.of(
            "HIBRID", "DIZEL", "BENZIN", "QAZ", "ELEKTRIK", "PLUGIN"
    );

    private static final List<String> TWO_WORD_BRANDS = List.of(
            "Aston Martin",
            "Land Rover",
            "Rolls-Royce",
            "Alfa Romeo",
            "Mercedes-Benz",
            "Mini Cooper",
            "Great Wall",
            "Tata Motors",
            "Morgan Motor",
            "Lucid Motors",
            "Fisker Automotive",
            "Genesis Motor",
            "De Tomaso",
            "Polestar Automotive"
    );

    private IddaMarkSplitter() {
    }

    public static IddaMarkSplit split(String mark, String typeDescription) {
        String core = stripTrailingType(mark == null ? "" : mark.trim(), typeDescription);
        core = stripFuelParen(core).replaceAll("\\s+", " ").trim();
        if (core.isEmpty()) {
            return new IddaMarkSplit(null, null);
        }
        TwoWord twoWord = matchTwoWordBrand(core);
        if (twoWord != null) {
            return new IddaMarkSplit(twoWord.canonical(), blankToNull(core.substring(twoWord.cut()).trim()));
        }
        int space = core.indexOf(' ');
        if (space < 0) {
            return new IddaMarkSplit(core, null);
        }
        return new IddaMarkSplit(blankToNull(core.substring(0, space)), blankToNull(core.substring(space + 1).trim()));
    }

    private static TwoWord matchTwoWordBrand(String core) {
        String foldedCore = fold(core);
        TwoWord best = null;
        for (String canonical : TWO_WORD_BRANDS) {
            for (String pattern : patterns(canonical)) {
                String foldedPattern = fold(pattern);
                if (!startsWithWord(foldedCore, foldedPattern)) {
                    continue;
                }
                if (best == null || foldedPattern.length() > best.cut()) {
                    best = new TwoWord(canonical, foldedPattern.length());
                }
            }
        }
        return best;
    }

    private static List<String> patterns(String canonical) {
        if (canonical.indexOf('-') < 0) {
            return List.of(canonical);
        }
        return List.of(canonical, canonical.replace('-', ' '));
    }

    private static boolean startsWithWord(String foldedCore, String foldedPattern) {
        if (foldedPattern.isEmpty() || foldedCore.length() < foldedPattern.length() || !foldedCore.startsWith(foldedPattern)) {
            return false;
        }
        return foldedCore.length() == foldedPattern.length()
                || !Character.isLetterOrDigit(foldedCore.charAt(foldedPattern.length()));
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

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private record TwoWord(String canonical, int cut) {
    }
}
