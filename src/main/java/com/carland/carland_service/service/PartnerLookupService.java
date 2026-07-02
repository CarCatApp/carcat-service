package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.PartnerDataResponse;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.EnumPartnerId;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerLookupService {

    private final PartnerRepository partnerRepository;

    public Optional<Partner> findById(Long partnerId) {
        if (partnerId == null) {
            return Optional.empty();
        }
        return partnerRepository.findById(partnerId);
    }

    public Partner requireActivePartner(Long partnerId) {
        if (partnerId == null) {
            throw new MissingFieldException("partnerId is required");
        }
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found for partnerId: " + partnerId));
        if (!Boolean.TRUE.equals(partner.getActive())) {
            throw new ResourceNotFoundException("Partner is not active for partnerId: " + partnerId);
        }
        return partner;
    }

    public Optional<Partner> find(EnumPartnerId partnerId) {
        return partnerRepository.findById(partnerId.getId());
    }

    public Map<Long, Partner> loadByIds(Collection<Long> partnerIds) {
        if (partnerIds == null || partnerIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = partnerIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Partner> loaded = partnerRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Partner::getId, partner -> partner, (a, b) -> a, HashMap::new));
        ids.stream()
                .filter(id -> !loaded.containsKey(id))
                .forEach(id -> log.warn("Partner id={} not found in DB, using enum fallback", id));
        return loaded;
    }

    public PartnerDataResponse toDataResponse(Long partnerId, Map<Long, Partner> partnerById) {
        EnumPartnerId enumPartner = EnumPartnerId.fromId(partnerId).orElse(null);
        Partner partner = partnerId != null && partnerById != null ? partnerById.get(partnerId) : null;
        return toDataResponse(partner, enumPartner);
    }

    public PartnerDataResponse toDataResponse(Partner partner, EnumPartnerId enumPartner) {
        if (partner != null) {
            return PartnerDataResponse.builder()
                    .id(partner.getId())
                    .name(partner.getName())
                    .dealer(partner.getDealer())
                    .logoUrl(partner.getLogoUrl())
                    .active(partner.getActive())
                    .source(partner.getSource())
                    .build();
        }
        if (enumPartner != null) {
            return PartnerDataResponse.builder()
                    .id(enumPartner.getId())
                    .name(enumPartner.getDefaultName())
                    .active(true)
                    .source(enumPartner.getSource())
                    .build();
        }
        return null;
    }

    public Long resolvePartnerId(Long storedPartnerId, EnumPartnerId fallback) {
        return storedPartnerId != null ? storedPartnerId : fallback.getId();
    }

    public String resolvePartnerName(Long storedPartnerId, String storedName, Map<Long, Partner> partnerById, EnumPartnerId fallback) {
        Long partnerId = resolvePartnerId(storedPartnerId, fallback);
        Partner partner = partnerById.get(partnerId);
        if (partner != null) {
            return partner.getName();
        }
        if (storedName != null && !storedName.isBlank()) {
            return storedName;
        }
        return fallback.getDefaultName();
    }
}
