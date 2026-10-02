package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: Şube fərdi xidmətini açar veya kapatır. Fiyatlar silinmez.
 * en: Turns a branch individual service on or off. Prices are kept.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffIndividualServiceActiveRequest {
    Boolean active;
}
