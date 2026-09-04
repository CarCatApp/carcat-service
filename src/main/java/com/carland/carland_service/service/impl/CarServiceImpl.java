package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.CarRequest;
import com.carland.carland_service.dto.request.PercentageRequest;
import com.carland.carland_service.dto.request.RecordRequest;
import com.carland.carland_service.dto.response.*;
import com.carland.carland_service.entity.*;
import com.carland.carland_service.enums.EngineTypeTranslation;
import com.carland.carland_service.enums.BodyTypeTranslation;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.exceptions.*;
import com.carland.carland_service.repository.*;
import com.carland.carland_service.service.AfterAddCarSyncService;
import com.carland.carland_service.service.HyperPercentageSyncService;
import com.carland.carland_service.service.CarService;
import com.carland.carland_service.service.PushNotificationService;
import com.carland.carland_service.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * tr: Araç yönetiminin ana implementasyonudur: VIN kontrolü/decode (NHTSA), araç ekleme-silme-düzenleme,
 *     kilometre güncelleme, müşteri servis kayıtları, bakım şablonuna ve Hyper partner verisine dayalı
 *     servis yüzdesi hesaplama/listeleme/düzenleme ve servis hatırlatma push bildirimleri.
 * en: Main implementation of car management: VIN check/decode (NHTSA), adding/removing/editing cars,
 *     mileage updates, customer service records, service percentage calculation/listing/editing based on
 *     the maintenance template and Hyper partner data, and service reminder push notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final MaintenanceTemplateRepository maintenanceTemplateRepository;
    private final ServiceHistoryRepository serviceHistoryRepository;
    private final AdminRepository adminRepository;
    private final CustomerServiceRecordRepository customerServiceRecordRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final VinService vinService;
    private final ColorRepository colorRepository;
    private final PercentageRepository percentageRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushNotificationService pushNotificationService;
    private final LogRepository logRepository;
    private final EngineTypeRepository engineTypeRepository;
    private final AfterAddCarSyncService afterAddCarSyncService;
    private final HyperPercentageSyncService hyperPercentageSyncService;
    private final RedisCacheService redisCacheService;
//    private static final List<String> simulatedVins = List.of(
//            "JTJGB7CX2R4121777",
//            "LFMAAA0C6S0640604",
//            "19XZE4F54NE012640",
//            "5TDKBRCH8RS143667"
//    );

    /**
     * tr: VIN'i kontrol eder: araç DB'de bir müşteriye bağlıysa AlreadyExistsException fırlatır;
     *     müşterisiz kayıtlıysa DB'deki verilerle ("fromDb"), hiç yoksa NHTSA decoder sonucuyla
     *     ("fromDecoderTool") doldurulmuş CarResponse döner. VIN'den elde edilen alanlar
     *     vinProvidedFields listesinde işaretlenir.
     * en: Checks the VIN: throws AlreadyExistsException when the car is already linked to a customer in
     *     the DB; returns a CarResponse filled from the DB record ("fromDb") when it exists without a
     *     customer, otherwise from the NHTSA decoder result ("fromDecoderTool"). Fields resolved from
     *     the VIN are flagged in the vinProvidedFields list.
     */
    @Override
    public CarResponse checkVin(String vin, String acceptLanguage) {


        // 2️⃣ Normal DB kontrolu
        Car carFromDb = carRepository.findByVin(vin);

        if (carFromDb != null && carFromDb.getCustomer() != null) {
            log.info("car ucun car !=null ve car.get customer != null controlu yandi");
            throw new AlreadyExistsException(MessagesLangValues.CAR_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        } else if (carFromDb != null && carFromDb.getCustomer() == null) {

            List<String> vinProvidedFields = carFromDb != null ? carFromDb.getVinProvidedFields() : null;


            return CarResponse.builder()
                    .vin(carFromDb != null ? carFromDb.getVin() : null)
                    .brand(carFromDb != null ? carFromDb.getBrand() : null)
                    .model(carFromDb != null ? carFromDb.getModel() : null)
                    .modelYear(carFromDb != null ? carFromDb.getModelYear() : null)
                    .bodyType(BodyTypeTranslation.translate(
                            carFromDb != null ? carFromDb.getBodyType() : null,
                            acceptLanguage
                    ))
                    .transmissionType(carFromDb != null ? carFromDb.getTransmissionType() : null)
                    .engineVolume(carFromDb != null ? carFromDb.getEngineVolume() : null)
                    .engineType(
                            EngineTypeTranslation.translate(
                                    carFromDb.getEngineType(),
                                    acceptLanguage
                            )
                    )
                    .vinProvidedFields(vinProvidedFields)
                    .resource("fromDb")
                    .plateNumber(carFromDb != null ? carFromDb.getPlateNumber() : null)
                    .engineTypeId(carFromDb.getEngineTypeId())
                    .mileage(carFromDb != null ? carFromDb.getMileage() : null)
                    .build();
        } else {

            // 3️⃣ NHTSA decode flow
            Map<String, String> decodeVin = vinService.extractFieldsFromVin(vin);

            List<String> vinProvidedFields = new ArrayList<>();

            String brand = decodeVin.get("brand");
            if (hasValue(brand)) vinProvidedFields.add("brand");

            String model = decodeVin.get("model");
            if (hasValue(model)) vinProvidedFields.add("model");

            String modelYearStr = decodeVin.get("modelYear");
            Integer modelYear = null;
            if (hasValue(modelYearStr)) {
                try {
                    modelYear = Integer.valueOf(modelYearStr);
                    vinProvidedFields.add("modelYear");
                } catch (NumberFormatException e) {
                    log.warn("Model year parse edilmedi: {}", modelYearStr);
                }
            }

            String bodyType = decodeVin.get("bodyType");
            if (hasValue(bodyType)) vinProvidedFields.add("bodyType");

            String transmissionType = decodeVin.get("transmissionType");
            if (hasValue(transmissionType)) vinProvidedFields.add("transmissionType");

            String engineVolumeStr = decodeVin.get("engineVolume");
            Integer engineVolume = null;
            if (hasValue(engineVolumeStr)) {
                engineVolume = convertEngineVolumeSafe(engineVolumeStr);
                if (engineVolume != null) {
                    vinProvidedFields.add("engineVolume");
                }
            }

            String engineType = decodeVin.get("engineType");
            if (hasValue(engineType)) vinProvidedFields.add("engineType");


            return CarResponse.builder()
                    .vin(vin)
                    .brand(brand)
                    .model(model)
                    .modelYear(modelYear)
                    .bodyType(BodyTypeTranslation.translate(bodyType, acceptLanguage))
                    .transmissionType(transmissionType)
                    .engineVolume(engineVolume)
                    .engineType(EngineTypeTranslation.translate(engineType, acceptLanguage))
                    .vinProvidedFields(vinProvidedFields)
                    .resource("fromDecoderTool")
                    .build();

//            return CarResponse.builder()
//                    .engineTypeId(null)
//                    .vin(vin)
//                    .bodyType(null)
//                    .mileage(null)
//                    .plateNumber(null)
//                    .model(null)
//                    .engineType(null)
//                    .color(null)
//                    .modelYear(null)
//                    .vinProvidedFields(Collections.emptyList())
//                    .resource("unknown")
//                    .message("vin min yoxdu :)")
//                    .build();
        }
    }

    /**
     * tr: Renk listesini dile göre çevirip dile özgü sıralamayla (az için sabit sıra, diğer dillerde
     *     alfabetik ve "Other" en sonda) döner; liste boşsa ResourceNotFoundException fırlatır.
     * en: Returns the color list translated per language with language-specific ordering (a fixed order
     *     for az, alphabetical with "Other" last for other languages); throws ResourceNotFoundException
     *     when the list is empty.
     */
    @Override
    public List<Color> getColors(String acceptLanguage) {
        return redisCacheService.getOrLoadColors(acceptLanguage, () -> loadColors(acceptLanguage));
    }

    private List<Color> loadColors(String acceptLanguage) {

        List<Color> colors = colorRepository.findAll();

        if (colors.isEmpty()) {
            throw new ResourceNotFoundException(
                    MessagesLangValues.COLOR_NOT_FOUND.getMessageByLang(acceptLanguage)
            );
        }

        List<String> azOrder = List.of(
                "Qara", "Yaş asfalt", "Boz", "Gümüşü", "Ağ", "Bej", "Tünd qırmızı", "Qırmızı",
                "Çəhrayı", "Narıncı", "Qızılı", "Sarı", "Xaki", "Tünd yaşıl", "Yaşıl", "Açıq yaşıl",
                "Mavi", "Göy", "Bənövşəyi", "Qəhvəyi", "Bordo", "Mat qara", "Metalik gümüş",
                "Tünd mavi", "İncə ağ", "Digər"
        );
        List<String> enOrder = List.of(
                "Black", "Wet Asphalt", "Gray / Grey", "Gray", "Silver", "White", "Beige", "Dark Red",
                "Red", "Pink", "Orange", "Gold", "Yellow", "Khaki", "Dark Green", "Green", "Light Green",
                "Light Blue", "Blue", "Purple", "Brown", "Maroon", "Matte Black", "Metallic silver",
                "Navy blue", "Pearl white", "Other"
        );
        List<String> ruOrder = List.of(
                "Чёрный", "Мокрый асфальт", "Серый", "Серебристый", "Белый", "Бежевый", "Тёмно-красный",
                "Красный", "Розовый", "Оранжевый", "Золотой", "Жёлтый", "Хаки", "Тёмно-зелёный", "Зелёный",
                "Светло-зелёный", "Голубой", "Синий", "Фиолетовый", "Коричневый", "Тёмно-бордовый",
                "Матовый чёрный", "Серебристый металлик", "Тёмно-синий", "Жемчужно-белый", "Другой"
        );
        String lang = acceptLanguage == null ? "az" : acceptLanguage.toLowerCase();
        List<String> order = lang.startsWith("en") ? enOrder : lang.startsWith("ru") ? ruOrder : azOrder;

        return colors.stream()
                .map(src -> Color.builder()
                        .colorId(src.getColorId())
                        .color(src.nameForLang(acceptLanguage))
                        .hex(src.getHex())
                        .build())
                .sorted((c1, c2) -> {
                    int i1 = order.indexOf(c1.getColor());
                    int i2 = order.indexOf(c2.getColor());
                    if (i1 < 0 && i2 < 0) {
                        String n1 = c1.getColor() == null ? "" : c1.getColor();
                        String n2 = c2.getColor() == null ? "" : c2.getColor();
                        return n1.compareToIgnoreCase(n2);
                    }
                    if (i1 < 0) {
                        return 1;
                    }
                    if (i2 < 0) {
                        return -1;
                    }
                    return Integer.compare(i1, i2);
                })
                .toList();
    }


    /**
     * tr: Araca ait müşteri servis kayıtlarını döner. Eksik parametrede MissingFieldException, müşteri
     *     bulunamazsa UserNotFoundException, araç müşteriye ait değilse veya kayıt listesi boşsa
     *     ResourceNotFoundException fırlatır.
     * en: Returns the customer service records of the car. Throws MissingFieldException on missing
     *     parameters, UserNotFoundException when the customer is not found, and ResourceNotFoundException
     *     when the car does not belong to the customer or the record list is empty.
     */
    @Override
    public List<RecordResponse> getServiceRecords(Long carId, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {

        if (carId == null || phoneNumber == null || userIdHeader == null || acceptLanguage == null) {
            log.info("body de missing fieldler var");
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        log.info("Customer adi:{}", customer.getName());

        Car car = requireCustomerCar(carId, customer, acceptLanguage);
        log.info("car id : {}", car.getCarId());

        List<CustomerServiceRecord> customerServiceRecordList = car.getServiceRecordList();
        log.info("[hist-debug] getServiceRecords | carId={} recordCount={}",
                car.getCarId(), customerServiceRecordList != null ? customerServiceRecordList.size() : null);

        if (customerServiceRecordList == null || customerServiceRecordList.isEmpty()) {
            log.info("[hist-debug] getServiceRecords empty | carId={}", car.getCarId());
            log.info("customer service record list bosdur");
            throw new ResourceNotFoundException(MessagesLangValues.RECORD_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        log.info("OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        log.info("record list:  {}", customerServiceRecordList);
        log.info("OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");


        List<RecordResponse> responses = customerServiceRecordList.stream()
                .map(record -> RecordResponse.builder()
                        .id(record.getId())
                        .serviceId(record.getServiceId())
                        .serviceName(record.getServiceName())
                        .serviceNameAz(record.getServiceNameAz())
                        .serviceNameEn(record.getServiceNameEn())
                        .serviceNameRu(record.getServiceNameRu())
                        .actionType(record.getActionType())
                        .doneDate(record.getDoneDate())
                        .doneKm(record.getDoneKm())
                        .build())
                .toList();
        log.info("responses: {}", responses);
        log.info("[hist-debug] getServiceRecords mapped | carId={} ids={} names={} (servicedStatus NOT in this response)",
                car.getCarId(),
                responses.stream().map(RecordResponse::getId).toList(),
                responses.stream().map(RecordResponse::getServiceName).toList());
        return responses;
    }


    /**
     * tr: Aracın gönderilen (null/boş olmayan) alanlarını günceller (marka, model, km, plaka, motor tipi,
     *     motor hacmi, kasa tipi, model yılı) ve güncel aracı döner. Eksik zorunlu alanlarda
     *     MissingFieldException; müşteri/araç doğrulamasında UserNotFoundException veya
     *     ResourceNotFoundException fırlatır.
     * en: Updates only the provided (non-null/non-blank) fields of the car (brand, model, mileage, plate,
     *     engine type, engine volume, body type, model year) and returns the updated car. Throws
     *     MissingFieldException on missing required fields, and UserNotFoundException or
     *     ResourceNotFoundException from the customer/car ownership checks.
     */
    @Override
    public CarResponse editCarDetails(CarRequest carRequest, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {

        if (carRequest == null || phoneNumber == null || userIdHeader == null || carRequest.getCarId() == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }


        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(carRequest.getCarId(), customer, acceptLanguage);

        if (carRequest.getBrand() == null) {
            log.info("null");
        } else if (carRequest.getBrand().equals("")) {
            log.info("blank");
        } else {
            car.setBrand(carRequest.getBrand());
        }

        if (carRequest.getModel() == null) {
            log.info("null");
        } else if (carRequest.getModel().equals("")) {
            log.info("blank");
        } else {
            car.setModel(carRequest.getModel());
        }


        if (carRequest.getMileage() != null) {
            car.setMileage(carRequest.getMileage());
        }

        if (carRequest.getPlateNumber() != null) {
            car.setPlateNumber(carRequest.getPlateNumber());
        }

//        if (carRequest.getColorId() != null) {
//            car.setColorId(carRequest.getColorId());
//        }

        if (carRequest.getEngineTypeId() != null) {
            EngineType engineType = engineTypeRepository.findByEngineTypeId(carRequest.getEngineTypeId());
            car.setEngineType(engineType.getEngineType());
        }

        if (carRequest.getEngineVolume() != null) {
            car.setEngineVolume(carRequest.getEngineVolume());
        }

//        if (carRequest.getTransmissionType() != null) {
//            car.setTransmissionType(carRequest.getTransmissionType());
//        }

        if (carRequest.getBodyType() != null) {
            car.setBodyType(carRequest.getBodyType());
        }

        if (carRequest.getModelYear() != null) {
            car.setModelYear(carRequest.getModelYear());
        }
        carRepository.save(car);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        return convertCarEntityToResponse(car, acceptLanguage, "null");
    }


//    @Override
//    public void calculateAndPushNotification() {
//        List<Car> cars = carRepository.findAllWithCustomer();
//
//        for (Car car : cars) {
//
//            Customer customer = car.getCustomer();
//            if (customer == null) continue;
//
//            DeviceToken deviceToken = deviceTokenRepository.findByUserId(customer.getUserId());
//            if (deviceToken == null || deviceToken.getDeviceToken() == null) continue;
//
//            List<Percentage> percentages = percentageRepository.findAllByCarId(car.getCarId());
//            if (percentages == null || percentages.isEmpty()) continue;
//
//            for (Percentage percentage : percentages) {
//
//                if ((percentage.getKmPercentage() == null || percentage.getKmPercentage() == 0)
//                        && (percentage.getMonthPercentage() == null || percentage.getMonthPercentage() == 0)) {
//                    continue;
//                }
//
//                // 1 hafta kontrolü
//                if (!canSendNotification(percentage)) continue;
//
//                // %10 threshold
//                boolean kmLow = percentage.getKmPercentage() != null && percentage.getKmPercentage() <= 10;
//                boolean monthLow = percentage.getMonthPercentage() != null && percentage.getMonthPercentage() <= 10;
//
//                if (!kmLow && !monthLow) continue;
//
//                // Mesaj oluştur
//                String[] message = buildMessage(percentage, customer.getNotificationLanguage());
//
//                // Push gönder
//                boolean pushSent = sendServiceReminder(deviceToken.getDeviceToken(), message[0], message[1]);
//
//                if (pushSent) {
//                    percentage.setLastNotificationSentAt(LocalDateTime.now());
//                    percentageRepository.save(percentage);
//                }
//            }
//        }
//    }

    /**
     * tr: Tüm araçları müşterileriyle birlikte yükler ve cihaz token'ı olan her müşteriye sabit metinli
     *     yağ değişimi hatırlatma push'u gönderir. (Yüzde bazlı dinamik bildirim mantığı üstteki yorum
     *     satırlarındadır ve şu an devre dışıdır.)
     * en: Loads all cars with their customers and sends a fixed-text oil change reminder push to every
     *     customer that has a device token. (The percentage-based dynamic notification logic is in the
     *     commented block above and is currently disabled.)
     */
    @Override
    public void calculateAndPushNotification() {
        List<Car> cars = carRepository.findAllWithCustomer();

        for (Car car : cars) {

            Customer customer = car.getCustomer();
            if (customer == null) continue;

            DeviceToken deviceToken = deviceTokenRepository.findByUserId(customer.getUserId());
            if (deviceToken == null || deviceToken.getDeviceToken() == null) continue;


            sendServiceReminder(deviceToken.getDeviceToken(), "Yağ dəyişimi vaxtı yaxınlaşır ⏰\n", car.getPlateNumber() + " - yağ dəyişiminə 230 km qalıb. Vaxtında baxım avtomobilinizi qoruyar \uD83D\uDEDE");
        }
    }

    /**
     * tr: Aracın bakım şablonundaki her servis için yüzde satırı üretir ve kalan ömre göre sıralı liste döner.
     *     Manuel düzenlenmiş (EDITED_BY_*) yüzdeler kayıtlı değerlerden, CREATED olanlar önce Hyper partner
     *     satırından, o da yoksa müşteri kaydı/servis geçmişinden hesaplanır. Eksik parametrede
     *     MissingFieldException; müşteri/araç doğrulamasında UserNotFoundException/ResourceNotFoundException fırlatır.
     * en: Builds a percentage row for every service in the car's maintenance template and returns the list
     *     sorted by remaining service life. Manually edited (EDITED_BY_*) percentages use the stored values;
     *     CREATED ones are computed first from the Hyper partner line, otherwise from customer records/service
     *     history. Throws MissingFieldException on missing parameters, and
     *     UserNotFoundException/ResourceNotFoundException from the customer/car ownership checks.
     */
    @Override
    public PercentageResponse getServicePercentageList(Long carId, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {

        if (carId == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(carId, customer, acceptLanguage);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.forLanguageTag(acceptLanguage));

        List<CarServicePercentageResponse> responseList = new ArrayList<>();

        MaintenanceTemplate template = car.getMaintenanceTemplate();
        log.info("template,{}", template.getId());
        List<ServiceEntity> serviceEntities = serviceEntityRepository.findAllByMaintenanceTemplate(template);
        log.info("service entities names,{}", serviceEntities.stream().map(serviceEntity -> serviceEntity.getServiceName() + " , ").toList());
        List<CustomerServiceRecord> customerServiceRecordList = customerServiceRecordRepository.findAllByCar(car);
        log.info("customerServiceRecordList, {}", customerServiceRecordList.stream().map(customerServiceRecord -> customerServiceRecord.getServiceName() + " , ").toList());
        log.info("[hist-debug] percentages CSR statuses | carId={} statuses={}",
                car.getCarId(),
                customerServiceRecordList.stream()
                        .map(r -> r.getServiceName() + "=" + r.getServicedStatus())
                        .toList());
        List<ServiceHistory> serviceHistories = serviceHistoryRepository.findAllByCar(car);
        log.info("serviceHistories {}", serviceHistories.stream().map(serviceHistory -> serviceHistory.getServiceName() + " , ").toList());
        log.info("[hist-debug] percentages tables | carId={} csrCount={} v1HistoryCount={} mileage={}",
                car.getCarId(), customerServiceRecordList.size(), serviceHistories.size(), car.getMileage());
        for (ServiceEntity serviceEntity : serviceEntities) {
            CustomerServiceRecord record = customerServiceRecordRepository.findByServiceIdAndCar(serviceEntity.getId(), car);

            Percentage percentage = percentageRepository.findByServiceIdAndCarId(serviceEntity.getId(), car.getCarId());

            PercentageStatus listStatus = percentage != null
                    ? PercentageStatus.fromStored(percentage.getStatus())
                    : PercentageStatus.CREATED;
            boolean useEditedPercentage = percentage != null && listStatus.isManuallySet();

            // ===================== EDITED FLOW =====================
            if (useEditedPercentage) {

                LocalDate lastServiceDate = percentage.getLastServiceDate();
                Integer lastServiceKm = percentage.getLastServiceKm();
                Integer nextServiceKm = percentage.getNextServiceKm();
                LocalDate nextServiceDate = percentage.getNextServiceDate();

                Integer kmPercentage = computeKmPercentage(lastServiceKm, nextServiceKm, car.getMileage());
                Integer remainingKm = computeRemainingKm(lastServiceKm, nextServiceKm, car.getMileage());
                Integer monthPercentageDigit = computeMonthPercentage(lastServiceDate, nextServiceDate);
                String remainingDaysValue = computeRemainingDaysValue(lastServiceDate, nextServiceDate);

                responseList.add(

                        CarServicePercentageResponse.builder()
                                .percentageId(percentage.getId())
                                .serviceId(serviceEntity.getId())
                                .serviceName(serviceEntity.getServiceName())
                                .serviceNameAz(serviceEntity.getNameAz())
                                .serviceNameEn(serviceEntity.getNameEn())
                                .serviceNameRu(serviceEntity.getNameRu())
                                .actionType(percentage.getActionType())
                                .intervalKm(percentage.getIntervalKm())
                                .intervalMonth(percentage.getIntervalMonth())
                                .kmPercentage(kmPercentage)
                                .monthPercentageDigit(monthPercentageDigit)
                                .remainingKm(remainingKm)
                                .remainingMonths(remainingDaysValue)
                                .lastServiceKm(lastServiceKm)
                                .lastServiceDate(formatWithCapitalizedMonth(lastServiceDate, formatter, Locale.forLanguageTag(acceptLanguage)))
                                .nextServiceKm(nextServiceKm)
                                .nextServiceDate(formatWithCapitalizedMonth(nextServiceDate, formatter, Locale.forLanguageTag(acceptLanguage)))
                                .status(listStatus.name())
                                .editable(listStatus.isEditable())
                                .servicedStatus(record != null ? record.getServicedStatus() : null)
                                .important(percentage.isImportant())
                                .build()
                );

                continue;
            }


// ================= CREATED / DEFAULT FLOW =================

            Optional<HyperPercentageSyncService.PartnerLineSnapshot> partnerLine =
                    hyperPercentageSyncService.findBestPartnerLineForService(
                            car,
                            serviceEntity.getNameEn(),
                            serviceEntity.getIntervalKm(),
                            serviceEntity.getIntervalMonth()
                    );

            if (partnerLine.isPresent()) {
                HyperPercentageSyncService.PartnerLineSnapshot snap = partnerLine.get();
                log.info("[hist-debug] percentages using partner visit line | carId={} serviceId={} nameEn={} lastDate={} lastKm={} csrStatus={}",
                        car.getCarId(),
                        serviceEntity.getId(),
                        serviceEntity.getNameEn(),
                        snap.lastServiceDate(),
                        snap.lastServiceKm(),
                        record != null ? record.getServicedStatus() : null);
                LocalDate lastServiceDate = snap.lastServiceDate() != null
                        ? snap.lastServiceDate()
                        : car.getCreatedAt().toLocalDate();
                Integer lastServiceKm = snap.lastServiceKm() != null ? snap.lastServiceKm() : 0;
                Integer nextServiceKm = snap.nextServiceKm();
                LocalDate nextServiceDate = snap.nextServiceDate();

                Integer kmPercentage = computeKmPercentage(lastServiceKm, nextServiceKm, car.getMileage());
                Integer remainingKm = computeRemainingKm(lastServiceKm, nextServiceKm, car.getMileage());
                Integer monthPercentageDigit = computeMonthPercentage(lastServiceDate, nextServiceDate);
                String remainingDaysValue = computeRemainingDaysValue(lastServiceDate, nextServiceDate);

                responseList.add(
                        CarServicePercentageResponse.builder()
                                .percentageId(percentage != null ? percentage.getId() : null)
                                .serviceId(serviceEntity.getId())
                                .serviceName(serviceEntity.getServiceName())
                                .serviceNameAz(serviceEntity.getNameAz())
                                .serviceNameEn(serviceEntity.getNameEn())
                                .serviceNameRu(serviceEntity.getNameRu())
                                .actionType(serviceEntity.getActionType())
                                .intervalKm(serviceEntity.getIntervalKm())
                                .intervalMonth(serviceEntity.getIntervalMonth())
                                .kmPercentage(kmPercentage)
                                .monthPercentageDigit(monthPercentageDigit)
                                .remainingKm(remainingKm)
                                .remainingMonths(remainingDaysValue)
                                .lastServiceKm(lastServiceKm)
                                .lastServiceDate(formatWithCapitalizedMonth(lastServiceDate, formatter, Locale.forLanguageTag(acceptLanguage)))
                                .nextServiceKm(nextServiceKm)
                                .nextServiceDate(formatWithCapitalizedMonth(nextServiceDate, formatter, Locale.forLanguageTag(acceptLanguage)))
                                .status(listStatus.name())
                                .editable(listStatus.isEditable())
                                .servicedStatus(record != null ? record.getServicedStatus() : null)
                                .important(percentage != null ? percentage.isImportant() : serviceEntity.isImportant())
                                .build()
                );
                continue;
            }

            List<CustomerServiceRecord> csrList = customerServiceRecordList.stream()
                    .filter(r -> serviceEntity.getId().equals(r.getServiceId()))
                    .toList();

            // ServiceHistory Hyper kaynakli; serviceId yok, isim+actionType ile eslesir
            List<ServiceHistory> shList = serviceHistories.stream()
                    .filter(h -> h.getServiceName().equalsIgnoreCase(serviceEntity.getServiceName())
                            && h.getActionType() != null
                            && h.getActionType().stream().anyMatch(action -> action.equalsIgnoreCase(serviceEntity.getActionType())))
                    .toList();

            log.info("[hist-debug] percentages CSR/v1 fallback | carId={} serviceId={} name={} csrMatches={} v1HistoryMatches={} csrStatus={}",
                    car.getCarId(),
                    serviceEntity.getId(),
                    serviceEntity.getServiceName(),
                    csrList.size(),
                    shList.size(),
                    record != null ? record.getServicedStatus() : null);

            LocalDate lastServiceDate = Stream.concat(csrList.stream()
                            .map(CustomerServiceRecord::getDoneDate), shList
                            .stream()
                            .map(ServiceHistory::getDoneDate))
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(car.getCreatedAt().toLocalDate());

            Integer lastServiceKm = Stream.concat(csrList.stream().map(CustomerServiceRecord::getDoneKm), shList
                            .stream()
                            .map(ServiceHistory::getDoneKm))
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);

// ================= KM CALC =================

            Integer remainingKm = null;
            Integer kmPercentage = null;
            Integer nextServiceKm = null;

            if (serviceEntity.getIntervalKm() != null && car.getMileage() != null) {

                long intervalKm = serviceEntity.getIntervalKm();
                long usedKm = car.getMileage() - lastServiceKm;

                remainingKm = (int) Math.max(intervalKm - usedKm, 0);
                nextServiceKm = Math.toIntExact(lastServiceKm + intervalKm);

                kmPercentage = (int) Math.round((remainingKm * 100.0) / intervalKm);

                kmPercentage = Math.max(0, Math.min(100, kmPercentage));
            }

// ================= MONTH CALC (GÜN BAZLI) =================

            String remainingDaysValue = null;
            Integer monthPercentageDigit = null;
            LocalDate nextServiceDate = null;

            if (serviceEntity.getIntervalMonth() != null) {
                nextServiceDate = lastServiceDate.plusMonths(serviceEntity.getIntervalMonth());
                monthPercentageDigit = computeMonthPercentage(lastServiceDate, nextServiceDate);
                remainingDaysValue = computeRemainingDaysValue(lastServiceDate, nextServiceDate);
            }

// ================= RESPONSE =================

            responseList.add(
                    CarServicePercentageResponse.builder()
                            .percentageId(percentage != null ? percentage.getId() : null)
                            .serviceId(serviceEntity.getId())
                            .serviceName(serviceEntity.getServiceName())
                            .serviceNameAz(serviceEntity.getNameAz())
                            .serviceNameEn(serviceEntity.getNameEn())
                            .serviceNameRu(serviceEntity.getNameRu())
                            .actionType(serviceEntity.getActionType())
                            .intervalKm(serviceEntity.getIntervalKm())
                            .intervalMonth(serviceEntity.getIntervalMonth())
                            .kmPercentage(kmPercentage)
                            .monthPercentageDigit(monthPercentageDigit)
                            .remainingKm(remainingKm)
                            .remainingMonths(remainingDaysValue)
                            .lastServiceKm(lastServiceKm)
                            .lastServiceDate(formatWithCapitalizedMonth(lastServiceDate, formatter, Locale.forLanguageTag(acceptLanguage)))
                            .nextServiceKm(nextServiceKm)
                            .nextServiceDate(formatWithCapitalizedMonth(nextServiceDate, formatter, Locale.forLanguageTag(acceptLanguage)))
                            .status(listStatus.name())
                            .editable(listStatus.isEditable())
                            .servicedStatus(record != null ? record.getServicedStatus() : null)
                            .important(percentage != null ? percentage.isImportant() : serviceEntity.isImportant())
                            .build()
            );

        }
        responseList.sort(Comparator
                .comparingInt(this::remainingServiceScore)
                .thenComparing(CarServicePercentageResponse::getServiceName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        log.info("[hist-debug] percentages done | carId={} itemCount={}", car.getCarId(), responseList.size());
        return PercentageResponse.builder()
                .carId(car.getCarId())
                .vin(car.getVin())
                .responseList(responseList)
                .build();
    }

    /**
     * tr: Müşterinin bir yüzde kaydını (son/sonraki servis km ve tarihleri) düzenlemesini sağlar ve durumu
     *     EDITED_BY_CUSTOMER yaparak günceller. Eksik alanlarda MissingFieldException; müşteri/araç
     *     doğrulamasında UserNotFoundException/ResourceNotFoundException; yüzde bulunamaz veya araca ait
     *     değilse ResourceNotFoundException; kayıt partner kilitliyse (EDITED_BY_PARTNER) ConflictException fırlatır.
     * en: Lets the customer edit a percentage record (last/next service km and dates) and saves it with the
     *     EDITED_BY_CUSTOMER status. Throws MissingFieldException on missing fields;
     *     UserNotFoundException/ResourceNotFoundException from the customer/car checks;
     *     ResourceNotFoundException when the percentage is missing or belongs to another car; and
     *     ConflictException when the record is partner-locked (EDITED_BY_PARTNER).
     */
    @Override
    public CarServicePercentageResponse editPercentage(PercentageRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {

        if (request == null || request.getCarId() == null || request.getPercentageId() == null || phoneNumber == null || userIdHeader == null) {
            log.error("missing body var");
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(request.getCarId(), customer, acceptLanguage);

        Percentage percentage = percentageRepository.findById(request.getPercentageId())
                .orElseThrow(() -> {
                    log.error("Percentage tapılmadı. ID: {}", request.getPercentageId());
                    return new ResourceNotFoundException("Hesablama tapilmadi");
                });
        log.info("[pct-status-debug] editPercentage START | carId={}, percentageId={}, serviceId={}, serviceName={}, statusBefore={}, thread={}",
                request.getCarId(), percentage.getId(), percentage.getServiceId(), percentage.getServiceName(),
                percentage.getStatus(), Thread.currentThread().getName());
        if (!percentage.getCarId().equals(car.getCarId())) {
            log.error("Hesablama bu avtomobile aid deyil");
            throw new ResourceNotFoundException("Hesablama bu avtomobile aid deyil");
        }

        // Partner-locked percentages cannot be edited by the customer (backend enforcement).
        if (PercentageStatus.fromStored(percentage.getStatus()) == PercentageStatus.EDITED_BY_PARTNER) {
            log.warn("editPercentage rejected: percentage is partner-locked | percentageId={}", percentage.getId());
            throw new ConflictException("Bu hesablama partnyor tərəfindən yenilənib və redaktə edilə bilməz");
        }

        if (request.getLastServiceKm() != null) {
            percentage.setLastServiceKm(request.getLastServiceKm());
        }
        if (request.getNextServiceKm() != null) {
            percentage.setNextServiceKm(request.getNextServiceKm());
        }
        if (request.getLastServiceDate() != null) {
            percentage.setLastServiceDate(request.getLastServiceDate());
        }
        if (request.getNextServiceDate() != null) {
            percentage.setNextServiceDate(request.getNextServiceDate());
        }
        percentage.setStatus(PercentageStatus.EDITED_BY_CUSTOMER.name());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.forLanguageTag(acceptLanguage));

        percentageRepository.save(percentage);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        log.info("[pct-status-debug] editPercentage SAVED | carId={}, percentageId={}, serviceName={}, statusAfter={}, thread={}",
                car.getCarId(), percentage.getId(), percentage.getServiceName(),
                percentage.getStatus(), Thread.currentThread().getName());

        CarServicePercentageResponse response = CarServicePercentageResponse.builder()
                .percentageId(percentage.getId())
                .serviceId(percentage.getServiceId())
                .serviceName(percentage.getServiceName())
                .serviceNameAz(percentage.getServiceNameAz())
                .serviceNameEn(percentage.getServiceNameEn())
                .serviceNameRu(percentage.getServiceNameRu())
                .actionType(percentage.getActionType())

                .intervalKm(percentage.getIntervalKm())
                .intervalMonth(percentage.getIntervalMonth())

                .kmPercentage(percentage.getKmPercentage())
                .monthPercentage(percentage.getMonthPercentage())

                .remainingKm(percentage.getRemainingKm())
                .remainingMonths(percentage.getRemainingMonths() != null ? percentage.getRemainingMonths().format(formatter) : null)

                .lastServiceKm(percentage.getLastServiceKm())
                .lastServiceDate(percentage.getLastServiceDate() != null ? percentage.getLastServiceDate().format(formatter) : null)
                .nextServiceKm(percentage.getNextServiceKm())
                .nextServiceDate(percentage.getNextServiceDate() != null ? percentage.getNextServiceDate().format(formatter) : null)
                .status(PercentageStatus.fromStored(percentage.getStatus()).name())
                .editable(PercentageStatus.fromStored(percentage.getStatus()).isEditable())
                .important(percentage.isImportant())
                .build();
        log.info("response: {}", response);
        return response;
    }


    /**
     * tr: Aracın tüm yüzde kayıtlarını yeniden hesaplayıp kaydeder: manuel düzenlenmiş (EDITED_BY_*) olanların
     *     durumu korunarak sadece türetilmiş alanları tazelenir; diğerleri önce Hyper partner satırından
     *     (EDITED_BY_PARTNER), yoksa müşteri kaydı/servis geçmişinden (CREATED) hesaplanır. Eksik parametrede
     *     MissingFieldException; müşteri/araç doğrulamasında UserNotFoundException/ResourceNotFoundException;
     *     araçta hiç yüzde yoksa ResourceNotFoundException fırlatır.
     * en: Recomputes and persists all percentage records of the car: manually edited (EDITED_BY_*) ones keep
     *     their status and only get their derived fields refreshed; the rest are computed from the Hyper
     *     partner line (EDITED_BY_PARTNER) when available, otherwise from customer records/service history
     *     (CREATED). Throws MissingFieldException on missing parameters,
     *     UserNotFoundException/ResourceNotFoundException from the customer/car checks, and
     *     ResourceNotFoundException when the car has no percentages at all.
     */
    @Override
    public PercentageResponse executeServicePercentages(
            Long carId,
            String phoneNumber,
            String userIdHeader,
            String timezone,
            String acceptLanguage) {

        if (carId == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(
                    MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(carId, customer, acceptLanguage);

        List<Percentage> percentages =
                percentageRepository.findAllByCarId(car.getCarId());

        if (percentages.isEmpty()) {
            throw new ResourceNotFoundException(
                    MessagesLangValues.SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        log.info("[pct-status-debug] executeServicePercentages START | carId={}, vin={}, count={}, thread={}",
                car.getCarId(), car.getVin(), percentages.size(), Thread.currentThread().getName());
        percentages.forEach(p -> log.info(
                "[pct-status-debug] execute snapshot at load | carId={}, percentageId={}, serviceId={}, serviceName={}, status={}",
                car.getCarId(), p.getId(), p.getServiceId(), p.getServiceName(), p.getStatus()));

        int recomputeSavedCount = 0;
        int manualPreservedCount = 0;

        /* ===== Locale & Date Formatter ===== */
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag(acceptLanguage));
        List<CarServicePercentageResponse> responseList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        Set<Long> serviceIds = percentages.stream()
                .map(Percentage::getServiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ServiceEntity> servicesById = serviceEntityRepository.findAllById(serviceIds).stream()
                .collect(Collectors.toMap(ServiceEntity::getId, s -> s));

        for (Percentage loaded : percentages) {
            Percentage percentage = percentageRepository.findById(loaded.getId()).orElse(loaded);

            ServiceEntity service = percentage.getServiceId() != null
                    ? servicesById.get(percentage.getServiceId())
                    : null;
            if (service != null) {
                percentage.setImportant(service.isImportant());
                percentage.setServiceName(service.getServiceName());
                percentage.setServiceNameAz(service.getNameAz());
                percentage.setServiceNameEn(service.getNameEn());
                percentage.setServiceNameRu(service.getNameRu());
            }

            PercentageStatus execStatus = PercentageStatus.fromStored(percentage.getStatus());
            if (execStatus.isManuallySet()) {
                log.info("[pct-status-debug] execute SKIP recompute (manual status preserved) | carId={}, percentageId={}, serviceName={}, status={}, thread={}",
                        car.getCarId(), percentage.getId(), percentage.getServiceName(), execStatus.name(),
                        Thread.currentThread().getName());
                manualPreservedCount++;

                LocalDate lastServiceDate = percentage.getLastServiceDate();
                Integer lastServiceKm = percentage.getLastServiceKm();
                Integer nextServiceKm = percentage.getNextServiceKm();
                LocalDate nextServiceDate = percentage.getNextServiceDate();

                Integer kmPercentage = computeKmPercentage(lastServiceKm, nextServiceKm, car.getMileage());
                Integer monthPercentage = computeMonthPercentage(lastServiceDate, nextServiceDate);
                Integer remainingKm = computeRemainingKm(lastServiceKm, nextServiceKm, car.getMileage());

                if (kmPercentage != null) {
                    percentage.setKmPercentage(kmPercentage);
                }
                if (monthPercentage != null) {
                    percentage.setMonthPercentage(monthPercentage);
                }
                if (remainingKm != null) {
                    percentage.setRemainingKm(remainingKm);
                }

                responseList.add(
                        CarServicePercentageResponse.builder()
                                .percentageId(percentage.getId())
                                .serviceId(percentage.getServiceId())
                                .serviceName(percentage.getServiceName())
                                .serviceNameAz(percentage.getServiceNameAz())
                                .serviceNameEn(percentage.getServiceNameEn())
                                .serviceNameRu(percentage.getServiceNameRu())
                                .actionType(percentage.getActionType())
                                .intervalKm(percentage.getIntervalKm())
                                .intervalMonth(percentage.getIntervalMonth())
                                .kmPercentage(kmPercentage)
                                .monthPercentage(monthPercentage)
                                .monthPercentageDigit(monthPercentage)
                                .remainingKm(remainingKm)
                                .remainingMonths(
                                        nextServiceDate != null
                                                ? nextServiceDate.format(formatter)
                                                : (percentage.getRemainingMonths() != null
                                                ? percentage.getRemainingMonths().format(formatter)
                                                : null)
                                )
                                .lastServiceKm(lastServiceKm)
                                .lastServiceDate(
                                        lastServiceDate != null
                                                ? lastServiceDate.format(formatter)
                                                : null
                                )
                                .nextServiceKm(nextServiceKm)
                                .nextServiceDate(
                                        nextServiceDate != null
                                                ? nextServiceDate.format(formatter)
                                                : null
                                )
                                .status(execStatus.name())
                                .editable(execStatus.isEditable())
                                .important(percentage.isImportant())
                                .build()
                );

                percentageRepository.save(percentage);
                continue;
            }

            String statusAtSnapshot = percentage.getStatus();
            log.info("[pct-status-debug] execute RECOMPUTE branch | carId={}, percentageId={}, serviceName={}, statusAtSnapshot={}, thread={}",
                    car.getCarId(), percentage.getId(), percentage.getServiceName(), statusAtSnapshot,
                    Thread.currentThread().getName());

            String serviceNameEn = service != null ? service.getNameEn() : percentage.getServiceNameEn();
            Optional<HyperPercentageSyncService.PartnerLineSnapshot> partnerLine =
                    hyperPercentageSyncService.findBestPartnerLineForService(
                            car,
                            serviceNameEn,
                            percentage.getIntervalKm(),
                            percentage.getIntervalMonth()
                    );

            if (partnerLine.isPresent()) {
                HyperPercentageSyncService.PartnerLineSnapshot snap = partnerLine.get();
                LocalDate lastServiceDate = snap.lastServiceDate() != null
                        ? snap.lastServiceDate()
                        : (car.getCreatedAt() != null ? car.getCreatedAt().toLocalDate() : today);
                Integer lastServiceKm = snap.lastServiceKm() != null ? snap.lastServiceKm() : 0;
                Integer nextServiceKm = snap.nextServiceKm();
                LocalDate nextServiceDate = snap.nextServiceDate();

                Integer kmPercentage = computeKmPercentage(lastServiceKm, nextServiceKm, car.getMileage());
                Integer monthPercentage = computeMonthPercentage(lastServiceDate, nextServiceDate);
                Integer remainingKm = computeRemainingKm(lastServiceKm, nextServiceKm, car.getMileage());

                responseList.add(
                        CarServicePercentageResponse.builder()
                                .percentageId(percentage.getId())
                                .serviceId(percentage.getServiceId())
                                .serviceName(percentage.getServiceName())
                                .serviceNameAz(percentage.getServiceNameAz())
                                .serviceNameEn(percentage.getServiceNameEn())
                                .serviceNameRu(percentage.getServiceNameRu())
                                .actionType(percentage.getActionType())
                                .intervalKm(percentage.getIntervalKm())
                                .intervalMonth(percentage.getIntervalMonth())
                                .kmPercentage(kmPercentage)
                                .monthPercentage(monthPercentage)
                                .monthPercentageDigit(monthPercentage)
                                .remainingKm(remainingKm)
                                .remainingMonths(nextServiceDate != null ? nextServiceDate.format(formatter) : null)
                                .lastServiceKm(lastServiceKm)
                                .lastServiceDate(lastServiceDate.format(formatter))
                                .nextServiceKm(nextServiceKm)
                                .nextServiceDate(nextServiceDate != null ? nextServiceDate.format(formatter) : null)
                                .status(PercentageStatus.EDITED_BY_PARTNER.name())
                                .editable(PercentageStatus.EDITED_BY_PARTNER.isEditable())
                                .important(percentage.isImportant())
                                .build()
                );

                percentage.setLastServiceDate(lastServiceDate);
                percentage.setLastServiceKm(lastServiceKm);
                percentage.setNextServiceDate(nextServiceDate);
                percentage.setNextServiceKm(nextServiceKm);
                percentage.setRemainingKm(remainingKm);
                if (nextServiceDate != null) {
                    percentage.setRemainingMonths(nextServiceDate);
                }
                percentage.setKmPercentage(kmPercentage);
                percentage.setMonthPercentage(monthPercentage);
                percentage.setStatus(PercentageStatus.EDITED_BY_PARTNER.name());
                percentage.setLastPartnerSyncAt(LocalDateTime.now());

                percentageRepository.save(percentage);
                recomputeSavedCount++;
                log.info("[pct-status-debug] execute SAVED from partner visit line | carId={}, percentageId={}, serviceName={}, thread={}",
                        car.getCarId(), percentage.getId(), percentage.getServiceName(), Thread.currentThread().getName());
                continue;
            }

            CustomerServiceRecord customerRecord =
                    customerServiceRecordRepository
                            .findByServiceIdAndCar(percentage.getServiceId(), car);

            // ServiceHistory Hyper kaynakli; serviceId yok, isimle eslesir
            ServiceHistory serviceHistory =
                    serviceHistoryRepository
                            .findTopByServiceNameAndCarOrderByDoneDateDesc(
                                    percentage.getServiceName(), car)
                            .orElse(null);

            /* ===== LAST SERVICE ===== */
            LocalDate lastServiceDate = car.getCreatedAt() != null ? car.getCreatedAt().toLocalDate() : today;
            Integer lastServiceKm = 0;

            if (customerRecord != null && serviceHistory != null) {
                LocalDate customerDone = customerRecord.getDoneDate() != null ? customerRecord.getDoneDate() : lastServiceDate;
                int customerKm = customerRecord.getDoneKm() != null ? customerRecord.getDoneKm() : 0;

                LocalDate historyDone = serviceHistory.getDoneDate() != null ? serviceHistory.getDoneDate() : lastServiceDate;
                int historyKm = serviceHistory.getDoneKm() != null ? serviceHistory.getDoneKm() : 0;

                if (customerDone.isAfter(historyDone)) {
                    lastServiceDate = customerDone;
                    lastServiceKm = customerKm;
                } else {
                    lastServiceDate = historyDone;
                    lastServiceKm = historyKm;
                }
            } else if (customerRecord != null) {
                lastServiceDate = customerRecord.getDoneDate() != null ? customerRecord.getDoneDate() : lastServiceDate;
                lastServiceKm = customerRecord.getDoneKm() != null ? customerRecord.getDoneKm() : 0;
            } else if (serviceHistory != null) {
                lastServiceDate = serviceHistory.getDoneDate() != null ? serviceHistory.getDoneDate() : lastServiceDate;
                lastServiceKm = serviceHistory.getDoneKm() != null ? serviceHistory.getDoneKm() : 0;
            }

            /* ===== TIME & KM ===== */
            long monthsPassed = (today.getYear() - lastServiceDate.getYear()) * 12L
                    + (today.getMonthValue() - lastServiceDate.getMonthValue());
            if (monthsPassed < 0) monthsPassed = 0;

            long kmPassed = Math.max(0, car.getMileage() - lastServiceKm);

            /* ===== USED % ===== */
            int intervalKm = Math.toIntExact(percentage.getIntervalKm() != null ? percentage.getIntervalKm() : 0L);
            int intervalMonth = percentage.getIntervalMonth() != null ? percentage.getIntervalMonth() : 0;

            double usedKmPercentage = intervalKm != 0 ? (double) kmPassed / intervalKm * 100 : 0;
            double usedMonthPercentage = intervalMonth != 0 ? (double) monthsPassed / intervalMonth * 100 : 0;

            usedKmPercentage = Math.min(100.0, usedKmPercentage);
            usedMonthPercentage = Math.min(100.0, usedMonthPercentage);

            /* ===== REMAINING % (INTEGER) ===== */
            int remainingKmPercentage = (int) Math.round(100.0 - usedKmPercentage);
            int remainingMonthPercentage = (int) Math.round(100.0 - usedMonthPercentage);

            /* ===== NEXT SERVICE ===== */
            Integer nextServiceKm = lastServiceKm + intervalKm;
            LocalDate nextServiceDate = lastServiceDate.plusMonths(intervalMonth);
            Integer remainingKm = (int) Math.max(0, intervalKm - kmPassed);

            /* ===== RESPONSE ===== */
            responseList.add(
                    CarServicePercentageResponse.builder()
                            .percentageId(percentage.getId())
                            .serviceId(percentage.getServiceId())
                            .serviceName(percentage.getServiceName())
                            .serviceNameAz(percentage.getServiceNameAz())
                            .serviceNameEn(percentage.getServiceNameEn())
                            .serviceNameRu(percentage.getServiceNameRu())
                            .actionType(percentage.getActionType())
                            .intervalKm((long) intervalKm)
                            .intervalMonth(intervalMonth)
                            .kmPercentage(remainingKmPercentage)
                            .monthPercentage(remainingMonthPercentage)
                            .monthPercentageDigit(remainingMonthPercentage)
                            .remainingKm(remainingKm)
                            .remainingMonths(nextServiceDate.format(formatter))
                            .lastServiceKm(lastServiceKm)
                            .lastServiceDate(lastServiceDate.format(formatter))
                            .nextServiceKm(nextServiceKm)
                            .nextServiceDate(nextServiceDate.format(formatter))
                            .status(PercentageStatus.CREATED.name())
                            .editable(true)
                            .important(percentage.isImportant())
                            .build()
            );

            /* ===== SAVE BACK TO PERCENTAGE ===== */
            Optional<Percentage> freshBeforeSave = percentageRepository.findById(percentage.getId());
            if (freshBeforeSave.isPresent()
                    && PercentageStatus.fromStored(freshBeforeSave.get().getStatus()).isManuallySet()) {
                log.warn("[pct-status-debug] execute SKIP template save — DB now has manual status | carId={}, percentageId={}, serviceName={}, dbStatus={}, statusAtSnapshot={}, thread={}",
                        car.getCarId(), percentage.getId(), percentage.getServiceName(),
                        freshBeforeSave.get().getStatus(), statusAtSnapshot, Thread.currentThread().getName());
                continue;
            }

            percentage.setLastServiceDate(lastServiceDate);
            percentage.setLastServiceKm(lastServiceKm);
            percentage.setNextServiceDate(nextServiceDate);
            percentage.setNextServiceKm(nextServiceKm);
            percentage.setRemainingKm(remainingKm);
            percentage.setRemainingMonths(nextServiceDate);
            percentage.setKmPercentage(remainingKmPercentage);
            percentage.setMonthPercentage(remainingMonthPercentage);
            percentage.setStatus(PercentageStatus.CREATED.name());

            percentageRepository.save(percentage);
            recomputeSavedCount++;
            log.info("[pct-status-debug] execute SAVED recompute as CREATED | carId={}, percentageId={}, serviceName={}, thread={}",
                    car.getCarId(), percentage.getId(), percentage.getServiceName(), Thread.currentThread().getName());
        }

        log.info("[pct-status-debug] executeServicePercentages END | carId={}, recomputeSaved={}, manualPreserved={}, thread={}",
                car.getCarId(), recomputeSavedCount, manualPreservedCount, Thread.currentThread().getName());

        redisCacheService.evictCarListAfterCommit(userIdHeader);
        return PercentageResponse.builder()
                .carId(car.getCarId())
                .vin(car.getVin())
                .responseList(responseList)
                .build();
    }


    /**
     * tr: Müşteri adına araç ekler: zorunlu alanları (vin, plaka, km, engineTypeId) doğrular, VIN daha önce
     *     müşterisiz kayıtlıysa aracı müşteriye bağlar, yoksa motor tipine uygun bakım şablonuyla yeni araç
     *     oluşturup her şablon servisi için Percentage ve CustomerServiceRecord kayıtları açar; commit
     *     sonrası asenkron yüzde+Hyper senkronizasyonunu tetikler. Eksik/geçersiz alanlarda
     *     MissingFieldException; müşteri yoksa UserNotFoundException; VIN başka müşteriye bağlıysa, plaka
     *     kullanımdaysa veya DB constraint ihlalinde AlreadyExistsException; renk/motor tipi/şablon
     *     bulunamazsa ResourceNotFoundException fırlatır. Tüm hatalar log tablosuna da yazılır.
     * en: Adds a car for the customer: validates required fields (vin, plate, mileage, engineTypeId), links
     *     the car to the customer when the VIN already exists without an owner, otherwise creates a new car
     *     with the maintenance template matching the engine type and creates Percentage and
     *     CustomerServiceRecord rows for each template service; triggers the async percentage+Hyper sync
     *     after commit. Throws MissingFieldException on missing/invalid fields; UserNotFoundException when
     *     the customer is missing; AlreadyExistsException when the VIN belongs to another customer, the
     *     plate is taken, or a DB constraint is violated; ResourceNotFoundException when the color, engine
     *     type, or template cannot be found. All failures are also persisted to the log table.
     */
    @Override
    @Transactional
    public CarResponse addCar(CarRequest carRequest, String phoneNumber, String userIdHeader,
                              String timezone, String acceptLanguage) {

        String logUserId = userIdHeader != null ? userIdHeader : "unknown";

        log.info("[addCar] START | phoneNumber={}, userIdHeader={}, timezone={}, acceptLanguage={}",
                phoneNumber, userIdHeader, timezone, acceptLanguage);
        log.info("[addCar] CarRequest | vin={}, plateNumber={}, brand={}, model={}, modelYear={}, colorId={}, " +
                        "engineType={}, engineTypeId={}, engineVolume={}, transmissionType={}, bodyType={}, " +
                        "mileage={}, carId={}, vinProvidedFields={}",
                carRequest != null ? carRequest.getVin() : null,
                carRequest != null ? carRequest.getPlateNumber() : null,
                carRequest != null ? carRequest.getBrand() : null,
                carRequest != null ? carRequest.getModel() : null,
                carRequest != null ? carRequest.getModelYear() : null,
                carRequest != null ? carRequest.getColorId() : null,
                carRequest != null ? carRequest.getEngineType() : null,
                carRequest != null ? carRequest.getEngineTypeId() : null,
                carRequest != null ? carRequest.getEngineVolume() : null,
                carRequest != null ? carRequest.getTransmissionType() : null,
                carRequest != null ? carRequest.getBodyType() : null,
                carRequest != null ? carRequest.getMileage() : null,
                carRequest != null ? carRequest.getCarId() : null,
                carRequest != null ? carRequest.getVinProvidedFields() : null);

        try {
            if (carRequest == null) {
                throwAddCarFailure(logUserId,
                        "carRequest is null",
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }
            if (phoneNumber == null || phoneNumber.isBlank()) {
                throwAddCarFailure(logUserId,
                        "phoneNumber is null or blank",
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }
            if (userIdHeader == null || userIdHeader.isBlank()) {
                throwAddCarFailure(logUserId,
                        "userIdHeader is null or blank",
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }
            if (carRequest.getEngineTypeId() == null) {
                throwAddCarFailure(logUserId,
                        "engineTypeId is null",
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }

            if (carRequest.getVin() == null) {
                throwAddCarFailure(logUserId,
                        "vin is null",
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }
            String vin = carRequest.getVin().trim();
            if (vin.isBlank()) {
                throwAddCarFailure(logUserId,
                        "vin is blank after trim",
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }

            if (carRequest.getPlateNumber() == null) {
                throwAddCarFailure(logUserId,
                        "plateNumber is null",
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }
            String plateNumber = carRequest.getPlateNumber().trim();
            if (plateNumber.isBlank()) {
                throwAddCarFailure(logUserId,
                        "plateNumber is blank after trim",
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }

            if (carRequest.getMileage() == null) {
                throwAddCarFailure(logUserId,
                        "mileage is null",
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }
            if (carRequest.getMileage() < 0) {
                throwAddCarFailure(logUserId,
                        "mileage is negative | mileage=" + carRequest.getMileage(),
                        new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            }

            Long userId = parseAddCarUserId(userIdHeader, logUserId, acceptLanguage);
            logUserId = userId.toString();

            log.info("[addCar] customerRepository.findByUserIdAndPhoneNumberAndStatus | userId={}, phoneNumber={}, status={}",
                    userId, phoneNumber, UserStatus.ACTIVE.name());
            Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(
                    userId, phoneNumber.trim(), UserStatus.ACTIVE.name());
            if (customer == null) {
                throwAddCarFailure(logUserId,
                        "customer not found | userId=" + userId + ", phoneNumber=" + phoneNumber,
                        new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage)));
            }
            log.info("[addCar] PASS customer found | customerUserId={}", customer.getUserId());

            if (carRequest.getColorId() != null) {
                Color color = colorRepository.findByColorId(carRequest.getColorId());
                if (color == null) {
                    throwAddCarFailure(logUserId,
                            "colorId not found | colorId=" + carRequest.getColorId(),
                            new ResourceNotFoundException(MessagesLangValues.COLOR_NOT_FOUND.getMessageByLang(acceptLanguage)));
                }
            }

            log.info("[addCar] carRepository.findByVin | vin={}", vin);
            Car existingCar = carRepository.findByVin(vin);
            log.info("[addCar] existing car lookup result | carId={}, hasCustomer={}, customerUserId={}",
                    existingCar != null ? existingCar.getCarId() : null,
                    existingCar != null && existingCar.getCustomer() != null,
                    existingCar != null && existingCar.getCustomer() != null ? existingCar.getCustomer().getUserId() : null);

            if (existingCar != null && existingCar.getCustomer() != null) {
                throwAddCarFailure(logUserId,
                        "car already linked to another customer | carId=" + existingCar.getCarId()
                                + ", ownerUserId=" + existingCar.getCustomer().getUserId()
                                + ", requestUserId=" + customer.getUserId()
                                + ", vin=" + vin,
                        new AlreadyExistsException(MessagesLangValues.CAR_ALREADY_EXISTS.getMessageByLang(acceptLanguage)));
            }

            if (existingCar != null) {
                log.info("[addCar] BRANCH existing car without customer | linking carId={} to customerUserId={}",
                        existingCar.getCarId(), customer.getUserId());
                existingCar.setCustomer(customer);
                if (customer.getCars() == null) {
                    customer.setCars(new ArrayList<>());
                }
                if (!customer.getCars().contains(existingCar)) {
                    customer.getCars().add(existingCar);
                }
                carRepository.save(existingCar);
                customerRepository.save(customer);
                CarResponse response = convertCarEntityToResponse(existingCar, acceptLanguage, "fromDb");
                redisCacheService.evictCarListAfterCommit(userIdHeader);
                log.info("[addCar] END success (existing car linked) | carId={}, vin={}", response.getCarId(), response.getVin());
                return response;
            }

            Optional<Car> plateOwner = carRepository.findByPlateNumberIgnoreCase(plateNumber);
            if (plateOwner.isPresent()) {
                Car conflictingCar = plateOwner.get();
                // Only block when plate is already owned; orphan cars (no customer) can be claimed.
                if (conflictingCar.getCustomer() != null) {
                    throwAddCarFailure(logUserId,
                            "plateNumber already exists | plateNumber=" + plateNumber
                                    + ", existingCarId=" + conflictingCar.getCarId()
                                    + ", existingVin=" + conflictingCar.getVin()
                                    + ", ownerUserId=" + conflictingCar.getCustomer().getUserId()
                                    + ", requestVin=" + vin,
                            new AlreadyExistsException(
                                    MessagesLangValues.PLATE_NUMBER_ALREADY_EXISTS.getMessageByLang(acceptLanguage)));
                }
                log.info("[addCar] BRANCH existing plate without customer | linking carId={} to customerUserId={}, plateNumber={}, existingVin={}, requestVin={}",
                        conflictingCar.getCarId(), customer.getUserId(), plateNumber, conflictingCar.getVin(), vin);
                conflictingCar.setCustomer(customer);
                if (customer.getCars() == null) {
                    customer.setCars(new ArrayList<>());
                }
                if (!customer.getCars().contains(conflictingCar)) {
                    customer.getCars().add(conflictingCar);
                }
                carRepository.save(conflictingCar);
                customerRepository.save(customer);
                CarResponse response = convertCarEntityToResponse(conflictingCar, acceptLanguage, "fromDb");
                redisCacheService.evictCarListAfterCommit(userIdHeader);
                log.info("[addCar] END success (existing plate car linked) | carId={}, vin={}, plateNumber={}",
                        response.getCarId(), response.getVin(), response.getPlateNumber());
                return response;
            }
            log.info("[addCar] PASS plateNumber uniqueness check | plateNumber={}", plateNumber);

            log.info("[addCar] BRANCH new car flow | engineTypeId={}", carRequest.getEngineTypeId());
            EngineType engineType = engineTypeRepository.findByEngineTypeId(carRequest.getEngineTypeId());
            if (engineType == null) {
                throwAddCarFailure(logUserId,
                        "engineType not found | engineTypeId=" + carRequest.getEngineTypeId(),
                        new ResourceNotFoundException(MessagesLangValues.ENGINE_TYPE_NOT_FOUND.getMessageByLang(acceptLanguage)));
            }
            log.info("[addCar] engine type lookup result | engineTypeId={}, engineType={}",
                    engineType.getEngineTypeId(), engineType.getEngineType());

            MaintenanceTemplate maintenanceTemplate = maintenanceTemplateRepository.findByEngineType(engineType)
                    .orElse(null);
            if (maintenanceTemplate == null) {
                throwAddCarFailure(logUserId,
                        "maintenance template not found | engineTypeId=" + engineType.getEngineTypeId()
                                + ", engineType=" + engineType.getEngineType(),
                        new ResourceNotFoundException(MessagesLangValues.TEMPLATE_NOT_FOUND.getMessageByLang(acceptLanguage)));
            }
            List<ServiceEntity> templateServices = maintenanceTemplate.getServices();
            if (templateServices == null || templateServices.isEmpty()) {
                throwAddCarFailure(logUserId,
                        "maintenance template has no services | templateId=" + maintenanceTemplate.getId()
                                + ", engineType=" + engineType.getEngineType(),
                        new ResourceNotFoundException(MessagesLangValues.TEMPLATE_NOT_FOUND.getMessageByLang(acceptLanguage)));
            }
            log.info("[addCar] PASS maintenance template found | templateId={}, templateName={}, serviceCount={}",
                    maintenanceTemplate.getId(), maintenanceTemplate.getName(), templateServices.size());

            Car newCar = Car.builder()
                    .vin(vin)
                    .plateNumber(plateNumber)
                    .brand(carRequest.getBrand())
                    .model(carRequest.getModel())
                    .modelYear(carRequest.getModelYear())
                    .engineType(engineType.getEngineType())
                    .engineTypeId(engineType.getEngineTypeId())
                    .engineVolume(carRequest.getEngineVolume())
                    .transmissionType(carRequest.getTransmissionType())
                    .bodyType(carRequest.getBodyType())
                    .mileage(carRequest.getMileage())
                    .colorId(carRequest.getColorId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .customer(customer)
                    .maintenanceTemplate(maintenanceTemplate)
                    .vinProvidedFields(carRequest.getVinProvidedFields())
                    .build();

            try {
                carRepository.save(newCar);
            } catch (DataIntegrityViolationException e) {
                throwAddCarFailure(logUserId,
                        "DB constraint violation on car save | vin=" + vin
                                + ", plateNumber=" + plateNumber
                                + ", cause=" + e.getMostSpecificCause().getMessage(),
                        new AlreadyExistsException(MessagesLangValues.CAR_ALREADY_EXISTS.getMessageByLang(acceptLanguage)));
            }
            log.info("[addCar] car saved | carId={}, vin={}", newCar.getCarId(), newCar.getVin());

            for (ServiceEntity serviceEntity : templateServices) {
                if (serviceEntity == null) {
                    throwAddCarFailure(logUserId,
                            "null service entity in maintenance template | templateId=" + maintenanceTemplate.getId(),
                            new ResourceNotFoundException(MessagesLangValues.TEMPLATE_NOT_FOUND.getMessageByLang(acceptLanguage)));
                }
                if (serviceEntity.getServiceName() == null || serviceEntity.getServiceName().isBlank()) {
                    throwAddCarFailure(logUserId,
                            "service entity has blank serviceName | templateId=" + maintenanceTemplate.getId()
                                    + ", serviceId=" + serviceEntity.getId(),
                            new ResourceNotFoundException(MessagesLangValues.TEMPLATE_NOT_FOUND.getMessageByLang(acceptLanguage)));
                }

                Percentage percentage = Percentage.builder()
                        .intervalKm(serviceEntity.getIntervalKm())
                        .intervalMonth(serviceEntity.getIntervalMonth())
                        .serviceName(serviceEntity.getServiceName())
                        .serviceNameAz(serviceEntity.getNameAz())
                        .serviceNameEn(serviceEntity.getNameEn())
                        .serviceNameRu(serviceEntity.getNameRu())
                        .actionType(serviceEntity.getActionType())
                        .serviceId(serviceEntity.getId())
                        .important(serviceEntity.isImportant())
                        .status(PercentageStatus.CREATED.name())
                        .carId(newCar.getCarId())
                        .build();
                percentageRepository.save(percentage);

                CustomerServiceRecord customerServiceRecord = CustomerServiceRecord.builder()
                        .serviceName(serviceEntity.getServiceName())
                        .serviceNameAz(serviceEntity.getNameAz())
                        .serviceNameEn(serviceEntity.getNameEn())
                        .serviceNameRu(serviceEntity.getNameRu())
                        .actionType(serviceEntity.getActionType())
                        .serviceId(serviceEntity.getId())
                        .car(newCar)
                        .build();
                customerServiceRecordRepository.save(customerServiceRecord);
            }
            log.info("[addCar] PASS all service records created | count={}", templateServices.size());

            if (customer.getCars() == null) {
                customer.setCars(new ArrayList<>());
            }
            customer.getCars().add(newCar);
            customerRepository.save(customer);

            CarResponse response = convertCarEntityToResponse(newCar, acceptLanguage, "fromDecoderTool");
            redisCacheService.evictCarListAfterCommit(userIdHeader);
            triggerAfterAddCarSync(newCar.getCarId(), newCar.getVin(), phoneNumber, userIdHeader, timezone, acceptLanguage);

            log.info("[addCar] END success (new car) | carId={}, vin={}, plateNumber={}",
                    response.getCarId(), response.getVin(), response.getPlateNumber());
            return response;
        } catch (RuntimeException ex) {
            if (!isKnownAddCarException(ex)) {
                logAddCarFailure(logUserId,
                        "unexpected runtime error | type=" + ex.getClass().getSimpleName()
                                + ", message=" + ex.getMessage()
                                + ", vin=" + (carRequest != null ? carRequest.getVin() : null));
            }
            throw ex;
        } catch (Exception ex) {
            logAddCarFailure(logUserId,
                    "unexpected error | type=" + ex.getClass().getSimpleName()
                            + ", message=" + ex.getMessage()
                            + ", vin=" + (carRequest != null ? carRequest.getVin() : null));
            throw ex;
        }
    }

    /**
     * tr: userIdHeader'ı Long'a çevirir; sayı değilse hatayı loglayıp MissingFieldException fırlatır.
     * en: Parses the userIdHeader into a Long; logs the failure and throws MissingFieldException when it is not numeric.
     */
    private Long parseAddCarUserId(String userIdHeader, String logUserId, String acceptLanguage) {
        try {
            return Long.valueOf(userIdHeader.trim());
        } catch (NumberFormatException e) {
            throwAddCarFailure(logUserId,
                    "userIdHeader is not a valid number | userIdHeader=" + userIdHeader,
                    new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage)));
            throw new IllegalStateException("unreachable");
        }
    }

    /**
     * tr: İstisnanın addCar akışında beklenen (bilinen) iş istisnalarından biri olup olmadığını döner.
     * en: Returns whether the exception is one of the expected (known) business exceptions in the addCar flow.
     */
    private boolean isKnownAddCarException(RuntimeException ex) {
        return ex instanceof MissingFieldException
                || ex instanceof UserNotFoundException
                || ex instanceof AlreadyExistsException
                || ex instanceof ResourceNotFoundException;
    }

    /**
     * tr: addCar hatasını hem uygulama loguna hem log tablosuna yazar ve verilen istisnayı fırlatır.
     * en: Writes the addCar failure to both the application log and the log table, then throws the given exception.
     */
    private void throwAddCarFailure(String userId, String detail, RuntimeException exception) {
        log.warn("[addCar] FAIL | userId={}, detail={}", userId, detail);
        logAddCarFailure(userId, detail);
        throw exception;
    }

    /**
     * tr: addCar hata detayını log tablosuna kaydeder; kayıt başarısız olursa istisnayı yutup sadece loglar.
     * en: Persists the addCar failure detail to the log table; swallows and only logs any persistence error.
     */
    private void logAddCarFailure(String userId, String detail) {
        try {
            logRepository.save(Log.builder()
                    .userId(userId != null ? userId : "unknown")
                    .log(LocalDateTime.now() + " | addCar | " + detail)
                    .build());
        } catch (Exception logEx) {
            log.error("[addCar] failed to persist failure log | userId={}, detail={}", userId, detail, logEx);
        }
    }

    /**
     * Schedules the async percentage + Hyper sync to run after the addCar transaction commits.
     *
     * tr: Asenkron yüzde + Hyper senkronizasyonunu addCar transaction'ı commit olduktan sonra çalışacak
     *     şekilde planlar; aktif transaction yoksa hemen başlatır.
     * en: Schedules the async percentage + Hyper sync to run after the addCar transaction commits;
     *     starts it immediately when no transaction synchronization is active.
     */
    private void triggerAfterAddCarSync(Long carId, String vin, String phoneNumber,
                                        String userIdHeader, String timezone, String acceptLanguage) {
        log.info("[pct-status-debug] addCar scheduling async sync after commit | carId={}, vin={}", carId, vin);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("[pct-status-debug] addCar afterCommit fired, starting async sync | carId={}, vin={}", carId, vin);
                    afterAddCarSyncService.syncAfterAddCar(carId, vin, phoneNumber, userIdHeader, timezone, acceptLanguage);
                }
            });
        } else {
            log.info("[pct-status-debug] addCar no active tx sync, starting async sync immediately | carId={}, vin={}", carId, vin);
            afterAddCarSyncService.syncAfterAddCar(carId, vin, phoneNumber, userIdHeader, timezone, acceptLanguage);
        }
    }

    /**
     * tr: Aracı müşteriden ayırır (kaydı silmez, customer bağını null yapar) ve başarı mesajı döner.
     *     Eksik alanlarda MissingFieldException; müşteri/araç doğrulamasında
     *     UserNotFoundException/ResourceNotFoundException fırlatır.
     * en: Detaches the car from the customer (does not delete the record, just nulls the customer link)
     *     and returns a success message. Throws MissingFieldException on missing fields, and
     *     UserNotFoundException/ResourceNotFoundException from the customer/car ownership checks.
     */
    @Override
    public CarResponse removeCar(CarRequest carRequest, String phoneNumber, String userIdHeader, String timezone,
                                 String acceptLanguage) {

        if (carRequest == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(carRequest.getCarId(), customer, acceptLanguage);
        customer.getCars().remove(car);
        car.setCustomer(null);
        carRepository.save(car);
        customerRepository.save(customer);
        redisCacheService.evictCarListAfterCommit(userIdHeader);

        return CarResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }


    /**
     * tr: VIN'e göre aracı bulur ve detaylarını döner. Eksik parametrede MissingFieldException, müşteri
     *     bulunamazsa UserNotFoundException, araç yoksa ResourceNotFoundException, araç başka müşteriye
     *     aitse NotMatchException fırlatır.
     * en: Finds the car by VIN and returns its details. Throws MissingFieldException on missing parameters,
     *     UserNotFoundException when the customer is not found, ResourceNotFoundException when the car does
     *     not exist, and NotMatchException when the car belongs to a different customer.
     */
    @Override
    public CarResponse getCarByVinCode(String vin, String phoneNumber, String userIdHeader,
                                       String timezone, String acceptLanguage) {

        if (vin == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }


        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader), phoneNumber, UserStatus.ACTIVE.name());

        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        Car existingCar = carRepository.findByVin(vin);

        if (existingCar == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        Customer carOwner = existingCar.getCustomer();
        if (carOwner != null && !carOwner.getUserId().equals(customer.getUserId())) {
            throw new NotMatchException(MessagesLangValues.CAR_NOT_MATCH_WITH_CUSTOMER.getMessageByLang(acceptLanguage));
        }

        return convertCarEntityToResponse(existingCar, acceptLanguage, "null");

    }

    /**
     * tr: Müşterinin araçlarını eklenme tarihine göre azalan sırada döner ve isteği log tablosuna yazar.
     *     Eksik parametrede MissingFieldException, müşteri bulunamazsa UserNotFoundException, araç listesi
     *     boşsa ResourceNotFoundException fırlatır.
     * en: Returns the customer's cars ordered by creation date descending and writes the request to the log
     *     table. Throws MissingFieldException on missing parameters, UserNotFoundException when the customer
     *     is not found, and ResourceNotFoundException when the car list is empty.
     */
    @Override
    public List<CarResponse> getCarListByUserId(String phoneNumber, String userIdHeader,
                                                String timezone, String acceptLanguage) {
        if (phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader), phoneNumber, UserStatus.ACTIVE.name());

        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        return redisCacheService.getOrLoadCarList(userIdHeader, acceptLanguage, () -> {
            List<Car> carList = carRepository.findAllByCustomerOrderByCreatedAtDesc(customer);

            if (carList == null || carList.isEmpty()) {
                throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
            }

            List<CarResponse> responses = carList.stream().map(car -> convertCarEntityToResponse(car, acceptLanguage, "null")).collect(Collectors.toList());
            Log log1 = new Log();
            List<Long> carIds = responses.stream().map(CarResponse::getCarId).toList();
            log1.setUserId(userIdHeader);
            log1.setLog(LocalDateTime.now() + " **** " + phoneNumber + " **** " + userIdHeader + " **** " + carIds);
            logRepository.save(log1);
            return responses;
        });
    }


    //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

//>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>


    /**
     * tr: Aracın kilometresini günceller: istek sahibi araç sahibi (customer) ise kendi aracında, admin ise
     *     VIN ile herhangi bir araçta güncelleme yapabilir. Eksik alanlarda MissingFieldException; istek
     *     sahibi ne müşteri ne admin ise InvalidStatusException; araç bulunamazsa
     *     ResourceNotFoundException fırlatır.
     * en: Updates the car's mileage: the caller may update their own car as the owner (customer), or any
     *     car by VIN as an admin. Throws MissingFieldException on missing fields, InvalidStatusException
     *     when the caller is neither a customer nor an admin, and ResourceNotFoundException when the car
     *     cannot be found.
     */
    @Override
    public CarResponse updateMileage(CarRequest carRequest, String phoneNumber, String userIdHeader, String timezone,
                                     String acceptLanguage) {
        if (carRequest == null || carRequest.getVin() == null || carRequest.getMileage() == null || phoneNumber == null
                || userIdHeader == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Long userId = Long.valueOf(userIdHeader);
        Car car;

        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(userId, phoneNumber,
                UserStatus.ACTIVE.name());

        if (customer != null) {
            log.info("Mileage update edən avtomobil sahibidir: {}", customer.getUserId());
            car = carRepository.findByVinAndCustomer(carRequest.getVin(), customer);
        } else {
            Admin admin = adminRepository.findByUserIdAndPhoneNumberAndStatus(userId, phoneNumber,
                    UserStatus.ACTIVE.name());

            if (admin == null) {
                log.warn("Mileage update eden ne avtomobil sahibi ne de admindir. Istek redd edilir.");
                throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
            }

            log.info("Mileage update eden admindir : {}", admin.getUserId());
            car = carRepository.findByVin(carRequest.getVin());
        }

        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        car.setMileage(carRequest.getMileage());
        car.setUpdatedAt(LocalDateTime.now(ZoneId.of(timezone)));

        carRepository.save(car);
        String ownerId = customer != null ? String.valueOf(customer.getUserId()) : redisCacheService.ownerUserId(car);
        redisCacheService.evictCarListAfterCommit(ownerId);

        return convertCarEntityToResponse(car, acceptLanguage, "null");
    }

    /**
     * tr: Araca yeni müşteri servis kaydı ekler; servis, serviceId ile ya da isim+actionType ile bulunur.
     *     Eksik alanlarda MissingFieldException; müşteri/araç doğrulamasında
     *     UserNotFoundException/ResourceNotFoundException; servis tanımı bulunamazsa veya aynı servis için
     *     kayıt zaten varsa ResourceNotFoundException fırlatır.
     * en: Adds a new customer service record to the car; the service is resolved by serviceId or by
     *     name+actionType. Throws MissingFieldException on missing fields;
     *     UserNotFoundException/ResourceNotFoundException from the customer/car checks; and
     *     ResourceNotFoundException when the service definition is missing or a record for the same
     *     service already exists.
     */
    @Override
    public RecordResponse addRecord(RecordRequest request, String phoneNumber, String userIdHeader,
                                    String timezone, String acceptLanguage) {

        if (request == null || request.getCarId() == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(request.getCarId(), customer, acceptLanguage);

        if (!customer.getCars().contains(car)) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        ServiceEntity serviceEntity = request.getServiceId() != null
                ? serviceEntityRepository.findById(request.getServiceId()).orElse(null)
                : serviceEntityRepository.findByServiceNameAndActionType(request.getServiceName(), request.getActionType());

        if (serviceEntity == null) {
            log.warn("[hist-debug] addRecord service not found | carId={} serviceId={} serviceName={} actionType={}",
                    car.getCarId(), request.getServiceId(), request.getServiceName(), request.getActionType());
            throw new ResourceNotFoundException(MessagesLangValues.SERVICE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        CustomerServiceRecord existingRecord = customerServiceRecordRepository.findByServiceIdAndCar(serviceEntity.getId(), car);
        if (existingRecord != null) {
            log.warn("[hist-debug] addRecord already exists | carId={} serviceId={} existingRecordId={} existingStatus={}",
                    car.getCarId(), serviceEntity.getId(), existingRecord.getId(), existingRecord.getServicedStatus());
            throw new ResourceNotFoundException(MessagesLangValues.RECORD_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        }
        log.info("[hist-debug] addRecord creating | carId={} serviceId={} doneDate={} doneKm={}",
                car.getCarId(), serviceEntity.getId(), request.getDoneDate(), request.getDoneKm());
        CustomerServiceRecord record = CustomerServiceRecord.builder()
                .serviceName(serviceEntity.getServiceName())
                .serviceNameAz(serviceEntity.getNameAz())
                .serviceNameEn(serviceEntity.getNameEn())
                .serviceNameRu(serviceEntity.getNameRu())
                .actionType(serviceEntity.getActionType())
                .serviceId(serviceEntity.getId())
                .doneDate(request.getDoneDate())
                .doneKm(request.getDoneKm())
                .car(car)
                .build();
        customerServiceRecordRepository.save(record);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        return RecordResponse.builder()
                .id(record.getId())
                .serviceId(record.getServiceId())
                .serviceName(record.getServiceName())
                .serviceNameAz(record.getServiceNameAz())
                .serviceNameEn(record.getServiceNameEn())
                .serviceNameRu(record.getServiceNameRu())
                .actionType(record.getActionType())
                .doneDate(record.getDoneDate())
                .doneKm(record.getDoneKm())
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    /**
     * tr: Mevcut servis kaydının doneDate/doneKm alanlarını (gönderilmişse) ve servicedStatus'unu günceller
     *     ve güncel kaydı döner. Eksik alanlarda MissingFieldException; müşteri/araç doğrulamasında
     *     UserNotFoundException/ResourceNotFoundException; kayıt bulunamazsa ResourceNotFoundException fırlatır.
     * en: Updates the doneDate/doneKm fields (when provided) and the servicedStatus of an existing service
     *     record and returns the updated record. Throws MissingFieldException on missing fields;
     *     UserNotFoundException/ResourceNotFoundException from the customer/car checks; and
     *     ResourceNotFoundException when the record cannot be found.
     */
    @Override
    public RecordResponse updateRecord(RecordRequest request, String phoneNumber, String userIdHeader,
                                       String timezone, String acceptLanguage) {
        log.info("[hist-debug] updateRecord start | carId={} recordId={} doneDate={} doneKm={} servicedStatus={}",
                request != null ? request.getCarId() : null,
                request != null ? request.getRecordId() : null,
                request != null ? request.getDoneDate() : null,
                request != null ? request.getDoneKm() : null,
                request != null ? request.getServicedStatus() : null);
        log.info("basladi  WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        log.info("Request :  {}", request);
        if (request == null || request.getCarId() == null || request.getRecordId() == null || phoneNumber == null
                || userIdHeader == null) {
            log.info("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
            log.info("Request body xeta verdi");
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(request.getCarId(), customer, acceptLanguage);

        CustomerServiceRecord record = customerServiceRecordRepository.findByIdAndCar(request.getRecordId(), car);
        log.info("Bazadan gelen record budur: {}", record);
        if (record == null) {
            log.info("[hist-debug] updateRecord not found | carId={} recordId={}", car.getCarId(), request.getRecordId());
            log.info("Record null oldu");
            throw new ResourceNotFoundException(MessagesLangValues.RECORD_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        log.info("[hist-debug] updateRecord found | carId={} recordId={} currentStatus={} currentDoneDate={} currentDoneKm={}",
                car.getCarId(), record.getId(), record.getServicedStatus(), record.getDoneDate(), record.getDoneKm());

        if (request.getDoneDate() != null) {
            record.setDoneDate(request.getDoneDate());
            log.info("Request done date null deyil ve set olundu");
        }

        if (request.getDoneKm() != null) {
            record.setDoneKm(request.getDoneKm());
            log.info("Request done km null deyil ve set olundu");

        }
        record.setServicedStatus(request.getServicedStatus());
        log.info("Request.getServicedStatus budur : {}", request.getServicedStatus());
        customerServiceRecordRepository.save(record);
        log.info("[hist-debug] updateRecord saved | carId={} recordId={} newStatus={} newDoneDate={} newDoneKm={}",
                car.getCarId(), record.getId(), record.getServicedStatus(), record.getDoneDate(), record.getDoneKm());
        log.info("Record yekun olaraq budur: -----------------> {}", record);
        log.info("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");

        redisCacheService.evictCarListAfterCommit(userIdHeader);
        return RecordResponse.builder()
                .id(record.getId())
                .serviceId(record.getServiceId())
                .serviceName(record.getServiceName())
                .serviceNameAz(record.getServiceNameAz())
                .serviceNameEn(record.getServiceNameEn())
                .serviceNameRu(record.getServiceNameRu())
                .actionType(record.getActionType())
                .doneDate(record.getDoneDate())
                .doneKm(record.getDoneKm())
                .servicedStatus(record.getServicedStatus())
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    /**
     * tr: Servis kaydını serviceId veya serviceName ile bulup döner. Eksik alanlarda MissingFieldException;
     *     müşteri/araç doğrulamasında UserNotFoundException/ResourceNotFoundException; kayıt bulunamazsa
     *     ResourceNotFoundException fırlatır.
     * en: Finds and returns the service record by serviceId or serviceName. Throws MissingFieldException on
     *     missing fields; UserNotFoundException/ResourceNotFoundException from the customer/car checks; and
     *     ResourceNotFoundException when the record cannot be found.
     */
    @Override
    public RecordResponse getRecord(RecordRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {
        if (request == null || request.getCarId() == null
                || (request.getServiceId() == null && request.getServiceName() == null)
                || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Customer customer = requireActiveCustomer(userIdHeader, phoneNumber, acceptLanguage);
        Car car = requireCustomerCar(request.getCarId(), customer, acceptLanguage);

        CustomerServiceRecord record = request.getServiceId() != null
                ? customerServiceRecordRepository.findByServiceIdAndCar(request.getServiceId(), car)
                : customerServiceRecordRepository.findByServiceNameAndCar(request.getServiceName(), car);

        if (record == null) {
            throw new ResourceNotFoundException(MessagesLangValues.RECORD_NOT_FOUND.getMessageByLang(acceptLanguage));
        }


        return RecordResponse.builder()
                .id(record.getId())
                .serviceId(record.getServiceId())
                .serviceName(record.getServiceName())
                .serviceNameAz(record.getServiceNameAz())
                .serviceNameRu(record.getServiceNameRu())
                .serviceNameEn(record.getServiceNameEn())
                .actionType(record.getActionType())
                .doneDate(record.getDoneDate())
                .doneKm(record.getDoneKm())
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }


    /**
     * tr: userIdHeader + phoneNumber ile AKTİF customer'ı bulur; yoksa UserNotFoundException fırlatır.
     *     (Eskiden her metodda tekrarlanan lookup bloğunun ortak hali.)
     * en: Finds the ACTIVE customer by userIdHeader + phoneNumber; throws UserNotFoundException otherwise.
     *     (Shared form of the lookup block previously repeated in every method.)
     */
    private Customer requireActiveCustomer(String userIdHeader, String phoneNumber, String acceptLanguage) {
        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(
                Long.valueOf(userIdHeader), phoneNumber, UserStatus.ACTIVE.name());
        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return customer;
    }

    /**
     * tr: carId'nin verilen customer'a ait olduğunu doğrulayıp aracı döner; değilse ResourceNotFoundException fırlatır.
     * en: Verifies the carId belongs to the given customer and returns the car; throws ResourceNotFoundException otherwise.
     */
    private Car requireCustomerCar(Long carId, Customer customer, String acceptLanguage) {
        Car car = carRepository.findByCarIdAndCustomer(carId, customer);
        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return car;
    }

    /**
     * tr: Kalan km'yi hesaplar: max(nextServiceKm - araç km'si, 0); girdilerden biri null ise null döner.
     * en: Computes remaining km: max(nextServiceKm - car mileage, 0); returns null when any input is null.
     */
    private Integer computeRemainingKm(Integer lastServiceKm, Integer nextServiceKm, Long carMileage) {
        if (lastServiceKm == null || nextServiceKm == null || carMileage == null) {
            return null;
        }
        return (int) Math.max(nextServiceKm - carMileage, 0);
    }

    /**
     * tr: Sonraki servise kalan gün sayısını string olarak döner (negatifse 0); tarihlerden biri null ise null döner.
     * en: Returns the remaining days to the next service as a string (clamped at 0); null when either date is null.
     */
    private String computeRemainingDaysValue(LocalDate lastServiceDate, LocalDate nextServiceDate) {
        if (lastServiceDate == null || nextServiceDate == null) {
            return null;
        }
        long remainingDays = Math.max(nextServiceDate.toEpochDay() - LocalDate.now().toEpochDay(), 0);
        return String.valueOf(remainingDays);
    }

    /**
     * tr: Tarihi formatlayıp az dili için ay adının baş harfini büyütür; tarih null ise null döner.
     * en: Formats the date and capitalizes the month name for the az locale; returns null for a null date.
     */
    private String formatWithCapitalizedMonth(LocalDate date, DateTimeFormatter formatter, Locale locale) {
        return date != null ? capitalizeMonth(date.format(formatter), locale) : null;
    }

    /**
     * tr: Car entity'sini, renk/motor/kasa tipi çevirileriyle birlikte CarResponse DTO'suna dönüştürür.
     * en: Converts a Car entity into a CarResponse DTO, including color/engine/body type translations.
     */
    private CarResponse convertCarEntityToResponse(Car car, String acceptLanguage, String resource) {
        Color color = colorRepository.findByColorId(car.getColorId());
        String colorResponse = color != null ? color.nameForLang(acceptLanguage) : "unknown";
        Customer customer = car.getCustomer();
        return CarResponse.builder()
                .carId(car.getCarId())
                .customerId(customer != null ? customer.getUserId() : 0L)
                .vin(car.getVin())
                .plateNumber(car.getPlateNumber())
                .brand(car.getBrand())
                .model(car.getModel())
                .modelYear(car.getModelYear())
                .color(colorResponse)
                .engineType(
                        EngineTypeTranslation.translate(
                                car.getEngineType(),
                                acceptLanguage
                        )
                )
                .engineVolume(car.getEngineVolume())
                .engineTypeId(car.getEngineTypeId())
                .transmissionType(car.getTransmissionType())
                .mileage(car.getMileage())
                .updatedAt(car.getUpdatedAt())
                .createdAt(car.getCreatedAt())  // added getCreatedAt into  response json from entity
                .bodyType(BodyTypeTranslation.translate(car.getBodyType(), acceptLanguage))
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .vinProvidedFields(car.getVinProvidedFields())
                .servicedPartnerIds(car.getServicedPartnerIds() != null
                        ? car.getServicedPartnerIds()
                        : Collections.emptyList())
                .allTimeCost(car.getAllTimeCost())
                .resource(resource)
                .build();
    }

    /**
     * tr: Motor hacmi metnini güvenli şekilde cc değerine çevirir; boş veya parse edilemeyen değerde null döner.
     * en: Safely converts an engine volume string to a cc value; returns null on blank or unparseable input.
     */
    private Integer convertEngineVolumeSafe(String val) {
        if (val == null || val.isBlank()) {
            return null;
        }

        try {
            return convertEngineVolume(val);
        } catch (Exception e) {
            log.warn("Engine volume parse edilemedi: {}", val);
            return null;
        }
    }

    private Integer convertEngineVolume(String value) {
        return VinService.parseEngineVolumeCc(value);
    }

    /**
     * tr: Sadece az dili için formatlanmış tarihteki ay adının baş harfini büyütür; diğer dillerde değiştirmez.
     * en: Capitalizes the month name in a formatted date only for the az locale; leaves other languages unchanged.
     */
    private String capitalizeMonth(String date, Locale locale) {
        if (!locale.getLanguage().equals("az")) {
            return date;
        }

        String[] parts = date.split(" ");
        if (parts.length < 3) return date;

        parts[1] = parts[1].substring(0, 1).toUpperCase(locale)
                + parts[1].substring(1);

        return String.join(" ", parts);
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Lower score = less remaining service life (km or time) = sort higher on the list.
     * Uses the minimum of km and month remaining percentages when both are present.
     *
     * tr: Kalan km yüzdesini hesaplar: (nextServiceKm - araç km'si) / (nextServiceKm - lastServiceKm) * 100,
     *     0-100 aralığına sıkıştırılır; girdilerden biri null ise null döner.
     * en: Computes the remaining km percentage: (nextServiceKm - car mileage) / (nextServiceKm - lastServiceKm) * 100,
     *     clamped to 0-100; returns null when any input is null.
     */
    private Integer computeKmPercentage(Integer lastServiceKm, Integer nextServiceKm, Long carMileage) {
        if (lastServiceKm == null || nextServiceKm == null || carMileage == null) {
            return null;
        }
        long totalKm = nextServiceKm - lastServiceKm;
        long remainingKmRaw = nextServiceKm - carMileage;
        if (totalKm > 0) {
            int kmPct = (int) Math.round((remainingKmRaw * 100.0) / totalKm);
            return Math.max(0, Math.min(100, kmPct));
        }
        return 0;
    }

    /**
     * tr: Kalan süre yüzdesini gün bazında hesaplar (kalan gün / toplam gün * 100, 0-100 aralığında);
     *     tarihlerden biri null ise null döner.
     * en: Computes the remaining time percentage on a day basis (remaining days / total days * 100,
     *     clamped to 0-100); returns null when either date is null.
     */
    private Integer computeMonthPercentage(LocalDate lastServiceDate, LocalDate nextServiceDate) {
        if (lastServiceDate == null || nextServiceDate == null) {
            return null;
        }
        long lastDay = lastServiceDate.toEpochDay();
        long nextDay = nextServiceDate.toEpochDay();
        long nowDay = LocalDate.now().toEpochDay();
        long totalDays = nextDay - lastDay;
        long remainingDays = Math.max(nextDay - nowDay, 0);
        if (totalDays > 0) {
            int monthPct = (int) Math.round((remainingDays * 100.0) / totalDays);
            return Math.max(0, Math.min(100, monthPct));
        }
        return 0;
    }

    /**
     * tr: Liste sıralaması için skor üretir: km ve süre yüzdelerinden küçük olanı döner (düşük skor = az
     *     kalan ömür = listede üstte); ikisi de null ise Integer.MAX_VALUE döner.
     * en: Produces the sorting score for the list: returns the smaller of the km and time percentages
     *     (lower score = less remaining life = higher on the list); returns Integer.MAX_VALUE when both are null.
     */
    private int remainingServiceScore(CarServicePercentageResponse item) {
        Integer kmRemaining = item.getKmPercentage();
        Integer monthRemaining = item.getMonthPercentageDigit() != null
                ? item.getMonthPercentageDigit()
                : item.getMonthPercentage();

        if (kmRemaining == null && monthRemaining == null) {
            return Integer.MAX_VALUE;
        }
        if (kmRemaining == null) {
            return monthRemaining;
        }
        if (monthRemaining == null) {
            return kmRemaining;
        }
        return Math.min(kmRemaining, monthRemaining);
    }

    /**
     * tr: Bu yüzde için tekrar bildirim gönderilip gönderilemeyeceğini kontrol eder (şu an test için
     *     5 dakikalık aralık; prod'da 7 gün olarak düşünülmüştür).
     * en: Checks whether a new notification may be sent for this percentage (currently a 5-minute interval
     *     for testing; intended to be 7 days in production).
     */
    private boolean canSendNotification(Percentage percentage) {

        LocalDateTime lastSent = percentage.getLastNotificationSentAt();

        if (lastSent == null) {
            return true;
        }

        // TEST için: 15 dakikada bir tekrar gönder
        return lastSent.plusMinutes(5).isBefore(LocalDateTime.now());

        // PROD için tekrar 7 güne döndür
        // return lastSent.plusDays(7).isBefore(LocalDateTime.now());
    }


    /**
     * tr: Yüzde kaydına göre bildirim başlığı ve gövdesini üretir: km ve süre yüzdelerinden düşük olana göre
     *     km ya da gün bazlı mesaj seçer; az ve en dillerini destekler. [başlık, gövde] dizisi döner.
     * en: Builds the notification title and body for a percentage record: picks a km- or day-based message
     *     depending on which of the km/time percentages is lower; supports az and en. Returns a [title, body] array.
     */
    private String[] buildMessage(Percentage percentage, String lang) {
        // ---------------- Service Name ----------------
//        String serviceNameTranslated = ServiceNameAz.translate(percentage.getServiceName(), lang);

        // ---------------- Threshold türünü seç (en düşük olan) ----------------
        Integer km = percentage.getKmPercentage();
        Integer month = percentage.getMonthPercentage();

        boolean kmLow = km != null && km > 0 && km <= 10;
        boolean monthLow = month != null && month > 0 && month <= 10;

        boolean isKmBased;

        if (kmLow && monthLow) {
            // ikisi de düşükse en küçük olanı baz al
            isKmBased = km <= month;
        } else if (kmLow) {
            isKmBased = true;
        } else if (monthLow) {
            isKmBased = false;
        } else {
            // hiçbiri düşük değilse default km
            isKmBased = true;
        }

        String title;
        String body;

        // ---------------- GÜN SAYISINI MANUEL HESAPLA ----------------
        long remainingDays = 0;
        if (!isKmBased) {
            LocalDate today = LocalDate.now();
            LocalDate nextServiceDate = percentage.getNextServiceDate();

            if (nextServiceDate != null) {
                remainingDays = nextServiceDate.toEpochDay() - today.toEpochDay();
                if (remainingDays < 0) remainingDays = 0; // negatif olmasın
            }
        }

        // ---------------- MESAJ OLUŞTUR ----------------
        if ("az".equalsIgnoreCase(lang)) {
            title = percentage.getServiceNameAz() + " vaxtı yaxınlaşır🛞";

            if (isKmBased) {
                body = "Avtomobilinizin " + percentage.getServiceNameAz()
                        + " üçün " + percentage.getRemainingKm()
                        + " km qalıb. Zəhmət olmasa baxımı planlayın😀";
            } else {
                body = "Avtomobilinizin " + percentage.getServiceNameAz()
                        + " üçün " + remainingDays
                        + " gün qalıb. Zəhmət olmasa baxımı planlayın😀";
            }

        } else { // default en
            title = percentage.getServiceName() + " reminder 🛞";

            if (isKmBased) {
                body = "Your car’s " + percentage.getServiceName()
                        + " is due in " + percentage.getRemainingKm()
                        + " km. Please schedule your maintenance 😀";
            } else {
                body = "Your car’s " + percentage.getServiceName()
                        + " is due in " + remainingDays
                        + " days. Please schedule your maintenance 😀";
            }
        }

        return new String[]{title, body};
    }


    /**
     * tr: Cihaza servis hatırlatma push'u gönderir; başarıda true, hata durumunda (istisna yutulur, loglanır) false döner.
     * en: Sends a service reminder push to the device; returns true on success, false on failure (the exception is swallowed and logged).
     */
    private boolean sendServiceReminder(String deviceToken, String title, String body) {
        try {
            pushNotificationService.send(title, body, deviceToken);
            log.info("PUSH SENT -> token={}, title={}, body={}", deviceToken, title, body);
            return true;
        } catch (RuntimeException e) {
            log.error("Push gönderilemedi -> token={}, title={}, body={}, hata={}", deviceToken, title, body, e.getMessage());
            return false;
        }
    }


}


