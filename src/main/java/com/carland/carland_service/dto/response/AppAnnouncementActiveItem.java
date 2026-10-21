package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Mobil duyuru öğesi. Hedefleme alanları burada yok; {@code id} JSON sayısıdır.
 * en: Mobile announcement item. Targeting fields stay off this type; {@code id} is a JSON number.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppAnnouncementActiveItem {
    private long id;
    private String type;
    private boolean dismissible;
    private String title;
    private String body;
    private String ctaLabel;
    private String ctaUrl;
}
