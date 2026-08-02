package com.carland.carland_service.enums;

import lombok.Getter;


/**
 * tr: Kullanıcının kayıt/aktivasyon yaşam döngüsü durumlarını (davet edildi, OTP bekliyor, aktif, engellendi vb.) tanımlayan enum.
 * en: Enum defining a user's registration/activation lifecycle statuses (invited, OTP pending, active, blocked, etc.).
 */
@Getter
public enum UserStatus {
    INVITED,
    OTP_PENDING,
    OTP_VERIFIED,
    ACTIVE,
    BLOCKED,
    JOINED
}
