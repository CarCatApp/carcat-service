package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * tr: Partner paneline dönen tek hizmet kategorisi. Başlıklar az/en/ru map.
 * en: One service category returned to the partner panel. Titles are an az/en/ru map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCatalogCategoryView {
    Long id;
    String code;
    Map<String, String> title;
    Map<String, String> description;
    String iconUrl;
    Boolean openable;
    Boolean toggleable;
    Boolean active;
    Integer sortOrder;
}
