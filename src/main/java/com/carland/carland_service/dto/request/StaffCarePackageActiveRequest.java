package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Paketi açar veya kapatır. Xidmət satırlarına dokunmaz.
 * en: Turns a package on or off. Service lines stay as they are.
 */
@Data
public class StaffCarePackageActiveRequest {
    Boolean active;
}
