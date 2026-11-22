package com.carland.carland_service.service.technicalprofile;

import com.carland.carland_service.enums.CarDriveType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TechnicalProfileOptionsTest {

    @Test
    void driveTypeInfoFollowsAcceptLanguage() {
        assertEquals("Ön ötürücü seçildi.",
                TechnicalProfileOptions.driveTypeSelectedInfo(CarDriveType.FWD, "az"));
        assertEquals("Arxa ötürücü seçildi. Differensial yağı xidməti əlavə edildi.",
                TechnicalProfileOptions.driveTypeSelectedInfo(CarDriveType.RWD, "az-AZ"));
        assertEquals("Tam ötürücü seçildi. Differensial və Paylayıcı qutusu yağı xidmətləri əlavə edildi.",
                TechnicalProfileOptions.driveTypeSelectedInfo(CarDriveType.AWD, null));

        assertEquals("Front-wheel drive selected.",
                TechnicalProfileOptions.driveTypeSelectedInfo(CarDriveType.FWD, "en"));
        assertEquals("Rear-wheel drive selected. Differential oil service was added.",
                TechnicalProfileOptions.driveTypeSelectedInfo(CarDriveType.RWD, "en-US"));
        assertEquals("All-wheel drive selected. Differential and transfer case oil services were added.",
                TechnicalProfileOptions.driveTypeSelectedInfo(CarDriveType.AWD, "en"));

        assertEquals("Выбран передний привод.",
                TechnicalProfileOptions.driveTypeSelectedInfo(CarDriveType.FWD, "ru"));
        assertEquals("Выбран задний привод. Добавлена услуга масла дифференциала.",
                TechnicalProfileOptions.driveTypeSelectedInfo(CarDriveType.RWD, "ru"));
        assertEquals("Выбран полный привод. Добавлены услуги масла дифференциала и раздаточной коробки.",
                TechnicalProfileOptions.driveTypeSelectedInfo(CarDriveType.AWD, "ru-RU"));
    }
}
