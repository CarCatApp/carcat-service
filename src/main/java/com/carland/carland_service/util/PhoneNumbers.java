package com.carland.carland_service.util;

public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String p = raw.trim().replace(" ", "").replace("-", "");
        if (p.matches("0\\d{9}")) {
            p = "+994" + p.substring(1);
        }
        if (!p.matches("\\+994\\d{9}")) {
            return null;
        }
        return p;
    }
}
