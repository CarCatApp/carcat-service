package com.carland.carland_service.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * tr: Partner webhook ziyaret güncelleme akışında tek bir parçanın güncellenme sonucunu taşıyan DTO.
 * en: DTO carrying the update result of a single part in the partner webhook visit-update flow.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartUpdateDetail {
    private String name;
    private BigDecimal qty;
    private String unit;
    private Long partId;
    private boolean updated;
}
