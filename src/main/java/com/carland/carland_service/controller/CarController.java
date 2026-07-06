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
import com.carland.carland_service.service.interfaces.CarService;
import com.carland.carland_service.dto.response.v2.CarVinServiceHistoryV2Response;
import com.carland.carland_service.service.interfaces.CarVinHistoryService;
import com.carland.carland_service.dto.response.v2.CarVinHistoryServiceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/car")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final CarVinHistoryService carVinHistoryService;
    private final CarVinHistoryServiceV2 carVinHistoryServiceV2;
    private final MaintenanceTemplateRepository maintenanceTemplateRepository;
    private final CarRepository carRepository;
    private final CustomerRepository customerRepository;
    private final HyperTokenService hyperTokenService;
    private final RestTemplate restTemplate;

    @PostMapping("/add")
    public CarResponse addCar(@RequestBody CarRequest carRequest,
                              @RequestHeader("Authorization") String token,
                              @RequestHeader("phoneNumber") String phoneNumber,
                              @RequestHeader("X-User-Id") String userIdHeader,
                              @RequestHeader("X-Client-Timezone") String timezone,
                              @RequestHeader("Accept-Language") String acceptLanguage) {
        return carService.addCar(carRequest, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

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

    @GetMapping("/check/vin")
    public CarResponse checkVin(@RequestParam String vin,
                                @RequestHeader("Accept-Language") String acceptLanguage) {


        return CarResponse.builder()
                .engineTypeId(null)
                .bodyType(null)
                .mileage(null)
                .plateNumber(null)
                .model(null)
                .engineType(null)
                .color(null)
                .modelYear(null)
                .vinProvidedFields(Collections.emptyList())
                .message("vin min yoxdu :)")
                .build();
//        return carService.checkVin(vin, acceptLanguage);
    }

    @PutMapping("/remove")
    public CarResponse removeCar(@RequestBody CarRequest carRequest,
                                 @RequestHeader("Authorization") String token,
                                 @RequestHeader("phoneNumber") String phoneNumber,
                                 @RequestHeader("X-User-Id") String userIdHeader,
                                 @RequestHeader("X-Client-Timezone") String timezone,
                                 @RequestHeader("Accept-Language") String acceptLanguage) {
        return carService.removeCar(carRequest, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

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

    @GetMapping("/get/list/by/user")
    public List<CarResponse> getCarListByUserId(@RequestHeader("Authorization") String token,
                                                @RequestHeader("phoneNumber") String phoneNumber,
                                                @RequestHeader("X-User-Id") String userIdHeader,
                                                @RequestHeader("X-Client-Timezone") String timezone,
                                                @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/get/list/by/user cagrildi");
        return carService.getCarListByUserId(phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


    @PutMapping("/service/execute/percentages")
    public PercentageResponseMain executeServicePercentages(@RequestHeader("Authorization") String token,
                                                            @RequestParam Long carId,
                                                            @RequestHeader("phoneNumber") String phoneNumber,
                                                            @RequestHeader("X-User-Id") String userIdHeader,
                                                            @RequestHeader("X-Client-Timezone") String timezone,
                                                            @RequestHeader("Accept-Language") String acceptLanguage) {

        System.err.println("[pct-status-debug] HTTP PUT /service/execute/percentages | carId=" + carId
                + ", userId=" + userIdHeader + ", thread=" + Thread.currentThread().getName());
        return carService.executeServicePercentages(carId, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


    @GetMapping("/service/percentages")
    public PercentageResponseMain getServicePercentageList(@RequestHeader("Authorization") String token,
                                                           @RequestParam Long carId,
                                                           @RequestHeader("phoneNumber") String phoneNumber,
                                                           @RequestHeader("X-User-Id") String userIdHeader,
                                                           @RequestHeader("X-Client-Timezone") String timezone,
                                                           @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/service/percentages cagrildi");
        return carService.getServicePercentageList(carId, phoneNumber, userIdHeader, timezone, acceptLanguage);

    }

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


    @PostMapping("/add/record")
    public RecordResponse addRecord(@RequestHeader("Authorization") String token,
                                    @RequestBody RecordRequest request,
                                    @RequestHeader("phoneNumber") String phoneNumber,
                                    @RequestHeader("X-User-Id") String userIdHeader,
                                    @RequestHeader("X-Client-Timezone") String timezone,
                                    @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/add/record cagrildi");
        return carService.addRecord(request, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    @PutMapping("/update/record")
    public RecordResponse updateRecord(@RequestHeader("Authorization") String token,
                                       @RequestBody RecordRequest request,
                                       @RequestHeader("phoneNumber") String phoneNumber,
                                       @RequestHeader("X-User-Id") String userIdHeader,
                                       @RequestHeader("X-Client-Timezone") String timezone,
                                       @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/update/record cagrildi");
        return carService.updateRecord(request, phoneNumber, userIdHeader, timezone, acceptLanguage);

    }

    @PutMapping("/get/record")
    public RecordResponse getRecord(@RequestHeader("Authorization") String token,
                                    @RequestBody RecordRequest request,
                                    @RequestHeader("phoneNumber") String phoneNumber,
                                    @RequestHeader("X-User-Id") String userIdHeader,
                                    @RequestHeader("X-Client-Timezone") String timezone,
                                    @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/get/record cagrildi");
        return carService.getRecord(request, phoneNumber, userIdHeader, timezone, acceptLanguage);

    }

    @GetMapping("/get/color/list")
    public List<Color> getColors(@RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/get/color/list cagrildi");
        return carService.getColors(acceptLanguage);
    }

    @GetMapping("/get/service/records")
    public List<RecordResponse> getServiceRecords(@RequestParam Long carId,
                                                  @RequestHeader("phoneNumber") String phoneNumber,
                                                  @RequestHeader("X-User-Id") String userIdHeader,
                                                  @RequestHeader("X-Client-Timezone") String timezone,
                                                  @RequestHeader("Accept-Language") String acceptLanguage) {
        System.err.println("/get/service/records cagrildi");
        return carService.getServiceRecords(carId, phoneNumber, userIdHeader, timezone, acceptLanguage);

    }

    @GetMapping("/{vin}/service-history")
    public CarVinServiceHistoryResponse getServiceHistoryByVin(@PathVariable String vin,
                                                                @RequestHeader("phoneNumber") String phoneNumber,
                                                                @RequestHeader("X-User-Id") String userIdHeader,
                                                                @RequestHeader("Accept-Language") String acceptLanguage) {
        return carVinHistoryService.getServiceHistoryByVin(vin, phoneNumber, userIdHeader, acceptLanguage);
    }

    @GetMapping("/{vin}/service-history/v2")
    public CarVinServiceHistoryV2Response getServiceHistoryByVinV2(@PathVariable String vin,
                                                                   @RequestHeader("phoneNumber") String phoneNumber,
                                                                   @RequestHeader("X-User-Id") String userIdHeader,
                                                                   @RequestHeader("Accept-Language") String acceptLanguage) {
        return carVinHistoryServiceV2.getServiceHistoryByVin(vin, phoneNumber, userIdHeader, acceptLanguage);
    }


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


    @GetMapping("/test/hyper")
    public String testHyperApi() {

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

            return "TESTTT     EHEHEHEHEHEHEHEHEHEHHEEHEEHHEHEHEHHEHAHAHAHAAHAHAHHAHAHAHAHAHAHAHAHHAHAHA"+response.getBody();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}


