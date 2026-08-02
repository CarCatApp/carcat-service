package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.PartnerRequest;
import com.carland.carland_service.dto.response.PartnerDataResponse;
import com.carland.carland_service.dto.response.PartnerResponse;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * tr: İş ortaklarını (Partner) yöneten servis; süper admin yetkisiyle partner oluşturma ve güncelleme işlemlerini yapar, isim+source ikilisinin benzersizliğini kontrol eder.
 * en: Service managing business partners (Partner); handles partner creation and update with super-admin authorization and enforces uniqueness of the name+source pair.
 */
@Service
@RequiredArgsConstructor
public class PartnerServiceImpl implements PartnerService {

    @Value("${super.admin.phone}")
    private String superAdminPhoneNumber;

    private final PartnerRepository partnerRepository;

    /**
     * tr: Yeni partner oluşturur ve PartnerResponse döner. Çağıran süper admin değilse InvalidStatusException, name/source alanları eksikse MissingFieldException, aynı isim+source ile partner zaten varsa AlreadyExistsException fırlatır. active alanı verilmezse true kabul edilir.
     * en: Creates a new partner and returns a PartnerResponse. Throws InvalidStatusException if the caller is not the super admin, MissingFieldException if name/source fields are missing, and AlreadyExistsException if a partner with the same name+source already exists. The active flag defaults to true when not provided.
     */
    @Override
    public PartnerResponse createPartner(PartnerRequest request, String phoneNumber, String acceptLanguage) {
        assertSuperAdmin(phoneNumber, acceptLanguage);

        if (request == null || request.getName() == null || request.getName().isBlank()
                || request.getSource() == null || request.getSource().isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        String name = request.getName().trim();
        String source = request.getSource().trim();

        partnerRepository.findByNameIgnoreCaseAndSourceIgnoreCase(name, source).ifPresent(existing -> {
            throw new AlreadyExistsException(MessagesLangValues.PARTNER_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
        });

        Partner partner = Partner.builder()
                .name(name)
                .dealer(request.getDealer())
                .logoUrl(request.getLogoUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .source(source)
                .webhookSecret(request.getWebhookSecret())
                .apiClientId(trimToNull(request.getApiClientId()))
                .apiClientSecret(trimToNull(request.getApiClientSecret()))
                .build();

        Partner saved = partnerRepository.save(partner);
        return toResponse(saved, MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage));
    }

    /**
     * tr: Mevcut partneri kısmi olarak günceller (yalnızca gönderilen alanlar değişir) ve PartnerResponse döner. Süper admin değilse InvalidStatusException, id yoksa MissingFieldException, partner bulunamazsa ResourceNotFoundException, yeni isim+source başka bir partnerle çakışırsa AlreadyExistsException fırlatır.
     * en: Partially updates an existing partner (only provided fields change) and returns a PartnerResponse. Throws InvalidStatusException if the caller is not the super admin, MissingFieldException if the id is missing, ResourceNotFoundException if the partner is not found, and AlreadyExistsException if the new name+source collides with another partner.
     */
    @Override
    public PartnerResponse updatePartner(PartnerRequest request, String phoneNumber, String acceptLanguage) {
        assertSuperAdmin(phoneNumber, acceptLanguage);

        if (request == null || request.getId() == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Partner partner = partnerRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        MessagesLangValues.PARTNER_NOT_FOUND.getMessageByLang(acceptLanguage)));

        String newName = request.getName() != null && !request.getName().isBlank()
                ? request.getName().trim()
                : partner.getName();
        String newSource = request.getSource() != null && !request.getSource().isBlank()
                ? request.getSource().trim()
                : partner.getSource();

        if (!newName.equalsIgnoreCase(partner.getName()) || !newSource.equalsIgnoreCase(partner.getSource())) {
            partnerRepository.findByNameIgnoreCaseAndSourceIgnoreCase(newName, newSource).ifPresent(existing -> {
                if (!existing.getId().equals(partner.getId())) {
                    throw new AlreadyExistsException(MessagesLangValues.PARTNER_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
                }
            });
            partner.setName(newName);
            partner.setSource(newSource);
        }

        if (request.getDealer() != null) {
            partner.setDealer(request.getDealer());
        }
        if (request.getLogoUrl() != null) {
            partner.setLogoUrl(request.getLogoUrl());
        }
        if (request.getActive() != null) {
            partner.setActive(request.getActive());
        }
        if (request.getWebhookSecret() != null) {
            partner.setWebhookSecret(StringUtils.hasText(request.getWebhookSecret())
                    ? request.getWebhookSecret().trim()
                    : null);
        }
        if (request.getApiClientId() != null) {
            partner.setApiClientId(trimToNull(request.getApiClientId()));
        }
        if (request.getApiClientSecret() != null) {
            partner.setApiClientSecret(trimToNull(request.getApiClientSecret()));
        }

        Partner saved = partnerRepository.save(partner);
        return toResponse(saved, MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage));
    }

    private void assertSuperAdmin(String phoneNumber, String acceptLanguage) {
        if (phoneNumber == null || superAdminPhoneNumber == null || !superAdminPhoneNumber.equals(phoneNumber)) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }
    }

    private PartnerResponse toResponse(Partner partner, String message) {
        return PartnerResponse.builder()
                .message(message)
                .partner(PartnerDataResponse.builder()
                        .id(partner.getId())
                        .name(partner.getName())
                        .dealer(partner.getDealer())
                        .logoUrl(partner.getLogoUrl())
                        .active(partner.getActive())
                        .source(partner.getSource())
                        .build())
                .build();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
