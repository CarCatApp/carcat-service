package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.VinServiceOpen;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.VinServiceOpenRepository;
import com.carland.carland_service.service.commission.CommissionCustomerStatusService;
import com.carland.carland_service.service.commission.CommissionStatus;
import com.carland.carland_service.service.commission.CommissionVin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * tr: Köhnə Hyper loyalty → şube siyahısında yalnız partner 1.
 *     new / reactivated yapışır (açıq). Əvvəlki vin_service_open bayrağı da açıq saxlayır.
 * en: Old Hyper loyalty → branch list keeps only partner 1.
 *     new / reactivated stick (open). A prior vin_service_open flag also stays open.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HyperBranchLockService {

    static final int DEFAULT_LOCK_DAYS = 365;

    private final CarRepository carRepository;
    private final VinServiceOpenRepository vinServiceOpenRepository;
    private final CommissionCustomerStatusService commissionCustomerStatusService;

    /**
     * tr: true ise çağıran yalnız partner 1 şubelerini bırakır.
     * en: When true, the caller keeps only partner 1 branches.
     */
    @Transactional
    public boolean locked(Long carId, Long userId) {
        Car car = ownedCar(carId, userId);
        String vin = CommissionVin.normalize(car.getVin());
        if (vin != null && !vin.isEmpty()) {
            Optional<VinServiceOpen> open = vinServiceOpenRepository.findByVin(vin);
            if (open.isEmpty()) {
                String spaced = car.getVin() == null ? "" : car.getVin().replaceAll("\\s+", "").toUpperCase();
                if (!spaced.isEmpty() && !spaced.equals(vin)) {
                    open = vinServiceOpenRepository.findByVin(spaced);
                }
            }
            if (open.isPresent()) {
                log.info("HYPER_LOCK carId={} vin={} rule=OPEN_FLAG reason={} locked=false",
                        carId, vin, open.get().getReason());
                return false;
            }
        }

        String status = commissionCustomerStatusService.resolveStatus(car.getVin());
        boolean locked = CommissionStatus.OLD.equals(status);
        log.info("HYPER_LOCK carId={} vin={} rule=LOYALTY status={} locked={}",
                carId, vin, status == null ? "none" : status, locked);
        return locked;
    }

    private Car ownedCar(Long carId, Long userId) {
        Car car = carRepository.findByCarId(carId);
        if (car == null) {
            throw new ResourceNotFoundException("car not found");
        }
        Customer owner = car.getCustomer();
        if (owner == null || owner.getUserId() == null || !owner.getUserId().equals(userId)) {
            throw new ForbiddenException("car is not yours");
        }
        return car;
    }
}
