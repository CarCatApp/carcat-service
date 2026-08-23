package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.CarRequest;
import com.carland.carland_service.dto.request.PercentageRequest;
import com.carland.carland_service.dto.request.RecordRequest;
import com.carland.carland_service.dto.response.*;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.MaintenanceTemplateRepository;
import com.carland.carland_service.service.impl.HyperTokenService;
import com.carland.carland_service.service.CarService;
import com.carland.carland_service.service.CarVinHistoryService;
import com.carland.carland_service.dto.response.VisitHistoryResponse;
import com.carland.carland_service.service.VisitHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * tr: Araç REST controller'ı; araç ekleme/düzenleme/silme, VIN kontrolü ve VIN'e göre sorgu, kilometre güncelleme, bakım yüzdesi hesaplama/düzenleme, servis kayıtları ve VIN bazlı servis geçmişi uçlarını sunar.
 * en: REST controller for cars; exposes endpoints for adding/editing/removing cars, VIN check and lookup, mileage updates, maintenance percentage calculation/editing, service records, and VIN-based service history.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/car")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final CarVinHistoryService carVinHistoryService;
    private final VisitHistoryService visitHistoryService;
    private final MaintenanceTemplateRepository maintenanceTemplateRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final HyperTokenService hyperTokenService;
    private final RestTemplate restTemplate;
    /**
     * tr: Feature-flag scanner duman testi; catalog'a GET /api/v1/car/test/scanner eklenmeli.
     * en: Feature-flag scanner smoke test; catalog should gain GET /api/v1/car/test/scanner.
     */
    @GetMapping("/test/scanner")
    public Void testScannerCatalog() {
        return null;
    }

    /**
     * tr: Hyper API bağlantısını test eder; token alıp sabit bir VIN için araç sorgusu yapar ve ham cevabı string olarak döner, hata olursa hata mesajını döner.
     * en: Tests the Hyper API connection; obtains a token, queries a hardcoded VIN, and returns the raw response as a string, or the error message on failure.
     */
    @GetMapping("/test/hyper")
    public String testHyperApi(HttpServletRequest requestk) {

        try {
            String token = hyperTokenService.getToken();

            String url = "https://api.hyper.az/partner/v1/vehicles/by-vin/3FA6P0HDXKR168752";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            return "TESTTT     EHEHEHEHEHEHEHEHEHEHHEEHEEHHEHEHEHHEHAHAHAHAAHAHAHHAHAHAHAHAHAHAHAHHAHAHA"+response.getBody()+requestk.getRequestURI();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    /**
     * tr: Gövdedeki CarRequest ile kullanıcıya yeni araç ekler; phoneNumber ve X-User-Id header'larından kullanıcı belirlenir ve eklenen araç döner.
     * en: Adds a new car for the user from the CarRequest body; the user is resolved from the phoneNumber and X-User-Id headers, returns the added car.
     */
    @PostMapping("/add")
    public CarResponse addCar(@RequestBody CarRequest carRequest,
                              @RequestHeader("Authorization") String token,
                              @RequestHeader("phoneNumber") String phoneNumber,
                              @RequestHeader("X-User-Id") String userIdHeader,
                              @RequestHeader("X-Client-Timezone") String timezone,
                              @RequestHeader("Accept-Language") String acceptLanguage) {
        return carService.addCar(carRequest, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Gövdedeki CarRequest ile mevcut aracın detaylarını günceller ve güncellenmiş aracı döner.
     * en: Updates the details of an existing car from the CarRequest body and returns the updated car.
     */
    @PutMapping("/edit/details")
    public CarResponse editCarDetails(@RequestBody CarRequest carRequest,
                                      @RequestHeader("Authorization") String token,
                                      @RequestHeader("phoneNumber") String phoneNumber,
                                      @RequestHeader("X-User-Id") String userIdHeader,
                                      @RequestHeader("X-Client-Timezone") String timezone,
                                      @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/edit/details cagrildi");
        return carService.editCarDetails(carRequest, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Verilen VIN kodunu kontrol eder ve VIN'den elde edilebilen araç bilgilerini (marka, model, yıl vb.) döner.
     * en: Checks the given VIN code and returns whatever car information (brand, model, year etc.) can be resolved from it.
     */
    @GetMapping("/check/vin")
    public CarResponse checkVin(@RequestParam String vin,
                                @RequestHeader("Accept-Language") String acceptLanguage) {


//        return CarResponse.builder()
//                .engineTypeId(null)
//                .vin(vin)
//                .bodyType(null)
//                .mileage(null)
//                .plateNumber(null)
//                .model(null)
//                .engineType(null)
//                .color(null)
//                .modelYear(null)
//                .vinProvidedFields(Collections.emptyList())
//                .message("vin min yoxdu :)")
//                .build();
        return carService.checkVin(vin, acceptLanguage);
    }

    /**
     * tr: Gövdedeki CarRequest ile belirtilen aracı kullanıcıdan kaldırır ve sonucu döner.
     * en: Removes the car specified in the CarRequest body from the user and returns the result.
     */
    @PutMapping("/remove")
    public CarResponse removeCar(@RequestBody CarRequest carRequest,
                                 @RequestHeader("Authorization") String token,
                                 @RequestHeader("phoneNumber") String phoneNumber,
                                 @RequestHeader("X-User-Id") String userIdHeader,
                                 @RequestHeader("X-Client-Timezone") String timezone,
                                 @RequestHeader("Accept-Language") String acceptLanguage) {
        return carService.removeCar(carRequest, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Gövdedeki CarRequest ile aracın kilometre (mileage) bilgisini günceller ve güncellenmiş aracı döner.
     * en: Updates the car's mileage from the CarRequest body and returns the updated car.
     */
    @PutMapping("/update/mileage")
    public CarResponse updateMileage(@RequestBody CarRequest carRequest,
                                     @RequestHeader("Authorization") String token,
                                     @RequestHeader("phoneNumber") String phoneNumber,
                                     @RequestHeader("X-User-Id") String userIdHeader,
                                     @RequestHeader("X-Client-Timezone") String timezone,
                                     @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/update/mileage cagrildi");
        return carService.updateMileage(carRequest, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


    /**
     * tr: Verilen VIN koduna göre çağıran kullanıcının aracını getirir.
     * en: Fetches the calling user's car by the given VIN code.
     */
    @GetMapping("/get/by/vin")
    public CarResponse getCarByVinCode(@RequestParam String vin,
                                       @RequestHeader("Authorization") String token,
                                       @RequestHeader("phoneNumber") String phoneNumber,
                                       @RequestHeader("X-User-Id") String userIdHeader,
                                       @RequestHeader("X-Client-Timezone") String timezone,
                                       @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/get/by/vin cagrildi");
        return carService.getCarByVinCode(vin, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: phoneNumber ve X-User-Id header'larından belirlenen kullanıcının araç listesini döner.
     * en: Returns the list of cars for the user resolved from the phoneNumber and X-User-Id headers.
     */
    @GetMapping("/get/list/by/user")
    public List<CarResponse> getCarListByUserId(@RequestHeader("Authorization") String token,
                                                @RequestHeader("phoneNumber") String phoneNumber,
                                                @RequestHeader("X-User-Id") String userIdHeader,
                                                @RequestHeader("X-Client-Timezone") String timezone,
                                                @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/get/list/by/user cagrildi");
        return carService.getCarListByUserId(phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


    /**
     * tr: Verilen carId için bakım kalemi yüzdelerini (aşınma durumlarını) hesaplayıp günceller ve sonuç listesini döner.
     * en: Calculates and updates the maintenance item percentages (wear statuses) for the given carId and returns the resulting list.
     */
    @PutMapping("/service/execute/percentages")
    public PercentageResponse executeServicePercentages(@RequestHeader("Authorization") String token,
                                                            @RequestParam Long carId,
                                                            @RequestHeader("phoneNumber") String phoneNumber,
                                                            @RequestHeader("X-User-Id") String userIdHeader,
                                                            @RequestHeader("X-Client-Timezone") String timezone,
                                                            @RequestHeader("Accept-Language") String acceptLanguage) {

        System.err.println("[pct-status-debug] HTTP PUT /service/execute/percentages | carId=" + carId
                + ", userId=" + userIdHeader + ", thread=" + Thread.currentThread().getName());
        return carService.executeServicePercentages(carId, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


    /**
     * tr: Verilen carId için mevcut bakım kalemi yüzdelerinin listesini döner (yeniden hesaplama yapmaz).
     * en: Returns the current list of maintenance item percentages for the given carId (does not recalculate).
     */
    @GetMapping("/service/percentages")
    public PercentageResponse getServicePercentageList(@RequestHeader("Authorization") String token,
                                                           @RequestParam Long carId,
                                                           @RequestHeader("phoneNumber") String phoneNumber,
                                                           @RequestHeader("X-User-Id") String userIdHeader,
                                                           @RequestHeader("X-Client-Timezone") String timezone,
                                                           @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/service/percentages cagrildi");
        log.info("[hist-debug] GET /service/percentages | carId={} userId={}", carId, userIdHeader);
        return carService.getServicePercentageList(carId, phoneNumber, userIdHeader, timezone, acceptLanguage);

    }

    /**
     * tr: Gövdedeki PercentageRequest (carId + percentageId) ile tek bir bakım kalemi yüzdesini günceller ve güncellenmiş kaydı döner.
     * en: Updates a single maintenance item percentage using the PercentageRequest body (carId + percentageId) and returns the updated record.
     */
    @PutMapping("/service/edit/percentage")
    public CarServicePercentageResponse editPercentage(@RequestHeader("Authorization") String token,
                                                       @RequestBody PercentageRequest request,
                                                       @RequestHeader("phoneNumber") String phoneNumber,
                                                       @RequestHeader("X-User-Id") String userIdHeader,
                                                       @RequestHeader("X-Client-Timezone") String timezone,
                                                       @RequestHeader("Accept-Language") String acceptLanguage) {

        System.err.println("[pct-status-debug] HTTP PUT /service/edit/percentage | carId=" + request.getCarId()
                + ", percentageId=" + request.getPercentageId() + ", userId=" + userIdHeader
                + ", thread=" + Thread.currentThread().getName());
        return carService.editPercentage(request, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


    /**
     * tr: Gövdedeki RecordRequest ile araca yeni bir servis kaydı ekler ve eklenen kaydı döner.
     * en: Adds a new service record to a car from the RecordRequest body and returns the added record.
     */
    @PostMapping("/add/record")
    public RecordResponse addRecord(@RequestHeader("Authorization") String token,
                                    @RequestBody RecordRequest request,
                                    @RequestHeader("phoneNumber") String phoneNumber,
                                    @RequestHeader("X-User-Id") String userIdHeader,
                                    @RequestHeader("X-Client-Timezone") String timezone,
                                    @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/add/record cagrildi");
        log.info("[hist-debug] POST /add/record | carId={} recordId={} serviceId={} serviceName={} actionType={} doneDate={} doneKm={} servicedStatus={} userId={}",
                request != null ? request.getCarId() : null,
                request != null ? request.getRecordId() : null,
                request != null ? request.getServiceId() : null,
                request != null ? request.getServiceName() : null,
                request != null ? request.getActionType() : null,
                request != null ? request.getDoneDate() : null,
                request != null ? request.getDoneKm() : null,
                request != null ? request.getServicedStatus() : null,
                userIdHeader);
        return carService.addRecord(request, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Gövdedeki RecordRequest ile mevcut bir servis kaydını günceller ve güncellenmiş kaydı döner.
     * en: Updates an existing service record from the RecordRequest body and returns the updated record.
     */
    @PutMapping("/update/record")
    public RecordResponse updateRecord(@RequestHeader("Authorization") String token,
                                       @RequestBody RecordRequest request,
                                       @RequestHeader("phoneNumber") String phoneNumber,
                                       @RequestHeader("X-User-Id") String userIdHeader,
                                       @RequestHeader("X-Client-Timezone") String timezone,
                                       @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/update/record cagrildi");
        log.info("[hist-debug] PUT /update/record | carId={} recordId={} serviceId={} serviceName={} actionType={} doneDate={} doneKm={} servicedStatus={} userId={}",
                request != null ? request.getCarId() : null,
                request != null ? request.getRecordId() : null,
                request != null ? request.getServiceId() : null,
                request != null ? request.getServiceName() : null,
                request != null ? request.getActionType() : null,
                request != null ? request.getDoneDate() : null,
                request != null ? request.getDoneKm() : null,
                request != null ? request.getServicedStatus() : null,
                userIdHeader);
        return carService.updateRecord(request, phoneNumber, userIdHeader, timezone, acceptLanguage);

    }

    /**
     * tr: Gövdedeki RecordRequest kriterlerine göre tek bir servis kaydını getirir.
     * en: Fetches a single service record based on the RecordRequest body criteria.
     */
    @PutMapping("/get/record")
    public RecordResponse getRecord(@RequestHeader("Authorization") String token,
                                    @RequestBody RecordRequest request,
                                    @RequestHeader("phoneNumber") String phoneNumber,
                                    @RequestHeader("X-User-Id") String userIdHeader,
                                    @RequestHeader("X-Client-Timezone") String timezone,
                                    @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/get/record cagrildi");
        log.info("[hist-debug] PUT /get/record | carId={} recordId={} serviceId={} serviceName={} userId={}",
                request != null ? request.getCarId() : null,
                request != null ? request.getRecordId() : null,
                request != null ? request.getServiceId() : null,
                request != null ? request.getServiceName() : null,
                userIdHeader);
        return carService.getRecord(request, phoneNumber, userIdHeader, timezone, acceptLanguage);

    }

    /**
     * tr: Accept-Language header'ına göre yerelleştirilmiş renk listesini döner.
     * en: Returns the list of colors localized according to the Accept-Language header.
     */
    @GetMapping("/get/color/list")
    public List<Color> getColors(@RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/get/color/list cagrildi");
        return carService.getColors(acceptLanguage);
    }

    /**
     * tr: Verilen carId'ye ait tüm servis kayıtlarının listesini döner.
     * en: Returns the list of all service records belonging to the given carId.
     */
    @GetMapping("/get/service/records")
    public List<RecordResponse> getServiceRecords(@RequestParam Long carId,
                                                  @RequestHeader("phoneNumber") String phoneNumber,
                                                  @RequestHeader("X-User-Id") String userIdHeader,
                                                  @RequestHeader("X-Client-Timezone") String timezone,
                                                  @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/get/service/records cagrildi");
        log.info("[hist-debug] GET /get/service/records | carId={} userId={}", carId, userIdHeader);
        return carService.getServiceRecords(carId, phoneNumber, userIdHeader, timezone, acceptLanguage);

    }

    /**
     * tr: Path'teki VIN koduna göre aracın servis geçmişini (v1 format, service_histories) döner.
     * en: Returns the car's service history (v1 format, service_histories) for the VIN code in the path.
     */
    @Operation(
            summary = "Service history v1 (legacy flat rows)",
            description = """
                    Legacy Hyper flatten into `service_histories`. Percentages still read this table.
                    Visit-based model is v2 (`GET .../service-history/v2`, `visits` table).
                    """
    )
    @GetMapping("/{vin}/service-history")
    public CarVinServiceHistoryResponse getServiceHistoryByVin(@PathVariable String vin,
                                                                @RequestHeader("phoneNumber") String phoneNumber,
                                                                @RequestHeader("X-User-Id") String userIdHeader,
                                                                @RequestHeader("Accept-Language") String acceptLanguage) {
        log.info("[hist-debug] GET /{}/service-history (v1) | userId={} lang={}", vin, userIdHeader, acceptLanguage);
        return carVinHistoryService.getServiceHistoryByVin(vin, phoneNumber, userIdHeader, acceptLanguage);
    }

    /**
     * tr: Path'teki VIN koduna göre aracın ziyaret bazlı servis geçmişini (v2 format) döner.
     * en: Returns the car's visit-based service history (v2 format) for the VIN code in the path.
     */
    @Operation(
            summary = "Service history (visit-based)",
            description = """
                    Current model: `visits` + lines/parts. Used by add-car Hyper sync and partner webhooks.
                    Hyper `GET /partner/v1/vehicles/by-vin/{vin}` is cached into `visits` when empty.
                    """
    )
    @GetMapping("/{vin}/service-history/v2")
    public VisitHistoryResponse getServiceHistoryByVinV2(@PathVariable String vin,
                                                                   @RequestHeader("phoneNumber") String phoneNumber,
                                                                   @RequestHeader("X-User-Id") String userIdHeader,
                                                                   @RequestHeader("Accept-Language") String acceptLanguage) {
        log.info("[hist-debug] GET /{}/service-history/v2 | userId={} lang={}", vin, userIdHeader, acceptLanguage);
        return visitHistoryService.getServiceHistoryByVin(vin, phoneNumber, userIdHeader, acceptLanguage);
    }


    /**
     * tr: Verilen VIN'e sahip aracın müşteri bağlantısını koparır (test/simülasyon amaçlı); araç bulunamazsa ResourceNotFoundException fırlatır.
     * en: Detaches the customer link from the car with the given VIN (for test/simulation purposes); throws ResourceNotFoundException if the car is not found.
     */
    @PostMapping("/remove/simulated/customer")
    @Transactional
    public void removeSimulatedCustomer(@RequestParam String vin) {

        Car car = carRepository.findByVin(vin);

        if (car == null) {
            throw new ResourceNotFoundException("masin yoxdu");
        }

        Customer customer = car.getCustomer();

        if (customer != null) {
            customer.getCars().remove(car);
            car.setCustomer(null);
        }

        carRepository.save(car);
    }



}


