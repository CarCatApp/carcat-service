package com.carland.carland_service.dto.booking;

import com.carland.carland_service.util.PlateNumberSerializer;
import com.carland.carland_service.util.PlateNumbers;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingInspectionView {
    Long id;
    Long bookingId;
    Long customerId;
    String customerName;
    String customerPhone;
    Long rangeId;
    String day;
    String slotStart;
    Long branchId;
    String branchName;
    Long partnerId;
    String ref;
    String message;
    String vin;
    Long carId;
    @JsonSerialize(using = PlateNumberSerializer.class)
    String plateNumber;
    String carBrand;
    String carModel;
    Integer carYear;
    String createdAt;

    /** tr: Görüntü formatı. / en: Display format. */
    public String getPlateNumber() {
        return PlateNumbers.format(plateNumber);
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = PlateNumbers.format(plateNumber);
    }
}
