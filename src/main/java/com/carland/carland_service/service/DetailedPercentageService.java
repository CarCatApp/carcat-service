package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.BookingPackageLine;
import com.carland.carland_service.entity.BookingSelectedService;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.enums.VisitLineKind;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BookingPackageLineRepository;
import com.carland.carland_service.repository.BookingSelectedServiceRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: Detailed şubede xidməti tamamla, yüzdeyi Hyper adıyla değil bu arabanın services.id değeriyle günceller.
 *     Webhook ve Hyper toplu geçmiş bu sınıfa girmez. Çağıran, aynı transaction içinde
 *     {@code evictCarAndHistoryAfterCommit} ile carlist'i düşürür. Yüzde listesinin kendi Redis key'i yoktur.
 * en: On a detailed branch, complete-service updates the percentage by this car's services.id,
 *     not by a Hyper name. Webhook and Hyper bulk history do not enter this class. The caller
 *     evicts the car list in the same transaction. The percentage list has no Redis key of its own.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DetailedPercentageService {

    private final BookingPercentageSnapshot snapshot;
    private final BookingIndividualLineRepository individualLineRepository;
    private final BookingPackageLineRepository packageLineRepository;
    private final BookingSelectedServiceRepository selectedServiceRepository;
    private final IndividualServiceRepository individualServiceRepository;
    private final OfferedServiceRepository offeredServiceRepository;
    private final PercentageRepository percentageRepository;
    private final ServiceEntityRepository serviceEntityRepository;

    /**
     * tr: Ziyaretteki dövri satırlar için yüzdeyi günceller. Book'ta kopyalanmış id varsa onu kullanır.
     *     Kopya boşsa katalog kodundan o an çözer (eski book ve tamamlanırken eklenen kart).
     * en: Updates percentages for periodic visit lines. Uses the id copied onto the booking when present.
     *     A blank copy is resolved from the catalog code now (older bookings and cards added at completion).
     */
    public void applyFromVisit(Car car, Booking booking, Visit visit) {
        if (car == null || booking == null || booking.getId() == null || visit == null || visit.getServices() == null) {
            return;
        }
        Map<Long, Long> individualIds = individualIds(booking.getId());
        Map<Long, Long> offeredIds = offeredIds(booking.getId());
        String recordId = visit.getId() == null ? null : "visit:" + visit.getId();
        for (VisitServiceLine line : visit.getServices()) {
            VisitLineKind kind = VisitLineKind.fromApi(line.getKind());
            if (line == null || kind == null || !kind.periodic()) {
                continue;
            }
            Long serviceId = serviceId(car, line, individualIds, offeredIds);
            if (serviceId == null) {
                log.info("panel percentage skipped, no template service | carId={} bookingId={} offered={} individual={}",
                        car.getCarId(), booking.getId(), line.getOfferedServiceId(), line.getIndividualServiceId());
                continue;
            }
            LocalDate doneDate = line.getDoneDate() != null ? line.getDoneDate() : visit.getLastServiceDate();
            Integer doneKm = line.getDoneKm() != null ? line.getDoneKm() : visit.getLastServiceMileage();
            apply(car, serviceId, doneDate, doneKm, line.getNextServiceDate(), line.getNextServiceMileage(), recordId);
        }
    }

    private Long serviceId(Car car, VisitServiceLine line, Map<Long, Long> individualIds, Map<Long, Long> offeredIds) {
        if (line.getIndividualServiceId() != null) {
            Long copied = individualIds.get(line.getIndividualServiceId());
            if (copied != null) {
                return copied;
            }
            return individualServiceRepository.findById(line.getIndividualServiceId())
                    .map(service -> snapshot.resolve(car, service.getMaintenanceCode()))
                    .orElse(null);
        }
        if (line.getOfferedServiceId() == null) {
            return null;
        }
        Long copied = offeredIds.get(line.getOfferedServiceId());
        if (copied != null) {
            return copied;
        }
        return offeredServiceRepository.findById(line.getOfferedServiceId())
                .filter(service -> Boolean.TRUE.equals(service.getPeriodic()))
                .map(service -> snapshot.resolve(car, service.getMaintenanceCode()))
                .orElse(null);
    }

    private Map<Long, Long> individualIds(Long bookingId) {
        Map<Long, Long> out = new HashMap<>();
        List<BookingIndividualLine> lines = individualLineRepository.findByBooking_IdOrderByIdAsc(bookingId);
        if (lines == null) {
            return out;
        }
        for (BookingIndividualLine line : lines) {
            if (line.getIndividualServiceId() != null && line.getPercentageServiceId() != null) {
                out.put(line.getIndividualServiceId(), line.getPercentageServiceId());
            }
        }
        return out;
    }

    private Map<Long, Long> offeredIds(Long bookingId) {
        Map<Long, Long> out = new HashMap<>();
        List<BookingPackageLine> packageLines = packageLineRepository.findByBooking_IdOrderByIdAsc(bookingId);
        if (packageLines != null) {
            for (BookingPackageLine line : packageLines) {
                if (line.getOfferedServiceId() != null && line.getPercentageServiceId() != null) {
                    out.put(line.getOfferedServiceId(), line.getPercentageServiceId());
                }
            }
        }
        List<BookingSelectedService> selected = selectedServiceRepository.findByBooking_IdOrderByIdAsc(bookingId);
        if (selected != null) {
            for (BookingSelectedService line : selected) {
                if (line.getOfferedServiceId() != null && line.getPercentageServiceId() != null) {
                    out.putIfAbsent(line.getOfferedServiceId(), line.getPercentageServiceId());
                }
            }
        }
        return out;
    }

    private void apply(Car car, Long serviceId, LocalDate doneDate, Integer doneKm,
                       LocalDate requestedNextDate, Integer requestedNextKm, String recordId) {
        Percentage percentage = percentageRepository.findByServiceIdAndCarId(serviceId, car.getCarId());
        if (percentage == null) {
            percentage = createFromTemplate(car, serviceId);
            if (percentage == null) {
                log.info("panel percentage skipped, service is not on this car | carId={} serviceId={}",
                        car.getCarId(), serviceId);
                return;
            }
        }
        if (isOlderThanStored(doneDate, doneKm, percentage)) {
            log.info("panel percentage skipped, stored service is newer | carId={} serviceId={} doneDate={} storedDate={}",
                    car.getCarId(), serviceId, doneDate, percentage.getLastServiceDate());
            return;
        }
        if (doneDate != null) {
            percentage.setLastServiceDate(doneDate);
        }
        if (doneKm != null) {
            percentage.setLastServiceKm(doneKm);
        }
        Integer nextKm = resolveNextKm(requestedNextKm, percentage.getLastServiceKm(), percentage.getIntervalKm());
        if (nextKm != null) {
            percentage.setNextServiceKm(nextKm);
        }
        LocalDate nextDate = requestedNextDate;
        Integer intervalMonth = percentage.getIntervalMonth();
        if (nextDate == null && percentage.getLastServiceDate() != null && intervalMonth != null && intervalMonth > 0) {
            nextDate = percentage.getLastServiceDate().plusMonths(intervalMonth);
        }
        if (nextDate != null) {
            percentage.setNextServiceDate(nextDate);
        }
        Integer kmPercentage = PercentageLifeService.kmPercentage(
                percentage.getLastServiceKm(), percentage.getNextServiceKm(), car.getMileage());
        if (kmPercentage != null) {
            percentage.setKmPercentage(kmPercentage);
        }
        Integer remainingKm = PercentageLifeService.remainingKm(
                percentage.getLastServiceKm(), percentage.getNextServiceKm(), car.getMileage());
        if (remainingKm != null) {
            percentage.setRemainingKm(remainingKm);
        }
        Integer monthPercentage = PercentageLifeService.monthPercentage(
                percentage.getLastServiceDate(), percentage.getNextServiceDate());
        if (monthPercentage != null) {
            percentage.setMonthPercentage(monthPercentage);
        }
        if (percentage.getNextServiceDate() != null) {
            percentage.setRemainingMonths(percentage.getNextServiceDate());
        }
        percentage.setStatus(PercentageStatus.EDITED_BY_PARTNER.name());
        percentage.setPartnerRecordId(recordId);
        percentage.setLastPartnerSyncAt(LocalDateTime.now());
        percentageRepository.save(percentage);
        log.info("panel percentage applied | carId={} serviceId={} lastKm={} nextKm={} lastDate={} nextDate={}",
                car.getCarId(), serviceId, percentage.getLastServiceKm(), percentage.getNextServiceKm(),
                percentage.getLastServiceDate(), percentage.getNextServiceDate());
    }

    private Percentage createFromTemplate(Car car, Long serviceId) {
        ServiceEntity entity = serviceEntityRepository.findById(serviceId).orElse(null);
        MaintenanceTemplate carTemplate = car.getMaintenanceTemplate();
        MaintenanceTemplate serviceTemplate = entity == null ? null : entity.getMaintenanceTemplate();
        if (entity == null || carTemplate == null || carTemplate.getId() == null
                || serviceTemplate == null || !carTemplate.getId().equals(serviceTemplate.getId())) {
            return null;
        }
        return Percentage.builder()
                .intervalKm(entity.getIntervalKm())
                .intervalMonth(entity.getIntervalMonth())
                .serviceName(entity.getServiceName())
                .serviceNameAz(entity.getNameAz())
                .serviceNameEn(entity.getNameEn())
                .serviceNameRu(entity.getNameRu())
                .actionType(entity.getActionType())
                .serviceId(entity.getId())
                .important(entity.isImportant())
                .status(PercentageStatus.CREATED.name())
                .carId(car.getCarId())
                .build();
    }

    /** true when the stored last service is later than this completion. Same day with a higher km also stays. */
    private static boolean isOlderThanStored(LocalDate doneDate, Integer doneKm, Percentage percentage) {
        LocalDate storedDate = percentage.getLastServiceDate();
        if (doneDate == null || storedDate == null) {
            return false;
        }
        int cmp = storedDate.compareTo(doneDate);
        if (cmp > 0) {
            return true;
        }
        if (cmp < 0) {
            return false;
        }
        Integer storedKm = percentage.getLastServiceKm();
        return storedKm != null && doneKm != null && storedKm > doneKm;
    }

    private Integer resolveNextKm(Integer requested, Integer lastKm, Long intervalKm) {
        if (requested != null && lastKm != null && requested > lastKm) {
            return requested;
        }
        if (lastKm != null && intervalKm != null && intervalKm > 0) {
            try {
                return Math.toIntExact(lastKm.longValue() + intervalKm);
            } catch (ArithmeticException ex) {
                log.warn("panel percentage next km overflow | lastKm={} intervalKm={}", lastKm, intervalKm);
            }
        }
        if (requested != null && (lastKm == null || requested > lastKm)) {
            return requested;
        }
        return null;
    }
}
