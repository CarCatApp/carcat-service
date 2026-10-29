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

import com.carland.carland_service.enums.HyperServiceMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * tr: Detailed şubede xidməti tamamla, eşleşebilen her dövri satırın yüzdesini bu arabanın services.id
 *     değeriyle günceller. Book kopyası, katalog kodu (enum, takma ad veya İngilizce ad) ve satırdaki
 *     Hyper kimliği sırayla denenir. Yazılan satıra Hyper kimliği konur ki geçmiş yenilemesi bu ziyareti
 *     görsün. Yürüyüş değiştiyse diğer kalemlerin km yüzdesi de tazelenir. Webhook bu sınıfa girmez.
 *     Çağıran, aynı transaction içinde {@code evictCarAndHistoryAfterCommit} ile carlist'i düşürür.
 * en: On a detailed branch, complete-service updates every periodic line that can be tied to this car's
 *     services.id. The booking copy, the catalog code (enum, alias or English name) and the line's Hyper
 *     id are tried in that order. The written line keeps a Hyper id so a history reload sees this visit.
 *     When mileage changed, the other items' km percentages are refreshed too. Webhook does not enter
 *     this class. The caller evicts the car list in the same transaction.
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
     * tr: Ziyaretteki her dövri satır için, bu arabaya bağlanabilen yüzdeyi günceller.
     *     Sıra: book kopyası, katalog kodu, satırdaki Hyper kimliği. Bağlanan satıra Hyper kimliği yazılır.
     *     Ardından yürüyüşe göre diğer kalemlerin km yüzdesi tazelenir.
     * en: Updates the percentage of every periodic line that can be tied to this car.
     *     Order: booking copy, catalog code, the line's Hyper id. A tied line stores the Hyper id.
     *     Then the other items' km percentages are refreshed from the current mileage.
     */
    public void applyFromVisit(Car car, Booking booking, Visit visit) {
        if (car == null || booking == null || booking.getId() == null || visit == null || visit.getServices() == null) {
            return;
        }
        Map<Long, Long> individualIds = individualIds(booking.getId());
        Map<Long, Long> offeredIds = offeredIds(booking.getId());
        String recordId = visit.getId() == null ? null : "visit:" + visit.getId();
        Set<Long> applied = new HashSet<>();
        for (VisitServiceLine line : visit.getServices()) {
            if (line == null) {
                continue;
            }
            VisitLineKind kind = VisitLineKind.fromApi(line.getKind());
            if (kind == null || !kind.periodic()) {
                continue;
            }
            Long serviceId = serviceId(car, line, individualIds, offeredIds);
            if (serviceId == null) {
                log.info("panel percentage skipped, no template service | carId={} bookingId={} offered={} individual={} code={}",
                        car.getCarId(), booking.getId(), line.getOfferedServiceId(), line.getIndividualServiceId(),
                        line.getUniversalServiceId());
                continue;
            }
            LocalDate doneDate = line.getDoneDate() != null ? line.getDoneDate() : visit.getLastServiceDate();
            Integer doneKm = line.getDoneKm() != null ? line.getDoneKm() : visit.getLastServiceMileage();
            if (apply(car, line, serviceId, doneDate, doneKm, line.getNextServiceDate(), line.getNextServiceMileage(), recordId)) {
                applied.add(serviceId);
            }
        }
        refreshOtherKmLives(car, applied);
    }

    /**
     * tr: Bu satırı bu arabanın bir bakım kalemine bağlayan ilk id. Kopya bu arabada değilse katalog ve
     *     satır kimliği denenir.
     * en: First id that ties this line to a maintenance item on this car. When the copy is not on this
     *     car, the catalog code and the line id are tried.
     */
    private Long serviceId(Car car, VisitServiceLine line, Map<Long, Long> individualIds, Map<Long, Long> offeredIds) {
        Long copied = copiedServiceId(line, individualIds, offeredIds);
        if (onThisCar(car, copied)) {
            return copied;
        }
        Long fromCatalog = resolveCatalog(car, line);
        if (onThisCar(car, fromCatalog)) {
            return fromCatalog;
        }
        Long fromLine = snapshot.resolve(car, line.getUniversalServiceId());
        if (onThisCar(car, fromLine)) {
            return fromLine;
        }
        return null;
    }

    private Long copiedServiceId(VisitServiceLine line, Map<Long, Long> individualIds, Map<Long, Long> offeredIds) {
        if (line.getIndividualServiceId() != null) {
            return individualIds.get(line.getIndividualServiceId());
        }
        if (line.getOfferedServiceId() != null) {
            return offeredIds.get(line.getOfferedServiceId());
        }
        return null;
    }

    private Long resolveCatalog(Car car, VisitServiceLine line) {
        String code = null;
        if (line.getIndividualServiceId() != null) {
            code = individualServiceRepository.findById(line.getIndividualServiceId())
                    .map(service -> service.getMaintenanceCode())
                    .orElse(null);
        } else if (line.getOfferedServiceId() != null) {
            code = offeredServiceRepository.findById(line.getOfferedServiceId())
                    .map(service -> service.getMaintenanceCode())
                    .orElse(null);
        }
        if (code == null || code.isBlank()) {
            return null;
        }
        return snapshot.resolve(car, code);
    }

    /** True when this car already has the percentage row, or the service sits on its template. */
    private boolean onThisCar(Car car, Long serviceId) {
        if (car == null || car.getCarId() == null || serviceId == null) {
            return false;
        }
        if (percentageRepository.findByServiceIdAndCarId(serviceId, car.getCarId()) != null) {
            return true;
        }
        ServiceEntity entity = findService(serviceId);
        MaintenanceTemplate carTemplate = car.getMaintenanceTemplate();
        MaintenanceTemplate serviceTemplate = entity == null ? null : entity.getMaintenanceTemplate();
        return entity != null && carTemplate != null && carTemplate.getId() != null
                && serviceTemplate != null && carTemplate.getId().equals(serviceTemplate.getId());
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

    private boolean apply(Car car, VisitServiceLine line, Long serviceId, LocalDate doneDate, Integer doneKm,
                          LocalDate requestedNextDate, Integer requestedNextKm, String recordId) {
        Percentage percentage = percentageRepository.findByServiceIdAndCarId(serviceId, car.getCarId());
        if (percentage == null) {
            percentage = createFromTemplate(car, serviceId);
            if (percentage == null) {
                log.info("panel percentage skipped, service is not on this car | carId={} serviceId={}",
                        car.getCarId(), serviceId);
                return false;
            }
        }
        if (isOlderThanStored(doneDate, doneKm, percentage)) {
            log.info("panel percentage skipped, stored service is newer | carId={} serviceId={} doneDate={} storedDate={}",
                    car.getCarId(), serviceId, doneDate, percentage.getLastServiceDate());
            return false;
        }
        fillFromTemplate(percentage, findService(serviceId));
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
        stampHyperId(line, percentage.getServiceNameEn());
        percentageRepository.save(percentage);
        log.info("panel percentage applied | carId={} serviceId={} lastKm={} nextKm={} lastDate={} nextDate={}",
                car.getCarId(), serviceId, percentage.getLastServiceKm(), percentage.getNextServiceKm(),
                percentage.getLastServiceDate(), percentage.getNextServiceDate());
        return true;
    }

    /** Copies a missing interval or English name from the template so next km and next date can be built. */
    private void fillFromTemplate(Percentage percentage, ServiceEntity entity) {
        if (percentage == null || entity == null) {
            return;
        }
        if (percentage.getIntervalKm() == null && entity.getIntervalKm() != null) {
            percentage.setIntervalKm(entity.getIntervalKm());
        }
        if (percentage.getIntervalMonth() == null && entity.getIntervalMonth() != null) {
            percentage.setIntervalMonth(entity.getIntervalMonth());
        }
        if ((percentage.getServiceNameEn() == null || percentage.getServiceNameEn().isBlank())
                && entity.getNameEn() != null && !entity.getNameEn().isBlank()) {
            percentage.setServiceNameEn(entity.getNameEn());
        }
    }

    /**
     * tr: Geçmiş yenilemesi satırı Hyper adıyla eşleştirebilsin diye kimliği yazar.
     *     Satır zaten eşleşiyorsa dokunulmaz.
     * en: Writes the Hyper id so a history reload can match the line by name.
     *     A line that already matches is left as it is.
     */
    private void stampHyperId(VisitServiceLine line, String nameEn) {
        if (line == null || HyperServiceMapping.matches(line.getUniversalServiceId(), nameEn)) {
            return;
        }
        String canonical = HyperServiceMapping.canonicalId(nameEn);
        if (canonical != null) {
            line.setUniversalServiceId(canonical);
        }
    }

    /** Km life of items this visit did not reset, using the car mileage already stored on the car. */
    private void refreshOtherKmLives(Car car, Set<Long> appliedServiceIds) {
        if (car == null || car.getCarId() == null) {
            return;
        }
        List<Percentage> rows = percentageRepository.findAllByCarId(car.getCarId());
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (Percentage percentage : rows) {
            if (percentage.getServiceId() != null && appliedServiceIds.contains(percentage.getServiceId())) {
                continue;
            }
            if (!refreshKmLife(percentage, car.getMileage())) {
                continue;
            }
            percentageRepository.save(percentage);
            log.info("panel percentage life refreshed | carId={} serviceId={} kmPercentage={} remainingKm={}",
                    car.getCarId(), percentage.getServiceId(), percentage.getKmPercentage(), percentage.getRemainingKm());
        }
    }

    private boolean refreshKmLife(Percentage percentage, Long mileage) {
        Integer km = PercentageLifeService.kmPercentage(
                percentage.getLastServiceKm(), percentage.getNextServiceKm(), mileage);
        Integer remaining = PercentageLifeService.remainingKm(
                percentage.getLastServiceKm(), percentage.getNextServiceKm(), mileage);
        boolean changed = false;
        if (km != null && !km.equals(percentage.getKmPercentage())) {
            percentage.setKmPercentage(km);
            changed = true;
        }
        if (remaining != null && !remaining.equals(percentage.getRemainingKm())) {
            percentage.setRemainingKm(remaining);
            changed = true;
        }
        return changed;
    }

    private ServiceEntity findService(Long serviceId) {
        if (serviceId == null) {
            return null;
        }
        Optional<ServiceEntity> found = serviceEntityRepository.findById(serviceId);
        return found == null ? null : found.orElse(null);
    }

    private Percentage createFromTemplate(Car car, Long serviceId) {
        ServiceEntity entity = findService(serviceId);
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
