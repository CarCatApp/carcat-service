package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.hyper.HyperServiceHistoryItemResponse;
import com.carland.carland_service.dto.response.hyper.HyperServiceLineResponse;
import com.carland.carland_service.dto.response.hyper.HyperServicePartResponse;
import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.dto.response.VisitHistoryResponse;
import com.carland.carland_service.dto.response.MoneyResponse;
import com.carland.carland_service.dto.response.VisitServiceLineResponse;
import com.carland.carland_service.dto.response.VisitPartResponse;
import com.carland.carland_service.dto.response.VisitHistorySummaryResponse;
import com.carland.carland_service.dto.response.VisitHistoryItemResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitPart;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.PartnerId;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.enums.ServiceTypeTranslation;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.service.HyperPercentageSyncService;
import com.carland.carland_service.service.PartnerLookupService;
import com.carland.carland_service.service.RedisCacheService;
import com.carland.carland_service.service.VisitHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * tr: VIN'e göre servis ziyareti geçmişini yöneten servistir; Redis miss'te Hyper API'den çeker,
 *     mevcut recordId'leri günceller (upsert), percentage senkronunu ve servicedPartnerIds güncellemesini
 *     tetikler. (Eski adı: CarVinHistoryServiceV2Impl)
 * en: Service managing service visit history by VIN; on Redis miss fetches Hyper, upserts existing
 *     recordIds, and triggers percentage sync plus servicedPartnerIds refresh.
 *     (Former name: CarVinHistoryServiceV2Impl)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VisitHistoryServiceImpl implements VisitHistoryService {

    private static final String CACHE_SOURCE = "cache";
    private static final String LIVE_SOURCE = "live";
    private static final PartnerId HYPER_PARTNER = PartnerId.HYPER;

    private static final ConcurrentHashMap<Long, Object> CAR_PERSIST_LOCKS = new ConcurrentHashMap<>();

    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final VisitRepository visitRepository;
    private final HyperPercentageSyncService hyperPercentageSyncService;
    private final PartnerLookupService partnerLookupService;
    private final HyperTokenService hyperTokenService;
    private final RestTemplate restTemplate;
    private final RedisCacheService redisCacheService;

    @Value("${hyper.auth.base-url}")
    private String hyperBaseUrl;

    /**
     * tr: VIN'e göre servis geçmişini döner. Parametreleri doğrular (eksikse MissingFieldException),
     *     aktif müşteriyi bulur (yoksa UserNotFoundException), aracın müşteriye ait olduğunu kontrol eder
     *     (değilse ResourceNotFoundException). Redis hit ise cache, miss ise Hyper'dan çekip visits'e upsert eder.
     * en: Returns service history by VIN. Validates parameters (MissingFieldException when missing),
     *     resolves the active customer (UserNotFoundException when absent), verifies car ownership
     *     (ResourceNotFoundException otherwise). Redis hit returns cache; miss fetches Hyper and upserts visits.
     */
    @Override
    @Transactional
    public VisitHistoryResponse getServiceHistoryByVin(String vin, String phoneNumber, String userIdHeader, String acceptLanguage) {
        log.info("[hist-debug] v2 getServiceHistoryByVin start | vin={} userId={}", vin, userIdHeader);
        if (vin == null || vin.isBlank() || phoneNumber == null || userIdHeader == null) {
            log.warn("[hist-debug] v2 missing field | vinBlank={} phoneNull={} userIdNull={}",
                    vin == null || vin.isBlank(), phoneNumber == null, userIdHeader == null);
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(
                Long.valueOf(userIdHeader), phoneNumber, UserStatus.ACTIVE.name());
        if (customer == null) {
            log.warn("[hist-debug] v2 customer not found | userId={}", userIdHeader);
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        Car car = carRepository.findByVinAndCustomer(vin, customer);
        if (car == null) {
            log.warn("[hist-debug] v2 car not found for customer | vin={} userId={}", vin, userIdHeader);
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        return redisCacheService.getOrLoadHistoryV2(vin, acceptLanguage, () -> loadHistoryUncached(car, vin, acceptLanguage));
    }

    private VisitHistoryResponse loadHistoryUncached(Car car, String vin, String acceptLanguage) {
        synchronized (lockForCar(car.getCarId())) {
            HyperVehicleByVinResponse hyperResponse = fetchHyperHistoryLenient(car, vin);
            int hyperCount = hyperResponse != null && hyperResponse.getServiceHistory() != null
                    ? hyperResponse.getServiceHistory().size() : 0;
            log.info("[hist-debug] v2 hyper fetch | carId={} vin={} hyperItems={}", car.getCarId(), vin, hyperCount);

            if (hyperResponse == null || hyperResponse.getServiceHistory() == null || hyperResponse.getServiceHistory().isEmpty()) {
                List<Visit> cachedVisits = loadCachedVisits(car);
                if (cachedVisits != null) {
                    hyperPercentageSyncService.syncFromVisits(car, cachedVisits);
                    refreshServicedPartnerIds(car);
                    log.info("[hist-debug] v2 hyper empty, returning db cache | carId={} visits={}",
                            car.getCarId(), cachedVisits.size());
                    return buildResponse(car, vin, CACHE_SOURCE, cachedVisits, acceptLanguage);
                }
                log.info("[hist-debug] v2 hyper empty | carId={} vin={}", car.getCarId(), vin);
                return buildResponse(car, vin, LIVE_SOURCE, Collections.emptyList(), acceptLanguage);
            }

            persistHyperVisits(car, hyperResponse.getServiceHistory());
            List<Visit> allVisits = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car);
            hyperPercentageSyncService.resyncFromVisits(car, allVisits);
            refreshServicedPartnerIds(car);
            redisCacheService.evictCarListAfterCommit(redisCacheService.ownerUserId(car));
            log.info("[hist-debug] v2 upserted visits | carId={} visits={}", car.getCarId(), allVisits.size());
            return buildResponse(car, vin, LIVE_SOURCE, allVisits, acceptLanguage);
        }
    }

    /**
     * tr: Aracın servicedPartnerIds alanını kayıtlı ziyaretlerle senkron tutar; partner sırası en yeni
     *     ziyaretten en eskiye doğrudur, değişiklik yoksa DB'ye yazmaz.
     * en: Keeps {@link Car#getServicedPartnerIds()} in sync with persisted visits; partners are ordered
     *     by most recent service date and no DB write happens when unchanged.
     */
    private void refreshServicedPartnerIds(Car car) {
        if (!visitRepository.existsByCar(car)) {
            return;
        }

        List<Visit> allVisits = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car);
        LinkedHashSet<String> orderedPartnerIds = new LinkedHashSet<>();
        for (Visit visit : allVisits) {
            Long partnerId = visit.getServiceCenterId();
            if (partnerId != null) {
                orderedPartnerIds.add(String.valueOf(partnerId));
            }
        }

        List<String> updated = new ArrayList<>(orderedPartnerIds);
        List<String> current = car.getServicedPartnerIds();
        if (current != null && current.equals(updated)) {
            return;
        }

        car.setServicedPartnerIds(updated);
        carRepository.save(car);
        redisCacheService.evictCarListAfterCommit(redisCacheService.ownerUserId(car));
        log.info("Updated servicedPartnerIds | carId={}, partnerIds={}", car.getCarId(), updated);
    }

    /**
     * tr: Cache varsa ziyaretleri döner; araç için hiç ziyaret yoksa null döner (önce ucuz exists kontrolü).
     * en: Returns visits when cache exists; null when DB has no visits for this car (cheap exists check first).
     */
    private List<Visit> loadCachedVisits(Car car) {
        if (!visitRepository.existsByCar(car)) {
            return null;
        }
        return visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car);
    }

    /**
     * tr: Ziyaret listesini API response modeline çevirir; partner bilgilerini toplu yükler ve özet üretir.
     * en: Converts the visit list into the API response model; bulk-loads partner info and builds the summary.
     */
    private VisitHistoryResponse buildResponse(Car car, String vin, String source, List<Visit> visits, String acceptLanguage) {
        Map<Long, Partner> partnerById = loadPartnersForVisits(visits);
        List<VisitHistoryItemResponse> items = visits.stream()
                .map(visit -> mapVisit(visit, partnerById, acceptLanguage))
                .toList();

        return VisitHistoryResponse.builder()
                .vin(vin)
                .source(source)
                .summary(buildSummary(items))
                .items(items)
                .build();
    }

    /**
     * tr: Ziyaret kalemlerinden toplam tutar ve servis sayısı özetini üretir; para birimi yoksa AZN varsayar.
     * en: Builds the total-amount and service-count summary from visit items; defaults currency to AZN.
     */
    private VisitHistorySummaryResponse buildSummary(List<VisitHistoryItemResponse> items) {
        BigDecimal total = items.stream()
                .map(item -> item.getAmount() != null ? item.getAmount().getAmount() : null)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = items.stream()
                .map(item -> item.getAmount() != null ? item.getAmount().getCurrency() : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("AZN");

        return VisitHistorySummaryResponse.builder()
                .serviceCount(items.size())
                .totalAmount(MoneyResponse.builder().amount(total).currency(currency).build())
                .build();
    }

    /**
     * tr: Ziyaretlerde geçen partner id'lerini (Hyper dahil) toplayıp partner kayıtlarını toplu yükler.
     * en: Collects partner ids referenced by the visits (including Hyper) and bulk-loads partner records.
     */
    private Map<Long, Partner> loadPartnersForVisits(List<Visit> visits) {
        Set<Long> partnerIds = visits.stream()
                .map(Visit::getServiceCenterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        partnerIds.add(HYPER_PARTNER.getId());
        return partnerLookupService.loadByIds(partnerIds);
    }

    /**
     * tr: Tek bir Visit entity'sini, partner ve çeviri bilgileriyle zenginleştirilmiş response modeline çevirir.
     * en: Maps a single Visit entity into the response model enriched with partner and translation info.
     */
    private VisitHistoryItemResponse mapVisit(Visit visit, Map<Long, Partner> partnerById, String acceptLanguage) {
        Long partnerId = partnerLookupService.resolvePartnerId(visit.getServiceCenterId(), HYPER_PARTNER);
        PartnerId enumPartner = PartnerId.fromId(partnerId).orElse(HYPER_PARTNER);
        String partnerName = partnerLookupService.resolvePartnerName(
                visit.getServiceCenterId(), visit.getServiceCenterName(), partnerById, enumPartner);

        List<String> serviceGroups = visit.getServiceGroups() == null
                ? Collections.emptyList()
                : ServiceTypeTranslation.translateList(visit.getServiceGroups(), acceptLanguage);

        return VisitHistoryItemResponse.builder()
                .id(visit.getId())
                .partnerRecordId(visit.getHyperRecordId())
                .type(ServiceTypeTranslation.translate(visit.getServiceType(), acceptLanguage))
                .serviceGroups(serviceGroups)
                .services(mapServiceLines(visit.getServices(), acceptLanguage))
                .date(visit.getLastServiceDate())
                .mileage(visit.getLastServiceMileage())
                .serviceCenterId(partnerId)
                .serviceCenterName(partnerName)
                .partner(partnerLookupService.toDataResponse(partnerById.get(partnerId), enumPartner))
                .dealer(visit.getDealer())
                .cost(toMoney(visit.getCostAmount(), visit.getCostCurrency()))
                .amount(toMoney(visit.getFinalCostAmount(), visit.getFinalCostCurrency()))
                .invoiceNumber(visit.getInvoiceNumber())
                .parts(mapParts(visit.getParts()))
                .build();
    }

    /**
     * tr: Ziyaretin servis satırlarını, servis gruplarını dile göre çevirerek response modeline dönüştürür.
     * en: Converts the visit's service lines to response models, translating service groups per language.
     */
    private List<VisitServiceLineResponse> mapServiceLines(List<VisitServiceLine> lines, String acceptLanguage) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        return lines.stream()
                .map(line -> {
                    List<String> lineGroups = line.getServiceGroups() == null
                            ? Collections.emptyList()
                            : ServiceTypeTranslation.translateList(line.getServiceGroups(), acceptLanguage);
                    return VisitServiceLineResponse.builder()
                            .serviceCode(line.getServiceCode())
                            .universalServiceId(line.getUniversalServiceId())
                            .serviceName(line.getServiceName())
                            .serviceGroups(lineGroups)
                            .cost(toMoney(line.getCostAmount(), line.getCostCurrency()))
                            .nextServiceDate(line.getNextServiceDate())
                            .nextServiceMileage(line.getNextServiceMileage())
                            .build();
                })
                .toList();
    }

    /**
     * tr: Ziyaretin parça kayıtlarını response modeline dönüştürür; liste boşsa boş liste döner.
     * en: Converts the visit's part records to response models; returns an empty list when none exist.
     */
    private List<VisitPartResponse> mapParts(List<VisitPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }
        return parts.stream()
                .map(part -> VisitPartResponse.builder()
                        .name(part.getName())
                        .qty(part.getQty())
                        .unit(part.getUnit())
                        .build())
                .toList();
    }

    /**
     * tr: Tutar ve para biriminden MoneyResponse üretir; ikisi de null ise null döner, para birimi yoksa AZN kullanır.
     * en: Builds a MoneyResponse from amount and currency; returns null when both are null, defaults currency to AZN.
     */
    private MoneyResponse toMoney(BigDecimal amount, String currency) {
        if (amount == null && currency == null) {
            return null;
        }
        return MoneyResponse.builder()
                .amount(amount)
                .currency(currency != null ? currency : "AZN")
                .build();
    }

    /**
     * tr: Araç bazlı persist kilidini döner; aynı araç için eşzamanlı Hyper çekişini engeller.
     * en: Returns the per-car persist lock; prevents concurrent Hyper fetches for the same car.
     */
    private Object lockForCar(Long carId) {
        return CAR_PERSIST_LOCKS.computeIfAbsent(carId, id -> new Object());
    }

    /**
     * tr: Hyper çekişini yumuşatır; ağ/5xx/token hatasında null döner ki DB'deki ziyaretler kaybolmasın.
     * en: Softens the Hyper fetch; returns null on network/5xx/token errors so DB visits are still served.
     */
    private HyperVehicleByVinResponse fetchHyperHistoryLenient(Car car, String vin) {
        try {
            return fetchHyperHistory(vin);
        } catch (RuntimeException e) {
            log.warn("[hist-debug] v2 hyper fetch failed | carId={} vin={} err={}",
                    car.getCarId(), vin, e.toString());
            return null;
        }
    }

    /**
     * tr: Hyper API'den VIN'e göre araç servis geçmişini çeker; Hyper "vehicle_not_found" dönerse null,
     *     diğer 404'lerde exception fırlatır.
     * en: Fetches the vehicle service history by VIN from the Hyper API; returns null when Hyper responds
     *     "vehicle_not_found", rethrows other 404 errors.
     */
    private HyperVehicleByVinResponse fetchHyperHistory(String vin) {
        String token = hyperTokenService.getToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = hyperBaseUrl + "/partner/v1/vehicles/by-vin/" + vin;
        try {
            ResponseEntity<HyperVehicleByVinResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, HyperVehicleByVinResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            if (e.getResponseBodyAsString() != null && e.getResponseBodyAsString().contains("vehicle_not_found")) {
                return null;
            }
            throw e;
        }
    }

    /**
     * tr: Hyper'dan gelen ziyaret kayıtlarını Visit'e çevirip kaydeder. Aynı recordId varsa km/tutar/dealer/
     *     satır/parça Hyper anlık görüntüsüyle değiştirilir (GET snapshot replace; webhook PUT merge-add ayrıdır).
     *     Aynı yanıttaki mükerrer recordId atlanır. Aracın allTimeCost toplamı Hyper JSON toplamına çekilir.
     * en: Maps Hyper visit records to Visit entities and persists them. Existing recordIds are replaced
     *     (km, cost, dealer, lines, parts) from the Hyper snapshot (GET snapshot replace; webhook PUT
     *     merge-add is separate). Duplicate recordIds in the same response are skipped. allTimeCost is
     *     set to the Hyper JSON total.
     */
    private List<Visit> persistHyperVisits(Car car, List<HyperServiceHistoryItemResponse> items) {
        Set<Long> seenRecordIds = new HashSet<>();
        BigDecimal allTimeCost = BigDecimal.ZERO;

        Partner hyperPartner = partnerLookupService.find(HYPER_PARTNER).orElse(null);
        Long partnerId = HYPER_PARTNER.getId();
        String partnerName = hyperPartner != null ? hyperPartner.getName() : HYPER_PARTNER.getDefaultName();

        Map<Long, Visit> existingByRecordId = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car).stream()
                .filter(visit -> visit.getHyperRecordId() != null)
                .collect(Collectors.toMap(Visit::getHyperRecordId, visit -> visit, (a, b) -> a));

        List<Visit> toSave = new ArrayList<>();

        for (HyperServiceHistoryItemResponse item : items) {
            if (item.getRecordId() == null) {
                log.warn("Skipping Hyper visit without recordId for carId={}", car.getCarId());
                continue;
            }
            if (!seenRecordIds.add(item.getRecordId())) {
                log.info("Skipping duplicate Hyper recordId in same response for carId={}: {}", car.getCarId(), item.getRecordId());
                continue;
            }

            allTimeCost = allTimeCost.add(resolveVisitFinalCost(item));

            Visit existing = existingByRecordId.get(item.getRecordId());
            if (existing != null) {
                applyHyperSnapshot(existing, item);
                toSave.add(existing);
            } else {
                toSave.add(mapHyperItemToVisit(car, item, partnerId, partnerName));
            }
        }

        List<Visit> persisted = toSave.isEmpty() ? List.of() : visitRepository.saveAll(toSave);

        car.setAllTimeCost(allTimeCost);
        carRepository.save(car);
        log.info("Updated allTimeCost for carId={} | total={} upserted={}", car.getCarId(), allTimeCost, persisted.size());

        return persisted.stream()
                .sorted(Comparator.comparing(Visit::getLastServiceDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Visit::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * tr: Mevcut ziyaretin başlık alanlarını ve satır/parçalarını Hyper anlık görüntüsüyle değiştirir.
     *     serviceCenterId/Name ve editHistory dokunulmaz (GET cache, webhook geçmişi korunur).
     * en: Replaces an existing visit's header fields and lines/parts from the Hyper snapshot.
     *     serviceCenterId/Name and editHistory are left unchanged (GET cache; webhook history preserved).
     */
    private void applyHyperSnapshot(Visit visit, HyperServiceHistoryItemResponse item) {
        visit.setServiceType(item.getServiceType());
        visit.setLastServiceDate(item.getLastServiceDate());
        visit.setLastServiceMileage(item.getLastServiceMileage());
        visit.setInvoiceNumber(item.getInvoiceNumber());
        visit.setDealer(item.getDealer());
        visit.setCostAmount(item.getCost() != null ? item.getCost().getAmount() : null);
        visit.setCostCurrency(item.getCost() != null ? item.getCost().getCurrency() : null);
        visit.setFinalCostAmount(item.getFinalCost() != null ? item.getFinalCost().getAmount() : null);
        visit.setFinalCostCurrency(item.getFinalCost() != null ? item.getFinalCost().getCurrency() : null);
        visit.setServiceGroups(item.getServiceGroups() != null ? new ArrayList<>(item.getServiceGroups()) : new ArrayList<>());
        visit.setUpdatedAt(LocalDateTime.now());
        replaceVisitChildren(visit, item);
        log.info("Updated existing Hyper visit | carId={} recordId={} mileage={} finalCost={}",
                visit.getCar() != null ? visit.getCar().getCarId() : null,
                item.getRecordId(),
                item.getLastServiceMileage(),
                visit.getFinalCostAmount());
    }

    /**
     * tr: orphanRemoval ile satır ve parçaları Hyper listesiyle birebir değiştirir (fazla satır düşer).
     * en: Replaces lines and parts 1:1 with the Hyper lists via orphanRemoval (extra rows are dropped).
     */
    private void replaceVisitChildren(Visit visit, HyperServiceHistoryItemResponse item) {
        if (visit.getServices() == null) {
            visit.setServices(new ArrayList<>());
        }
        if (visit.getParts() == null) {
            visit.setParts(new ArrayList<>());
        }
        visit.getServices().size();
        visit.getParts().size();
        visit.getServices().clear();
        visit.getParts().clear();

        if (item.getServices() != null) {
            for (HyperServiceLineResponse line : item.getServices()) {
                visit.addService(mapHyperLineToEntity(line));
            }
        }
        if (item.getParts() != null) {
            for (HyperServicePartResponse part : item.getParts()) {
                visit.addPart(mapHyperPartToEntity(part));
            }
        }
    }

    private VisitPart mapHyperPartToEntity(HyperServicePartResponse part) {
        return VisitPart.builder()
                .name(part.getName())
                .qty(part.getQty())
                .unit(part.getUnit())
                .build();
    }

    /**
     * tr: Ziyaretin toplam tutarını belirler: finalCost (indirim sonrası) esastır, yoksa cost'a düşer, o da yoksa 0.
     * en: Resolves the visit total: finalCost (post-discount) is authoritative; falls back to cost, else zero.
     */
    private BigDecimal resolveVisitFinalCost(HyperServiceHistoryItemResponse item) {
        if (item.getFinalCost() != null && item.getFinalCost().getAmount() != null) {
            return item.getFinalCost().getAmount();
        }
        if (item.getCost() != null && item.getCost().getAmount() != null) {
            return item.getCost().getAmount();
        }
        return BigDecimal.ZERO;
    }

    /**
     * tr: Tek bir Hyper kaydını, satırları ve parçaları dahil olmak üzere Visit entity'sine dönüştürür.
     * en: Maps a single Hyper record into a Visit entity, including its service lines and parts.
     */
    private Visit mapHyperItemToVisit(Car car, HyperServiceHistoryItemResponse item, Long partnerId, String partnerName) {
        Visit visit = Visit.builder()
                .car(car)
                .hyperRecordId(item.getRecordId())
                .serviceType(item.getServiceType())
                .lastServiceDate(item.getLastServiceDate())
                .lastServiceMileage(item.getLastServiceMileage())
                .invoiceNumber(item.getInvoiceNumber())
                .dealer(item.getDealer())
                .serviceCenterId(partnerId)
                .serviceCenterName(partnerName)
                .costAmount(item.getCost() != null ? item.getCost().getAmount() : null)
                .costCurrency(item.getCost() != null ? item.getCost().getCurrency() : null)
                .finalCostAmount(item.getFinalCost() != null ? item.getFinalCost().getAmount() : null)
                .finalCostCurrency(item.getFinalCost() != null ? item.getFinalCost().getCurrency() : null)
                .serviceGroups(item.getServiceGroups() != null ? new ArrayList<>(item.getServiceGroups()) : new ArrayList<>())
                .build();

        if (item.getServices() != null) {
            for (HyperServiceLineResponse line : item.getServices()) {
                visit.addService(mapHyperLineToEntity(line));
            }
        }
        if (item.getParts() != null) {
            for (HyperServicePartResponse part : item.getParts()) {
                visit.addPart(mapHyperPartToEntity(part));
            }
        }

        return visit;
    }

    /**
     * tr: Hyper servis satırını VisitServiceLine entity'sine çevirir; universalServiceId'yi normalize eder.
     * en: Maps a Hyper service line to a VisitServiceLine entity; normalizes the universalServiceId.
     */
    private VisitServiceLine mapHyperLineToEntity(HyperServiceLineResponse line) {
        return VisitServiceLine.builder()
                .serviceCode(line.getServiceCode())
                .serviceName(line.getServiceName())
                .universalServiceId(normalizeUniversalServiceId(line.getUniversalServiceId()))
                .serviceGroups(line.getServiceGroups() != null ? new ArrayList<>(line.getServiceGroups()) : new ArrayList<>())
                .costAmount(line.getCost() != null ? line.getCost().getAmount() : null)
                .costCurrency(line.getCost() != null ? line.getCost().getCurrency() : null)
                .nextServiceDate(line.getNextServiceDate())
                .nextServiceMileage(line.getNextServiceMileage())
                .build();
    }

    /**
     * tr: Hyper'ın universalServiceId değerini ham haliyle korur; sadece trim'ler ve "other" placeholder'ını null'a çevirir.
     *     (Dikkat: webhook ingest tarafındaki normalize boş string döner — davranış bilinçli olarak korunmuştur.)
     * en: Keeps Hyper's universalServiceId raw; only trims and maps the explicit "other" placeholder to null.
     *     (Note: the webhook ingest normalize returns an empty string — behavior intentionally preserved.)
     */
    private String normalizeUniversalServiceId(String raw) {
        if (raw == null || raw.isBlank() || "other".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        return raw.trim();
    }
}
