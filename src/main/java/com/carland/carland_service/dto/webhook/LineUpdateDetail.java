package com.carland.carland_service.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Partner webhook ziyaret güncelleme akışında tek bir servis satırının güncellenme sonucunu taşıyan DTO.
 * en: DTO carrying the update result of a single service line in the partner webhook visit-update flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineUpdateDetail {
    private Integer serviceCode;
    private Long lineId;
    private boolean updated;
}
