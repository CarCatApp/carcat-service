package com.carland.carland_service.idda;

import com.carland.carland_service.dto.request.CarRequest;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.CustomerServiceRecord;
import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.CustomerServiceRecordRepository;
import com.carland.carland_service.repository.EngineTypeRepository;
import com.carland.carland_service.repository.MaintenanceTemplateRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.service.CarIddaColor;
import com.carland.carland_service.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * tr: IDDA add car. VIN satırı varsa plakası güncellenir, VIN metni değişmez.
 *     VIN yoksa yeni satır açılır. Plakası tutan diğer satırların sahibi null olur, VIN'leri durur.
 *     Var olan yakıt ve şablon değişmez. Yeni satırda yakıt bir kez seçilir.
 *     Redis: sahip düşen ve yeni sahip için carlist:{userId}:* commit sonrası silinir.
 *     history anahtarı VIN'dedir; VIN değişmediği için history evict yok.
 * en: IDDA add car. An existing VIN row keeps its VIN text and receives the new plate.
 *     A missing VIN inserts a new row. Other rows with that plate lose their customer; their VIN stays.
 *     An existing fuel type and template stay. A new row gets the fuel choice once.
 *     Redis: carlist:{userId}:* is deleted after commit for the dropped owner and the new owner.
 *     History keys are by VIN; the VIN is unchanged, so history is not evicted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IddaCarWriteService {

    private final IddaPlateCheckRepository iddaPlateCheckRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final ColorRepository colorRepository;
    private final EngineTypeRepository engineTypeRepository;
    private final MaintenanceTemplateRepository maintenanceTemplateRepository;
    private final PercentageRepository percentageRepository;
    private final CustomerServiceRecordRepository customerServiceRecordRepository;
    private final RedisCacheService redisCacheService;

    public IddaAddResult add(CarRequest carRequest, String phoneNumber, String userIdHeader, String acceptLanguage) {
        if (carRequest == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = requireCustomer(phoneNumber, userIdHeader, acceptLanguage);
        if (carRequest.getMileage() == null || carRequest.getMileage() < 0) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        String plateKey = IddaPlateKey.of(carRequest.getPlateNumber());
        if (plateKey.isEmpty()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        if (carRequest.getColorId() != null && colorRepository.findByColorId(carRequest.getColorId()) == null) {
            throw new ResourceNotFoundException(MessagesLangValues.COLOR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        LocalDateTime since = LocalDateTime.now().minusMonths(1);
        List<IddaPlateCheck> recent = iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(plateKey, since);
        if (recent.isEmpty() || recent.get(0).getVin() == null || recent.get(0).getVin().isBlank()) {
            throw new MissingFieldException(MessagesLangValues.IDDA_CHECK_REQUIRED.getMessageByLang(acceptLanguage));
        }
        IddaPlateCheck own = recent.stream()
                .filter(row -> customer.getUserId().equals(row.getQuerierUserId()))
                .findFirst()
                .orElse(null);
        IddaPlateCheck snapshot = own != null ? own : recent.get(0);
        Car vinCar = findVin(snapshot.getVin());
        orphanOthers(carRepository.findByPlateKey(plateKey), vinCar == null ? null : vinCar.getCarId());
        if (vinCar != null) {
            claim(vinCar, snapshot, carRequest, customer);
            redisCacheService.evictCarListAfterCommit(String.valueOf(customer.getUserId()));
            log.info("IDDA add claimed carId={} vin={}", vinCar.getCarId(), vinCar.getVin());
            return new IddaAddResult(vinCar, false);
        }
        Car created = insert(snapshot, carRequest, customer, acceptLanguage);
        redisCacheService.evictCarListAfterCommit(String.valueOf(customer.getUserId()));
        log.info("IDDA add inserted carId={} vin={}", created.getCarId(), created.getVin());
        return new IddaAddResult(created, true);
    }

    private void claim(Car car, IddaPlateCheck snapshot, CarRequest carRequest, Customer customer) {
        Customer previous = car.getCustomer();
        if (previous != null && previous.getUserId() != null
                && !previous.getUserId().equals(customer.getUserId())) {
            redisCacheService.evictCarListAfterCommit(String.valueOf(previous.getUserId()));
        }
        car.setCustomer(customer);
        car.setPlateNumber(snapshot.getPlateNumber());
        car.setBrand(snapshot.getBrand());
        car.setModel(snapshot.getModel());
        car.setModelYear(snapshot.getModelYear());
        car.setEngineVolume(snapshot.getEngineCapacity());
        car.setMileage(carRequest.getMileage());
        if (carRequest.getBodyType() != null && !carRequest.getBodyType().isBlank()) {
            car.setBodyType(carRequest.getBodyType().trim());
        }
        CarIddaColor.apply(car, carRequest.getColorId(), iddaColorText(snapshot, carRequest));
        car.setIddaProvidedFields(lockedFields(car));
        if (car.getEngineTypeId() == null && carRequest.getEngineTypeId() != null) {
            EngineType engineType = engineTypeRepository.findByEngineTypeId(carRequest.getEngineTypeId());
            if (engineType != null) {
                car.setEngineType(engineType.getEngineType());
                car.setEngineTypeId(engineType.getEngineTypeId());
            }
        }
        car.setFromIdda(true);
        car.setUpdatedAt(LocalDateTime.now());
        carRepository.save(car);
        attach(customer, car);
    }

    private Car insert(IddaPlateCheck snapshot, CarRequest carRequest, Customer customer, String acceptLanguage) {
        if (carRequest.getEngineTypeId() == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        EngineType engineType = engineTypeRepository.findByEngineTypeId(carRequest.getEngineTypeId());
        if (engineType == null) {
            throw new ResourceNotFoundException(MessagesLangValues.ENGINE_TYPE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        MaintenanceTemplate template = maintenanceTemplateRepository.findByEngineType(engineType).orElse(null);
        if (template == null || template.getServices() == null || template.getServices().isEmpty()) {
            throw new ResourceNotFoundException(MessagesLangValues.TEMPLATE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        LocalDateTime now = LocalDateTime.now();
        Car car = Car.builder()
                .vin(snapshot.getVin().trim().toUpperCase(Locale.ROOT))
                .plateNumber(snapshot.getPlateNumber())
                .brand(snapshot.getBrand())
                .model(snapshot.getModel())
                .modelYear(snapshot.getModelYear())
                .engineType(engineType.getEngineType())
                .engineTypeId(engineType.getEngineTypeId())
                .engineVolume(snapshot.getEngineCapacity())
                .bodyType(blankToNull(carRequest.getBodyType()))
                .mileage(carRequest.getMileage())
                .createdAt(now)
                .updatedAt(now)
                .customer(customer)
                .maintenanceTemplate(template)
                .fromIdda(true)
                .build();
        CarIddaColor.apply(car, carRequest.getColorId(), iddaColorText(snapshot, carRequest));
        car.setIddaProvidedFields(lockedFields(car));
        try {
            carRepository.save(car);
        } catch (DataIntegrityViolationException ex) {
            throw new AlreadyExistsException(MessagesLangValues.CAR_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        }
        for (ServiceEntity serviceEntity : template.getServices()) {
            if (serviceEntity == null || serviceEntity.getServiceName() == null || serviceEntity.getServiceName().isBlank()) {
                throw new ResourceNotFoundException(MessagesLangValues.TEMPLATE_NOT_FOUND.getMessageByLang(acceptLanguage));
            }
            percentageRepository.save(Percentage.builder()
                    .intervalKm(serviceEntity.getIntervalKm())
                    .intervalMonth(serviceEntity.getIntervalMonth())
                    .serviceName(serviceEntity.getServiceName())
                    .serviceNameAz(serviceEntity.getNameAz())
                    .serviceNameEn(serviceEntity.getNameEn())
                    .serviceNameRu(serviceEntity.getNameRu())
                    .actionType(serviceEntity.getActionType())
                    .serviceId(serviceEntity.getId())
                    .important(serviceEntity.isImportant())
                    .status(PercentageStatus.CREATED.name())
                    .carId(car.getCarId())
                    .build());
            customerServiceRecordRepository.save(CustomerServiceRecord.builder()
                    .serviceName(serviceEntity.getServiceName())
                    .serviceNameAz(serviceEntity.getNameAz())
                    .serviceNameEn(serviceEntity.getNameEn())
                    .serviceNameRu(serviceEntity.getNameRu())
                    .actionType(serviceEntity.getActionType())
                    .serviceId(serviceEntity.getId())
                    .car(car)
                    .build());
        }
        attach(customer, car);
        return car;
    }

    private void orphanOthers(List<Car> plateCars, Long keepCarId) {
        if (plateCars == null) {
            return;
        }
        for (Car other : plateCars) {
            if (other == null || (keepCarId != null && keepCarId.equals(other.getCarId()))) {
                continue;
            }
            Customer previous = other.getCustomer();
            if (previous != null && previous.getUserId() != null) {
                redisCacheService.evictCarListAfterCommit(String.valueOf(previous.getUserId()));
            }
            other.setCustomer(null);
            other.setUpdatedAt(LocalDateTime.now());
            carRepository.save(other);
        }
    }

    private void attach(Customer customer, Car car) {
        if (customer.getCars() == null) {
            customer.setCars(new ArrayList<>());
        }
        if (!customer.getCars().contains(car)) {
            customer.getCars().add(car);
        }
        customerRepository.save(customer);
    }

    private Car findVin(String vin) {
        List<Car> cars = carRepository.findAllByVinIgnoreCase(vin);
        if (cars == null || cars.isEmpty()) {
            return null;
        }
        for (Car car : cars) {
            if (car.getVin() != null && car.getVin().equalsIgnoreCase(vin)) {
                return car;
            }
        }
        return cars.get(0);
    }

    private Customer requireCustomer(String phoneNumber, String userIdHeader, String acceptLanguage) {
        if (phoneNumber == null || phoneNumber.isBlank() || userIdHeader == null || userIdHeader.isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(
                userId, phoneNumber.trim(), UserStatus.ACTIVE.name());
        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return customer;
    }

    private static String iddaColorText(IddaPlateCheck snapshot, CarRequest carRequest) {
        if (CarIddaColor.normalize(carRequest.getIddaColor()) != null) {
            return carRequest.getIddaColor();
        }
        return snapshot == null ? null : snapshot.getColorRaw();
    }

    private static List<String> lockedFields(Car car) {
        String color = car.getColorId() == null ? car.getIddaColor() : null;
        return new ArrayList<>(IddaProvidedFields.names(color));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
