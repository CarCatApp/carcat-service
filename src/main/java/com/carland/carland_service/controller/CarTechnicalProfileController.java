package com.carland.carland_service.controller;

import com.carland.carland_service.dto.request.CarDriveTypeRequest;
import com.carland.carland_service.dto.request.CarTransmissionVariantRequest;
import com.carland.carland_service.dto.request.TimingBeltChoiceRequest;
import com.carland.carland_service.dto.response.CarResponse;
import com.carland.carland_service.dto.response.LabeledOptionResponse;
import com.carland.carland_service.service.technicalprofile.CarTechnicalProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * tr: Kia çekiş / transmisyon ve (tüm markalar) timing belt seçim uçları.
 * en: Kia drive / transmission and (all brands) timing-belt choice endpoints.
 */
@RestController
@RequestMapping("/api/v1/car/technical-profile")
@RequiredArgsConstructor
public class CarTechnicalProfileController {

    private final CarTechnicalProfileService carTechnicalProfileService;

    @GetMapping("/drive-types")
    public List<LabeledOptionResponse> driveTypes(@RequestHeader("Accept-Language") String acceptLanguage) {
        return carTechnicalProfileService.driveTypeOptions(acceptLanguage);
    }

    @GetMapping("/transmission-variants")
    public List<LabeledOptionResponse> transmissionVariants(
            @RequestHeader("Accept-Language") String acceptLanguage) {
        return carTechnicalProfileService.transmissionVariantOptions(acceptLanguage);
    }

    @GetMapping("/timing-belt-choices")
    public List<LabeledOptionResponse> timingBeltChoices(
            @RequestHeader("Accept-Language") String acceptLanguage) {
        return carTechnicalProfileService.timingBeltChoiceOptions(acceptLanguage);
    }

    @PostMapping("/drive-type")
    public CarResponse setDriveType(@RequestBody CarDriveTypeRequest request,
                                    @RequestHeader("Authorization") String token,
                                    @RequestHeader("phoneNumber") String phoneNumber,
                                    @RequestHeader("X-User-Id") String userIdHeader,
                                    @RequestHeader("Accept-Language") String acceptLanguage) {
        return carTechnicalProfileService.setDriveType(request, phoneNumber, userIdHeader, acceptLanguage);
    }

    @PostMapping("/transmission-variant")
    public CarResponse setTransmissionVariant(@RequestBody CarTransmissionVariantRequest request,
                                              @RequestHeader("Authorization") String token,
                                              @RequestHeader("phoneNumber") String phoneNumber,
                                              @RequestHeader("X-User-Id") String userIdHeader,
                                              @RequestHeader("Accept-Language") String acceptLanguage) {
        return carTechnicalProfileService.setTransmissionVariant(
                request, phoneNumber, userIdHeader, acceptLanguage);
    }

    @PostMapping("/timing-belt-choice")
    public CarResponse setTimingBeltChoice(@RequestBody TimingBeltChoiceRequest request,
                                           @RequestHeader("Authorization") String token,
                                           @RequestHeader("phoneNumber") String phoneNumber,
                                           @RequestHeader("X-User-Id") String userIdHeader,
                                           @RequestHeader("Accept-Language") String acceptLanguage) {
        return carTechnicalProfileService.setTimingBeltChoice(
                request, phoneNumber, userIdHeader, acceptLanguage);
    }
}
