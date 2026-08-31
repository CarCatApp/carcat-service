package com.carland.carland_service.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * tr: Semantic Versioning 2.0 karşılaştırma birimi (major.minor.patch + pre-release).
 *     Build metadata parse edilir, sıralamaya girmez. Feature-flag gate için {@link #isAtLeast}.
 * en: SemVer 2.0 compare unit. Build metadata is parsed and ignored for ordering.
 *     Feature-flag gate uses {@link #isAtLeast}. Invalid client versions → isAtLeast false
 *     (caller logs; this class does not log).
 */
public final class SemVer implements Comparable<SemVer> {

    private final int major;
    private final int minor;
    private final int patch;
    private final List<String> preRelease;

    private SemVer(int major, int minor, int patch, List<String> preRelease) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = List.copyOf(preRelease);
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public List<String> preRelease() {
        return preRelease;
    }

    public boolean isPreRelease() {
        return !preRelease.isEmpty();
    }

    /**
     * tr: Geçersiz string'de {@link SemVerFormatException}.
     * en: Throws {@link SemVerFormatException} on invalid input.
     */
    public static SemVer parse(String raw) {
        SemVer parsed = parseOrNull(raw);
        if (parsed == null) {
            throw new SemVerFormatException("Invalid SemVer: " + raw);
        }
        return parsed;
    }

    /**
     * tr: Çökmez; geçersiz / boş / null → empty.
     * en: Never throws; invalid / blank / null → empty.
     */
    public static Optional<SemVer> tryParse(String raw) {
        return Optional.ofNullable(parseOrNull(raw));
    }

    public static int compare(SemVer a, SemVer b) {
        return a.compareTo(b);
    }

    /**
     * tr: client ≥ min. Parse edilemeyen tarafta false (flag uygulanmaz).
     * en: client ≥ min. Either side unparseable → false (flag does not apply).
     */
    public static boolean isAtLeast(String client, String min) {
        SemVer c = parseOrNull(client);
        SemVer m = parseOrNull(min);
        if (c == null || m == null) {
            return false;
        }
        return c.compareTo(m) >= 0;
    }

    public static boolean isAtMost(String client, String max) {
        SemVer c = parseOrNull(client);
        SemVer m = parseOrNull(max);
        if (c == null || m == null) {
            return false;
        }
        return c.compareTo(m) <= 0;
    }

    /**
     * tr: Kapalı aralık [min, max].
     * en: Inclusive range [min, max].
     */
    public static boolean isInRange(String version, String min, String max) {
        return isAtLeast(version, min) && isAtMost(version, max);
    }

    @Override
    public int compareTo(SemVer other) {
        int cmp = Integer.compare(major, other.major);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(minor, other.minor);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(patch, other.patch);
        if (cmp != 0) {
            return cmp;
        }
        return comparePreRelease(preRelease, other.preRelease);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SemVer semVer)) {
            return false;
        }
        return compareTo(semVer) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease);
    }

    @Override
    public String toString() {
        String core = major + "." + minor + "." + patch;
        if (preRelease.isEmpty()) {
            return core;
        }
        return core + "-" + String.join(".", preRelease);
    }

    private static SemVer parseOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.length() > 1 && (s.charAt(0) == 'v' || s.charAt(0) == 'V') && Character.isDigit(s.charAt(1))) {
            s = s.substring(1);
        }
        int plus = s.indexOf('+');
        if (plus >= 0) {
            s = s.substring(0, plus);
        }
        if (s.isEmpty()) {
            return null;
        }
        String core;
        String pre;
        int hyphen = s.indexOf('-');
        if (hyphen >= 0) {
            core = s.substring(0, hyphen);
            pre = s.substring(hyphen + 1);
            if (core.isEmpty() || pre.isEmpty()) {
                return null;
            }
        } else {
            core = s;
            pre = null;
        }
        int[] parts = parseCore(core);
        if (parts == null) {
            return null;
        }
        List<String> identifiers = parsePreRelease(pre);
        if (identifiers == null) {
            return null;
        }
        return new SemVer(parts[0], parts[1], parts[2], identifiers);
    }

    private static int[] parseCore(String core) {
        String[] bits = core.split("\\.", -1);
        if (bits.length < 1 || bits.length > 3) {
            return null;
        }
        int[] out = new int[] {0, 0, 0};
        for (int i = 0; i < bits.length; i++) {
            Integer n = parseNumericIdentifier(bits[i]);
            if (n == null) {
                return null;
            }
            out[i] = n;
        }
        return out;
    }

    private static List<String> parsePreRelease(String pre) {
        if (pre == null) {
            return Collections.emptyList();
        }
        String[] bits = pre.split("\\.", -1);
        List<String> ids = new ArrayList<>(bits.length);
        for (String bit : bits) {
            if (bit.isEmpty() || !isValidPreIdentifier(bit)) {
                return null;
            }
            ids.add(bit);
        }
        return ids;
    }

    private static boolean isValidPreIdentifier(String bit) {
        if (isDigits(bit)) {
            return parseNumericIdentifier(bit) != null;
        }
        for (int i = 0; i < bit.length(); i++) {
            char c = bit.charAt(i);
            if (!isIdentChar(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isIdentChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || c == '-';
    }

    private static Integer parseNumericIdentifier(String bit) {
        if (bit == null || bit.isEmpty() || !isDigits(bit)) {
            return null;
        }
        if (bit.length() > 1 && bit.charAt(0) == '0') {
            return null;
        }
        try {
            return Integer.parseInt(bit);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean isDigits(String bit) {
        for (int i = 0; i < bit.length(); i++) {
            if (bit.charAt(i) < '0' || bit.charAt(i) > '9') {
                return false;
            }
        }
        return !bit.isEmpty();
    }

    /**
     * SemVer: empty pre (stable) &gt; any pre-release. Then identifier by identifier:
     * numeric vs numeric, alphanum vs alphanum; numeric &lt; alphanum; longer set wins if prefix equal.
     */
    private static int comparePreRelease(List<String> a, List<String> b) {
        boolean aPre = !a.isEmpty();
        boolean bPre = !b.isEmpty();
        if (!aPre && !bPre) {
            return 0;
        }
        if (!aPre) {
            return 1;
        }
        if (!bPre) {
            return -1;
        }
        int n = Math.min(a.size(), b.size());
        for (int i = 0; i < n; i++) {
            int cmp = compareIdentifier(a.get(i), b.get(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(a.size(), b.size());
    }

    private static int compareIdentifier(String a, String b) {
        boolean aNum = isDigits(a);
        boolean bNum = isDigits(b);
        if (aNum && bNum) {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        }
        if (aNum) {
            return -1;
        }
        if (bNum) {
            return 1;
        }
        return a.compareTo(b);
    }
}
