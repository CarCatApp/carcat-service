package com.carland.carland_service.service.technicalprofile;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CustomerServiceRecord;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.enums.CarDriveType;
import com.carland.carland_service.enums.CarTransmissionVariant;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.enums.TimingBeltChoice;
import com.carland.carland_service.repository.CustomerServiceRecordRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * tr: Kia çekiş / transmisyon ve timing belt seçimlerine göre percentage DEACTIVATED / CREATED uygular.
 *     Deaktif satır listede yok, push yok.
 * en: Applies DEACTIVATED / CREATED on percentages from Kia drive / transmission and timing-belt choices.
 *     Deactivated rows are absent from the list and from push.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KiaPercentageProfileService {

    private final PercentageRepository percentageRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final CustomerServiceRecordRepository customerServiceRecordRepository;

    /**
     * tr: Kia araçta percentage'ler oluştuktan sonra koşullu kalemleri kapatır (çekiş/transmisyon seçilene kadar).
     * en: After Kia percentages are created, deactivates conditional items until drive/transmission are chosen.
     */
    @Transactional
    public void bootstrapAfterAdd(Car car) {
        if (car == null || car.getCarId() == null) {
            return;
        }
        List<Percentage> rows = percentageRepository.findAllByCarId(car.getCarId());
        boolean kia = KiaBrand.isKia(car.getBrand());
        for (Percentage p : rows) {
            String nameEn = p.getServiceNameEn();
            if (MaintenanceItemNames.isTransmissionVariant(nameEn)) {
                deactivate(p);
            }
            if (!kia) {
                continue;
            }
            if (MaintenanceItemNames.isTransferCase(nameEn)
                    || MaintenanceItemNames.isDifferential(nameEn)) {
                deactivate(p);
            }
        }
        log.info("Kia percentage bootstrap | carId={} brand={}", car.getCarId(), car.getBrand());
    }

    @Transactional
    public void applyDriveType(Car car, CarDriveType driveType) {
        if (car == null || car.getCarId() == null || driveType == null) {
            return;
        }
        boolean electric = isElectricEngine(car.getEngineType());
        List<Percentage> rows = percentageRepository.findAllByCarId(car.getCarId());
        for (Percentage p : rows) {
            String nameEn = p.getServiceNameEn();
            if (MaintenanceItemNames.isTransferCase(nameEn)) {
                if (electric) {
                    deactivate(p);
                } else if (driveType == CarDriveType.AWD) {
                    activateIfDeactivated(p);
                } else {
                    deactivate(p);
                }
            } else if (MaintenanceItemNames.isDifferential(nameEn)) {
                if (driveType == CarDriveType.RWD || driveType == CarDriveType.AWD) {
                    activateIfDeactivated(p);
                } else {
                    deactivate(p);
                }
            }
        }
        log.info("Kia drive applied | carId={} driveType={} electric={}", car.getCarId(), driveType, electric);
    }

    @Transactional
    public void applyTransmissionVariant(Car car, CarTransmissionVariant variant) {
        if (car == null || car.getCarId() == null || variant == null) {
            return;
        }
        String keepNameEn = nameEnForVariant(variant);
        List<Percentage> rows = percentageRepository.findAllByCarId(car.getCarId());
        Percentage kept = null;
        for (Percentage p : rows) {
            String nameEn = p.getServiceNameEn();
            if (MaintenanceItemNames.isTransmissionGateway(nameEn)) {
                deactivate(p);
            } else if (MaintenanceItemNames.isTransmissionVariant(nameEn)) {
                if (keepNameEn.equalsIgnoreCase(nameEn != null ? nameEn.trim() : "")) {
                    kept = p;
                } else {
                    deactivate(p);
                }
            }
        }
        if (kept != null) {
            activateIfDeactivated(kept);
        } else {
            createVariantPercentage(car, keepNameEn);
        }
        log.info("Kia transmission applied | carId={} variant={}", car.getCarId(), variant);
    }

    @Transactional
    public void applyTimingBeltChoice(Car car, TimingBeltChoice choice) {
        if (car == null || car.getCarId() == null || choice == null) {
            return;
        }
        List<Percentage> rows = percentageRepository.findAllByCarId(car.getCarId());
        for (Percentage p : rows) {
            if (!MaintenanceItemNames.isTimingBelt(p.getServiceNameEn())) {
                continue;
            }
            if (choice == TimingBeltChoice.CHAIN) {
                deactivate(p);
            } else {
                activateIfDeactivated(p);
            }
        }
        log.info("Timing belt choice applied | carId={} choice={}", car.getCarId(), choice);
    }

    /**
     * tr: Kia Transmission fluid kapısı: tip seçilene kadar push / execute'a katılmaz.
     * en: Kia Transmission fluid gateway: excluded from push / execute until a variant is chosen.
     */
    public boolean isInertUntilChoice(Car car, Percentage percentage) {
        if (car == null || percentage == null) {
            return false;
        }
        if (!KiaBrand.isKia(car.getBrand())) {
            return false;
        }
        if (car.getTransmissionVariant() != null && !car.getTransmissionVariant().isBlank()) {
            return false;
        }
        return MaintenanceItemNames.isTransmissionGateway(percentage.getServiceNameEn());
    }

    /**
     * tr: Seçilmemiş transmissiya varyantı ve seçimden sonra gateway listede yok.
     * en: An unselected transmission variant, and the gateway after a choice, stay off the list.
     */
    public static boolean hiddenFromPercentageList(Car car, String nameEn) {
        if (MaintenanceItemNames.isTransmissionVariant(nameEn)) {
            String selected = selectedVariantName(car);
            return selected == null || !selected.equalsIgnoreCase(nameEn.trim());
        }
        if (MaintenanceItemNames.isTransmissionGateway(nameEn)) {
            return selectedVariantName(car) != null;
        }
        return false;
    }

    public static boolean isDeactivated(Percentage percentage) {
        return percentage != null
                && PercentageStatus.fromStored(percentage.getStatus()) == PercentageStatus.DEACTIVATED;
    }

    private void createVariantPercentage(Car car, String nameEn) {
        MaintenanceTemplate template = car.getMaintenanceTemplate();
        if (template == null || nameEn == null) {
            log.info("Kia transmission variant has no template | carId={} nameEn={}", car.getCarId(), nameEn);
            return;
        }
        ServiceEntity source = null;
        List<ServiceEntity> services = serviceEntityRepository.findAllByMaintenanceTemplate(template);
        if (services != null) {
            for (ServiceEntity service : services) {
                if (service != null && service.getNameEn() != null
                        && nameEn.equalsIgnoreCase(service.getNameEn().trim())) {
                    source = service;
                    break;
                }
            }
        }
        if (source == null || source.getId() == null) {
            log.info("Kia transmission variant missing from template | carId={} nameEn={}", car.getCarId(), nameEn);
            return;
        }
        if (percentageRepository.findByServiceIdAndCarId(source.getId(), car.getCarId()) != null) {
            return;
        }
        percentageRepository.save(Percentage.builder()
                .intervalKm(source.getIntervalKm())
                .intervalMonth(source.getIntervalMonth())
                .serviceName(source.getServiceName())
                .serviceNameAz(source.getNameAz())
                .serviceNameEn(source.getNameEn())
                .serviceNameRu(source.getNameRu())
                .actionType(source.getActionType())
                .serviceId(source.getId())
                .important(source.isImportant())
                .status(PercentageStatus.CREATED.name())
                .carId(car.getCarId())
                .build());
        if (customerServiceRecordRepository.findByServiceIdAndCar(source.getId(), car) == null) {
            customerServiceRecordRepository.save(CustomerServiceRecord.builder()
                    .serviceName(source.getServiceName())
                    .serviceNameAz(source.getNameAz())
                    .serviceNameEn(source.getNameEn())
                    .serviceNameRu(source.getNameRu())
                    .actionType(source.getActionType())
                    .serviceId(source.getId())
                    .car(car)
                    .build());
        }
    }

    private static String selectedVariantName(Car car) {
        if (car == null) {
            return null;
        }
        CarTransmissionVariant variant = CarTransmissionVariant.fromStored(car.getTransmissionVariant());
        return variant == null ? null : nameEnForVariant(variant);
    }

    private static String nameEnForVariant(CarTransmissionVariant variant) {
        return switch (variant) {
            case AT4 -> MaintenanceItemNames.TRANSMISSION_AT4;
            case AT6_8 -> MaintenanceItemNames.TRANSMISSION_AT6_8;
            case CVT_DCT -> MaintenanceItemNames.TRANSMISSION_CVT_DCT;
            case MANUAL -> MaintenanceItemNames.TRANSMISSION_MANUAL;
        };
    }

    private static boolean isElectricEngine(String engineType) {
        if (engineType == null || engineType.isBlank()) {
            return false;
        }
        String e = engineType.toLowerCase();
        return e.contains("electric") || e.contains("elektro") || e.contains("ev");
    }

    private void deactivate(Percentage p) {
        p.setStatus(PercentageStatus.DEACTIVATED.name());
        percentageRepository.save(p);
    }

    private void activateIfDeactivated(Percentage p) {
        if (PercentageStatus.fromStored(p.getStatus()) == PercentageStatus.DEACTIVATED) {
            p.setStatus(PercentageStatus.CREATED.name());
            percentageRepository.save(p);
        }
    }
}
