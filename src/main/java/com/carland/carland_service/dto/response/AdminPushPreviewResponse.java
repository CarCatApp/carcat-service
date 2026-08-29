package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * tr: Filtreye uyan, cihaz token'ı olan benzersiz kullanıcı sayısı.
 * en: Distinct users with a device token matching the filters.
 */
@Data
@Builder
public class AdminPushPreviewResponse {
    int audienceCount;
    String gender;
    String brand;
    Long engineTypeId;
    String engineTypeName;
}
