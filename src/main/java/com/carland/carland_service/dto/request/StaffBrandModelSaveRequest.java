package com.carland.carland_service.dto.request;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffBrandModelSaveRequest {
    String name;
    String series;
    String viscosity;
    String unit;
    /** Unit price in AZN; null clears / leaves unset. */
    BigDecimal price;
}
