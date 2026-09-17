package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * tr: Admin history ekranından aracı tamamen siler veya müşteri listesinden koparır; Redis evict commit sonrası.
 * en: From the admin history screen, fully deletes a car or unlinks it from the customer; Redis evict runs after commit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCarPurgeService {

    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final RedisCacheService redisCacheService;

    @PersistenceContext
    private EntityManager entityManager;

    public enum DeleteOutcome {
        DELETED,
        NOT_FOUND,
        VIN_MISMATCH
    }

    public enum UnlinkOutcome {
        UNLINKED,
        NOT_FOUND,
        ALREADY_ORPHAN
    }

    /**
     * tr: VIN onayı eşleşirse aracı ve bağlı visit/history/percentage/foto satırlarını siler, cache düşürür.
     * en: If the VIN confirmation matches, deletes the car and related visit/history/percentage/photo rows, then evicts cache.
     */
    @Transactional
    public DeleteOutcome deleteCompletely(Long carId, String confirmVin) {
        Car car = carRepository.findByCarId(carId);
        if (car == null) {
            return DeleteOutcome.NOT_FOUND;
        }
        if (!vinEquals(car.getVin(), confirmVin)) {
            log.warn("ADMIN_PURGE_VIN_MISMATCH carId={}", carId);
            return DeleteOutcome.VIN_MISMATCH;
        }

        String ownerUserId = redisCacheService.ownerUserId(car);
        String vin = car.getVin();
        Long id = car.getCarId();

        nativeDelete("DELETE FROM visit_parts WHERE visit_id IN (SELECT id FROM visits WHERE car_id = :carId)", id);
        nativeDelete("DELETE FROM visit_service_lines WHERE visit_id IN (SELECT id FROM visits WHERE car_id = :carId)", id);
        nativeDelete("DELETE FROM visits WHERE car_id = :carId", id);
        nativeDelete(
                "DELETE FROM service_history_parts WHERE service_history_id IN (SELECT id FROM service_histories WHERE car_id = :carId)",
                id);
        nativeDelete("DELETE FROM service_histories WHERE car_id = :carId", id);
        nativeDelete("DELETE FROM customer_service_records WHERE car_id = :carId", id);
        nativeDelete("DELETE FROM percentages WHERE car_id = :carId", id);
        nativeDelete("DELETE FROM photos WHERE car_id = :carId", id);
        nativeDelete("DELETE FROM cars WHERE car_id = :carId", id);
        entityManager.clear();

        log.info("ADMIN_PURGE_CAR carId={} vin={} owner={}", id, vin, ownerUserId);
        redisCacheService.evictCarAndHistoryAfterCommit(ownerUserId, vin);
        redisCacheService.evictCarPhotoAfterCommit(id);
        return DeleteOutcome.DELETED;
    }

    /**
     * tr: customer_id'yi null yapar (app listesinden çıkar) ve sahibin carlist cache'ini siler; history/visit durur.
     * en: Nulls customer_id (drops the car from the app list) and evicts that owner's carlist cache; history/visits stay.
     */
    @Transactional
    public UnlinkOutcome unlinkFromCustomer(Long carId) {
        Car car = carRepository.findByCarId(carId);
        if (car == null) {
            return UnlinkOutcome.NOT_FOUND;
        }
        Customer owner = car.getCustomer();
        if (owner == null || owner.getUserId() == null) {
            return UnlinkOutcome.ALREADY_ORPHAN;
        }
        String ownerUserId = String.valueOf(owner.getUserId());
        if (owner.getCars() != null) {
            owner.getCars().remove(car);
        }
        car.setCustomer(null);
        carRepository.save(car);
        customerRepository.save(owner);
        log.info("ADMIN_UNLINK_CAR carId={} owner={}", carId, ownerUserId);
        redisCacheService.evictCarListAfterCommit(ownerUserId);
        return UnlinkOutcome.UNLINKED;
    }

    private void nativeDelete(String sql, Long carId) {
        entityManager.createNativeQuery(sql)
                .setParameter("carId", carId)
                .executeUpdate();
    }

    static boolean vinEquals(String stored, String typed) {
        return normalizeVin(stored).equals(normalizeVin(typed)) && !normalizeVin(stored).isEmpty();
    }

    static String normalizeVin(String vin) {
        if (vin == null) {
            return "";
        }
        return vin.replace(" ", "").trim().toUpperCase(Locale.ROOT);
    }
}
