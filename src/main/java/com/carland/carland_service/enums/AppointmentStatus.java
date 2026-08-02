package com.carland.carland_service.enums;

import lombok.Getter;

/**
 * tr: Randevunun yaşam döngüsü durumlarını (bekliyor, kabul edildi, reddedildi vb.) tanımlayan enum.
 * en: Enum defining the lifecycle statuses of an appointment (pending, accepted, rejected, etc.).
 */
@Getter
public enum AppointmentStatus {

    PENDING,
    ACCEPTED,
    REJECTED,
    DELETED_BY_PATIENT,
    RECEPTION

}
