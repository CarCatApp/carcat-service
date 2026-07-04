package com.carland.carland_service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BodyTypeTranslation {

    SEDAN("Sedan", "Sedan", "Седан"),
    SUV("SUV", "SUV / Yolsuzluq", "Внедорожник"),
    HATCHBACK("Hatchback", "Hetçbek", "Хэтчбек"),
    CROSSOVER("Crossover", "Krossover", "Кроссовер"),
    WAGON("Wagon / Estate", "Universal", "Универсал"),
    PICKUP("Pickup", "Pikap", "Пикап"),
    MINIVAN("Minivan", "Minivan", "Минивэн"),
    COUPE("Coupe", "Kupe", "Купе"),
    LIFTBACK("Liftback", "Liftbek", "Лифтбек"),
    VAN("Van", "Mikroavtobus", "Фургон"),
    CONVERTIBLE("Convertible", "Kabriolet", "Кабриолет"),
    OTHER("Other", "Digər", "Другое");

    private final String en;
    private final String az;
    private final String ru;

    public static String translate(String enValue, String acceptLanguage) {
        if (enValue == null || enValue.isBlank()) {
            return enValue;
        }
        for (BodyTypeTranslation type : values()) {
            if (type.en.equalsIgnoreCase(enValue.trim())) {
                if ("az".equalsIgnoreCase(acceptLanguage)) {
                    return type.az;
                }
                if ("ru".equalsIgnoreCase(acceptLanguage)) {
                    return type.ru;
                }
                return type.en;
            }
        }
        return enValue;
    }
}
