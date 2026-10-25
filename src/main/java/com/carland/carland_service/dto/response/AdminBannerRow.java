package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminBannerRow {
    Long id;
    String type;
    String title;
    String description;
    Long branchId;
    String url;
    Integer sortOrder;
    Boolean showing;
    String status;
}
