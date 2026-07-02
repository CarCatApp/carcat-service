package com.carland.carland_service.exceptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookAuthErrorResponse {
    private String error;
    private String message;
    private Long partnerId;
    private LocalDateTime timeStamp;
    private Integer status;
}
