package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.VinServiceOpen;
import com.carland.carland_service.enums.PartnerId;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ServiceHistoryRepository;
import com.carland.carland_service.repository.VinServiceOpenRepository;
import com.carland.carland_service.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

/**
 * tr: CRCT-388. Seçili araç kilitliyse şube listesinde yalnız partner 1 kalır.
 *     Sıra: Hyper kaydı yok → 365 günden eski → ilk Karkat olayı randevu → yoksa kilit.
 *     365 ve randevu bir kez açınca bayrak yazılır; sonraki Hyper ziyareti geri kapatmaz.
 * en: CRCT-388. A locked car keeps only partner 1 on the branch list.
 *     Order: no Hyper record, then older than the lock window, then a first Karkat booking, else lock.
 *     The year rule and the booking rule write a flag once; a later Hyper visit does not close it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HyperBranchLockService {

    static final int DEFAULT_LOCK_DAYS = 365;

    private final CarRepository carRepository;
    private final VisitRepository visitRepository;
    private final ServiceHistoryRepository serviceHistoryRepository;
    private final BookingRepository bookingRepository;
    private final VinServiceOpenRepository vinServiceOpenRepository;
    private final VinServiceOpenWriter vinServiceOpenWriter;

    @Value("${booking.hyperservice-lock-days:365}")
    private int lockDays;

    /**
     * tr: true ise çağıran yalnız partner 1 şubelerini bırakır.
     * en: When true, the caller keeps only partner 1 branches.
     */
    @Transactional(readOnly = true)
    public boolean locked(Long carId, Long userId) {
        Car car = ownedCar(carId, userId);
        String vin = normalizeVin(car.getVin());
        if (!vin.isEmpty()) {
            Optional<VinServiceOpen> open = vinServiceOpenRepository.findByVin(vin);
            if (open.isPresent()) {
                log.info("HYPER_LOCK carId={} vin={} rule=OPEN_FLAG reason={} locked=false",
                        carId, vin, open.get().getReason());
                return false;
            }
        }

        Long hyperId = PartnerId.HYPER.getId();
        boolean hyperVisit = visitRepository.countHyperVisits(car.getCarId(), hyperId) > 0;
        boolean hyperHistory = serviceHistoryRepository.countHyperRows(car.getCarId(), hyperId) > 0;
        if (!hyperVisit && !hyperHistory) {
            log.info("HYPER_LOCK carId={} vin={} rule=NOT_HYPER locked=false", carId, vin);
            return false;
        }

        LocalDate lastVisit = latest(car.getCarId(), hyperId);
        LocalDate today = LocalDate.now(StaffSlotWindows.ZONE);
        if (lastVisit != null && lastVisit.isBefore(today.minusDays(resolvedLockDays()))) {
            open(vin, car.getCarId(), VinServiceOpen.PASSIVE_YEAR);
            log.info("HYPER_LOCK carId={} vin={} rule=PASSIVE_YEAR lastVisit={} locked=false",
                    carId, vin, lastVisit);
            return false;
        }

        if (openedByFirstBooking(userId)) {
            open(vin, car.getCarId(), VinServiceOpen.KARKAT_BOOKING);
            log.info("HYPER_LOCK carId={} vin={} rule=KARKAT_BOOKING locked=false", carId, vin);
            return false;
        }

        log.info("HYPER_LOCK carId={} vin={} rule=ACTIVE_HYPER lastVisit={} locked=true",
                carId, vin, lastVisit);
        return true;
    }

    /**
     * tr: İlk randevudan önce ziyaret varsa müşteri import ile gelmiştir. Tarihsiz ziyaret de geçmiş sayılır.
     *     Ziyaret hiç yoksa tarihsiz service_history satırı import geçmişidir.
     * en: A visit before the first booking means the customer arrived by import. A visit with no timestamp counts too.
     *     When there is no visit, an undated service_history row is import history.
     */
    private boolean openedByFirstBooking(Long userId) {
        OffsetDateTime firstBooking = bookingRepository.findEarliestCreatedAtByCustomer(userId);
        if (firstBooking == null || userId == null) {
            return false;
        }
        if (visitRepository.existsByCar_Customer_UserIdAndCreatedAtIsNull(userId)) {
            return false;
        }
        LocalDateTime earliestVisit = visitRepository.findEarliestCreatedAtByCustomer(userId);
        if (earliestVisit != null) {
            Instant visitAt = earliestVisit.atZone(ZoneId.systemDefault()).toInstant();
            return !visitAt.isBefore(firstBooking.toInstant());
        }
        return !serviceHistoryRepository.existsByCar_Customer_UserId(userId);
    }

    private LocalDate latest(Long carId, Long hyperId) {
        LocalDate visit = visitRepository.findLatestHyperVisitDate(carId, hyperId);
        LocalDate history = serviceHistoryRepository.findLatestHyperDoneDate(carId, hyperId);
        if (visit == null) {
            return history;
        }
        if (history == null || visit.isAfter(history)) {
            return visit;
        }
        return history;
    }

    private void open(String vin, Long carId, String reason) {
        if (vin == null || vin.isBlank()) {
            return;
        }
        try {
            vinServiceOpenWriter.saveIfAbsent(vin, reason, carId);
        } catch (DataIntegrityViolationException ex) {
            log.info("HYPER_LOCK flag already stored vin={}", vin);
        }
    }

    private int resolvedLockDays() {
        return lockDays < 1 ? DEFAULT_LOCK_DAYS : lockDays;
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

    /** tr: Büyük harf, bütün boşluklar silinir. / en: Upper case, every space removed. */
    static String normalizeVin(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
