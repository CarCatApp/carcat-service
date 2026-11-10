package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Filtreye veya telefon listesine uyan, cihaz token'ı olan benzersiz kullanıcı sayısı.
 * en: Distinct users with a device token matching filters or phone list.
 */
@Data
@Builder
public class AdminPushPreviewResponse {
    int audienceCount;
    String gender;
    String brand;
    Long engineTypeId;
    String engineTypeName;
    List<String> phoneNumbers;
}
