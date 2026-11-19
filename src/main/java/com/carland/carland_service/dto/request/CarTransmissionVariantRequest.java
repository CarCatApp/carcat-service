package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarTransmissionVariantRequest {
    private Long carId;
    /** {@code AT4}, {@code AT6_8}, {@code CVT_DCT}, {@code MANUAL} */
    private String transmissionVariant;
}
