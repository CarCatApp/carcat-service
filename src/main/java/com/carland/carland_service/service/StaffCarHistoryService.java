package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffCarHistoryResponse;
import com.carland.carland_service.dto.response.VisitHistoryItemResponse;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.util.PlateNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * tr: Partner paneli VIN geçmişi. Admin cars history ile aynı visits satırları; yalnız bu şube.
 *     Hyper API çağırmaz. Redis yazmaz.
 * en: Partner-panel VIN history. The same visits rows as admin cars history, limited to this branch.
 *     Does not call Hyper. Does not write Redis.
 */
@Service
@RequiredArgsConstructor
public class StaffCarHistoryService {

    private static final Comparator<VisitHistoryItemResponse> NEWEST_FIRST = Comparator
            .comparing(VisitHistoryItemResponse::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(VisitHistoryItemResponse::getId, Comparator.nullsLast(Comparator.reverseOrder()));

    private final BookingStaffAccess bookingStaffAccess;
    private final CarRepository carRepository;
    private final VisitHistoryService visitHistoryService;

    /**
     * tr: Staff'ın yetkili olduğu şubede, aracın o şubeye ait ziyaretlerini döner.
     *     Partner 1 ve boş branch_id, şube 10 sayılır.
     * en: Returns the car's visits that belong to a branch the staff may open.
     *     Partner 1 with an empty branch_id counts as branch 10.
     */
    @Transactional(readOnly = true)
    public StaffCarHistoryResponse history(
            Long userId,
            boolean mustChangePassword,
            Long carId,
            String vin,
            Long branchId,
            String acceptLanguage
    ) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        Branch branch = bookingStaffAccess.requireWritableBranch(staff, branchId, acceptLanguage);
        String normalizedVin = normalizeVin(vin);
        if (carId == null && normalizedVin == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        List<Car> cars = resolveCars(carId, normalizedVin);
        if (cars.isEmpty()) {
            return StaffCarHistoryResponse.builder()
                    .vin(normalizedVin)
                    .branchId(branch.getId())
                    .branchName(branch.getName())
                    .items(List.of())
                    .build();
        }

        List<VisitHistoryItemResponse> items = new ArrayList<>();
        for (Car car : cars) {
            for (VisitHistoryItemResponse item : visitHistoryService.listStoredByCar(car, acceptLanguage)) {
                if (branch.getId().equals(item.getBranchId())) {
                    items.add(item);
                }
            }
        }
        items.sort(NEWEST_FIRST);
        Car header = cars.get(0);
        return StaffCarHistoryResponse.builder()
                .carId(header.getCarId())
                .vin(header.getVin())
                .brand(header.getBrand())
                .model(header.getModel())
                .plateNumber(PlateNumbers.format(header.getPlateNumber()))
                .modelYear(header.getModelYear())
                .branchId(branch.getId())
                .branchName(branch.getName())
                .items(items)
                .build();
    }

    private List<Car> resolveCars(Long carId, String vin) {
        if (carId != null) {
            Car byId = carRepository.findByCarId(carId);
            if (byId != null) {
                return List.of(byId);
            }
        }
        if (vin == null) {
            return List.of();
        }
        List<Car> matches = new ArrayList<>(carRepository.findAllByVinIgnoreCase(vin));
        matches.sort(Comparator.comparing(Car::getCarId, Comparator.nullsLast(Comparator.reverseOrder())));
        return matches;
    }

    private static String normalizeVin(String vin) {
        if (vin == null) {
            return null;
        }
        String trimmed = vin.replace(" ", "").trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
