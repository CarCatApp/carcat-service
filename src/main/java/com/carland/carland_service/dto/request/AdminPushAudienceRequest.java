package com.carland.carland_service.dto.request;

import lombok.Data;

import java.util.List;

/**
 * tr: Admin push kitle filtresi. phoneNumbers doluysa gender/brand/engine yok sayılır.
 * en: Admin push audience filter. When phoneNumbers is set, gender/brand/engine are ignored.
 */
@Data
public class AdminPushAudienceRequest {
    String gender;
    String brand;
    Long engineTypeId;
    /** Normalized or raw +994… numbers; one entry per phone. */
    List<String> phoneNumbers;
}
