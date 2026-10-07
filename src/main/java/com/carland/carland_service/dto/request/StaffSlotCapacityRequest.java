package com.carland.carland_service.dto.request;

import lombok.Data;

/**
 * tr: Bir saatin yer sayısını bir artırır veya bir azaltır.
 * en: Raises or lowers one hour's place count by one.
 */
@Data
public class StaffSlotCapacityRequest {
    Integer delta;
}
