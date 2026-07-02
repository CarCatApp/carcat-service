package com.carland.carland_service.exceptions;

import com.carland.carland_service.controller.WebhookController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(assignableTypes = WebhookController.class)
public class WebhookAuthExceptionHandler {

    @ExceptionHandler(WebhookAuthException.class)
    public ResponseEntity<WebhookAuthErrorResponse> handleWebhookAuth(WebhookAuthException ex) {
        WebhookAuthErrorResponse body = WebhookAuthErrorResponse.builder()
                .error(ex.getFailure().getError())
                .message(ex.getMessage())
                .partnerId(ex.getPartnerId())
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
