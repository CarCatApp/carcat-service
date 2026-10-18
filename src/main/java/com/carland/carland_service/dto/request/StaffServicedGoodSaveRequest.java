package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffServicedGoodSaveRequest {
    String name;
    /** AZN, optional. */
    java.math.BigDecimal price;
}
