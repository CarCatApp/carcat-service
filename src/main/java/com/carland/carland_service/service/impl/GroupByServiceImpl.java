package com.carland.carland_service.service.impl;

import com.carland.carland_service.entity.*;
import com.carland.carland_service.enums.BodyTypeTranslation;
import com.carland.carland_service.enums.EngineTypeTranslation;
import com.carland.carland_service.enums.EnumMessagesLangValues;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.*;
import com.carland.carland_service.service.interfaces.GroupByService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupByServiceImpl implements GroupByService {

    private final BrandRepository brandRepository;
    private final ModelRepository modelRepository;
    private final BodyTypeRepository bodyTypeRepository;
    private final TransmissionTypeRepository transmissionTypeRepository;
    private final EngineTypeRepository engineTypeRepository;
private final ModelYearRepository modelYearRepository;
    @Override
    public List<Model> getModelsByBrand(Long brandId, String timezone, String acceptLanguage) {

        List<Model> modelList = modelRepository.findAllByBrandId(brandId);

        if (modelList.isEmpty()) {
            throw new ResourceNotFoundException(EnumMessagesLangValues.MODEL_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        return modelList;
    }

    @Override
    public List<Brand> getAllBrands(String timezone, String acceptLanguage) {
        List<Brand> brandList = brandRepository.findAll();
        if (brandList.isEmpty()) {
            throw new ResourceNotFoundException(EnumMessagesLangValues.BRAND_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        return brandList;
    }

    @Override
    public List<BodyType> getBodyTypes(String timezone, String acceptLanguage) {

        List<BodyType> bodyTypes =
                bodyTypeRepository.findAllByStatusOrderByBodyTypeIdAsc("ACTIVE");

        if (bodyTypes.isEmpty()) {
            throw new ResourceNotFoundException(
                    EnumMessagesLangValues.BODY_TYPE_NOT_FOUND
                            .getMessageByLang(acceptLanguage)
            );
        }

        bodyTypes.forEach(bodyType ->
                bodyType.setBodyType(
                        BodyTypeTranslation.translate(bodyType.getBodyType(), acceptLanguage)
                )
        );

        return bodyTypes;
    }


    @Override
    public List<TransmissionType> getTransmissionTypes(String timezone, String acceptLanguage) {
        List<TransmissionType> transmissionTypes = transmissionTypeRepository.findAllByStatus("ACTIVE");
        if (transmissionTypes.isEmpty()) {
            throw new ResourceNotFoundException(EnumMessagesLangValues.TRANSMISSION_TYPE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        return transmissionTypes;
    }

    @Override
    public List<EngineType> getEngineTypes(String timezone, String acceptLanguage) {

        List<EngineType> engineTypes = engineTypeRepository.findAllByStatusOrderByEngineTypeIdAsc("ACTIVE");

        if (engineTypes.isEmpty()) {
            throw new ResourceNotFoundException(EnumMessagesLangValues.ENGINE_TYPE_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        engineTypes.forEach(engineType ->
                engineType.setEngineType(
                        EngineTypeTranslation.translate(engineType.getEngineType(), acceptLanguage)
                )
        );

        return engineTypes;
    }


    @Override
    public List<ModelYear> getYearList(String timezone, String acceptLanguage) {

        List<ModelYear> modelYears = modelYearRepository.findAllByStatusOrderByModelYearDesc("ACTIVE");

        if (modelYears.isEmpty()) {
            throw new ResourceNotFoundException(EnumMessagesLangValues.MODEL_YEAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return modelYears;
    }

    @Override
    public List<Brand> getAllBrandsWithModels() {
        List<Brand> brands = brandRepository.findAll();
        Map<Long, List<Model>> modelsByBrandId = modelRepository.findAll().stream()
                .collect(Collectors.groupingBy(Model::getBrandId));

        brands.forEach(brand ->
                brand.setModels(modelsByBrandId.getOrDefault(brand.getBrandId(), Collections.emptyList()))
        );

        return brands;
    }

}
