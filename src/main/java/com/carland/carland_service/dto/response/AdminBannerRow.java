package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminBannerRow {
    Long id;
    String title;
    String description;
    Long branchId;
    Boolean showing;
    String status;
}
