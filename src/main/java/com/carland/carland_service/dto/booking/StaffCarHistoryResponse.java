package com.carland.carland_service.dto.booking;

import com.carland.carland_service.dto.response.VisitHistoryItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Partner panelinde bir VIN'in bu şubedeki servis geçmişi. Kaynak admin cars history ile aynı: DB visits.
 * en: Service history of one VIN at this branch for the partner panel. Same source as admin cars history: DB visits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCarHistoryResponse {
    Long carId;
    String vin;
    String brand;
    String model;
    String plateNumber;
    Integer modelYear;
    Long branchId;
    String branchName;
    List<VisitHistoryItemResponse> items;
}
