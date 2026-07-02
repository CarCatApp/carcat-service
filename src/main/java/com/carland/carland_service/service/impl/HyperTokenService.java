package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.HyperTokenResponse;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.enums.EnumPartnerId;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.feign.HyperAuthClient;
import com.carland.carland_service.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;


/**
 * Manages OAuth tokens for outbound partner API calls (currently Hyper).
 * Credentials are loaded from {@code partners.api_client_id} / {@code api_client_secret}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HyperTokenService {

    private static final String CACHE_NAME = "hyper";

    private final HyperAuthClient hyperAuthClient;
    private final PartnerRepository partnerRepository;
    private final CacheManager cacheManager;

    @Scheduled(fixedRate = 59 * 60 * 1000)
    public void refreshToken() {
        fetchTokenAndCache(EnumPartnerId.HYPER.getId());
    }

    public String getToken() {
        return getToken(EnumPartnerId.HYPER.getId());
    }

    public String getToken(Long partnerId) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        String cacheKey = cacheKey(partnerId);

        if (cache != null) {
            String token = cache.get(cacheKey, String.class);
            if (token != null) {
                return token;
            }
        }

        return fetchTokenAndCache(partnerId);
    }

    private String fetchTokenAndCache(Long partnerId) {
        try {
            PartnerCredentials credentials = resolveCredentials(partnerId);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", credentials.clientId());
            form.add("client_secret", credentials.clientSecret());

            HyperTokenResponse response = hyperAuthClient.getToken(form);
            if (response == null || response.getAccessToken() == null) {
                throw new MissingFieldException("Partner API token response is null for partnerId=" + partnerId);
            }

            String token = response.getAccessToken();
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                cache.put(cacheKey(partnerId), token);
                log.info("Cached OAuth token for partnerId={}", partnerId);
            }
            return token;
        } catch (Exception e) {
            log.error("OAuth token fetch failed for partnerId={}", partnerId, e);
            return null;
        }
    }

    private PartnerCredentials resolveCredentials(Long partnerId) {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new MissingFieldException("Partner not found for partnerId=" + partnerId));

        if (!Boolean.TRUE.equals(partner.getActive())) {
            throw new MissingFieldException("Partner is not active for partnerId=" + partnerId);
        }

        String clientId = partner.getApiClientId();
        String clientSecret = partner.getApiClientSecret();
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new MissingFieldException(
                    "partners.api_client_id and api_client_secret must be configured for partnerId=" + partnerId
            );
        }

        return new PartnerCredentials(clientId.trim(), clientSecret.trim());
    }

    private static String cacheKey(Long partnerId) {
        return "partner_token_" + partnerId;
    }

    private record PartnerCredentials(String clientId, String clientSecret) {
    }
}
