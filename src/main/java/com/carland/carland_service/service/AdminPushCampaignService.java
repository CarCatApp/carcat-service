package com.carland.carland_service.service;

import com.carland.carland_service.digitalhistory.DigitalHistoryLang;
import com.carland.carland_service.dto.request.AdminPushAudienceRequest;
import com.carland.carland_service.dto.request.AdminPushSendRequest;
import com.carland.carland_service.dto.response.AdminPushCampaignView;
import com.carland.carland_service.dto.response.AdminPushFiltersResponse;
import com.carland.carland_service.dto.response.AdminPushPreviewResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.DeviceToken;
import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.PushCampaign;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.DeviceTokenRepository;
import com.carland.carland_service.repository.EngineTypeRepository;
import com.carland.carland_service.repository.PushCampaignRepository;
import com.carland.carland_service.util.GenderNormalizer;
import com.carland.carland_service.util.PhoneNumbers;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * tr: Admin push kampanyası: telefon listesi veya AND kitle, az zorunlu çok dilli metin, kuyruk.
 * en: Admin push campaign: phone list or AND audience, az-required multilingual copy, queue.
 */
@Service
public class AdminPushCampaignService {

    private static final int TITLE_MAX = 100;
    private static final int BODY_MAX = 300;
    private static final int PHONE_MAX = 2000;

    private final DeviceTokenRepository deviceTokenRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final EngineTypeRepository engineTypeRepository;
    private final PushCampaignRepository pushCampaignRepository;
    private final PushCampaignWorker pushCampaignWorker;

    public AdminPushCampaignService(
            DeviceTokenRepository deviceTokenRepository,
            CustomerRepository customerRepository,
            CarRepository carRepository,
            EngineTypeRepository engineTypeRepository,
            PushCampaignRepository pushCampaignRepository,
            @Lazy PushCampaignWorker pushCampaignWorker
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.customerRepository = customerRepository;
        this.carRepository = carRepository;
        this.engineTypeRepository = engineTypeRepository;
        this.pushCampaignRepository = pushCampaignRepository;
        this.pushCampaignWorker = pushCampaignWorker;
    }

    public AdminPushFiltersResponse filters() {
        List<AdminPushFiltersResponse.EngineTypeOption> engines = engineTypeRepository
                .findAllByStatusOrderByEngineTypeIdAsc("ACTIVE")
                .stream()
                .map(et -> AdminPushFiltersResponse.EngineTypeOption.builder()
                        .id(et.getEngineTypeId())
                        .name(et.getEngineType())
                        .build())
                .toList();
        return AdminPushFiltersResponse.builder()
                .genders(List.of("MALE", "FEMALE"))
                .brands(carRepository.findDistinctBrands())
                .engineTypes(engines)
                .build();
    }

    public AdminPushPreviewResponse preview(AdminPushAudienceRequest request) {
        ResolvedAudience audience = resolveAudience(request);
        return AdminPushPreviewResponse.builder()
                .audienceCount(audience.count())
                .gender(audience.phones().isEmpty() ? audience.gender() : null)
                .brand(audience.phones().isEmpty() ? audience.brandRaw() : null)
                .engineTypeId(audience.phones().isEmpty() ? audience.engineTypeId() : null)
                .engineTypeName(audience.phones().isEmpty() ? audience.engineTypeName() : null)
                .phoneNumbers(audience.phones().isEmpty() ? null : audience.phones())
                .build();
    }

    public AdminPushCampaignView queue(AdminPushSendRequest request, String actor) {
        LocalizedCopy copy = resolveCopy(request);
        ResolvedAudience audience = resolveAudience(request);
        if (audience.count() == 0) {
            throw new InvalidStatusException("No users with a device token match these filters");
        }
        PushCampaign campaign = pushCampaignRepository.save(PushCampaign.builder()
                .title(copy.titleAz())
                .body(copy.bodyAz())
                .titleEn(copy.titleEn())
                .bodyEn(copy.bodyEn())
                .titleRu(copy.titleRu())
                .bodyRu(copy.bodyRu())
                .filterGender(audience.phones().isEmpty() ? audience.gender() : null)
                .filterBrand(audience.phones().isEmpty() ? audience.brandRaw() : null)
                .filterEngineTypeId(audience.phones().isEmpty() ? audience.engineTypeId() : null)
                .filterEngineTypeName(audience.phones().isEmpty() ? audience.engineTypeName() : null)
                .filterPhones(audience.phones().isEmpty() ? null : String.join("\n", audience.phones()))
                .status(PushCampaign.QUEUED)
                .audienceCount(audience.count())
                .successCount(0)
                .failedCount(0)
                .createdBy(actor)
                .createdAt(LocalDateTime.now())
                .build());
        pushCampaignRepository.flush();
        pushCampaignWorker.run(campaign.getId());
        return toView(campaign);
    }

    public AdminPushCampaignView get(Long id) {
        PushCampaign campaign = pushCampaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        return toView(campaign);
    }

    public List<AdminPushCampaignView> recent() {
        return pushCampaignRepository.findAll(
                        PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(this::toView)
                .toList();
    }

    List<AudienceRow> loadAudience(PushCampaign campaign) {
        List<DeviceToken> tokens;
        List<String> phones = parseStoredPhones(campaign.getFilterPhones());
        if (!phones.isEmpty()) {
            tokens = deviceTokenRepository.findAudienceByPhones(phoneLookupKeys(phones));
        } else {
            String brand = campaign.getFilterBrand() == null
                    ? null
                    : campaign.getFilterBrand().trim().toLowerCase(Locale.ROOT);
            String engineName = campaign.getFilterEngineTypeName() == null
                    ? null
                    : campaign.getFilterEngineTypeName().trim().toLowerCase(Locale.ROOT);
            tokens = deviceTokenRepository.findAudience(
                    campaign.getFilterGender(),
                    brand,
                    campaign.getFilterEngineTypeId(),
                    engineName
            );
        }
        if (tokens.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = tokens.stream()
                .map(DeviceToken::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> langByUser = customerRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        Customer::getUserId,
                        c -> DigitalHistoryLang.normalize(c.getNotificationLanguage()),
                        (a, b) -> a
                ));
        List<AudienceRow> rows = new ArrayList<>(tokens.size());
        for (DeviceToken token : tokens) {
            String lang = langByUser.getOrDefault(token.getUserId(), "az");
            rows.add(new AudienceRow(token, lang));
        }
        return rows;
    }

    static String titleFor(PushCampaign campaign, String lang) {
        return pickText(lang, campaign.getTitleEn(), campaign.getTitleRu(), campaign.getTitle());
    }

    static String bodyFor(PushCampaign campaign, String lang) {
        return pickText(lang, campaign.getBodyEn(), campaign.getBodyRu(), campaign.getBody());
    }

    private static String pickText(String lang, String en, String ru, String az) {
        String code = DigitalHistoryLang.normalize(lang);
        if ("en".equals(code) && !isBlank(en)) {
            return en.trim();
        }
        if ("ru".equals(code) && !isBlank(ru)) {
            return ru.trim();
        }
        return az == null ? "" : az.trim();
    }

    private ResolvedAudience resolveAudience(AdminPushAudienceRequest request) {
        List<String> phones = normalizePhones(request == null ? null : request.getPhoneNumbers());
        if (!phones.isEmpty()) {
            int count = (int) deviceTokenRepository.countAudienceByPhones(phoneLookupKeys(phones));
            return new ResolvedAudience(count, null, null, null, null, phones);
        }
        String gender = request == null ? null : GenderNormalizer.filterOrNull(request.getGender());
        String brandRaw = request == null ? null : blankToNull(request.getBrand());
        String brand = brandRaw == null ? null : brandRaw.toLowerCase(Locale.ROOT);
        Long engineTypeId = request == null ? null : request.getEngineTypeId();
        String engineTypeName = null;
        if (engineTypeId != null) {
            EngineType et = engineTypeRepository.findByEngineTypeId(engineTypeId);
            if (et == null) {
                throw new IllegalArgumentException("Unknown engine type");
            }
            if (et.getEngineType() != null && !et.getEngineType().isBlank()) {
                engineTypeName = et.getEngineType().trim().toLowerCase(Locale.ROOT);
            }
        }
        int count = (int) deviceTokenRepository.countAudience(gender, brand, engineTypeId, engineTypeName);
        return new ResolvedAudience(count, gender, brandRaw, engineTypeId, engineTypeName, List.of());
    }

    private LocalizedCopy resolveCopy(AdminPushSendRequest request) {
        if (request == null) {
            throw new MissingFieldException("Title and body are required");
        }
        String titleAz = firstNonBlank(request.getTitleAz(), request.getTitle());
        String bodyAz = firstNonBlank(request.getBodyAz(), request.getBody());
        if (isBlank(titleAz) || isBlank(bodyAz)) {
            throw new MissingFieldException("Title and body (az) are required");
        }
        titleAz = titleAz.trim();
        bodyAz = bodyAz.trim();
        String titleEn = blankToNull(request.getTitleEn());
        String bodyEn = blankToNull(request.getBodyEn());
        String titleRu = blankToNull(request.getTitleRu());
        String bodyRu = blankToNull(request.getBodyRu());
        validateLen("az title", titleAz, TITLE_MAX);
        validateLen("az body", bodyAz, BODY_MAX);
        if (titleEn != null) {
            validateLen("en title", titleEn, TITLE_MAX);
        }
        if (bodyEn != null) {
            validateLen("en body", bodyEn, BODY_MAX);
        }
        if (titleRu != null) {
            validateLen("ru title", titleRu, TITLE_MAX);
        }
        if (bodyRu != null) {
            validateLen("ru body", bodyRu, BODY_MAX);
        }
        return new LocalizedCopy(titleAz, bodyAz, titleEn, bodyEn, titleRu, bodyRu);
    }

    private static void validateLen(String label, String value, int max) {
        if (value.length() > max) {
            throw new InvalidStatusException(label + " must be " + max + " characters or fewer");
        }
    }

    private List<String> normalizePhones(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            for (String part : entry.split("[,;\\n\\r]+")) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                String normalized = PhoneNumbers.normalize(part.trim());
                if (normalized == null) {
                    throw new IllegalArgumentException("Invalid phone number: " + part.trim());
                }
                out.add(normalized);
                if (out.size() > PHONE_MAX) {
                    throw new InvalidStatusException("At most " + PHONE_MAX + " phone numbers allowed");
                }
            }
        }
        return List.copyOf(out);
    }

    private static List<String> phoneLookupKeys(List<String> normalized) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String phone : normalized) {
            keys.add(phone);
            if (phone.startsWith("+")) {
                keys.add(phone.substring(1));
            } else {
                keys.add("+" + phone);
            }
        }
        return List.copyOf(keys);
    }

    private static List<String> parseStoredPhones(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String line : stored.split("\\R")) {
            String normalized = PhoneNumbers.normalize(line);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return List.copyOf(out);
    }

    private AdminPushCampaignView toView(PushCampaign c) {
        List<String> phones = parseStoredPhones(c.getFilterPhones());
        return AdminPushCampaignView.builder()
                .id(c.getId())
                .title(c.getTitle())
                .body(c.getBody())
                .titleEn(c.getTitleEn())
                .bodyEn(c.getBodyEn())
                .titleRu(c.getTitleRu())
                .bodyRu(c.getBodyRu())
                .status(c.getStatus())
                .audienceCount(c.getAudienceCount() == null ? 0 : c.getAudienceCount())
                .successCount(c.getSuccessCount() == null ? 0 : c.getSuccessCount())
                .failedCount(c.getFailedCount() == null ? 0 : c.getFailedCount())
                .gender(c.getFilterGender())
                .brand(c.getFilterBrand())
                .engineTypeId(c.getFilterEngineTypeId())
                .engineTypeName(c.getFilterEngineTypeName())
                .phoneNumbers(phones.isEmpty() ? null : phones)
                .createdBy(c.getCreatedBy())
                .errorMessage(c.getErrorMessage())
                .createdAt(c.getCreatedAt())
                .startedAt(c.getStartedAt())
                .finishedAt(c.getFinishedAt())
                .build();
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (!isBlank(primary)) {
            return primary.trim();
        }
        if (!isBlank(fallback)) {
            return fallback.trim();
        }
        return null;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    record AudienceRow(DeviceToken deviceToken, String lang) {
    }

    private record ResolvedAudience(
            int count,
            String gender,
            String brandRaw,
            Long engineTypeId,
            String engineTypeName,
            List<String> phones
    ) {
    }

    private record LocalizedCopy(
            String titleAz,
            String bodyAz,
            String titleEn,
            String bodyEn,
            String titleRu,
            String bodyRu
    ) {
    }
}
