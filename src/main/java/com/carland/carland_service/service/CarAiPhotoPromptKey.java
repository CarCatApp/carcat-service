package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * tr: AI prompt'una giren araç alanlarının parmak izi (marka, model, yıl, ban, renk, plaka).
 *     Renk: colorId doluysa id, boşsa colorNameText. Mileage gibi prompt dışı alanlar dahil değil.
 * en: Fingerprint of car fields that go into the AI prompt (brand, model, year, body, color, plate).
 *     Color is colorId when set, otherwise colorNameText. Mileage and other non-prompt fields are excluded.
 */
public final class CarAiPhotoPromptKey {

    private CarAiPhotoPromptKey() {
    }

    public static String of(Car car) {
        String raw = String.join("|",
                norm(car.getBrand()),
                norm(car.getModel()),
                car.getModelYear() == null ? "" : String.valueOf(car.getModelYear()),
                norm(car.getBodyType()),
                colorKey(car),
                plate(car.getPlateNumber()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256", e);
        }
    }

    private static String colorKey(Car car) {
        if (car.getColorId() != null) {
            return String.valueOf(car.getColorId());
        }
        return norm(CarColorNameText.normalize(car.getColorNameText()));
    }

    private static String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String plate(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
