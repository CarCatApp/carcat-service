package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: FIN başka SIMA-verified müşteride kayıtlı mı.
 * en: Whether another SIMA-verified customer already has this FIN.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinOccupiedResponse {
    /** true = another SIMA-verified customer already has this FIN. */
    private boolean occupied;
}
