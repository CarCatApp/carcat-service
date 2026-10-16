package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingIndividualServiceFilterView;
import com.carland.carland_service.dto.booking.BookingIndividualServiceFiltersResponse;
import com.carland.carland_service.dto.booking.BookingIndividualServiceView;
import com.carland.carland_service.dto.booking.BookingIndividualServicesResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.CustomerServiceRecord;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.IndividualServiceFilter;
import com.carland.carland_service.entity.IndividualServiceSource;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerServiceRecordRepository;
import com.carland.carland_service.repository.IndividualServiceFilterRepository;
import com.carland.carland_service.repository.IndividualServiceSourceRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final IndividualServiceSourceRepository individualServiceSourceRepository;
    private final PercentageLifeService percentageLifeService;
    private final CustomerServiceRecordRepository customerServiceRecordRepository;
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
                                                   Long carId, Long userId, String bookingEntry,
                                                   Long catalogServiceId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())
                || branch.getPartner() == null
                || !Boolean.TRUE.equals(branch.getPartner().getActive())) {
            throw new ResourceNotFoundException("Branch not found");
        }
        Car car = carId == null ? null : ownedCar(carId, userId);
        String lang = BookingMineService.langOf(acceptLanguage);
        String missingNote = missingNote(lang);
        List<BranchIndividualService> found = branchIndividualServiceRepository.findByBranch_Id(branchId);
        List<BranchIndividualService> rows = new ArrayList<>(found == null ? List.of() : found);
        Map<Long, PercentageLifeService.Snapshot> loaded = car == null ? Map.of() : percentageLifeService.byServiceId(car);
        Map<Long, PercentageLifeService.Snapshot> lives = loaded == null ? Map.of() : loaded;
        Map<Long, List<Long>> serviceIdsByIndividual = lives.isEmpty() ? Map.of() : linksOf(rows);
        Long clickedServiceId = carServiceEntry(bookingEntry) ? catalogServiceId : null;
        rows.sort(Comparator
                .comparing((BranchIndividualService row) -> sortOf(row.getIndividualService()))
                .thenComparing(row -> row.getIndividualService() == null ? 0L : row.getIndividualService().getId()));
        boolean all = isAll(filter);
        Map<Long, String> servicedStatuses = new HashMap<>();
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
            String note = null;
            Integer remainingKm = null;
            Integer remainingMonth = null;
            if (car != null) {
                Long serviceId = linkedServiceId(catalog.getId(), clickedServiceId, serviceIdsByIndividual, lives);
                PercentageLifeService.Snapshot life = serviceId == null ? null : lives.get(serviceId);
                if (life != null) {
                    remainingKm = life.remainingKm();
                    remainingMonth = life.remainingDays();
                    percentage = life.shownPercentage();
                }
                if (servicedStatusOf(serviceId, car, servicedStatuses) == null) {
                    note = missingNote;
                }
            }
            services.add(toView(catalog, row, lang, percentage, note, remainingKm, remainingMonth));
        }
        return BookingIndividualServicesResponse.builder()
                .branchId(branchId)
                .services(services)
                .preselectedServiceId(applyPreselect(services, bookingEntry, catalogServiceId))
                .build();
    }

    /**
     * tr: CAR_SERVICE deyilsə null (alan JSON-da yoxdur). Eşleşme yoxdursa boş Optional (JSON null).
     *     Ad oxunmur. Siyahı daralmaz. Bir neçə sətir varsa ən kiçik individual_services.id işarələnir.
     * en: Null when this is not CAR_SERVICE (the field is absent). Empty Optional when nothing matches (JSON null).
     *     Names are not read. The list is not narrowed. Several rows mark the smallest individual_services.id.
     */
    private Optional<Long> applyPreselect(List<BookingIndividualServiceView> services, String bookingEntry,
                                           Long catalogServiceId) {
        if (!carServiceEntry(bookingEntry)) {
            return null;
        }
        Long winner = null;
        if (catalogServiceId != null) {
            List<Long> linked = individualServiceSourceRepository.findIndividualServiceIdsByServiceId(catalogServiceId);
            Set<Long> ids = linked == null ? Set.of() : new HashSet<>(linked);
            for (BookingIndividualServiceView view : services) {
                Long id = view.getId();
                if (id == null || !ids.contains(id)) {
                    continue;
                }
                if (winner == null || id < winner) {
                    winner = id;
                }
            }
        }
        if (winner != null) {
            for (BookingIndividualServiceView view : services) {
                if (winner.equals(view.getId())) {
                    view.setPreselected(true);
                    view.setCatalogServiceId(catalogServiceId);
                    break;
                }
            }
        }
        return winner == null ? Optional.empty() : Optional.of(winner);
    }

    /** Trim and upper case. Anything other than CAR_SERVICE is the standard list. */
    private static boolean carServiceEntry(String bookingEntry) {
        if (bookingEntry == null) {
            return false;
        }
        return "CAR_SERVICE".equals(bookingEntry.trim().toUpperCase(Locale.ROOT));
    }

    /** The percentage screen's car. Another user's car is rejected. */
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

    private Map<Long, List<Long>> linksOf(List<BranchIndividualService> rows) {
        List<Long> individualIds = new ArrayList<>();
        for (BranchIndividualService row : rows) {
            IndividualService catalog = row.getIndividualService();
            if (catalog != null && catalog.getId() != null) {
                individualIds.add(catalog.getId());
            }
        }
        if (individualIds.isEmpty()) {
            return Map.of();
        }
        List<IndividualServiceSource> links = individualServiceSourceRepository.findByIndividualServiceIdIn(individualIds);
        Map<Long, List<Long>> byIndividual = new HashMap<>();
        if (links != null) {
            for (IndividualServiceSource link : links) {
                if (link.getIndividualServiceId() == null || link.getServiceId() == null) {
                    continue;
                }
                byIndividual.computeIfAbsent(link.getIndividualServiceId(), ignored -> new ArrayList<>())
                        .add(link.getServiceId());
            }
        }
        return byIndividual;
    }

    /**
     * tr: GET /service/percentages ile aynı servicedStatus (CustomerServiceRecord). Bağlı servis veya kayıt yoksa null.
     * en: Same servicedStatus as GET /service/percentages (CustomerServiceRecord). Null when no linked service or record.
     */
    private String servicedStatusOf(Long serviceId, Car car, Map<Long, String> cache) {
        if (serviceId == null) {
            return null;
        }
        if (!cache.containsKey(serviceId)) {
            CustomerServiceRecord record = customerServiceRecordRepository.findByServiceIdAndCar(serviceId, car);
            cache.put(serviceId, record == null ? null : record.getServicedStatus());
        }
        return cache.get(serviceId);
    }

    /**
     * tr: Bu individual servise bağlı şablon servisinin id'si. Kullanıcının tıkladığı kart bağlıysa o kart,
     *     yoksa birden çok eşleşmede en küçük id.
     * en: Id of the car's template service linked to this individual service. The clicked card when it is linked,
     *     otherwise the smallest id if several match.
     */
    private static Long linkedServiceId(Long individualServiceId, Long clickedServiceId,
                                        Map<Long, List<Long>> serviceIdsByIndividual,
                                        Map<Long, PercentageLifeService.Snapshot> lives) {
        if (individualServiceId == null || lives == null || lives.isEmpty()) {
            return null;
        }
        List<Long> serviceIds = serviceIdsByIndividual.get(individualServiceId);
        if (serviceIds == null || serviceIds.isEmpty()) {
            return null;
        }
        if (clickedServiceId != null && serviceIds.contains(clickedServiceId) && lives.containsKey(clickedServiceId)) {
            return clickedServiceId;
        }
        Long chosen = null;
        for (Long serviceId : serviceIds) {
            if (!lives.containsKey(serviceId)) {
                continue;
            }
            if (chosen == null || serviceId < chosen) {
                chosen = serviceId;
            }
        }
        return chosen;
    }

    private static String missingNote(String lang) {
        if ("en".equals(lang)) {
            return "No information has been entered for this service";
        }
        if ("ru".equals(lang)) {
            return "По этой услуге данные не введены";
        }
        return "Məlumat daxil etməmisiniz";
    }

    private BookingIndividualServiceView toView(IndividualService catalog, BranchIndividualService row, String lang,
                                                Integer percentage, String percentageNote,
                                                Integer remainingKm, Integer remainingMonth) {
        int[] qepik = BookingSelectionViews.qepik(row.getPriceSimple(), row.getPriceMedium(), row.getPriceComplex());
        Integer medium = row.getPriceMedium();
        return BookingIndividualServiceView.builder()
                .id(catalog.getId())
                .code(catalog.getCode())
                .name(BookingMineService.catalogText(titles(catalog.getTitleJson()), lang))
                .priceMin(qepik == null ? null : qepik[0])
                .priceMid(medium == null ? null : Math.multiplyExact(medium, 100))
                .priceMax(qepik == null ? null : qepik[1])
                .currency("AZN")
                .unit(BookingCreateService.UNIT)
                .individualServiceMappedPercentage(percentage)
                .percentageNote(percentageNote)
                .remainingKm(remainingKm)
                .remainingMonth(remainingMonth)
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
