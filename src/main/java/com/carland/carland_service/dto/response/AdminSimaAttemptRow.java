package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminSimaAttemptRow {
    String time;
    boolean success;
    String outcome;
    String liveness;
    String similarity;
    String code;
    String beforeLabel;
}
