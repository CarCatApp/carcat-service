package com.carland.carland_service.controller;

import com.carland.carland_service.entity.*;
import com.carland.carland_service.service.GroupByService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * tr: Referans veri (lookup) REST controller'ı; marka, model, kasa tipi, vites tipi, motor tipi ve model yılı listelerini döner.
 * en: REST controller for reference (lookup) data; returns lists of brands, models, body types, transmission types, engine types, and model years.
 */
@RestController
@RequestMapping("/api/v1/group/by")
@RequiredArgsConstructor
public class GroupByController {


    private final GroupByService groupByService;

    /**
     * tr: Tüm araç markalarının listesini döner.
     * en: Returns the list of all car brands.
     */
    @GetMapping("/get/brand/list")

    public List<Brand> getAllBrands(@RequestHeader("Authorization") String token,
                                    @RequestHeader("X-Client-Timezone") String timezone,
                                    @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getAllBrands(timezone, acceptLanguage);
    }

    /**
     * tr: Tüm markaları, her markaya bağlı modelleriyle birlikte döner.
     * en: Returns all brands together with the models belonging to each brand.
     */
    @GetMapping("/get/brand/list/with/models")
    public List<Brand> getAllBrandsWithModels() {
        return groupByService.getAllBrandsWithModels();
    }

    /**
     * tr: Verilen brandId'ye ait model listesini döner.
     * en: Returns the list of models belonging to the given brandId.
     */
    @GetMapping("/get/model/list/by/brand")
    public List<Model> getModelsByBrand(@RequestParam Long brandId,
                                        @RequestHeader("Authorization") String token,
                                        @RequestHeader("X-Client-Timezone") String timezone,
                                        @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getModelsByBrand(brandId, timezone, acceptLanguage);
    }

    /**
     * tr: Kasa tipi (body type) listesini döner.
     * en: Returns the list of body types.
     */
    @GetMapping("/get/body/list")
    public List<BodyType> getBodyTypes(@RequestHeader("Authorization") String token,
                                       @RequestHeader("X-Client-Timezone") String timezone,
                                       @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getBodyTypes(timezone, acceptLanguage);
    }

    /**
     * tr: Vites tipi (transmission type) listesini döner.
     * en: Returns the list of transmission types.
     */
    @GetMapping("/get/transmission/list")
    public List<TransmissionType> getTransmissionTypes(@RequestHeader("Authorization") String token,
                                                       @RequestHeader("X-Client-Timezone") String timezone,
                                                       @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getTransmissionTypes(timezone, acceptLanguage);
    }

    /**
     * tr: Motor tipi (engine type) listesini döner.
     * en: Returns the list of engine types.
     */
    @GetMapping("/get/engine/type/list")
    public List<EngineType> getEngineTypes(@RequestHeader("Authorization") String token,
                                           @RequestHeader("X-Client-Timezone") String timezone,
                                           @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getEngineTypes(timezone, acceptLanguage);
    }




    /**
     * tr: Model yılı listesini döner.
     * en: Returns the list of model years.
     */
    @GetMapping("/get/year/list")
    public List<ModelYear> getYearList(@RequestHeader("Authorization") String token,
                                   @RequestHeader("X-Client-Timezone") String timezone,
                                   @RequestHeader("Accept-Language") String acceptLanguage) {
        return groupByService.getYearList(timezone, acceptLanguage);
    }
}
