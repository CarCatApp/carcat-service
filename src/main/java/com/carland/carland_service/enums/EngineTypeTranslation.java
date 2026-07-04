package com.carland.carland_service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EngineTypeTranslation {

    PETROL_GASOLINE("Petrol (Gasoline)", "Benzin", "Бензин"),
    DIESEL("Diesel", "Dizel", "Дизель"),
    HYBRID("Hybrid", "Hibrid", "Гибрид"),
    PLUG_IN_HYBRID("Plug-in Hybrid", "Plug-in Hibrid", "Подключаемый гибрид"),
    ELECTRIC("Electric", "Elektro", "Электромобиль"),
    GAS_LPG_CNG("Gas (LPG / CNG)", "Qaz", "Газ (LPG / CNG)"),
    DIESEL_HYBRID("Diesel Hybrid", "Dizel-Hibrid", "Дизель-гибрид");

    private final String en;
    private final String az;
    private final String ru;

    public static String translate(String enValue, String acceptLanguage) {
        if (enValue == null || enValue.isBlank()) {
            return enValue;
        }
        for (EngineTypeTranslation type : values()) {
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
