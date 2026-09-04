package com.carland.carland_service.enums;

/**
 * tr: Araç fotoğrafı üretim/yükleme durumu (photos.photo_status). PO: küçük harf.
 * en: Car photo generation/upload status (photos.photo_status). PO: lowercase.
 */
public final class CarPhotoStatus {
    public static final String PENDING = "pending";
    public static final String READY = "ready";
    public static final String FAILED = "failed";

    private CarPhotoStatus() {
    }

    public static boolean isPending(String status) {
        return PENDING.equalsIgnoreCase(status);
    }

    public static boolean isReady(String status) {
        return status == null || status.isBlank() || READY.equalsIgnoreCase(status);
    }
}
