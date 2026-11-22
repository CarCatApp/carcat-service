package com.carland.carland_service.service.technicalprofile;

import com.carland.carland_service.dto.response.LabeledOptionResponse;
import com.carland.carland_service.enums.CarDriveType;
import com.carland.carland_service.enums.CarTransmissionVariant;
import com.carland.carland_service.enums.TimingBeltChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * tr: Çekiş / transmisyon / kayış-zincir seçenekleri. Metinler burada statik; değişince deploy.
 * en: Drive / transmission / belt-chain options. Labels are static here; change = deploy.
 */
public final class TechnicalProfileOptions {

    private TechnicalProfileOptions() {
    }

    public static List<LabeledOptionResponse> driveTypes(String acceptLanguage) {
        String lang = norm(acceptLanguage);
        List<LabeledOptionResponse> out = new ArrayList<>();
        out.add(opt(CarDriveType.FWD.name(),
                label(lang, "Ön ötürücü", "Front-wheel drive", "Передний привод"),
                label(lang, "Əlavə yağ xidməti yoxdur", "No additional oil services", "Дополнительных услуг по маслу нет")));
        out.add(opt(CarDriveType.RWD.name(),
                label(lang, "Arxa ötürücü", "Rear-wheel drive", "Задний привод"),
                label(lang, "Differensial yağı xidməti", "Differential oil service", "Обслуживание масла дифференциала")));
        out.add(opt(CarDriveType.AWD.name(),
                label(lang, "Tam ötürücü", "All-wheel drive", "Полный привод"),
                label(lang,
                        "Differensial və paylama qutusu yağı xidmətləri",
                        "Differential & transfer case oil services",
                        "Обслуживание масла дифференциала и раздаточной коробки")));
        return out;
    }

    /**
     * tr: Çekiş seçildikten sonra POST cevabındaki bilgi cümlesi.
     * en: Info sentence on the POST response after a drive type is chosen.
     */
    public static String driveTypeSelectedInfo(CarDriveType driveType, String acceptLanguage) {
        if (driveType == null) {
            return null;
        }
        String lang = norm(acceptLanguage);
        return switch (driveType) {
            case FWD -> label(lang,
                    "Ön ötürücü seçildi.",
                    "Front-wheel drive selected.",
                    "Выбран передний привод.");
            case RWD -> label(lang,
                    "Arxa ötürücü seçildi. Differensial yağı xidməti əlavə edildi.",
                    "Rear-wheel drive selected. Differential oil service was added.",
                    "Выбран задний привод. Добавлена услуга масла дифференциала.");
            case AWD -> label(lang,
                    "Tam ötürücü seçildi. Differensial və Paylayıcı qutusu yağı xidmətləri əlavə edildi.",
                    "All-wheel drive selected. Differential and transfer case oil services were added.",
                    "Выбран полный привод. Добавлены услуги масла дифференциала и раздаточной коробки.");
        };
    }

    public static List<LabeledOptionResponse> transmissionVariants(String acceptLanguage) {
        String lang = norm(acceptLanguage);
        List<LabeledOptionResponse> out = new ArrayList<>();
        out.add(opt(CarTransmissionVariant.AT4.name(),
                label(lang, "4 pilləli avtomat", "4-speed automatic", "4-ступенчатый автомат")));
        out.add(opt(CarTransmissionVariant.AT6_8.name(),
                label(lang, "6/8 pilləli avtomat", "6/8-speed automatic", "6/8-ступенчатый автомат")));
        out.add(opt(CarTransmissionVariant.CVT_DCT.name(),
                label(lang, "CVT/DCT", "CVT/DCT", "CVT/DCT")));
        out.add(opt(CarTransmissionVariant.MANUAL.name(),
                label(lang, "Mexaniki", "Manual", "Механика")));
        return out;
    }

    public static List<LabeledOptionResponse> timingBeltChoices(String acceptLanguage) {
        String lang = norm(acceptLanguage);
        List<LabeledOptionResponse> out = new ArrayList<>();
        out.add(opt(TimingBeltChoice.BELT.name(),
                label(lang, "Dişli qayış", "Timing belt", "Ремень ГРМ")));
        out.add(opt(TimingBeltChoice.CHAIN.name(),
                label(lang, "Zəncir", "Timing chain", "Цепь ГРМ")));
        return out;
    }

    private static LabeledOptionResponse opt(String code, String label) {
        return opt(code, label, null);
    }

    private static LabeledOptionResponse opt(String code, String label, String description) {
        return LabeledOptionResponse.builder().code(code).label(label).description(description).build();
    }

    private static String norm(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "az";
        }
        return acceptLanguage.trim().toLowerCase(Locale.ROOT);
    }

    private static String label(String lang, String az, String en, String ru) {
        if (lang.startsWith("en")) {
            return en;
        }
        if (lang.startsWith("ru")) {
            return ru;
        }
        return az;
    }
}
