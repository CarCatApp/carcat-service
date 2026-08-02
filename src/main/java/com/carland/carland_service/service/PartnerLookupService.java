package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.PartnerDataResponse;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.PartnerId;
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

/**
 * tr: Partner kayıtlarına merkezi erişim sağlayan yardımcı servistir: id ile arama, aktiflik doğrulama,
 *     toplu yükleme ve DB kaydı ile enum fallback'i arasında partner id/ad çözümleme işlemlerini yapar.
 * en: Helper service providing central access to partner records: lookup by id, active-state validation,
 *     bulk loading, and resolving partner id/name between the DB record and the enum fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerLookupService {

    private final PartnerRepository partnerRepository;

    /**
     * tr: Verilen id ile partneri arar; id null ise Optional.empty döner.
     * en: Looks up the partner by the given id; returns Optional.empty when the id is null.
     */
    public Optional<Partner> findById(Long partnerId) {
        if (partnerId == null) {
            return Optional.empty();
        }
        return partnerRepository.findById(partnerId);
    }

    /**
     * tr: Aktif bir partner döner. partnerId null ise MissingFieldException, kayıt bulunamazsa
     *     veya partner aktif değilse ResourceNotFoundException fırlatır.
     * en: Returns an active partner. Throws MissingFieldException when partnerId is null,
     *     and ResourceNotFoundException when the record is missing or the partner is inactive.
     */
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

    /**
     * tr: PartnerId enum değerine karşılık gelen partner kaydını DB'den arar.
     * en: Looks up the DB record corresponding to the given PartnerId enum value.
     */
    public Optional<Partner> find(PartnerId partnerId) {
        return partnerRepository.findById(partnerId.getId());
    }

    /**
     * tr: Verilen id koleksiyonundaki partnerleri tek sorguda yükler ve id->Partner map'i döner;
     *     DB'de bulunamayan id'ler için uyarı loglar (enum fallback kullanılır).
     * en: Loads the partners for the given id collection in a single query and returns an id->Partner map;
     *     logs a warning for ids not found in the DB (the enum fallback is used).
     */
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

    /**
     * tr: partnerId ve önceden yüklenmiş partner map'inden PartnerDataResponse üretir;
     *     DB kaydı yoksa enum bilgisine düşer.
     * en: Builds a PartnerDataResponse from the partnerId and a preloaded partner map;
     *     falls back to the enum info when the DB record is absent.
     */
    public PartnerDataResponse toDataResponse(Long partnerId, Map<Long, Partner> partnerById) {
        PartnerId enumPartner = PartnerId.fromId(partnerId).orElse(null);
        Partner partner = partnerId != null && partnerById != null ? partnerById.get(partnerId) : null;
        return toDataResponse(partner, enumPartner);
    }

    /**
     * tr: DB'deki Partner kaydından, o yoksa enum değerinden PartnerDataResponse üretir;
     *     ikisi de yoksa null döner.
     * en: Builds a PartnerDataResponse from the DB Partner record, or from the enum value when absent;
     *     returns null when neither is available.
     */
    public PartnerDataResponse toDataResponse(Partner partner, PartnerId enumPartner) {
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

    /**
     * tr: Kayıtlı partner id'sini döner; null ise fallback enum'un id'sini kullanır.
     * en: Returns the stored partner id; uses the fallback enum's id when it is null.
     */
    public Long resolvePartnerId(Long storedPartnerId, PartnerId fallback) {
        return storedPartnerId != null ? storedPartnerId : fallback.getId();
    }

    /**
     * tr: Partner adını sırasıyla DB kaydından, kayıtlı ad alanından ya da fallback enum'un
     *     varsayılan adından çözümleyerek döner.
     * en: Resolves the partner name in order: from the DB record, then the stored name field,
     *     then the fallback enum's default name.
     */
    public String resolvePartnerName(Long storedPartnerId, String storedName, Map<Long, Partner> partnerById, PartnerId fallback) {
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
