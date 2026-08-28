package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.CustomerInformationRequest;
import com.carland.carland_service.dto.response.*;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * tr: Kullanıcı REST controller'ı; müşterileri araçlarıyla birlikte listeleme, kullanıcı detayı ekleme, bildirim listesi getirme ve müşteri-araç verisini Excel'e aktarma uçlarını sunar.
 * en: REST controller for users; exposes endpoints to list customers with their cars, add user details, fetch the notification list, and export customer-car data to Excel.
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final ColorRepository colorRepository;

    /**
     * tr: Tüm müşterileri, her müşteriye bağlı araç bilgileriyle (renk çözümlenmiş halde) birlikte DTO listesi olarak döner.
     * en: Returns all customers together with their cars (with color resolved) mapped into a DTO list.
     */
    @GetMapping("/get/all/with/cars")
    public List<CustomerWithCarResponse> getCustomersWithCars() {

        List<Customer> customers = customerRepository.findAll();

        List<CustomerWithCarResponse> responses = new ArrayList<>();

        for (Customer customer : customers) {

            CustomerWithCarResponse response = new CustomerWithCarResponse();

            // CUSTOMER MAP
            CustomerResponse customerResponse = new CustomerResponse();
            customerResponse.setUserId(customer.getUserId());
            customerResponse.setName(customer.getName());
            customerResponse.setSurname(customer.getSurname());
            customerResponse.setPhoneNumber(customer.getPhoneNumber());
            customerResponse.setStatus(customer.getStatus());
            customerResponse.setCreatedAt(customer.getCreatedAt());
            // CAR MAP
            List<CarResponseForLink> carResponses =
                    customer.getCars() != null
                            ? customer.getCars()
                            .stream()
                            .map(car -> {
                                CarResponseForLink responseForLink = new CarResponseForLink();

                                Color color = colorRepository.findByColorId(car.getColorId());

                                responseForLink.setCarId(car.getCarId());
                                responseForLink.setVin(car.getVin());
                                responseForLink.setPlateNumber(car.getPlateNumber());
                                responseForLink.setBrand(car.getBrand());
                                responseForLink.setModel(car.getModel());
                                responseForLink.setModelYear(car.getModelYear());
                                responseForLink.setColor(color != null ? color.getColor() : "not set");
                                responseForLink.setEngineType(car.getEngineType());
                                responseForLink.setEngineVolume(car.getEngineVolume());
                                responseForLink.setTransmissionType(car.getTransmissionType());
                                responseForLink.setMileage(car.getMileage());
                                responseForLink.setUpdatedAt(car.getUpdatedAt());
                                responseForLink.setBodyType(car.getBodyType());

                                return responseForLink;
                            })
                            .toList()
                            : Collections.emptyList();

            response.setCustomerResponse(customerResponse);
            response.setCarResponses(carResponses);

            responses.add(response);
        }

        return responses;
    }

    /**
     * tr: Header'lardan gelen bilgilerle (X-User-Id, role, phoneNumber, inviterId vb.) kullanıcı detaylarını kaydeder ve kullanıcı bilgisini döner.
     * en: Saves the user's details using the header values (X-User-Id, role, phoneNumber, inviterId etc.) and returns the user info.
     */
    @PostMapping("/add-details")
    public UserResponse userAddDetails(@RequestHeader("Authorization") String token,
                                       @RequestHeader("role") String role,
                                       @RequestHeader("phoneNumber") String phoneNumber,
                                       @RequestHeader("X-User-Id") String userIdHeader,
                                       @RequestHeader("X-Client-Timezone") String timezone,
                                       @RequestHeader("Accept-Language") String acceptLanguage,
                                       @RequestHeader("inviterId") Long inviterId) {
        log.info("bura isledi ");
        Long userId = Long.valueOf(userIdHeader);
        log.info("header datas {} ", userIdHeader);
        log.info("header datas {} ", phoneNumber);
        log.info("header datas {} ", timezone);
        log.info("header datas {} ", role);
        log.info("header datas {} ", acceptLanguage);
        log.info("header datas {} ", token);
        log.info("header datas {} ", inviterId);
        return userService.userAddDetails(userId, role, phoneNumber, timezone, acceptLanguage, inviterId);
    }

    /**
     * tr: Çağıran kullanıcıya ait bildirim listesini döner.
     * en: Returns the notification list for the calling user.
     */
    @GetMapping("/notification/list")
    public List<NotificationResponse> getNotificationList(@RequestHeader("role") String role,
                                                          @RequestHeader("phoneNumber") String phoneNumber,
                                                          @RequestHeader("X-User-Id") String userIdHeader,
                                                          @RequestHeader("X-Client-Timezone") String timezone,
                                                          @RequestHeader("Accept-Language") String acceptLanguage) {
        return userService.getNotificationList(role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: "Məlumatlarım" ekranı için müşteri profilini döner (ad, soyad, e-posta, FIN, salt-okunur telefon).
     * en: Returns the customer profile for the "My information" screen (name, surname, e-mail, FIN, read-only phone).
     */
    @GetMapping("/information")
    public CustomerInformationResponse getCustomerInformation(@RequestHeader("role") String role,
                                                              @RequestHeader("phoneNumber") String phoneNumber,
                                                              @RequestHeader("X-User-Id") String userIdHeader,
                                                              @RequestHeader("Accept-Language") String acceptLanguage) {
        return userService.getCustomerInformation(role, phoneNumber, userIdHeader, acceptLanguage);
    }

    /**
     * tr: Müşteri profilini kaydeder. SIMA verified ise yalnızca e-posta güncellenir (ad/soyad/FIN kilitli).
     *     Verified değilse ad, soyad, e-posta ve FIN güncellenir. Telefon gövdeden alınmaz.
     * en: Saves the customer profile. When SIMA-verified only e-mail is updated (name/surname/FIN locked).
     *     Otherwise name, surname, e-mail and FIN are updated. Phone is not taken from the body.
     */
    @PutMapping("/information")
    public CustomerInformationResponse saveCustomerInformation(@RequestBody CustomerInformationRequest request,
                                                               @RequestHeader("role") String role,
                                                               @RequestHeader("phoneNumber") String phoneNumber,
                                                               @RequestHeader("X-User-Id") String userIdHeader,
                                                               @RequestHeader("Accept-Language") String acceptLanguage) {
        return userService.saveCustomerInformation(request, role, phoneNumber, userIdHeader, acceptLanguage);
    }

    /**
     * tr: FIN (pin) başka bir müşteride kayıtlı mı. Kendi FIN'i veya hiç yoksa occupied=false.
     * en: Whether the FIN (pin) is already registered on another customer. Own FIN or unused → occupied=false.
     */
    @GetMapping("/pin/is-occupied")
    public PinOccupiedResponse isOccupied(@RequestParam("pin") String pin,
                                          @RequestHeader("role") String role,
                                          @RequestHeader("phoneNumber") String phoneNumber,
                                          @RequestHeader("X-User-Id") String userIdHeader,
                                          @RequestHeader("Accept-Language") String acceptLanguage) {
        return userService.isPinOccupied(pin, role, phoneNumber, userIdHeader, acceptLanguage);
    }


    /**
     * tr: Tüm müşterileri araçlarıyla birlikte (aracı olmayan müşteriler tek satır olarak) XLSX dosyasına yazar ve indirilebilir olarak döner.
     * en: Writes all customers with their cars (customers without cars appear as a single row) into an XLSX file and returns it as a download.
     */
    @GetMapping("/customer-cars")
    public void exportCustomerCars(HttpServletResponse response) throws IOException {

        List<Customer> customers = customerRepository.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("data");

        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue("User ID");
        header.createCell(1).setCellValue("Name");
        header.createCell(2).setCellValue("Surname");
        header.createCell(3).setCellValue("Phone");
        header.createCell(4).setCellValue("Language");
        header.createCell(5).setCellValue("Status");

        header.createCell(6).setCellValue("Plate");
        header.createCell(7).setCellValue("VIN");
        header.createCell(8).setCellValue("Brand");
        header.createCell(9).setCellValue("Model");
        header.createCell(10).setCellValue("Year");
        header.createCell(11).setCellValue("Engine Volume");
        header.createCell(12).setCellValue("Engine Type");
        header.createCell(13).setCellValue("Mileage");
        header.createCell(14).setCellValue("Fuel Type");
        header.createCell(15).setCellValue("Transmission");
        header.createCell(16).setCellValue("Created At");
        header.createCell(17).setCellValue("Car Status");

        int rowNum = 1;

        for (Customer customer : customers) {

            List<Car> cars = carRepository.findByCustomer_UserId(customer.getUserId());
            if (cars.isEmpty()) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(customer.getUserId());
                row.createCell(1).setCellValue(customer.getName());
                row.createCell(2).setCellValue(customer.getSurname());
                row.createCell(3).setCellValue(customer.getPhoneNumber());
                row.createCell(4).setCellValue(customer.getNotificationLanguage());
                row.createCell(5).setCellValue(customer.getStatus().toString());
            }

            for (Car car : cars) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(customer.getUserId());
                row.createCell(1).setCellValue(customer.getName());
                row.createCell(2).setCellValue(customer.getSurname());
                row.createCell(3).setCellValue(customer.getPhoneNumber());
                row.createCell(4).setCellValue(customer.getNotificationLanguage());
                row.createCell(5).setCellValue(customer.getStatus().toString());

                row.createCell(6).setCellValue(car.getPlateNumber() != null ? car.getPlateNumber() : "");
                row.createCell(7).setCellValue(car.getVin() != null ? car.getVin() : "");
                row.createCell(8).setCellValue(car.getBrand() != null ? car.getBrand() : "");
                row.createCell(9).setCellValue(car.getModel() != null ? car.getModel() : "");
                row.createCell(10).setCellValue(car.getModelYear() != null ? car.getModelYear() : 0);
                row.createCell(11).setCellValue(car.getEngineVolume() != null ? car.getEngineVolume() : 0);
                row.createCell(12).setCellValue(car.getEngineType() != null ? car.getEngineType() : "");
                row.createCell(13).setCellValue(car.getMileage() != null ? car.getMileage() : 0);

                row.createCell(14).setCellValue(car.getEngineType() != null ? car.getEngineType() : "");

                row.createCell(15).setCellValue(car.getTransmissionType() != null ? car.getTransmissionType() : "");
                row.createCell(16).setCellValue(car.getCreatedAt() != null ? car.getCreatedAt().toString() : "");

                row.createCell(17).setCellValue("");
            }
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=data.xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}


