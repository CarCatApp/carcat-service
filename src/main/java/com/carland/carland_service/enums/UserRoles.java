package com.carland.carland_service.enums;

import feign.Body;
import lombok.Getter;

/**
 * tr: Sistemdeki kullanıcı rollerini (boss, superadmin, admin, kullanıcı) tanımlayan enum.
 * en: Enum defining user roles in the system (boss, superadmin, admin, user).
 */
@Getter

public enum UserRoles {
    BOSS,
    SUPER_ADMIN,
    ADMIN,
    USER;
}
