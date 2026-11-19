package com.carland.carland_service.service.technicalprofile;

import com.carland.carland_service.dto.request.CarDriveTypeRequest;
import com.carland.carland_service.dto.request.CarTransmissionVariantRequest;
import com.carland.carland_service.dto.request.TimingBeltChoiceRequest;
import com.carland.carland_service.dto.response.CarResponse;
import com.carland.carland_service.dto.response.LabeledOptionResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.enums.CarDriveType;
import com.carland.carland_service.enums.CarTransmissionVariant;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.TimingBeltChoice;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.service.CarService;
import com.carland.carland_service.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * tr: Çekiş / transmisyon / timing belt seçenek listeleri ve bir kez set API'leri.
 * en: Drive / transmission / timing-belt option lists and set-once APIs.
 */
@Slf4j
@Service
public class CarTechnicalProfileService {

    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final KiaPercentageProfileService kiaPercentageProfileService;
    private final RedisCacheService redisCacheService;
    private final CarService carService;

    public CarTechnicalProfileService(CarRepository carRepository,
                                      CustomerRepository customerRepository,
                                      KiaPercentageProfileService kiaPercentageProfileService,
                                      RedisCacheService redisCacheService,
                                      @Lazy CarService carService) {
        this.carRepository = carRepository;
        this.customerRepository = customerRepository;
        this.kiaPercentageProfileService = kiaPercentageProfileService;
        this.redisCacheService = redisCacheService;
        this.carService = carService;
    }

    public List<LabeledOptionResponse> driveTypeOptions(String acceptLanguage) {
        return TechnicalProfileOptions.driveTypes(acceptLanguage);
    }

    public List<LabeledOptionResponse> transmissionVariantOptions(String acceptLanguage) {
        return TechnicalProfileOptions.transmissionVariants(acceptLanguage);
    }

    public List<LabeledOptionResponse> timingBeltChoiceOptions(String acceptLanguage) {
        return TechnicalProfileOptions.timingBeltChoices(acceptLanguage);
    }

    @Transactional
    public CarResponse setDriveType(CarDriveTypeRequest request, String phoneNumber, String userIdHeader,
                                    String acceptLanguage) {
        if (request == null || request.getCarId() == null
                || request.getDriveType() == null || request.getDriveType().isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(request.getCarId(), customer, acceptLanguage);
        if (!KiaBrand.isKia(car.getBrand())) {
            throw new MissingFieldException("drive type is only for Kia cars");
        }
        if (car.getDriveType() != null && !car.getDriveType().isBlank()) {
            throw new AlreadyExistsException("drive type already set");
        }
        CarDriveType driveType;
        try {
            driveType = CarDriveType.fromStored(request.getDriveType());
        } catch (RuntimeException e) {
            throw new MissingFieldException("invalid driveType");
        }
        if (driveType == null) {
            throw new MissingFieldException("invalid driveType");
        }
        car.setDriveType(driveType.name());
        carRepository.save(car);
        kiaPercentageProfileService.applyDriveType(car, driveType);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        return reloadCar(car, phoneNumber, userIdHeader, acceptLanguage);
    }

    @Transactional
    public CarResponse setTransmissionVariant(CarTransmissionVariantRequest request, String phoneNumber,
                                              String userIdHeader, String acceptLanguage) {
        if (request == null || request.getCarId() == null
                || request.getTransmissionVariant() == null || request.getTransmissionVariant().isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(request.getCarId(), customer, acceptLanguage);
        if (!KiaBrand.isKia(car.getBrand())) {
            throw new MissingFieldException("transmission variant is only for Kia cars");
        }
        if (car.getTransmissionVariant() != null && !car.getTransmissionVariant().isBlank()) {
            throw new AlreadyExistsException("transmission variant already set");
        }
        CarTransmissionVariant variant;
        try {
            variant = CarTransmissionVariant.fromStored(request.getTransmissionVariant());
        } catch (RuntimeException e) {
            throw new MissingFieldException("invalid transmissionVariant");
        }
        if (variant == null) {
            throw new MissingFieldException("invalid transmissionVariant");
        }
        car.setTransmissionVariant(variant.name());
        carRepository.save(car);
        kiaPercentageProfileService.applyTransmissionVariant(car, variant);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        return reloadCar(car, phoneNumber, userIdHeader, acceptLanguage);
    }

    @Transactional
    public CarResponse setTimingBeltChoice(TimingBeltChoiceRequest request, String phoneNumber,
                                           String userIdHeader, String acceptLanguage) {
        if (request == null || request.getCarId() == null
                || request.getTimingBeltChoice() == null || request.getTimingBeltChoice().isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(request.getCarId(), customer, acceptLanguage);
        if (car.getTimingBeltChoice() != null && !car.getTimingBeltChoice().isBlank()) {
            throw new AlreadyExistsException("timing belt choice already set");
        }
        TimingBeltChoice choice;
        try {
            choice = TimingBeltChoice.fromStored(request.getTimingBeltChoice());
        } catch (RuntimeException e) {
            throw new MissingFieldException("invalid timingBeltChoice");
        }
        if (choice == null) {
            throw new MissingFieldException("invalid timingBeltChoice");
        }
        car.setTimingBeltChoice(choice.name());
        carRepository.save(car);
        kiaPercentageProfileService.applyTimingBeltChoice(car, choice);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        return reloadCar(car, phoneNumber, userIdHeader, acceptLanguage);
    }

    private CarResponse reloadCar(Car car, String phoneNumber, String userIdHeader, String acceptLanguage) {
        return carService.getCarByVinCode(car.getVin(), phoneNumber, userIdHeader, "Asia/Baku", acceptLanguage);
    }

    private Customer requireActiveCustomer(String userIdHeader, String phoneNumber, String acceptLanguage) {
        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(
                Long.valueOf(userIdHeader), phoneNumber, UserStatus.ACTIVE.name());
        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return customer;
    }

    private Car requireCustomerCar(Long carId, Customer customer, String acceptLanguage) {
        Car car = carRepository.findByCarIdAndCustomer(carId, customer);
        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return car;
    }
}
