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
        for (Percentage p : rows) {
            String nameEn = p.getServiceNameEn();
            if (MaintenanceItemNames.isTransmissionVariant(nameEn)
                    || MaintenanceItemNames.isTransferCase(nameEn)
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
        boolean wantDifferential = showsDifferential(driveType);
        boolean wantTransfer = showsTransfer(driveType, electric);
        List<Percentage> rows = percentageRepository.findAllByCarId(car.getCarId());
        Percentage differential = null;
        Percentage transfer = null;
        for (Percentage p : rows) {
            String nameEn = p.getServiceNameEn();
            if (MaintenanceItemNames.isTransferCase(nameEn)) {
                if (wantTransfer) {
                    transfer = p;
                } else {
                    deactivate(p);
                }
            } else if (MaintenanceItemNames.isDifferential(nameEn)) {
                if (wantDifferential) {
                    differential = p;
                } else {
                    deactivate(p);
                }
            }
        }
        if (wantDifferential) {
            if (differential != null) {
                activateIfDeactivated(differential);
            } else {
                createVariantPercentage(car, MaintenanceItemNames.DIFFERENTIAL_OIL);
            }
        }
        if (wantTransfer) {
            if (transfer != null) {
                activateIfDeactivated(transfer);
            } else {
                createVariantPercentage(car, MaintenanceItemNames.TRANSFER_CASE_OIL);
            }
        }
        log.info("Kia drive applied | carId={} driveType={} electric={}", car.getCarId(), driveType, electric);
    }

    /**
     * tr: Seçilen varyantın percentage satırını açar. Dönüş, o satırın
     *     {@code customer_service_records.id} değeridir. {@code PUT /update/record} bunu ister.
     * en: Activates the chosen variant's percentage. Returns that row's
     *     {@code customer_service_records.id}. {@code PUT /update/record} requires it.
     */
    @Transactional
    public Long applyTransmissionVariant(Car car, CarTransmissionVariant variant) {
        if (car == null || car.getCarId() == null || variant == null) {
            return null;
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
            kept = createVariantPercentage(car, keepNameEn);
        }
        Long recordId = recordIdOf(car, kept);
        log.info("Kia transmission applied | carId={} variant={} recordId={}", car.getCarId(), variant, recordId);
        return recordId;
    }

    /**
     * tr: Kayış seçilince dişli qayış kaydının id'si. Zincir seçilince null.
     *     Kayıt satırı silinmez; percentage kapanır ve listeden düşer.
     * en: Timing-belt record id when the choice is belt. Null when the choice is chain.
     *     The record row stays; the percentage is deactivated and leaves the list.
     */
    @Transactional
    public Long applyTimingBeltChoice(Car car, TimingBeltChoice choice) {
        if (car == null || car.getCarId() == null || choice == null) {
            return null;
        }
        List<Percentage> rows = percentageRepository.findAllByCarId(car.getCarId());
        Percentage kept = null;
        if (rows != null) {
            for (Percentage p : rows) {
                if (!MaintenanceItemNames.isTimingBelt(p.getServiceNameEn())) {
                    continue;
                }
                if (choice == TimingBeltChoice.CHAIN) {
                    deactivate(p);
                } else {
                    activateIfDeactivated(p);
                    if (kept == null) {
                        kept = p;
                    }
                }
            }
        }
        if (choice == TimingBeltChoice.CHAIN) {
            log.info("Timing belt choice applied | carId={} choice={} recordId=null", car.getCarId(), choice);
            return null;
        }
        if (kept == null) {
            kept = createTimingBeltPercentage(car);
        }
        Long recordId = recordIdOf(car, kept);
        log.info("Timing belt choice applied | carId={} choice={} recordId={}", car.getCarId(), choice, recordId);
        return recordId;
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
        if (MaintenanceItemNames.isDifferential(nameEn)) {
            return !showsDifferential(CarDriveType.fromStored(car == null ? null : car.getDriveType()));
        }
        if (MaintenanceItemNames.isTransferCase(nameEn)) {
            boolean electric = isElectricEngine(car == null ? null : car.getEngineType());
            return !showsTransfer(CarDriveType.fromStored(car == null ? null : car.getDriveType()), electric);
        }
        return false;
    }

    public static boolean isDeactivated(Percentage percentage) {
        return percentage != null
                && PercentageStatus.fromStored(percentage.getStatus()) == PercentageStatus.DEACTIVATED;
    }

    private Percentage createVariantPercentage(Car car, String nameEn) {
        MaintenanceTemplate template = car.getMaintenanceTemplate();
        if (template == null || nameEn == null) {
            log.info("Kia transmission variant has no template | carId={} nameEn={}", car.getCarId(), nameEn);
            return null;
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
            return null;
        }
        Percentage existing = percentageRepository.findByServiceIdAndCarId(source.getId(), car.getCarId());
        if (existing != null) {
            return existing;
        }
        Percentage created = percentageRepository.save(Percentage.builder()
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
        return created;
    }

    private Percentage createTimingBeltPercentage(Car car) {
        MaintenanceTemplate template = car.getMaintenanceTemplate();
        if (template == null) {
            return null;
        }
        List<ServiceEntity> services = serviceEntityRepository.findAllByMaintenanceTemplate(template);
        if (services == null) {
            return null;
        }
        for (ServiceEntity service : services) {
            if (service != null && MaintenanceItemNames.isTimingBelt(service.getNameEn())) {
                return createVariantPercentage(car, service.getNameEn().trim());
            }
        }
        return null;
    }

    private Long recordIdOf(Car car, Percentage percentage) {
        if (car == null || percentage == null || percentage.getServiceId() == null) {
            return null;
        }
        CustomerServiceRecord record = customerServiceRecordRepository.findByServiceIdAndCar(percentage.getServiceId(), car);
        if (record == null) {
            record = customerServiceRecordRepository.save(CustomerServiceRecord.builder()
                    .serviceName(percentage.getServiceName())
                    .serviceNameAz(percentage.getServiceNameAz())
                    .serviceNameEn(percentage.getServiceNameEn())
                    .serviceNameRu(percentage.getServiceNameRu())
                    .actionType(percentage.getActionType())
                    .serviceId(percentage.getServiceId())
                    .car(car)
                    .build());
        }
        return record.getId();
    }

    private static String selectedVariantName(Car car) {
        if (car == null) {
            return null;
        }
        CarTransmissionVariant variant = CarTransmissionVariant.fromStored(car.getTransmissionVariant());
        return variant == null ? null : nameEnForVariant(variant);
    }

    private static boolean showsDifferential(CarDriveType driveType) {
        return driveType == CarDriveType.RWD || driveType == CarDriveType.AWD;
    }

    private static boolean showsTransfer(CarDriveType driveType, boolean electric) {
        return !electric && driveType == CarDriveType.AWD;
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
