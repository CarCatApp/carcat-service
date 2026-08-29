package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Admin push filtre dropdown değerleri. All seçeneği istemcide boş değerdir.
 * en: Admin push filter dropdown values. The All option is a blank value on the client.
 */
@Data
@Builder
public class AdminPushFiltersResponse {
    List<String> genders;
    List<String> brands;
    List<EngineTypeOption> engineTypes;

    @Data
    @Builder
    public static class EngineTypeOption {
        Long id;
        String name;
    }
}
