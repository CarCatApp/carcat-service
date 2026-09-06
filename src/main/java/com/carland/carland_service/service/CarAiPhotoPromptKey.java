package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * tr: AI prompt'una giren araç alanlarının parmak izi (marka, model, yıl, colorId, plaka).
 *     Mileage gibi prompt dışı alanlar dahil değil.
 * en: Fingerprint of car fields that go into the AI prompt (brand, model, year, colorId, plate).
 *     Mileage and other non-prompt fields are excluded.
 */
public final class CarAiPhotoPromptKey {

    private CarAiPhotoPromptKey() {
    }

    public static String of(Car car) {
        String raw = String.join("|",
                norm(car.getBrand()),
                norm(car.getModel()),
                car.getModelYear() == null ? "" : String.valueOf(car.getModelYear()),
                car.getColorId() == null ? "" : String.valueOf(car.getColorId()),
                plate(car.getPlateNumber()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256", e);
        }
    }

    private static String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String plate(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
