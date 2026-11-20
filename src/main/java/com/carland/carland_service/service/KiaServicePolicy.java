package com.carland.carland_service.service;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.service.technicalprofile.KiaBrand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * tr: Kia partner şubelerinde marka kuralı. BMW şubeyi görmez.
 *     Kia ve Hyundai bütün hizmeti görür. Diğer marka yalnız razval ve balans görür, paket görmez.
 * en: Brand rule on Kia partner branches. BMW does not see the branch.
 *     Kia and Hyundai see every service. Other brands see only alignment and balancing, and no package.
 */
@Service
public class KiaServicePolicy {

    private final long kiaPartnerId;
    private final CarRepository carRepository;

    public KiaServicePolicy(@Value("${carland.booking.kia-partner-id:12}") long kiaPartnerId,
                            CarRepository carRepository) {
        this.kiaPartnerId = kiaPartnerId;
        this.carRepository = carRepository;
    }

    public Car requireOwned(Long carId, Long userId) {
        if (carId == null) {
            throw MissingFieldException.required("carId");
        }
        Car car = carRepository.findByCarId(carId);
        if (car == null) {
            throw new ResourceNotFoundException("car not found");
        }
        Customer owner = car.getCustomer();
        if (userId == null || owner == null || owner.getUserId() == null || !owner.getUserId().equals(userId)) {
            throw new ForbiddenException("car is not yours");
        }
        return car;
    }

    public boolean kiaPartner(Branch branch) {
        if (branch == null) {
            return false;
        }
        Partner partner = branch.getPartner();
        return partner != null && partner.getId() != null && partner.getId() == kiaPartnerId;
    }

    /** Partner id to drop from the branch list. Null when the car may see Kia branches. */
    public Long hiddenPartnerId(Car car) {
        if (car != null && KiaBrand.isBmw(car.getBrand())) {
            return kiaPartnerId;
        }
        return null;
    }

    public boolean fullCatalog(Car car) {
        return car != null
                && (Boolean.TRUE.equals(car.getIsKia()) || Boolean.TRUE.equals(car.getIsHyundai()));
    }

    public boolean limitedCatalog(Branch branch, Car car) {
        return kiaPartner(branch) && !fullCatalog(car) && !KiaBrand.isBmw(car == null ? null : car.getBrand());
    }

    public boolean hideCatalog(Branch branch, Car car) {
        return kiaPartner(branch) && KiaBrand.isBmw(car == null ? null : car.getBrand());
    }

    public boolean allowedWhenLimited(String titleAz, String titleEn) {
        String az = norm(titleAz);
        String en = norm(titleEn);
        return az.equals("təkərlərin düzlənməsi (razval)")
                || az.equals("təkərlərin balansı və yerdəyişməsi")
                || en.equals("wheel alignment")
                || en.equals("wheel balancing & rotation")
                || en.equals("wheel balancing&rotation");
    }

    private static String norm(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
