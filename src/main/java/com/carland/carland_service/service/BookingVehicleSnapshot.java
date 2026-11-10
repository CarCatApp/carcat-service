package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * tr: Partner hover üçün avtomobil sahələrini book yaradılarkən bookings sətrinə kopyalayır.
 * en: Copies vehicle fields onto the booking row at create time for the partner hover.
 */
@Service
@RequiredArgsConstructor
public class BookingVehicleSnapshot {

    private final ColorRepository colorRepository;

    /**
     * tr: Book anındakı marka, model, rəng və digər hover sahələrini yazır. Book-u düşürmür.
     * en: Writes brand, model, color and the other hover fields at booking time. Does not fail the booking.
     */
    public void capture(Booking booking, Car car, String lang) {
        if (booking == null || car == null) {
            return;
        }
        booking.setCarBrand(text(car.getBrand()));
        booking.setCarModel(text(car.getModel()));
        booking.setCarColor(colorName(car, lang));
        booking.setCarBodyType(text(car.getBodyType()));
        booking.setCarEngineType(text(car.getEngineType()));
        booking.setCarModelYear(car.getModelYear());
        booking.setCarEngineVolume(car.getEngineVolume());
        booking.setCarMileage(car.getMileage());
    }

    private String colorName(Car car, String lang) {
        if (car.getColorId() == null) {
            return text(car.getIddaColor());
        }
        Color color = colorRepository.findByColorId(car.getColorId());
        if (color == null) {
            return null;
        }
        return text(color.nameForLang(lang));
    }

    private static String text(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
