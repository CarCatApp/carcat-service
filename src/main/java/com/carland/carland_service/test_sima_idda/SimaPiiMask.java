package com.carland.carland_service.test_sima_idda;

/**
 * Log-only masking. DB still stores full KYC fields.
 * AR "Fərdi məlumatlar haqqında" Qanun: logs must not carry raw FIN / DOB / document.
 * Pattern: PCI-style partial reveal + CarCat OTP phone (CRCT-205: +994 70 ••• •• 89).
 */
public final class SimaPiiMask {

    private SimaPiiMask() {
    }

    /** IAMAS FIN (typically 7 chars): first 1 + stars + last 1. {@code 62HJ5KQ} → {@code 6*****Q}. */
    public static String fin(String pin) {
        return firstAndLast(pin, 1, 1);
    }

    /** Document no: first 2 + stars + last 2. {@code AB0668397} → {@code AB*****97}. */
    public static String documentNumber(String documentNumber) {
        return firstAndLast(documentNumber, 2, 2);
    }

    /** Never log calendar day/month. */
    public static String birthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            return null;
        }
        return "****-**-**";
    }

    /** First letter only. {@code ARAZ} → {@code A***}. */
    public static String name(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.charAt(0) + "***";
    }

    public static String address(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return trimmed.substring(0, 4) + "***";
    }

    /**
     * AZ mobile: {@code +994 70 ••• •• 89} (operator + last 2). Other lengths: last 2 only.
     */
    public static String phone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("994") && digits.length() >= 12) {
            String rest = digits.substring(3);
            String op = rest.substring(0, 2);
            String last2 = rest.substring(rest.length() - 2);
            return "+994 " + op + " ••• •• " + last2;
        }
        if (digits.length() <= 2) {
            return "••";
        }
        return "•••" + digits.substring(digits.length() - 2);
    }

    static String firstAndLast(String raw, int keepStart, int keepEnd) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        int len = value.length();
        if (len <= keepStart + keepEnd) {
            return "*".repeat(len);
        }
        return value.substring(0, keepStart)
                + "*".repeat(len - keepStart - keepEnd)
                + value.substring(len - keepEnd);
    }
}
