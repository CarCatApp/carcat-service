package com.carland.carland_service.test_sima_idda.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaVerifyOutcome {
    private int httpStatus;
    private SimaVerifyResponse body;
}
