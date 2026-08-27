package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: FIN (pin) başka bir müşteride kayıtlı mı sorusunun yanıtı.
 * en: Whether the FIN (pin) is already registered on another customer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinOccupiedResponse {
    /** true = this FIN belongs to a different user. */
    private boolean occupied;
}
