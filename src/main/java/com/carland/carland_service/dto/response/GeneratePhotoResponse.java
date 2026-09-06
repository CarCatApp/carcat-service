package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: AI generate cevabı. OpenAI işe alındıysa pending (HTTP 202); skip/hazırsa ready (HTTP 200).
 * en: AI generate body. pending + HTTP 202 when OpenAI is queued; ready + HTTP 200 on skip.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratePhotoResponse {
    Long carId;
    String photoStatus;
    /** ai_generated | user | default — same as X-Photo-Source. */
    String photoSource;
    String message;
}
