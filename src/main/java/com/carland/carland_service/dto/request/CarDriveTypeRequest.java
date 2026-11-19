package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarDriveTypeRequest {
    private Long carId;
    /** {@code FWD}, {@code RWD}, {@code AWD} */
    private String driveType;
}
