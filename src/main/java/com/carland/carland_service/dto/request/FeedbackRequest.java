package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: FeedbackController üzerinden kullanıcı geri bildirimi (tip, konu, açıklama, puan) göndermek için kullanılan istek DTO'su.
 * en: Request DTO used via FeedbackController to submit user feedback (type, subject, description, rating).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackRequest {
    String type;
    String subject;
    String description;
    Integer rating;

}
