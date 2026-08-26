package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.CarVinServiceHistoryResponse;
import com.carland.carland_service.dto.response.ServiceHistoryItemResponse;
import com.carland.carland_service.dto.response.ServiceHistoryPartResponse;
import com.carland.carland_service.dto.response.hyper.HyperServiceHistoryItemResponse;
import com.carland.carland_service.dto.response.hyper.HyperServicePartResponse;
import com.carland.carland_service.dto.response.hyper.HyperVehicleByVinResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.ServiceHistory;
import com.carland.carland_service.entity.ServiceHistoryPart;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.PartnerId;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.enums.ServiceTypeTranslation;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.ServiceHistoryPartRepository;
import com.carland.carland_service.repository.ServiceHistoryRepository;
import com.carland.carland_service.service.PartnerLookupService;
import com.carland.carland_service.service.CarVinHistoryService;
import com.carland.carland_service.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * tr: VIN'e göre servis geçmişini getiren implementasyondur: önce lokal cache'e (service_history tablosu)
 *     bakar, boşsa Hyper partner API'den geçmişi çekip parça detaylarıyla birlikte kaydeder. Araç bazlı
 *     kilitleme ile eşzamanlı isteklerde çift kayıt oluşmasını engeller.
 *     HTTP: GET /api/v1/car/{vin}/service-history.
 * en: Implementation that returns service history by VIN: reads the local cache (service_history table)
 *     first, and when empty fetches the history from the Hyper partner API and persists it together with
 *     part details. Uses per-car locking to prevent duplicate inserts under concurrent requests.
 *     HTTP: GET /api/v1/car/{vin}/service-history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarVinHistoryServiceImpl implements CarVinHistoryService {

    private static final String HYPERSERVICE_SOURCE = "hyperservice";
    private static final String CACHE_SOURCE = "cache";
    private static final PartnerId HYPER_PARTNER = PartnerId.HYPER;

    private static final ConcurrentHashMap<Long, Object> CAR_PERSIST_LOCKS = new ConcurrentHashMap<>();

    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final ServiceHistoryRepository serviceHistoryRepository;
    private final ServiceHistoryPartRepository serviceHistoryPartRepository;
    private final PartnerLookupService partnerLookupService;
    private final HyperTokenService hyperTokenService;
    private final RestTemplate restTemplate;
    private final RedisCacheService redisCacheService;

    @Value("${hyper.auth.base-url}")
    private String hyperBaseUrl;

    /**
     * tr: VIN'e göre servis geçmişini döner. Önce cache'i kontrol eder; boşsa Hyper'dan çekip kaydeder
     *     ve kaynak (cache/hyperservice) bilgisiyle döner. VIN/telefon/kullanıcı eksikse MissingFieldException,
     *     müşteri bulunamazsa UserNotFoundException, araç müşteriye ait değilse ResourceNotFoundException fırlatır.
     * en: Returns service history by VIN. Checks the cache first; when empty fetches from Hyper, persists
     *     the rows, and returns them with the source flag (cache/hyperservice). Throws MissingFieldException
     *     on missing VIN/phone/user, UserNotFoundException when the customer is not found, and
     *     ResourceNotFoundException when the car does not belong to the customer.
     */
    @Override
    @Transactional
    public CarVinServiceHistoryResponse getServiceHistoryByVin(String vin, String phoneNumber, String userIdHeader, String acceptLanguage) {
        log.info("[hist-debug] v1 getServiceHistoryByVin start | vin={} userId={}", vin, userIdHeader);
        if (vin == null || vin.isBlank() || phoneNumber == null || userIdHeader == null) {
            log.warn("[hist-debug] v1 missing field | vinBlank={} phoneNull={} userIdNull={}",
                    vin == null || vin.isBlank(), phoneNumber == null, userIdHeader == null);
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader), phoneNumber, UserStatus.ACTIVE.name());
        if (customer == null) {
            log.warn("[hist-debug] v1 customer not found | userId={}", userIdHeader);
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        Car car = carRepository.findByVinAndCustomer(vin, customer);
        if (car == null) {
            log.warn("[hist-debug] v1 car not found for customer | vin={} userId={}", vin, userIdHeader);
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        return redisCacheService.getOrLoadHistoryV1(vin, acceptLanguage, () -> loadHistoryUncached(car, vin, acceptLanguage));
    }

    private CarVinServiceHistoryResponse loadHistoryUncached(Car car, String vin, String acceptLanguage) {
        List<ServiceHistory> cachedRows = serviceHistoryRepository.findAllByCarOrderByDoneDateDescIdDesc(car);
        log.info("[hist-debug] v1 cache lookup | carId={} cachedRows={}", car.getCarId(), cachedRows.size());
        if (!cachedRows.isEmpty()) {
            log.info("[hist-debug] v1 returning cache | carId={} source={}", car.getCarId(), CACHE_SOURCE);
            return buildResponse(vin, CACHE_SOURCE, cachedRows, acceptLanguage);
        }

        synchronized (lockForCar(car.getCarId())) {
            cachedRows = serviceHistoryRepository.findAllByCarOrderByDoneDateDescIdDesc(car);
            if (!cachedRows.isEmpty()) {
                log.info("[hist-debug] v1 returning cache after lock | carId={} rows={}", car.getCarId(), cachedRows.size());
                return buildResponse(vin, CACHE_SOURCE, cachedRows, acceptLanguage);
            }

            HyperVehicleByVinResponse hyperResponse = fetchHyperHistory(vin);
            int hyperCount = hyperResponse != null && hyperResponse.getServiceHistory() != null
                    ? hyperResponse.getServiceHistory().size() : 0;
            log.info("[hist-debug] v1 hyper fetch | carId={} vin={} hyperItems={}", car.getCarId(), vin, hyperCount);
            if (hyperResponse == null || hyperResponse.getServiceHistory() == null || hyperResponse.getServiceHistory().isEmpty()) {
                log.info("[hist-debug] v1 hyper empty | carId={} vin={}", car.getCarId(), vin);
                return buildResponse(vin, HYPERSERVICE_SOURCE, Collections.emptyList(), acceptLanguage);
            }

            List<ServiceHistory> persisted = persistHyperRows(car, hyperResponse.getServiceHistory());
            log.info("[hist-debug] v1 persisted hyper rows | carId={} persisted={}", car.getCarId(), persisted.size());
            return buildResponse(vin, HYPERSERVICE_SOURCE, persisted, acceptLanguage);
        }
    }



    private CarVinServiceHistoryResponse buildResponse(String vin, String source, List<ServiceHistory> rows, String acceptLanguage) {
        Map<Long, Partner> partnerById = loadPartnersForRows(rows);
        return CarVinServiceHistoryResponse.builder()
                .vin(vin)
                .source(source)
                .items(rows.stream().map(row -> mapServiceHistory(row, partnerById, acceptLanguage)).toList())
                .build();
    }

    private Map<Long, Partner> loadPartnersForRows(List<ServiceHistory> rows) {
        Set<Long> partnerIds = rows.stream()
                .map(ServiceHistory::getServiceCenterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        partnerIds.add(HYPER_PARTNER.getId());
        return partnerLookupService.loadByIds(partnerIds);
    }

    private Object lockForCar(Long carId) {
        return CAR_PERSIST_LOCKS.computeIfAbsent(carId, id -> new Object());
    }

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

    private List<ServiceHistory> persistHyperRows(Car car, List<HyperServiceHistoryItemResponse> items) {
        List<ServiceHistory> persisted = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();

        for (HyperServiceHistoryItemResponse item : items) {
            String batchKey = hyperItemKey(item);
            if (!seenInBatch.add(batchKey)) {
                log.info("Skipping duplicate Hyper item in same response for carId={}: {}", car.getCarId(), batchKey);
                continue;
            }

            List<String> groups = item.getServiceGroups() == null ? Collections.emptyList() : item.getServiceGroups();
            BigDecimal serviceAmount = item.getFinalCost() != null ? item.getFinalCost().getAmount() : null;

            Optional<ServiceHistory> existing = serviceHistoryRepository
                    .findByCarAndServiceNameAndDoneDateAndDoneKmAndDealerAndServiceAmountAndSource(
                            car,
                            item.getServiceType(),
                            item.getLastServiceDate(),
                            item.getLastServiceMileage(),
                            item.getDealer(),
                            serviceAmount,
                            HYPERSERVICE_SOURCE
                    );

            if (existing.isPresent()) {
                log.info("Skipping duplicate Hyper service history for carId={}: {}", car.getCarId(), batchKey);
                persisted.add(existing.get());
                continue;
            }

            Partner partner = partnerLookupService.find(HYPER_PARTNER).orElse(null);
            String partnerName = partner != null ? partner.getName() : HYPER_PARTNER.getDefaultName();

            ServiceHistory history = ServiceHistory.builder()
                    .car(car)
                    .serviceName(item.getServiceType())
                    .actionType(groups)
                    .doneDate(item.getLastServiceDate())
                    .doneKm(item.getLastServiceMileage())
                    .serviceCenter(partnerName)
                    .serviceCenterId(HYPER_PARTNER.getId())
                    .serviceAmount(serviceAmount)
                    .dealer(item.getDealer())
                    .nextServiceDate(item.getNextServiceDate())
                    .nextServiceMileage(item.getNextServiceMileage())
                    .source(HYPERSERVICE_SOURCE)
                    .build();

            ServiceHistory saved = serviceHistoryRepository.save(history);
            persistParts(saved, item.getParts());
            persisted.add(saved);
        }

        return persisted.stream()
                .sorted(Comparator.comparing(ServiceHistory::getDoneDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ServiceHistory::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private String hyperItemKey(HyperServiceHistoryItemResponse item) {
        BigDecimal amount = item.getFinalCost() != null ? item.getFinalCost().getAmount() : null;
        return String.join("|",
                Objects.toString(item.getServiceType(), ""),
                Objects.toString(item.getLastServiceDate(), ""),
                Objects.toString(item.getLastServiceMileage(), ""),
                Objects.toString(item.getDealer(), ""),
                Objects.toString(amount, "")
        );
    }

    private void persistParts(ServiceHistory serviceHistory, List<HyperServicePartResponse> parts) {
        if (parts == null || parts.isEmpty()) {
            return;
        }

        List<ServiceHistoryPart> entities = parts.stream()
                .map(part -> ServiceHistoryPart.builder()
                        .serviceHistory(serviceHistory)
                        .name(part.getName())
                        .qty(part.getQty())
                        .unit(part.getUnit())
                        .cost(null)
                        .finalCost(null)
                        .discount(null)
                        .build())
                .toList();

        serviceHistoryPartRepository.saveAll(entities);
    }

    private ServiceHistoryItemResponse mapServiceHistory(ServiceHistory history, Map<Long, Partner> partnerById, String acceptLanguage) {
        List<ServiceHistoryPartResponse> parts = history.getId() == null
                ? Collections.emptyList()
                : serviceHistoryPartRepository.findAllByServiceHistory(history).stream()
                .map(part -> ServiceHistoryPartResponse.builder()
                        .name(part.getName())
                        .qty(part.getQty())
                        .unit(part.getUnit())
                        .cost(part.getCost())
                        .finalCost(part.getFinalCost())
                        .discount(part.getDiscount())
                        .build())
                .toList();

        List<String> actionType = history.getActionType() == null
                ? Collections.emptyList()
                : ServiceTypeTranslation.translateList(history.getActionType(), acceptLanguage);

        Long partnerId = partnerLookupService.resolvePartnerId(history.getServiceCenterId(), HYPER_PARTNER);
        PartnerId enumPartner = PartnerId.fromId(partnerId).orElse(HYPER_PARTNER);
        String partnerName = partnerLookupService.resolvePartnerName(
                history.getServiceCenterId(), history.getServiceCenter(), partnerById, enumPartner);

        return ServiceHistoryItemResponse.builder()
                .id(history.getId())
                .serviceName(ServiceTypeTranslation.translate(history.getServiceName(), acceptLanguage))
                .actionType(actionType)
                .doneDate(history.getDoneDate())
                .doneKM(history.getDoneKm())
                .serviceCenter(partnerName)
                .serviceCenterId(partnerId)
                .partner(partnerLookupService.toDataResponse(partnerById.get(partnerId), enumPartner))
                .serviceAmount(history.getServiceAmount())
                .dealer(history.getDealer())
                .parts(parts)
                .nextServiceDate(history.getNextServiceDate())
                .nextServiceMileage(history.getNextServiceMileage())
                .build();
    }
}
