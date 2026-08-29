package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Admin push kitle filtresi. Boş alanlar AND kombinasyonunda yok sayılır (all).
 * en: Admin push audience filter. Blank fields are ignored in the AND combination (all).
 */
@Data
public class AdminPushAudienceRequest {
    String gender;
    String brand;
    Long engineTypeId;
}
