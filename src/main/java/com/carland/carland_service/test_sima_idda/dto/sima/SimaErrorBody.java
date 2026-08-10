package com.carland.carland_service.test_sima_idda.dto.sima;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimaErrorBody {
    private Integer httpStatus;
    private Integer errorCode;
    private String errorMessage;
    private Integer errorOriginCode;
    private List<String> validationErrors;
    private String transactionId;
    private String idempotencyKey;
    private String processTime;
}
