package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.CarRequest;
import com.carland.carland_service.dto.request.PercentageRequest;
import com.carland.carland_service.dto.request.RecordRequest;
import com.carland.carland_service.dto.response.*;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.entity.CustomerServiceRecord;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * tr: Araç yönetiminin ana sözleşmesidir: araç ekleme/silme/güncelleme, VIN sorgulama, kilometre güncelleme,
 *     servis kayıtları ve bakım yüzdesi (percentage) hesaplama/düzenleme işlemlerini tanımlar.
 * en: Main contract for car management: adding/removing/updating cars, VIN lookup, mileage updates,
 *     service records, and maintenance percentage calculation/editing.
 */
public interface CarService {
    /**
     * tr: Müşteri adına yeni araç ekler ve eklenen aracı döner.
     * en: Adds a new car for the customer and returns the added car.
     */
    CarResponse addCar(CarRequest carRequest, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) ;

    /**
     * tr: VIN koduna göre aracı bulup detaylarını döner.
     * en: Finds a car by its VIN code and returns its details.
     */
    CarResponse getCarByVinCode(String  vin, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Müşteriye ait tüm araçların listesini döner.
     * en: Returns the list of all cars belonging to the customer.
     */
    List<CarResponse> getCarListByUserId( String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


   /**
    * tr: Aracın bakım şablonuna göre servis yüzdelerini hesaplayıp kaydeder ve sonucu döner.
    * en: Calculates and persists the car's service percentages based on its maintenance template and returns the result.
    */
   PercentageResponse executeServicePercentages(Long carId, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Aracın kilometresini günceller ve güncel aracı döner.
     * en: Updates the car's mileage and returns the updated car.
     */
    CarResponse updateMileage(CarRequest carRequest, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Araca yeni bir servis kaydı (record) ekler ve eklenen kaydı döner.
     * en: Adds a new service record to a car and returns the added record.
     */
    RecordResponse addRecord( RecordRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Var olan servis kaydını günceller ve güncel kaydı döner.
     * en: Updates an existing service record and returns the updated record.
     */
    RecordResponse updateRecord(RecordRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

    /**
     * tr: İstenen servis kaydının detaylarını döner.
     * en: Returns the details of the requested service record.
     */
    RecordResponse getRecord(RecordRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Aracı müşteriden kaldırır (siler) ve sonucu döner.
     * en: Removes (deletes) the car from the customer and returns the result.
     */
    CarResponse removeCar(CarRequest carRequest, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

    /**
     * tr: VIN kodunun sistemde kayıtlı olup olmadığını kontrol eder.
     * en: Checks whether the VIN code is already registered in the system.
     */
    CarResponse checkVin(String vin ,  String acceptLanguage);

    /**
     * tr: Tanımlı araç renklerinin listesini döner.
     * en: Returns the list of defined car colors.
     */
    List<Color> getColors(String  acceptLanguage);

    /**
     * tr: Araca ait servis kayıtlarının listesini döner.
     * en: Returns the list of service records for a car.
     */
    List<RecordResponse> getServiceRecords(Long carId, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

    /**
     * tr: Araç detaylarını (plaka, renk vb.) düzenler ve güncel aracı döner.
     * en: Edits the car details (plate, color, etc.) and returns the updated car.
     */
    CarResponse editCarDetails(CarRequest carRequest, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Araca ait servis yüzdesi listesini döner.
     * en: Returns the list of service percentages for a car.
     */
    PercentageResponse getServicePercentageList(Long carId, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

    /**
     * tr: Bir servis yüzdesi kaydını (son servis tarihi/km vb.) düzenler ve güncel yüzdeyi döner.
     * en: Edits a service percentage record (last service date/km, etc.) and returns the updated percentage.
     */
    CarServicePercentageResponse editPercentage(PercentageRequest request, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

    /**
     * tr: Zamanlanmış görev: tüm araçların yüzdelerini yeniden hesaplar ve gerekli push bildirimlerini gönderir.
     * en: Scheduled task: recalculates all cars' percentages and sends the necessary push notifications.
     */
    void calculateAndPushNotification();

}
