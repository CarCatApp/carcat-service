package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingPackageLine;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.MaintenanceTemplate;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.enums.HyperServiceMapping;
import com.carland.carland_service.repository.BookingPackageLineRepository;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * tr: Katalogdaki maintenance_code değerini bu arabanın şablonundaki services.id ile eşler.
 *     Book anında paketin dövri satırlarına bu id kopyalanır.
 * en: Maps a catalog maintenance_code to this car's template services.id.
 *     Periodic package lines copy that id when the booking is created.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingPercentageSnapshot {

    private final ServiceEntityRepository serviceEntityRepository;
    private final BranchCarePackageItemRepository carePackageItemRepository;
    private final BookingPackageLineRepository packageLineRepository;

    /**
     * tr: Kod boş, bilinmeyen veya bu şablonda karşılığı yoksa null. Book'u düşürmez.
     * en: Null when the code is blank, unknown, or absent from this template. Does not fail the booking.
     */
    public Long resolve(Car car, String maintenanceCode) {
        if (car == null || maintenanceCode == null || maintenanceCode.isBlank()) {
            return null;
        }
        String nameEn;
        try {
            String code = HyperServiceMapping.normalizeCode(maintenanceCode);
            nameEn = code == null ? null : HyperServiceMapping.toNameEn(code).orElse(null);
        } catch (IllegalArgumentException ex) {
            log.info("percentage code unknown | carId={} code={}", car.getCarId(), maintenanceCode);
            return null;
        }
        if (nameEn == null) {
            return null;
        }
        MaintenanceTemplate template = car.getMaintenanceTemplate();
        if (template == null) {
            return null;
        }
        Long found = null;
        List<ServiceEntity> services = serviceEntityRepository.findAllByMaintenanceTemplate(template);
        if (services == null) {
            return null;
        }
        for (ServiceEntity service : services) {
            if (service.getNameEn() == null || !service.getNameEn().trim().equalsIgnoreCase(nameEn)) {
                continue;
            }
            if (found != null) {
                log.info("percentage code matches several template rows, using the first | carId={} nameEn={} serviceId={}",
                        car.getCarId(), nameEn, found);
                break;
            }
            found = service.getId();
        }
        return found;
    }

    /**
     * tr: Paketin dövri satırlarını book'a kopyalar. Yüzde id'si o an çözülür.
     * en: Copies the package's periodic lines onto the booking. The percentage id is resolved then.
     */
    public void snapshotPackage(Booking booking, Car car) {
        if (booking == null || booking.getCarePackageId() == null || booking.getId() == null) {
            return;
        }
        List<BranchCarePackageItem> items = carePackageItemRepository.findByCarePackage_IdOrderByIdAsc(booking.getCarePackageId());
        if (items == null) {
            return;
        }
        for (BranchCarePackageItem item : items) {
            OfferedService service = item == null ? null : item.getOfferedService();
            if (item == null
                    || !Boolean.TRUE.equals(item.getEnabled())
                    || service == null
                    || service.getId() == null
                    || !Boolean.TRUE.equals(service.getActive())
                    || !Boolean.TRUE.equals(service.getPeriodic())) {
                continue;
            }
            packageLineRepository.save(BookingPackageLine.builder()
                    .booking(booking)
                    .offeredServiceId(service.getId())
                    .titleJson(service.getTitleJson())
                    .percentageServiceId(resolve(car, service.getMaintenanceCode()))
                    .build());
        }
    }
}
