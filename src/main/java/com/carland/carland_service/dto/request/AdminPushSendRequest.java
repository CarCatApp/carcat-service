package com.carland.carland_service.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * tr: Admin push gönderim: az zorunlu, en/ru opsiyonel; kitle filtreleri veya telefon listesi.
 * en: Admin push send: az required, en/ru optional; audience filters or phone list.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminPushSendRequest extends AdminPushAudienceRequest {
    String titleAz;
    String bodyAz;
    String titleEn;
    String bodyEn;
    String titleRu;
    String bodyRu;

    /** @deprecated kept so older clients still map to az */
    @Deprecated
    String title;
    /** @deprecated kept so older clients still map to az */
    @Deprecated
    String body;
}
