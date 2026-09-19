package com.carland.carland_service.enums;

import lombok.Getter;

/**
 * tr: Auth JWT rolleri. Panel telefonla tanınır; ADMIN/SUPER_ADMIN yok.
 * en: Auth JWT roles. Panel is identified by phone; no ADMIN/SUPER_ADMIN.
 */
@Getter

public enum UserRoles {
    BOSS,
    USER,
    PARTNER_ADMIN,
    BRANCH_ADMIN;
}
