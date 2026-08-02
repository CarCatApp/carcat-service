package com.carland.carland_service.enums;

import lombok.Getter;


/**
 * tr: Davet kodunun durumunu (bekliyor, kullanıldı) tanımlayan enum.
 * en: Enum defining the status of an invite code (pending, used).
 */
@Getter
public enum InviteStatus {
    PENDING,
    USED
}
