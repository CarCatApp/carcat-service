package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminPushAudienceRequest;
import com.carland.carland_service.dto.request.AdminPushSendRequest;
import com.carland.carland_service.dto.response.AdminPushCampaignView;
import com.carland.carland_service.dto.response.AdminPushFiltersResponse;
import com.carland.carland_service.dto.response.AdminPushPreviewResponse;
import com.carland.carland_service.entity.DeviceToken;
import com.carland.carland_service.entity.EngineType;
import com.carland.carland_service.entity.PushCampaign;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.DeviceTokenRepository;
import com.carland.carland_service.repository.EngineTypeRepository;
import com.carland.carland_service.repository.PushCampaignRepository;
import com.carland.carland_service.util.GenderNormalizer;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * tr: Admin push kampanyası: kombinasyonlu AND kitle, önizleme sayısı, kuyruk kaydı.
 * en: Admin push campaign: AND audience filters, preview count, queue record.
 */
@Service
public class AdminPushCampaignService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final CarRepository carRepository;
    private final EngineTypeRepository engineTypeRepository;
    private final PushCampaignRepository pushCampaignRepository;
    private final PushCampaignWorker pushCampaignWorker;

    public AdminPushCampaignService(
            DeviceTokenRepository deviceTokenRepository,
            CarRepository carRepository,
            EngineTypeRepository engineTypeRepository,
            PushCampaignRepository pushCampaignRepository,
            @Lazy PushCampaignWorker pushCampaignWorker
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
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
        ResolvedFilters f = resolve(request);
        int count = (int) deviceTokenRepository.countAudience(
                f.gender, f.brand, f.engineTypeId, f.engineTypeName);
        return AdminPushPreviewResponse.builder()
                .audienceCount(count)
                .gender(f.gender)
                .brand(request != null ? blankToNull(request.getBrand()) : null)
                .engineTypeId(f.engineTypeId)
                .engineTypeName(f.engineTypeName)
                .build();
    }

    public AdminPushCampaignView queue(AdminPushSendRequest request, String actor) {
        if (request == null || isBlank(request.getTitle()) || isBlank(request.getBody())) {
            throw new MissingFieldException("Title and body are required");
        }
        String title = request.getTitle().trim();
        String body = request.getBody().trim();
        if (title.length() > 100) {
            throw new InvalidStatusException("Başlıq mətni 100 simvolu keçə bilməz");
        }
        if (body.length() > 300) {
            throw new InvalidStatusException("Mesaj mətni 300 simvolu keçə bilməz");
        }
        ResolvedFilters f = resolve(request);
        int count = (int) deviceTokenRepository.countAudience(
                f.gender, f.brand, f.engineTypeId, f.engineTypeName);
        if (count == 0) {
            throw new InvalidStatusException("No users with a device token match these filters");
        }
        PushCampaign campaign = pushCampaignRepository.save(PushCampaign.builder()
                .title(title)
                .body(body)
                .filterGender(f.gender)
                .filterBrand(blankToNull(request.getBrand()))
                .filterEngineTypeId(f.engineTypeId)
                .filterEngineTypeName(f.engineTypeName)
                .status(PushCampaign.QUEUED)
                .audienceCount(count)
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

    List<DeviceToken> loadAudience(PushCampaign campaign) {
        String brand = campaign.getFilterBrand() == null
                ? null
                : campaign.getFilterBrand().trim().toLowerCase(Locale.ROOT);
        String engineName = campaign.getFilterEngineTypeName() == null
                ? null
                : campaign.getFilterEngineTypeName().trim().toLowerCase(Locale.ROOT);
        return deviceTokenRepository.findAudience(
                campaign.getFilterGender(),
                brand,
                campaign.getFilterEngineTypeId(),
                engineName
        );
    }

    private ResolvedFilters resolve(AdminPushAudienceRequest request) {
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
        return new ResolvedFilters(gender, brand, engineTypeId, engineTypeName);
    }

    private AdminPushCampaignView toView(PushCampaign c) {
        return AdminPushCampaignView.builder()
                .id(c.getId())
                .title(c.getTitle())
                .body(c.getBody())
                .status(c.getStatus())
                .audienceCount(c.getAudienceCount() == null ? 0 : c.getAudienceCount())
                .successCount(c.getSuccessCount() == null ? 0 : c.getSuccessCount())
                .failedCount(c.getFailedCount() == null ? 0 : c.getFailedCount())
                .gender(c.getFilterGender())
                .brand(c.getFilterBrand())
                .engineTypeId(c.getFilterEngineTypeId())
                .engineTypeName(c.getFilterEngineTypeName())
                .createdBy(c.getCreatedBy())
                .errorMessage(c.getErrorMessage())
                .createdAt(c.getCreatedAt())
                .startedAt(c.getStartedAt())
                .finishedAt(c.getFinishedAt())
                .build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private record ResolvedFilters(String gender, String brand, Long engineTypeId, String engineTypeName) {
    }
}
