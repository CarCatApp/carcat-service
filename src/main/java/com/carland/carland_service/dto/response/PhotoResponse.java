package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: PhotoController fotoğraf yükleme/indirme işlemlerinde dönen yanıt DTO'su (görsel verisi ve mesaj).
 * en: Response DTO returned by PhotoController photo upload/download operations (image bytes and message).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoResponse {
    byte[]imageData;
    String message;
}

