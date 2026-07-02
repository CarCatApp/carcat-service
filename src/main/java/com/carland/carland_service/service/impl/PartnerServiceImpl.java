package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.PartnerRequest;
import com.carland.carland_service.dto.response.PartnerDataResponse;
import com.carland.carland_service.dto.response.PartnerResponse;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.EnumMessagesLangValues;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.service.interfaces.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PartnerServiceImpl implements PartnerService {

    @Value("${super.admin.phone}")
    private String superAdminPhoneNumber;

    private final PartnerRepository partnerRepository;

    @Override
    public PartnerResponse createPartner(PartnerRequest request, String phoneNumber, String acceptLanguage) {
        assertSuperAdmin(phoneNumber, acceptLanguage);

        if (request == null || request.getName() == null || request.getName().isBlank()
                || request.getSource() == null || request.getSource().isBlank()) {
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        String name = request.getName().trim();
        String source = request.getSource().trim();

        partnerRepository.findByNameIgnoreCaseAndSourceIgnoreCase(name, source).ifPresent(existing -> {
            throw new AlreadyExistsException(EnumMessagesLangValues.PARTNER_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
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
        return toResponse(saved, EnumMessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage));
    }

    @Override
    public PartnerResponse updatePartner(PartnerRequest request, String phoneNumber, String acceptLanguage) {
        assertSuperAdmin(phoneNumber, acceptLanguage);

        if (request == null || request.getId() == null) {
            throw new MissingFieldException(EnumMessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        Partner partner = partnerRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        EnumMessagesLangValues.PARTNER_NOT_FOUND.getMessageByLang(acceptLanguage)));

        String newName = request.getName() != null && !request.getName().isBlank()
                ? request.getName().trim()
                : partner.getName();
        String newSource = request.getSource() != null && !request.getSource().isBlank()
                ? request.getSource().trim()
                : partner.getSource();

        if (!newName.equalsIgnoreCase(partner.getName()) || !newSource.equalsIgnoreCase(partner.getSource())) {
            partnerRepository.findByNameIgnoreCaseAndSourceIgnoreCase(newName, newSource).ifPresent(existing -> {
                if (!existing.getId().equals(partner.getId())) {
                    throw new AlreadyExistsException(EnumMessagesLangValues.PARTNER_ALREADY_EXISTS.getMessageByLang(acceptLanguage));
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
        return toResponse(saved, EnumMessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage));
    }

    private void assertSuperAdmin(String phoneNumber, String acceptLanguage) {
        if (phoneNumber == null || superAdminPhoneNumber == null || !superAdminPhoneNumber.equals(phoneNumber)) {
            throw new InvalidStatusException(EnumMessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
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
