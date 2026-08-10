package com.carland.carland_service.test_sima_idda.dto.idda;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Placeholder IDDA vehicle item — refactor when real IDDA contract arrives.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IddaCarItem {
    private String vin;
    private String plateNumber;
    private String brand;
    private String model;
    private Integer modelYear;
}
