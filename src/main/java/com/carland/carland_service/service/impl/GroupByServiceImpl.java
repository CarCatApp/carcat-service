package com.carland.carland_service.service.impl;

import com.carland.carland_service.entity.*;
import com.carland.carland_service.enums.BodyTypeTranslation;
import com.carland.carland_service.enums.EngineTypeTranslation;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.*;
import com.carland.carland_service.service.GroupByService;
import com.carland.carland_service.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * tr: Araç referans verilerinin implementasyonudur: marka/model listelerinde "yeni set" işaretine (isnew)
 *     göre filtreleme yapar; kasa/motor tiplerini dile göre çevirir ve boş sonuçlarda
 *     ResourceNotFoundException fırlatır.
 * en: Implementation for car reference data: filters brand/model lists by the "new set" marker (isnew),
 *     translates body/engine types by language, and throws ResourceNotFoundException on empty results.
 */
@Service
@RequiredArgsConstructor
public class GroupByServiceImpl implements GroupByService {

    private static final String NEW_SET_MARKER = ".";

    private final BrandRepository brandRepository;
    private final ModelRepository modelRepository;
    private final BodyTypeRepository bodyTypeRepository;
    private final TransmissionTypeRepository transmissionTypeRepository;
    private final EngineTypeRepository engineTypeRepository;
    private final ModelYearRepository modelYearRepository;
    private final RedisCacheService redisCacheService;

    private boolean isNewBrandModelSetActive() {
        return brandRepository.existsByIsnew(NEW_SET_MARKER);
    }

    private List<Brand> loadActiveBrands() {
        return isNewBrandModelSetActive()
                ? brandRepository.findAllByIsnew(NEW_SET_MARKER)
                : brandRepository.findAll();
    }

    private List<Model> loadModelsForBrand(Long brandId) {
        return isNewBrandModelSetActive()
                ? modelRepository.findAllByBrandIdAndIsnew(brandId, NEW_SET_MARKER)
                : modelRepository.findAllByBrandId(brandId);
    }

    private List<Model> loadAllActiveModels() {
        return isNewBrandModelSetActive()
                ? modelRepository.findAllByIsnew(NEW_SET_MARKER)
                : modelRepository.findAll();
    }

    /**
     * tr: Markaya ait modelleri (aktif sete göre filtreli) döner; liste boşsa ResourceNotFoundException fırlatır.
     * en: Returns the models of the brand (filtered by the active set); throws ResourceNotFoundException when empty.
     */
    @Override
    public List<Model> getModelsByBrand(Long brandId, String timezone, String acceptLanguage) {
        return redisCacheService.getOrLoadModels(brandId, () -> {
            List<Model> modelList = loadModelsForBrand(brandId);
            if (modelList.isEmpty()) {
                throw new ResourceNotFoundException(MessagesLangValues.MODEL_NOT_FOUND.getMessageByLang(acceptLanguage));
            }
            return modelList;
        });
    }

    /**
     * tr: Tüm markaları (aktif sete göre filtreli) döner; liste boşsa ResourceNotFoundException fırlatır.
     * en: Returns all brands (filtered by the active set); throws ResourceNotFoundException when empty.
     */
    @Override
    public List<Brand> getAllBrands(String timezone, String acceptLanguage) {
        return redisCacheService.getOrLoadBrands(() -> {
            List<Brand> brandList = loadActiveBrands();
            if (brandList.isEmpty()) {
                throw new ResourceNotFoundException(MessagesLangValues.BRAND_NOT_FOUND.getMessageByLang(acceptLanguage));
            }
            return brandList;
        });
    }

    /**
     * tr: ACTIVE durumdaki kasa tiplerini dile göre çevirip döner; liste boşsa ResourceNotFoundException fırlatır.
     * en: Returns ACTIVE body types translated to the requested language; throws ResourceNotFoundException when empty.
     */
    @Override
    public List<BodyType> getBodyTypes(String timezone, String acceptLanguage) {
        return redisCacheService.getOrLoadBodyTypes(acceptLanguage, () -> {
            List<BodyType> bodyTypes =
                    bodyTypeRepository.findAllByStatusOrderByBodyTypeIdAsc("ACTIVE");

            if (bodyTypes.isEmpty()) {
                throw new ResourceNotFoundException(
                        MessagesLangValues.BODY_TYPE_NOT_FOUND
                                .getMessageByLang(acceptLanguage)
                );
            }

            bodyTypes.forEach(bodyType ->
                    bodyType.setBodyType(
                            BodyTypeTranslation.translate(bodyType.getBodyType(), acceptLanguage)
                    )
            );

            return bodyTypes;
        });
    }


    /**
     * tr: ACTIVE durumdaki vites tiplerini döner; liste boşsa ResourceNotFoundException fırlatır.
     * en: Returns ACTIVE transmission types; throws ResourceNotFoundException when empty.
     */
    @Override
    public List<TransmissionType> getTransmissionTypes(String timezone, String acceptLanguage) {
        return redisCacheService.getOrLoadTransmissions(() -> {
            List<TransmissionType> transmissionTypes = transmissionTypeRepository.findAllByStatus("ACTIVE");
            if (transmissionTypes.isEmpty()) {
                throw new ResourceNotFoundException(MessagesLangValues.TRANSMISSION_TYPE_NOT_FOUND.getMessageByLang(acceptLanguage));
            }
            return transmissionTypes;
        });
    }

    /**
     * tr: ACTIVE durumdaki motor tiplerini dile göre çevirip döner; liste boşsa ResourceNotFoundException fırlatır.
     * en: Returns ACTIVE engine types translated to the requested language; throws ResourceNotFoundException when empty.
     */
    @Override
    public List<EngineType> getEngineTypes(String timezone, String acceptLanguage) {
        return redisCacheService.getOrLoadEngineTypes(acceptLanguage, () -> {
            List<EngineType> engineTypes = engineTypeRepository.findAllByStatusOrderByEngineTypeIdAsc("ACTIVE");

            if (engineTypes.isEmpty()) {
                throw new ResourceNotFoundException(MessagesLangValues.ENGINE_TYPE_NOT_FOUND.getMessageByLang(acceptLanguage));
            }

            engineTypes.forEach(engineType ->
                    engineType.setEngineType(
                            EngineTypeTranslation.translate(engineType.getEngineType(), acceptLanguage)
                    )
            );

            return engineTypes;
        });
    }


    /**
     * tr: ACTIVE model yıllarını azalan sırada döner; liste boşsa ResourceNotFoundException fırlatır.
     * en: Returns ACTIVE model years in descending order; throws ResourceNotFoundException when empty.
     */
    @Override
    public List<ModelYear> getYearList(String timezone, String acceptLanguage) {
        return redisCacheService.getOrLoadYears(() -> {
            List<ModelYear> modelYears = modelYearRepository.findAllByStatusOrderByModelYearDesc("ACTIVE");
            if (modelYears.isEmpty()) {
                throw new ResourceNotFoundException(MessagesLangValues.MODEL_YEAR_NOT_FOUND.getMessageByLang(acceptLanguage));
            }
            return modelYears;
        });
    }

    /**
     * tr: Tüm markaları, her markaya modelleri gruplayıp atayarak döner (aktif sete göre filtreli).
     * en: Returns all brands with their models grouped and assigned per brand (filtered by the active set).
     */
    @Override
    public List<Brand> getAllBrandsWithModels() {
        return redisCacheService.getOrLoadBrandsWithModels(() -> {
            List<Brand> brands = loadActiveBrands();
            Map<Long, List<Model>> modelsByBrandId = loadAllActiveModels().stream()
                    .collect(Collectors.groupingBy(Model::getBrandId));

            brands.forEach(brand ->
                    brand.setModels(modelsByBrandId.getOrDefault(brand.getBrandId(), Collections.emptyList()))
            );

            return brands;
        });
    }

}
