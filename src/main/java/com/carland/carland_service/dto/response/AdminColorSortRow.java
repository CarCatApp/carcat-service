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
    /** tr: Kayıtlı AZ adı. Boşsa input boş kalır; ekran adı yedek dilden gelebilir. / en: Stored AZ name. */
    String nameAz;
    /** tr: Kayıtlı EN adı (`colors.color`). / en: Stored EN name (`colors.color`). */
    String nameEn;
    /** tr: Kayıtlı RU adı. / en: Stored RU name. */
    String nameRu;
    String hex;
    Integer sortOrder;
    String swatch;
}
