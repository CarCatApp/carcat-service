package com.carland.carland_service.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * tr: Auth'un Feign ile okuduğu ham sonuç. Cümle auth'ta kurulur.
 * en: Raw facts auth reads over Feign. Auth builds the sentence.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromoPreviewResponse {

    boolean hasPromo;

    /** tr: Müşterinin kayıtlı kodu yazılan kodla aynı. / en: The stored code matches the typed code. */
    boolean sameCode;

    /**
     * tr: Yazılan kod: NOT_FOUND, DEACTIVATED, EXPIRED, NOT_STARTED, USABLE.
     * en: The typed code: NOT_FOUND, DEACTIVATED, EXPIRED, NOT_STARTED, USABLE.
     */
    String codeState;
}
