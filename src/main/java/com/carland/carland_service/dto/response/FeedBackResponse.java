package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: FeedbackController geri bildirim gönderiminden sonra dönen yanıt DTO'su (mesaj, tahmini yanıt süresi, ticket id).
 * en: Response DTO returned after FeedbackController feedback submission (message, estimated response time, ticket id).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponse {
    String message;
    String estimatedResponseTime;
    Long ticketId;
}
