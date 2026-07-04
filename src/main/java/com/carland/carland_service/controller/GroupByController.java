package com.carland.carland_service.controller;

import com.carland.carland_service.entity.*;
import com.carland.carland_service.service.interfaces.GroupByService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group/by")
@RequiredArgsConstructor
public class GroupByController {


    private final GroupByService groupByService;

    @GetMapping("/get/brand/list")

    public List<Brand> getAllBrands(@RequestHeader("Authorization") String token,
                                    @RequestHeader("X-Client-Timezone") String timezone,
                                    @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getAllBrands(timezone, acceptLanguage);
    }

    @GetMapping("/get/brand/list/with/models")
    public List<Brand> getAllBrandsWithModels() {
        return groupByService.getAllBrandsWithModels();
    }

    @GetMapping("/get/model/list/by/brand")
    public List<Model> getModelsByBrand(@RequestParam Long brandId,
                                        @RequestHeader("Authorization") String token,
                                        @RequestHeader("X-Client-Timezone") String timezone,
                                        @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getModelsByBrand(brandId, timezone, acceptLanguage);
    }

    @GetMapping("/get/body/list")
    public List<BodyType> getBodyTypes(@RequestHeader("Authorization") String token,
                                       @RequestHeader("X-Client-Timezone") String timezone,
                                       @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getBodyTypes(timezone, acceptLanguage);
    }

    @GetMapping("/get/transmission/list")
    public List<TransmissionType> getTransmissionTypes(@RequestHeader("Authorization") String token,
                                                       @RequestHeader("X-Client-Timezone") String timezone,
                                                       @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getTransmissionTypes(timezone, acceptLanguage);
    }

    @GetMapping("/get/engine/type/list")
    public List<EngineType> getEngineTypes(@RequestHeader("Authorization") String token,
                                           @RequestHeader("X-Client-Timezone") String timezone,
                                           @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getEngineTypes(timezone, acceptLanguage);
    }




    @GetMapping("/get/year/list")
    public List<ModelYear> getYearList(@RequestHeader("Authorization") String token,
                                   @RequestHeader("X-Client-Timezone") String timezone,
                                   @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getYearList(timezone, acceptLanguage);
    }
}
