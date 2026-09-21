package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingWriteRequest {
    Long branchId;
    Long slotId;
    List<String> serviceKeys;
    String vin;
    Long carId;
}
