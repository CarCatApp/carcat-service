package com.carland.carland_service.service;

import com.carland.carland_service.entity.*;

import java.util.List;

/**
 * tr: Araç ekleme ekranı için referans (lookup) verilerini sağlayan sözleşmedir: marka, model, kasa tipi,
 *     vites tipi, motor tipi ve model yılı listeleri.
 * en: Contract providing reference (lookup) data for the add-car screen: brands, models, body types,
 *     transmission types, engine types, and model years.
 */
public interface GroupByService {
    /**
     * tr: Verilen markaya ait model listesini döner.
     * en: Returns the list of models for the given brand.
     */
    List<Model> getModelsByBrand(Long brandId, String timezone, String acceptLanguage);


    /**
     * tr: Tüm araç markalarının listesini döner.
     * en: Returns the list of all car brands.
     */
    List<Brand> getAllBrands(String timezone, String acceptLanguage);

    /**
     * tr: Kasa tiplerinin listesini döner.
     * en: Returns the list of body types.
     */
    List<BodyType> getBodyTypes(String timezone, String acceptLanguage);


    /**
     * tr: Vites tiplerinin listesini döner.
     * en: Returns the list of transmission types.
     */
    List<TransmissionType> getTransmissionTypes(String timezone, String acceptLanguage);


    /**
     * tr: Motor tiplerinin listesini döner.
     * en: Returns the list of engine types.
     */
    List<EngineType> getEngineTypes(String timezone, String acceptLanguage);


    /**
     * tr: Model yıllarının listesini döner.
     * en: Returns the list of model years.
     */
    List<ModelYear> getYearList(String timezone, String acceptLanguage);

    /**
     * tr: Tüm markaları modelleriyle birlikte döner.
     * en: Returns all brands together with their models.
     */
    List<Brand> getAllBrandsWithModels();

}
