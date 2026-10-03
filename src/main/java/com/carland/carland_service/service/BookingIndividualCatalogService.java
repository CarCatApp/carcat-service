package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingIndividualServiceFilterView;
import com.carland.carland_service.dto.booking.BookingIndividualServiceFiltersResponse;
import com.carland.carland_service.dto.booking.BookingIndividualServiceView;
import com.carland.carland_service.dto.booking.BookingIndividualServicesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceFilter;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.IndividualServiceFilterRepository;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tr: Müşteriye şubenin açık fərdi xidmət listesi.
 * en: Active individual services of a branch for the owner app.
 */
@Service
@RequiredArgsConstructor
public class BookingIndividualCatalogService {

    private static final TypeReference<Map<String, String>> MAP = new TypeReference<>() {};

    private final BranchRepository branchRepository;
    private final BranchIndividualServiceRepository branchIndividualServiceRepository;
    private final IndividualServiceFilterRepository individualServiceFilterRepository;
    private final CarRepository carRepository;
    private final PercentageRepository percentageRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public BookingIndividualServiceFiltersResponse filters(String acceptLanguage) {
        String lang = BookingMineService.langOf(acceptLanguage);
        List<BookingIndividualServiceFilterView> filters = new ArrayList<>();
        for (IndividualServiceFilter row : individualServiceFilterRepository.findAllByOrderByIdAsc()) {
            filters.add(BookingIndividualServiceFilterView.builder()
                    .id(row.getId())
                    .name(localizedName(row, lang))
                    .build());
        }
        return BookingIndividualServiceFiltersResponse.builder().filters(filters).build();
    }

    @Transactional(readOnly = true)
    public BookingIndividualServicesResponse list(Long branchId, String acceptLanguage, String filter,
                                                   Long carId, Long userId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }
        Map<String, Integer> byCode = carId == null ? null : percentagesOf(carId, userId);
        String lang = BookingMineService.langOf(acceptLanguage);
        List<BranchIndividualService> found = branchIndividualServiceRepository.findByBranch_Id(branchId);
        List<BranchIndividualService> rows = new ArrayList<>(found == null ? List.of() : found);
        rows.sort(Comparator
                .comparing((BranchIndividualService row) -> sortOf(row.getIndividualService()))
                .thenComparing(row -> row.getIndividualService() == null ? 0L : row.getIndividualService().getId()));
        boolean all = isAll(filter);
        List<BookingIndividualServiceView> services = new ArrayList<>();
        for (BranchIndividualService row : rows) {
            IndividualService catalog = row.getIndividualService();
            if (!Boolean.TRUE.equals(row.getActive())
                    || catalog == null
                    || !Boolean.TRUE.equals(catalog.getActive())) {
                continue;
            }
            if (!all && !matches(catalog.getFilter(), filter)) {
                continue;
            }
            Integer percentage = null;
            if (byCode != null) {
                if (!byCode.containsKey(catalog.getCode())) {
                    continue;
                }
                percentage = byCode.get(catalog.getCode());
            }
            services.add(toView(catalog, row, lang, percentage));
        }
        return BookingIndividualServicesResponse.builder().branchId(branchId).services(services).build();
    }

    /**
     * tr: Katalog kodu → kalan ömür yüzdesi. Km ve ay yüzdesinden küçük olanı yazılır.
     *     Kayıt yoksa null: liste daralmaz. Aynı koda birkaç satır düşerse daha küçük yüzde kalır.
     * en: Catalog code to remaining-life percent. The smaller of the km and month percents is stored.
     *     No rows means null: the list stays wide. When several rows share a code, the smaller percent stays.
     */
    private Map<String, Integer> percentagesOf(Long carId, Long userId) {
        Car car = carRepository.findByCarId(carId);
        if (car == null) {
            throw new ResourceNotFoundException("car not found");
        }
        Customer owner = car.getCustomer();
        if (owner == null || owner.getUserId() == null || !owner.getUserId().equals(userId)) {
            throw new ForbiddenException("car is not yours");
        }
        List<Percentage> found = percentageRepository.findAllByCarId(carId);
        if (found == null || found.isEmpty()) {
            return null;
        }
        List<Long> serviceIds = found.stream().map(Percentage::getServiceId).filter(id -> id != null).distinct().toList();
        Map<Long, ServiceEntity> services = new HashMap<>();
        if (!serviceIds.isEmpty()) {
            for (ServiceEntity entity : serviceEntityRepository.findAllById(serviceIds)) {
                services.put(entity.getId(), entity);
            }
        }
        Map<String, Integer> best = new HashMap<>();
        Map<String, Integer> score = new HashMap<>();
        Map<String, Long> nextDay = new HashMap<>();
        for (Percentage row : found) {
            ServiceEntity entity = row.getServiceId() == null ? null : services.get(row.getServiceId());
            String az = entity != null ? entity.getNameAz() : row.getServiceNameAz();
            String en = entity != null ? entity.getNameEn() : row.getServiceNameEn();
            String ru = entity != null ? entity.getNameRu() : row.getServiceNameRu();
            String code = IndividualCatalogSync.codeOf(az, en, ru);
            if (code.isEmpty()) {
                continue;
            }
            Integer km = kmRemaining(row.getLastServiceKm(), row.getNextServiceKm(), car.getMileage());
            if (km == null) {
                km = row.getKmPercentage();
            }
            Integer month = monthRemaining(row.getLastServiceDate(), row.getNextServiceDate());
            if (month == null) {
                month = row.getMonthPercentage();
            }
            Integer nearer = nearerPercent(km, month);
            int rowScore = nearer == null ? Integer.MAX_VALUE : nearer;
            long rowNext = row.getNextServiceDate() == null ? Long.MAX_VALUE : row.getNextServiceDate().toEpochDay();
            Integer current = score.get(code);
            if (current == null || rowScore < current || (rowScore == current && rowNext < nextDay.getOrDefault(code, Long.MAX_VALUE))) {
                best.put(code, nearer);
                score.put(code, rowScore);
                nextDay.put(code, rowNext);
            }
        }
        return best;
    }

    private static Integer nearerPercent(Integer km, Integer month) {
        if (km == null) {
            return month;
        }
        if (month == null) {
            return km;
        }
        return Math.min(km, month);
    }

    /** Same remaining-life percent as the percentage screen. 0 means due. */
    private static Integer kmRemaining(Integer lastKm, Integer nextKm, Long mileage) {
        if (lastKm == null || nextKm == null || mileage == null) {
            return null;
        }
        long totalKm = nextKm - lastKm;
        long remaining = nextKm - mileage;
        if (totalKm > 0) {
            int pct = (int) Math.round((remaining * 100.0) / totalKm);
            return Math.max(0, Math.min(100, pct));
        }
        return 0;
    }

    private static Integer monthRemaining(LocalDate lastDate, LocalDate nextDate) {
        if (lastDate == null || nextDate == null) {
            return null;
        }
        long totalDays = nextDate.toEpochDay() - lastDate.toEpochDay();
        long remainingDays = Math.max(nextDate.toEpochDay() - LocalDate.now().toEpochDay(), 0);
        if (totalDays > 0) {
            int pct = (int) Math.round((remainingDays * 100.0) / totalDays);
            return Math.max(0, Math.min(100, pct));
        }
        return 0;
    }

    private BookingIndividualServiceView toView(IndividualService catalog, BranchIndividualService row, String lang,
                                                Integer percentage) {
        int[] qepik = BookingSelectionViews.qepik(row.getPriceSimple(), row.getPriceMedium(), row.getPriceComplex());
        return BookingIndividualServiceView.builder()
                .id(catalog.getId())
                .code(catalog.getCode())
                .name(BookingMineService.catalogText(titles(catalog.getTitleJson()), lang))
                .priceMin(qepik == null ? null : qepik[0])
                .priceMax(qepik == null ? null : qepik[1])
                .currency("AZN")
                .unit(BookingCreateService.UNIT)
                .individualServiceMappedPercentage(percentage)
                .build();
    }

    private static boolean isAll(String filter) {
        return filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter.trim());
    }

    private static boolean matches(IndividualServiceFilter chip, String filter) {
        if (chip == null || chip.getId() == null) {
            return false;
        }
        String raw = filter.trim();
        if (raw.equals(String.valueOf(chip.getId()))) {
            return true;
        }
        return equalsName(chip.getNameAz(), raw)
                || equalsName(chip.getNameEn(), raw)
                || equalsName(chip.getNameRu(), raw);
    }

    private static boolean equalsName(String value, String raw) {
        return value != null && value.equalsIgnoreCase(raw);
    }

    private static String localizedName(IndividualServiceFilter row, String lang) {
        return BookingMineService.catalogText(Map.of(
                "az", row.getNameAz() == null ? "" : row.getNameAz(),
                "en", row.getNameEn() == null ? "" : row.getNameEn(),
                "ru", row.getNameRu() == null ? "" : row.getNameRu()
        ), lang);
    }

    private static int sortOf(IndividualService catalog) {
        if (catalog == null || catalog.getSortOrder() == null) {
            return 0;
        }
        return catalog.getSortOrder();
    }

    private Map<String, String> titles(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(json, MAP);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            return Map.of("az", json);
        }
    }
}
