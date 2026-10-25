package com.carland.carland_service.dto.booking;

import com.carland.carland_service.util.PlateNumberSerializer;
import com.carland.carland_service.util.PlateNumbers;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * tr: Yaradılan book. date gün.ay.il, saat Bakı. Qiymətlər qəpik.
 * en: Created booking. date is day.month.year, clock is Baku. Prices are qepik.
 */
@Data
@Builder
public class BookingAppointmentResponse {
    Long bookingId;
    String ref;
    String status;
    String bookingMode;
    Long branchId;
    Long rangeId;
    String date;
    String start;
    String end;
    Long carId;
    String vin;
    @JsonSerialize(using = PlateNumberSerializer.class)
    String plateNumber;
    String carBrand;
    String carModel;
    Integer carYear;
    Long packageId;
    String packageName;
    Integer packagePrice;
    List<BookingAppointmentServiceView> individualServices;
    String issue;
    Integer priceMin;
    Integer priceMax;
    String currency;
    String unit;

    /** tr: Görüntü formatı. / en: Display format. */
    public String getPlateNumber() {
        return PlateNumbers.format(plateNumber);
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = PlateNumbers.format(plateNumber);
    }
}
