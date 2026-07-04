package com.carland.carland_service.service.interfaces;

import com.carland.carland_service.entity.*;

import java.util.List;

public interface GroupByService {
    List<Model> getModelsByBrand(Long brandId, String timezone, String acceptLanguage);


    List<Brand> getAllBrands(String timezone, String acceptLanguage);

    List<BodyType> getBodyTypes(String timezone, String acceptLanguage);


    List<TransmissionType> getTransmissionTypes(String timezone, String acceptLanguage);


    List<EngineType> getEngineTypes(String timezone, String acceptLanguage);


    List<ModelYear> getYearList(String timezone, String acceptLanguage);

    List<Brand> getAllBrandsWithModels();

}
