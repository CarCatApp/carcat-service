package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AdminBannerRow {
    Long id;
    String type;
    String lang;
    String title;
    String description;
    String context;
    LocalDate expireDate;
    Long branchId;
    String url;
    Integer sortOrder;
    Boolean showing;
    String status;
}
