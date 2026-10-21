package com.carland.carland_service.util;

import java.util.Optional;

/**
 * tr: Duyuru hedeflemesi için major.minor.patch. Feature-flag {@link SemVer} değildir:
 *     ilk {@code +} veya {@code -} sonrası atılır, fazla segment yok sayılır, geçersiz girdi exception atmaz.
 * en: major.minor.patch for announcement targeting. Not feature-flag {@link SemVer}:
 *     the tail after the first {@code +} or {@code -} is dropped, extra segments are ignored,
 *     and invalid input does not throw.
 */
public final class AppVersion implements Comparable<AppVersion> {

    private final int major;
    private final int minor;
    private final int patch;

    private AppVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
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

    /**
     * tr: Boş, sayı olmayan veya int sınırını aşan segment → empty.
     * en: Empty when blank, non-numeric, or a segment overflows int.
     */
    public static Optional<AppVersion> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String core = raw.trim();
        if (core.isEmpty()) {
            return Optional.empty();
        }
        int cut = indexOfPlusOrMinus(core);
        if (cut >= 0) {
            core = core.substring(0, cut);
        }
        if (core.isEmpty()) {
            return Optional.empty();
        }
        String[] bits = core.split("\\.", -1);
        int major = 0;
        int minor = 0;
        int patch = 0;
        int used = Math.min(bits.length, 3);
        for (int i = 0; i < used; i++) {
            Integer part = parseSegment(bits[i]);
            if (part == null) {
                return Optional.empty();
            }
            if (i == 0) {
                major = part;
            } else if (i == 1) {
                minor = part;
            } else {
                patch = part;
            }
        }
        return Optional.of(new AppVersion(major, minor, patch));
    }

    @Override
    public int compareTo(AppVersion other) {
        int cmp = Integer.compare(major, other.major);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(minor, other.minor);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppVersion other)) {
            return false;
        }
        return compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        return result;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    private static int indexOfPlusOrMinus(String value) {
        int plus = value.indexOf('+');
        int minus = value.indexOf('-');
        if (plus < 0) {
            return minus;
        }
        if (minus < 0) {
            return plus;
        }
        return Math.min(plus, minus);
    }

    private static Integer parseSegment(String bit) {
        if (bit == null || bit.isEmpty()) {
            return null;
        }
        for (int i = 0; i < bit.length(); i++) {
            char c = bit.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
        }
        try {
            long value = Long.parseLong(bit);
            if (value > Integer.MAX_VALUE) {
                return null;
            }
            return (int) value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
