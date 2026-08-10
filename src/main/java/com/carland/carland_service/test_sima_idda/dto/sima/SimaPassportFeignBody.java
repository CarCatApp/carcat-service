package com.carland.carland_service.test_sima_idda.dto.sima;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaPassportFeignBody {
    private String pin;
    private String documentNumber;
    private String livePhoto;
    private String idempotencyKey;
}
