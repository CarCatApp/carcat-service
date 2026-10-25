package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Mobil banner öğesi. Alan adları admin kaydıyla aynıdır.
 * en: Mobile banner item. Field names match the admin record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerItem {
    private Long id;
    private String type;
    private String title;
    private String description;
    private Long branchId;
    private String url;
    private Integer sortOrder;
    private boolean showing;
    private String status;
}
