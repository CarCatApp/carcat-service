package com.carland.carland_service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * tr: Servis türü adlarının Azerice/İngilizce/Rusça çevirilerini tutan ve Accept-Language'a göre çeviri yapan enum.
 * en: Enum holding Azerbaijani/English/Russian translations of service type names, translating based on Accept-Language.
 */
@Getter
@AllArgsConstructor
public enum ServiceTypeTranslation {

    BATTERY("Akkumulyator xidməti", "Battery service", "Обслуживание аккумулятора"),
    SHOCK_ABSORBER("Amortizator xidməti", "Shock absorber service", "Обслуживание амортизаторов"),
    REINFORCEMENT("Armaturçu xidməti", "Reinforcement service", "Арматурные работы"),
    SUSPENSION("Asqı xidməti", "Suspension service", "Обслуживание подвески"),
    INTERNAL_AUTO_SERVICE_COSTS("Avtoservis daxili xərclər", "Internal auto service costs", "Внутренние расходы автосервиса"),
    CAR_WASH("Avto-yuma xidməti", "Car wash service", "Автомойка"),
    NITROGEN_AIR("Azot və hava xidməti", "Nitrogen & air service", "Сервис азота и воздуха"),
    WHEEL_BALANCING("Balans xidməti", "Wheel balancing service", "Балансировка колёс"),
    POLISHING("Cilalama xidməti", "Polishing service", "Полировка"),
    LOCKSMITH("Çilingər xidməti", "Locksmith service", "Слесарное обслуживание"),
    INTERNAL_EXPENSES("Daxili Xərclər", "Internal expenses", "Внутренние расходы"),
    METALWORK("Dəmirçi xidməti", "Metalwork service", "Слесарные работы"),
    DIAGNOSTICS("Diaqnostika xidməti", "Diagnostics service", "Диагностика"),
    DIFFERENTIAL_TRANSMISSION("Differensial, körpü və ötürücü qutu xidməti", "Differential, axle & transmission service", "Дифференциал, мост и коробка передач"),
    OTHER_SERVICES("Digər xidmətlər", "Other services", "Прочие услуги"),
    RIM("Disk xidməti", "Rim service", "Обслуживание дисков"),
    ELECTRICAL("Elektrik xidməti", "Electrical service", "Электрообслуживание"),
    BRAKE("Əyləc xidməti", "Brake service", "Обслуживание тормозов"),
    FILTER("Filtr xidməti", "Filter service", "Обслуживание фильтров"),
    KT("K/T", "K/T", "K/T"),
    PROMOTIONAL("Kampaniyalı xidmətlər", "Promotional services", "Акционные услуги"),
    OTHER_INCOME("Kənar gəlir", "Other income", "Прочий доход"),
    AIR_CONDITIONING("Kondisioner xidməti", "Air conditioning service", "Обслуживание кондиционера"),
    FLUID("Maye xidməti", "Fluid service", "Обслуживание жидкостей"),
    MULTISERVICE("Multiservis", "Multiservice", "Мультисервис"),
    ENGINE("Mühərrik xidməti", "Engine service", "Обслуживание двигателя"),
    RADIATOR("Radiator xidməti", "Radiator service", "Обслуживание радиатора"),
    PAINT("Rəngsaz xidməti", "Paint service", "Покраска"),
    INTERIOR("Salon xidməti", "Interior service", "Обслуживание салона"),
    STEERING("Sükan xidməti", "Steering service", "Обслуживание рулевого управления"),
    GEARBOX("Sürətlər qutusu xidməti", "Gearbox service", "Обслуживание коробки передач"),
    TIRE("Təkər xidməti", "Tire service", "Шиномонтаж"),
    WHEEL_ALIGNMENT("Təkərlərin hizalanma xidməti", "Wheel alignment service", "Развал-схождение"),
    CLEANING("Təmizlik xidməti", "Cleaning service", "Уборка"),
    TECHNICAL_INSPECTION("Texniki baxış", "Technical inspection", "Техосмотр"),
    FUEL_AI92("Yanacaq Aİ92", "Fuel AI-92", "Топливо AI-92"),
    INSPECTION("Yoxlanış xidməti", "Inspection service", "Осмотр");

    private final String az;
    private final String en;
    private final String ru;

    public static String translate(String value, String acceptLanguage) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String trimmed = value.trim();
        for (ServiceTypeTranslation type : values()) {
            if (type.az.equalsIgnoreCase(trimmed) || type.en.equalsIgnoreCase(trimmed)) {
                return type.getByLang(acceptLanguage);
            }
        }
        return value;
    }

    public static List<String> translateList(List<String> values, String acceptLanguage) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(value -> translate(value, acceptLanguage))
                .toList();
    }

    private String getByLang(String acceptLanguage) {
        if (acceptLanguage == null) {
            return az;
        }
        return switch (normalizeLang(acceptLanguage)) {
            case "en" -> en;
            case "ru" -> ru;
            default -> az;
        };
    }

    private static String normalizeLang(String acceptLanguage) {
        String lang = acceptLanguage.trim().toLowerCase();
        if (lang.length() >= 2) {
            lang = lang.substring(0, 2);
        }
        return lang;
    }
}
