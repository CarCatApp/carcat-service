package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.CarRequest;
import com.carland.carland_service.entity.Car;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * tr: AI generate kilidi — süre tek yerde. OTP lockedUntil / remainingSeconds ile aynı şekil.
 * en: AI generate lock — duration in one place. Same shape as OTP lockedUntil / remainingSeconds.
 */
public final class CarAiPhotoGenerateLock {

    public static final Duration DURATION = Duration.ofMinutes(2);

    private CarAiPhotoGenerateLock() {
    }

    public static LocalDateTime newLockedUntil() {
        return lockedUntilFrom(LocalDateTime.now());
    }

    public static LocalDateTime lockedUntilFrom(LocalDateTime startedAt) {
        return startedAt.plus(DURATION);
    }

    public static boolean isLocked(LocalDateTime lockedUntil) {
        return isLocked(lockedUntil, LocalDateTime.now());
    }

    public static boolean isLocked(LocalDateTime lockedUntil, LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public static long remainingSeconds(LocalDateTime lockedUntil) {
        return remainingSeconds(lockedUntil, LocalDateTime.now());
    }

    public static long remainingSeconds(LocalDateTime lockedUntil, LocalDateTime now) {
        if (!isLocked(lockedUntil, now)) {
            return 0L;
        }
        return Math.max(1L, Duration.between(now, lockedUntil).getSeconds());
    }

    /** brand / model / year / colorId / plate — mileage and other edit fields are ignored. */
    public static boolean changesPromptFields(Car car, CarRequest req) {
        if (car == null || req == null) {
            return false;
        }
        if (hasText(req.getBrand()) && !norm(req.getBrand()).equals(norm(car.getBrand()))) {
            return true;
        }
        if (hasText(req.getModel()) && !norm(req.getModel()).equals(norm(car.getModel()))) {
            return true;
        }
        if (req.getModelYear() != null && !req.getModelYear().equals(car.getModelYear())) {
            return true;
        }
        if (req.getColorId() != null && !req.getColorId().equals(car.getColorId())) {
            return true;
        }
        return req.getPlateNumber() != null
                && !plate(req.getPlateNumber()).equals(plate(car.getPlateNumber()));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String plate(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
