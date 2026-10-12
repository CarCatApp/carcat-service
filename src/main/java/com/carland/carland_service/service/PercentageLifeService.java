package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CustomerServiceRecord;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.entity.ServiceHistory;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.repository.CustomerServiceRecordRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.carland.carland_service.repository.ServiceHistoryRepository;
import com.carland.carland_service.service.HyperPercentageSyncService.PartnerLineSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * tr: GET /service/percentages ile aynı kalan km ve kalan gün.
 *     Elle düzenlenmiş satır kayıtlı son/sonrakini kullanır. CREATED önce Hyper ziyaretini,
 *     o yoksa son servis artı şablon aralığını kullanır.
 * en: Same remaining km and remaining days as GET /service/percentages.
 *     An edited row uses the stored last/next. CREATED uses the Hyper visit, otherwise
 *     the last service plus the template interval.
 */
@Service
@RequiredArgsConstructor
public class PercentageLifeService {

    private final ServiceEntityRepository serviceEntityRepository;
    private final PercentageRepository percentageRepository;
    private final CustomerServiceRecordRepository customerServiceRecordRepository;
    private final ServiceHistoryRepository serviceHistoryRepository;
    private final HyperPercentageSyncService hyperPercentageSyncService;

    public record Snapshot(Integer remainingKm, Integer remainingDays) {
    }

    public java.util.Map<Long, Snapshot> byServiceId(Car car) {
        if (car == null || car.getMaintenanceTemplate() == null || car.getCarId() == null) {
            return java.util.Map.of();
        }
        List<ServiceEntity> services = serviceEntityRepository.findAllByMaintenanceTemplate(car.getMaintenanceTemplate());
        if (services == null || services.isEmpty()) {
            return java.util.Map.of();
        }
        List<CustomerServiceRecord> records = customerServiceRecordRepository.findAllByCar(car);
        List<ServiceHistory> histories = serviceHistoryRepository.findAllByCar(car);
        java.util.Map<Long, Snapshot> out = new java.util.LinkedHashMap<>();
        for (ServiceEntity service : services) {
            if (service.getId() == null) {
                continue;
            }
            out.put(service.getId(), of(car, service,
                    records == null ? List.of() : records,
                    histories == null ? List.of() : histories));
        }
        return out;
    }

    public Snapshot of(Car car, ServiceEntity service, List<CustomerServiceRecord> records, List<ServiceHistory> histories) {
        Percentage percentage = percentageRepository.findByServiceIdAndCarId(service.getId(), car.getCarId());
        PercentageStatus status = percentage != null
                ? PercentageStatus.fromStored(percentage.getStatus())
                : PercentageStatus.CREATED;
        if (percentage != null && status.isManuallySet()) {
            return fromStored(percentage.getLastServiceDate(), percentage.getLastServiceKm(),
                    percentage.getNextServiceKm(), percentage.getNextServiceDate(), car.getMileage());
        }
        Optional<PartnerLineSnapshot> partner = hyperPercentageSyncService.findBestPartnerLineForService(
                car, service.getNameEn(), service.getIntervalKm(), service.getIntervalMonth());
        if (partner.isPresent()) {
            PartnerLineSnapshot snap = partner.get();
            LocalDate lastDate = snap.lastServiceDate() != null
                    ? snap.lastServiceDate()
                    : car.getCreatedAt().toLocalDate();
            Integer lastKm = snap.lastServiceKm() != null ? snap.lastServiceKm() : 0;
            return fromStored(lastDate, lastKm, snap.nextServiceKm(), snap.nextServiceDate(), car.getMileage());
        }
        return fromInterval(car, service, records == null ? List.of() : records, histories == null ? List.of() : histories);
    }

    private static Snapshot fromStored(LocalDate lastDate, Integer lastKm, Integer nextKm, LocalDate nextDate, Long mileage) {
        return new Snapshot(remainingKm(lastKm, nextKm, mileage), remainingDays(lastDate, nextDate));
    }

    private static Snapshot fromInterval(Car car, ServiceEntity service, List<CustomerServiceRecord> records,
                                         List<ServiceHistory> histories) {
        List<CustomerServiceRecord> csrList = records.stream()
                .filter(row -> service.getId().equals(row.getServiceId()))
                .toList();
        List<ServiceHistory> historyList = histories.stream()
                .filter(row -> row.getServiceName() != null
                        && row.getServiceName().equalsIgnoreCase(service.getServiceName())
                        && row.getActionType() != null
                        && row.getActionType().stream().anyMatch(action -> action != null
                        && action.equalsIgnoreCase(service.getActionType())))
                .toList();
        LocalDate lastDate = java.util.stream.Stream.concat(
                        csrList.stream().map(CustomerServiceRecord::getDoneDate),
                        historyList.stream().map(ServiceHistory::getDoneDate))
                .filter(java.util.Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(car.getCreatedAt().toLocalDate());
        Integer lastKm = java.util.stream.Stream.concat(
                        csrList.stream().map(CustomerServiceRecord::getDoneKm),
                        historyList.stream().map(ServiceHistory::getDoneKm))
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        Integer kmLeft = null;
        Integer nextKm = null;
        if (service.getIntervalKm() != null && car.getMileage() != null) {
            long intervalKm = service.getIntervalKm();
            long usedKm = car.getMileage() - lastKm;
            kmLeft = (int) Math.max(intervalKm - usedKm, 0);
            nextKm = Math.toIntExact(lastKm + intervalKm);
        }

        Integer daysLeft = null;
        if (service.getIntervalMonth() != null) {
            LocalDate nextDate = lastDate.plusMonths(service.getIntervalMonth());
            daysLeft = remainingDays(lastDate, nextDate);
        }
        return new Snapshot(kmLeft, daysLeft);
    }

    public static Integer remainingKm(Integer lastKm, Integer nextKm, Long mileage) {
        if (lastKm == null || nextKm == null || mileage == null) {
            return null;
        }
        return (int) Math.max(nextKm - mileage, 0);
    }

    public static Integer remainingDays(LocalDate lastDate, LocalDate nextDate) {
        if (lastDate == null || nextDate == null) {
            return null;
        }
        long days = Math.max(nextDate.toEpochDay() - LocalDate.now().toEpochDay(), 0);
        if (days > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) days;
    }
}
