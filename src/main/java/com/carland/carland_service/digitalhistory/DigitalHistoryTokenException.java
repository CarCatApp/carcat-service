package com.carland.carland_service.digitalhistory;

/**
 * tr: Paylaşım JWT'si yok, bozuk veya süresi dolmuş.
 * en: The share JWT is missing, broken, or expired.
 */
public class DigitalHistoryTokenException extends RuntimeException {

    private final boolean expired;

    private DigitalHistoryTokenException(boolean expired) {
        super(expired ? "expired" : "invalid");
        this.expired = expired;
    }

    public static DigitalHistoryTokenException invalid() {
        return new DigitalHistoryTokenException(false);
    }

    public static DigitalHistoryTokenException expired() {
        return new DigitalHistoryTokenException(true);
    }

    public boolean isExpired() {
        return expired;
    }
}
