package com.carland.carland_service.idda;

import com.carland.carland_service.digitalfinance.DigitalFinanceClient;
import com.carland.carland_service.dto.response.CarResponse;
import com.carland.carland_service.entity.Brand;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Model;
import com.carland.carland_service.enums.BodyTypeTranslation;
import com.carland.carland_service.enums.EngineTypeTranslation;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.repository.BrandRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.ModelRepository;
import com.carland.carland_service.util.PlateNumbers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * tr: Plaka check. Araç yalnız bu isteğin canlı IDDA başarısında döner, isRestrict false.
 *     İzin yok, dolu limit, hata ve diğer her status isRestrict true döner ve araç taşımaz.
 *     Eski Code 0 limite sayılır, ekrana basılmaz.
 *     Geçici test: aynı müşteri + plaka ayda 5 canlı, plaka geneli ayda 10 canlı.
 *     Code 4 ve timeout hakka yazılmaz. Yan yana iki A olan nişan (10AA000) IDDA'ya gitmez.
 *     Check araç satırı açmaz, sahip değiştirmez.
 *     Redis: bu servis carlist yazmaz. Araç yazısı IddaCarWriteService içindedir, orada evict vardır.
 * en: Plate check. The car is returned only from this request's live IDDA success, with isRestrict false.
 *     No permission, a full cap, errors and every other status return isRestrict true and no car.
 *     An old Code 0 still counts toward the cap and is not shown.
 *     Temporary test caps: 5 live calls per customer and plate, 10 live calls per plate.
 *     Code 4 and timeouts do not count. A plate with two adjacent A's (10AA000) never calls IDDA.
 *     The check does not insert a car or change an owner.
 *     Redis: this service does not write carlist. Car writes and evicts live in IddaCarWriteService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IddaPlateCheckService {

    private static final String NEW_SET_MARKER = ".";
    /**
     * tr: Geçici test hakkı. Canlıda aynı müşteri + plaka ayda 1, plaka geneli ayda 2 idi.
     * en: Temporary test cap. Production was 1 per customer+plate and 2 per plate each month.
     */
    private static final int USER_LIVE_CAP = 5;
    private static final int PLATE_LIVE_CAP = 10;
    /**
     * tr: Geçici kullanım kılavuzu. Bu müşteri bu plakada limite takılmaz ve hak yazmaz.
     * en: Temporary guide. This customer on this plate skips the cap and does not count.
     */
    private static final long GUIDE_USER_ID = 55L;
    private static final String GUIDE_PLATE_KEY = "77BU669";

    private final DigitalFinanceClient digitalFinanceClient;
    private final IddaPlateCheckRepository iddaPlateCheckRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final BrandRepository brandRepository;
    private final ModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CarResponse check(String plate, String phoneNumber, String userIdHeader, String acceptLanguage) {
        String plateKey = IddaPlateKey.of(plate);
        if (plateKey.isEmpty()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        IddaPlateKey.rejectAdjacentAa(plate, acceptLanguage);
        Customer customer = requireCustomer(phoneNumber, userIdHeader, acceptLanguage);
        LocalDateTime since = LocalDateTime.now().minusMonths(1);
        List<IddaPlateCheck> recent = iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(plateKey, since);

        List<IddaPlateCheck> ownRows = recent.stream()
                .filter(row -> customer.getUserId().equals(row.getQuerierUserId()))
                .toList();
        if (!guideDemo(plateKey, customer.getUserId())
                && (ownRows.size() >= USER_LIVE_CAP || recent.size() >= PLATE_LIVE_CAP)) {
            log.info("IDDA plate check cap plateKey={} own={} plate={}", plateKey, ownRows.size(), recent.size());
            return restricted(plate, MessagesLangValues.PLATE_QUERY_LIMIT.getMessageByLang(acceptLanguage));
        }
        return live(plateKey, customer, acceptLanguage);
    }

    private CarResponse live(String plateKey, Customer customer, String acceptLanguage) {
        String requestId = UUID.randomUUID().toString();
        String path = "/api/v1/DIN/vehicle/" + URLEncoder.encode(plateKey, StandardCharsets.UTF_8).replace("+", "%20");
        DigitalFinanceClient.Upstream upstream;
        try {
            upstream = digitalFinanceClient.exchange("GET", path, null, requestId, null);
        } catch (IOException | IllegalStateException ex) {
            log.warn("IDDA plate check unreachable plateKey={}", plateKey);
            return restricted(plateKey, MessagesLangValues.IDDA_UNAVAILABLE.getMessageByLang(acceptLanguage));
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(upstream.body());
        } catch (IOException ex) {
            log.warn("IDDA plate check unreadable plateKey={} http={}", plateKey, upstream.statusCode());
            return restricted(plateKey, MessagesLangValues.IDDA_UNAVAILABLE.getMessageByLang(acceptLanguage));
        }
        JsonNode status = root.path("Status");
        int code = status.path("Code").asInt(-1);
        if (code < 0) {
            log.warn("IDDA plate check missing status plateKey={} http={}", plateKey, upstream.statusCode());
            return restricted(plateKey, MessagesLangValues.IDDA_UNAVAILABLE.getMessageByLang(acceptLanguage));
        }
        IddaPlateCheck row = mapRow(root, plateKey, customer, requestId, code);
        iddaPlateCheckRepository.save(row);
        log.info("IDDA plate check live plateKey={} status={} counts={}", plateKey, code, row.isCountsTowardLimit());
        if (row.isVehicleSuccess()) {
            return toResponse(row, acceptLanguage);
        }
        return restrictedLive(row, acceptLanguage);
    }

    private IddaPlateCheck mapRow(JsonNode root, String plateKey, Customer customer, String requestId, int code) {
        JsonNode status = root.path("Status");
        JsonNode response = root.path("Response");
        JsonNode car = response.path("Car");
        JsonNode certificate = response.path("Certificate");
        JsonNode owner = response.path("Owner");
        JsonNode type = car.path("Type");
        String vin = text(car, "BodyNumber");
        String numberPlate = text(car, "NumberPlate");
        String rawMark = text(car, "Mark");
        String typeDescription = text(type, "Description");
        String statusName = text(status, "Name");
        boolean blocked = code == 4
                || (statusName != null && statusName.equalsIgnoreCase("ServiceRestrictedByPerson"));
        boolean success = code == 0 && vin != null && !blocked;
        IddaMarkSplit split = success
                ? IddaMarkSplitter.split(rawMark, typeDescription, loadBrands(), loadModels())
                : new IddaMarkSplit(null, null);
        String storedVin = vin == null ? null : vin.trim().toUpperCase(Locale.ROOT);
        return IddaPlateCheck.builder()
                .requestIdentifier(text(root, "RequestIdentifier") == null ? requestId : text(root, "RequestIdentifier"))
                .statusName(statusName)
                .statusCode(code)
                .statusMessage(text(status, "Message"))
                .plateKey(plateKey)
                .plateNumber(numberPlate == null ? plateKey : numberPlate.trim())
                .vin(storedVin)
                .rawMark(rawMark)
                .brand(split.brand())
                .model(split.model())
                .modelYear(integer(car, "ManufactoringYear"))
                .colorRaw(text(car, "Color"))
                .engineCapacity(integer(car, "EngineCapacity"))
                .engineNumber(text(car, "EngineNumber"))
                .vehicleTypeId(integer(type, "Id"))
                .vehicleTypeDescription(typeDescription)
                .certificateNumber(text(certificate, "Number"))
                .certificateGivenDate(text(certificate, "GivenDate"))
                .ownerName(text(owner, "Name"))
                .ownerSurname(text(owner, "Surname"))
                .ownerPatronymic(text(owner, "Patronymic"))
                .rawJson(root.toString())
                .querierUserId(customer.getUserId())
                .querierName(customer.getName())
                .querierSurname(customer.getSurname())
                .queriedAt(LocalDateTime.now())
                .countsTowardLimit(success && !guideDemo(plateKey, customer.getUserId()))
                .build();
    }

    private static boolean guideDemo(String plateKey, Long userId) {
        return GUIDE_PLATE_KEY.equals(plateKey) && Long.valueOf(GUIDE_USER_ID).equals(userId);
    }

    private CarResponse toResponse(IddaPlateCheck row, String acceptLanguage) {
        boolean success = row.isVehicleSuccess();
        Car existing = success && row.getVin() != null ? findVin(row.getVin()) : null;
        boolean engineLocked = existing != null && existing.getEngineTypeId() != null;
        String message = row.getStatusMessage();
        if (message == null || message.isBlank()) {
            message = success
                    ? MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage)
                    : row.getStatusName();
        }
        return CarResponse.builder()
                .carId(existing == null ? null : existing.getCarId())
                .customerId(existing != null && existing.getCustomer() != null ? existing.getCustomer().getUserId() : null)
                .vin(row.getVin())
                .plateNumber(PlateNumbers.format(row.getPlateNumber()))
                .brand(row.getBrand())
                .model(row.getModel())
                .modelYear(row.getModelYear())
                .engineVolume(row.getEngineCapacity())
                .engineTypeId(existing == null ? null : existing.getEngineTypeId())
                .engineType(existing == null ? null : EngineTypeTranslation.translate(existing.getEngineType(), acceptLanguage))
                .engineTypeLocked(engineLocked)
                .bodyType(existing == null ? null : BodyTypeTranslation.translate(existing.getBodyType(), acceptLanguage))
                .mileage(existing == null ? null : existing.getMileage())
                .colorId(existing == null ? null : existing.getColorId())
                .iddaColor(row.getColorRaw())
                .resource("idda")
                .fromIdda(success)
                .iddaProvidedFields(success ? IddaProvidedFields.names(row.getColorRaw()) : Collections.emptyList())
                .vinProvidedFields(existing == null || existing.getVinProvidedFields() == null
                        ? Collections.emptyList()
                        : existing.getVinProvidedFields())
                .iddaStatusCode(row.getStatusCode())
                .iddaStatusName(row.getStatusName())
                .iddaStatusMessage(row.getStatusMessage())
                .requestIdentifier(row.getRequestIdentifier())
                .isRestrict(false)
                .message(message)
                .build();
    }

    /**
     * tr: Araç yok. Limit, IDDA'ya ulaşılamama ve eksik gövde bu cevabı kullanır.
     * en: No car. The cap, an unreachable IDDA and an unreadable body use this response.
     */
    private CarResponse restricted(String plate, String message) {
        return CarResponse.builder()
                .plateNumber(plate)
                .resource("idda")
                .fromIdda(false)
                .iddaProvidedFields(Collections.emptyList())
                .vinProvidedFields(Collections.emptyList())
                .isRestrict(true)
                .message(message)
                .build();
    }

    /**
     * tr: IDDA cevap verdi ama başarı değil. Status durur, araç alanları boş kalır. cars tablosuna bakılmaz.
     * en: IDDA answered and it is not a success. The status stays, vehicle fields stay empty. The cars table is not read.
     */
    private CarResponse restrictedLive(IddaPlateCheck row, String acceptLanguage) {
        String message = row.getStatusMessage();
        if (message == null || message.isBlank()) {
            message = row.getStatusName();
        }
        if (message == null || message.isBlank()) {
            message = MessagesLangValues.IDDA_UNAVAILABLE.getMessageByLang(acceptLanguage);
        }
        return CarResponse.builder()
                .plateNumber(row.getPlateNumber())
                .resource("idda")
                .fromIdda(false)
                .iddaProvidedFields(Collections.emptyList())
                .vinProvidedFields(Collections.emptyList())
                .iddaStatusCode(row.getStatusCode())
                .iddaStatusName(row.getStatusName())
                .iddaStatusMessage(row.getStatusMessage())
                .requestIdentifier(row.getRequestIdentifier())
                .isRestrict(true)
                .message(message)
                .build();
    }

    private Car findVin(String vin) {
        List<Car> cars = carRepository.findAllByVinIgnoreCase(vin);
        if (cars == null || cars.isEmpty()) {
            return null;
        }
        for (Car car : cars) {
            if (car.getVin() != null && car.getVin().equalsIgnoreCase(vin)) {
                return car;
            }
        }
        return cars.get(0);
    }

    private List<Brand> loadBrands() {
        return brandRepository.existsByIsnew(NEW_SET_MARKER)
                ? brandRepository.findAllByIsnew(NEW_SET_MARKER)
                : brandRepository.findAll();
    }

    private List<Model> loadModels() {
        return brandRepository.existsByIsnew(NEW_SET_MARKER)
                ? modelRepository.findAllByIsnew(NEW_SET_MARKER)
                : modelRepository.findAll();
    }

    private Customer requireCustomer(String phoneNumber, String userIdHeader, String acceptLanguage) {
        if (phoneNumber == null || phoneNumber.isBlank() || userIdHeader == null || userIdHeader.isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        long userId;
        try {
            userId = Long.parseLong(userIdHeader.trim());
        } catch (NumberFormatException ex) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(
                userId, phoneNumber.trim(), UserStatus.ACTIVE.name());
        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return customer;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        if (text == null || text.isBlank() || "null".equals(text)) {
            return null;
        }
        return text.trim();
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        String text = text(node, field);
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
