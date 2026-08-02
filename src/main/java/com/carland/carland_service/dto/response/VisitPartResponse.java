package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * tr: Bir servis ziyaretinde kullanılan tek bir parçayı (ad, miktar, birim) temsil eden yanıt DTO'su; ziyaret geçmişi yanıtlarında kullanılır.
 * en: Response DTO representing a single part used in a service visit (name, quantity, unit); used in visit history responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitPartResponse {
    /** Carland DB id — optional, used by partner update requests. */
    private Long id;
    private String name;
    private BigDecimal qty;
    private String unit;
}
