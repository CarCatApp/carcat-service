package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingInspectionView;
import com.carland.carland_service.dto.booking.BookingServiceLineView;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.BookingInspection;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

final class BookingSelectionViews {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP = new TypeReference<>() {};

    private BookingSelectionViews() {
    }

    static BookingInspectionView inspection(BookingInspection row) {
        if (row == null) {
            return null;
        }
        return BookingInspectionView.builder()
                .id(row.getId())
                .bookingId(row.getBooking() == null ? null : row.getBooking().getId())
                .customerId(row.getCustomerUserId())
                .customerName(row.getCustomerName())
                .customerPhone(row.getCustomerPhone())
                .rangeId(row.getRangeId())
                .day(row.getSlotDay() == null ? null : row.getSlotDay().toString())
                .slotStart(row.getSlotStart() == null ? null : row.getSlotStart().toString())
                .branchId(row.getBranchId())
                .branchName(row.getBranchName())
                .partnerId(row.getPartnerId())
                .ref(row.getBookingRef())
                .message(row.getMessage())
                .vin(row.getVin())
                .carId(row.getCarId())
                .plateNumber(row.getPlateNumber())
                .carBrand(row.getCarBrand())
                .carModel(row.getCarModel())
                .carYear(row.getCarYear())
                .createdAt(row.getCreatedAt() == null ? null : row.getCreatedAt().toString())
                .build();
    }

    static BookingServiceLineView line(BookingIndividualLine row, String lang) {
        int[] qepik = qepik(row.getPriceSimple(), row.getPriceMedium(), row.getPriceComplex());
        return BookingServiceLineView.builder()
                .name(BookingMineService.catalogText(titles(row.getTitleJson()), lang))
                .priceMin(qepik == null ? null : qepik[0])
                .priceMax(qepik == null ? null : qepik[1])
                .currency("AZN")
                .unit(BookingCreateService.UNIT)
                .build();
    }

    static String label(String packageName, boolean individualServices, boolean serviceKeys, boolean repair) {
        boolean routine = (packageName != null && !packageName.isBlank()) || individualServices || serviceKeys;
        return BookingKindLabel.of(routine, repair);
    }

    static int[] qepik(Integer simple, Integer medium, Integer complex) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        boolean any = false;
        for (Integer manat : new Integer[]{simple, medium, complex}) {
            if (manat == null) {
                continue;
            }
            any = true;
            min = Math.min(min, manat);
            max = Math.max(max, manat);
        }
        if (!any) {
            return null;
        }
        return new int[]{Math.multiplyExact(min, 100), Math.multiplyExact(max, 100)};
    }

    private static Map<String, String> titles(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = JSON.readValue(json, MAP);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            return Map.of("az", json);
        }
    }
}
