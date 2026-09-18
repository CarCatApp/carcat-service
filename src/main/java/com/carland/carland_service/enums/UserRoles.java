package com.carland.carland_service.enums;

import lombok.Getter;

/**
 * tr: Sistemdeki kullanıcı rollerini tanımlayan enum.
 * en: Enum defining user roles in the system.
 */
@Getter

public enum UserRoles {
    BOSS,
    SUPER_ADMIN,
    ADMIN,
    USER,
    PARTNER_ADMIN,
    BRANCH_ADMIN;
}
