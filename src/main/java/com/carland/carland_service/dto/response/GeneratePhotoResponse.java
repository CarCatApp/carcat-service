package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: AI araç fotoğrafı generate isteğinin 202 cevabı (status + dil mesajı; gövde byte değil).
 * en: 202 body for AI car-photo generate (status + localized message; not image bytes).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratePhotoResponse {
    Long carId;
    String photoStatus;
    String message;
}
