package com.carland.carland_service.test_sima_idda.dto.sima;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaForeignFeignBody {
    private String pin;
    private String livePhoto;
    private String documentType;
    private String idempotencyKey;
}
