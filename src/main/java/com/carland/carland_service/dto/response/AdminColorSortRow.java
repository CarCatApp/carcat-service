package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Admin color sort satırı. İsim seçilen dil, swatch hex kodundan.
 * en: Admin color-sort row. Name is for the selected language; swatch comes from the hex code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminColorSortRow {
    Long colorId;
    String name;
    String hex;
    Integer sortOrder;
    String swatch;
}
