package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminBannerSaveRequest;
import com.carland.carland_service.dto.response.AdminBannerBranchOption;
import com.carland.carland_service.dto.response.AdminBannerRow;
import com.carland.carland_service.dto.response.BannerItem;
import com.carland.carland_service.entity.Banner;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BannerRepository;
import com.carland.carland_service.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * tr: Admin banner. Redis yok. showing=true birden fazla olabilir.
 *     Sıra sortOrder küçükten büyüğe, aynı sayıda id büyük önde.
 *     service branchId ister. adds ve info fotoğraf ister, url boş kalabilir.
 *     Fotoğraf photo:banner:{bannerId}. Yazan replaceBannerPhoto, commit sonrası evict.
 * en: Admin banner. No Redis. More than one row may have showing=true.
 *     Order is sortOrder ascending, then larger id first.
 *     service requires branchId. adds and info require a photo; url may be empty.
 *     Photo key photo:banner:{bannerId}. replaceBannerPhoto evicts it after commit.
 */
@Service
@RequiredArgsConstructor
public class AdminBannerService {

    static final int TITLE_MAX = 255;
    static final int DESCRIPTION_MAX = 4000;
    static final int DESCRIPTION_MIN = 3;
    static final int URL_MAX = 2000;

    private final BannerRepository bannerRepository;
    private final BranchRepository branchRepository;
    private final PhotoService photoService;

    @Transactional(readOnly = true)
    public List<BannerItem> visible() {
        return bannerRepository.findVisible(Banner.ACTIVE).stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBannerRow> active() {
        return bannerRepository.findActiveOrdered(Banner.ACTIVE).stream()
                .map(this::toRow)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBannerBranchOption> branches() {
        return branchRepository.findActiveWithActivePartner().stream()
                .sorted(Comparator
                        .comparing((Branch branch) -> branch.getName() == null ? "" : branch.getName(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Branch::getId, Comparator.nullsLast(Long::compareTo)))
                .map(branch -> AdminBannerBranchOption.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .build())
                .toList();
    }

    @Transactional
    public AdminBannerRow create(AdminBannerSaveRequest body) {
        return create(body, null);
    }

    @Transactional
    public AdminBannerRow create(AdminBannerSaveRequest body, MultipartFile file) {
        String type = readType(body == null ? null : body.getType());
        if (needsPhoto(type) && (file == null || file.isEmpty())) {
            throw MissingFieldException.required("file");
        }
        Banner banner = Banner.builder()
                .title(readTitle(body))
                .description(readDescription(body))
                .type(type)
                .branchId(readBranchId(type, body))
                .url(readUrl(type, body == null ? null : body.getUrl()))
                .sortOrder(readSortOrder(body == null ? null : body.getSortOrder()))
                .showing(false)
                .status(Banner.ACTIVE)
                .build();
        banner = bannerRepository.save(banner);
        if (needsPhoto(type)) {
            photoService.replaceBannerPhoto(file, banner.getId());
        }
        return toRow(banner);
    }

    @Transactional
    public AdminBannerRow replacePhoto(Long id, MultipartFile file) {
        Banner banner = requireBanner(id);
        if (!needsPhoto(storedType(banner))) {
            throw new InvalidStatusException("banner photo is only for adds and info");
        }
        photoService.replaceBannerPhoto(file, banner.getId());
        return toRow(banner);
    }

    @Transactional
    public AdminBannerRow update(Long id, AdminBannerSaveRequest body) {
        Banner banner = requireBanner(id);
        if (body == null) {
            throw new MissingFieldException("body is required");
        }
        if (body.getStatus() != null) {
            if (!Banner.DEACTIVE.equals(body.getStatus())) {
                throw new InvalidStatusException("status can only be set to deactive");
            }
            banner.setStatus(Banner.DEACTIVE);
            return toRow(bannerRepository.save(banner));
        }
        boolean text = body.getTitle() != null || body.getDescription() != null || body.getBranchId() != null
                || body.getSortOrder() != null || body.getUrl() != null;
        if (text) {
            String type = storedType(banner);
            banner.setTitle(readTitle(body));
            banner.setDescription(readDescription(body));
            if (body.getSortOrder() != null) {
                banner.setSortOrder(body.getSortOrder());
            }
            if (Banner.SERVICE.equals(type)) {
                banner.setBranchId(readBranchId(type, body));
                banner.setUrl(null);
            } else {
                banner.setBranchId(null);
                if (body.getUrl() != null) {
                    banner.setUrl(readUrl(type, body.getUrl()));
                }
            }
        } else if (body.getShowing() != null) {
            banner.setShowing(body.getShowing());
        } else {
            throw MissingFieldException.required("title");
        }
        return toRow(bannerRepository.save(banner));
    }

    private Banner requireBanner(Long id) {
        if (id == null) {
            throw new ResourceNotFoundException("banner not found");
        }
        return bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("banner not found"));
    }

    private BannerItem toItem(Banner banner) {
        return BannerItem.builder()
                .id(banner.getId())
                .type(storedType(banner))
                .title(banner.getTitle())
                .description(banner.getDescription())
                .branchId(banner.getBranchId())
                .url(banner.getUrl())
                .sortOrder(banner.getSortOrder())
                .showing(Boolean.TRUE.equals(banner.getShowing()))
                .status(banner.getStatus())
                .build();
    }

    private AdminBannerRow toRow(Banner banner) {
        return AdminBannerRow.builder()
                .id(banner.getId())
                .type(storedType(banner))
                .title(banner.getTitle())
                .description(banner.getDescription())
                .branchId(banner.getBranchId())
                .url(banner.getUrl())
                .sortOrder(banner.getSortOrder())
                .showing(Boolean.TRUE.equals(banner.getShowing()))
                .status(banner.getStatus())
                .build();
    }

    static String storedType(Banner banner) {
        if (banner.getType() == null || banner.getType().isBlank()) {
            return Banner.SERVICE;
        }
        return banner.getType();
    }

    static boolean needsPhoto(String type) {
        return Banner.ADDS.equals(type) || Banner.INFO.equals(type);
    }

    private static String readType(String raw) {
        if (raw == null || raw.isBlank()) {
            return Banner.SERVICE;
        }
        String type = raw.trim().toLowerCase(Locale.ROOT);
        if (!Banner.SERVICE.equals(type) && !Banner.ADDS.equals(type) && !Banner.INFO.equals(type)) {
            throw new InvalidStatusException("type must be service, adds or info");
        }
        return type;
    }

    private static String readTitle(AdminBannerSaveRequest body) {
        if (body == null || body.getTitle() == null) {
            throw MissingFieldException.required("title");
        }
        String title = body.getTitle().trim();
        if (title.isEmpty()) {
            throw MissingFieldException.required("title");
        }
        if (title.length() > TITLE_MAX) {
            throw new MissingFieldException("title is too long");
        }
        return title;
    }

    private static String readDescription(AdminBannerSaveRequest body) {
        if (body == null || body.getDescription() == null) {
            throw MissingFieldException.required("description");
        }
        String description = body.getDescription().trim();
        if (description.length() < DESCRIPTION_MIN) {
            throw new MissingFieldException("description must be at least 3 characters");
        }
        if (description.length() > DESCRIPTION_MAX) {
            throw new MissingFieldException("description is too long");
        }
        return description;
    }

    private static Long readBranchId(String type, AdminBannerSaveRequest body) {
        if (!Banner.SERVICE.equals(type)) {
            return null;
        }
        if (body == null || body.getBranchId() == null) {
            throw MissingFieldException.required("branchId");
        }
        return body.getBranchId();
    }

    private static String readUrl(String type, String raw) {
        if (Banner.SERVICE.equals(type)) {
            return null;
        }
        if (raw == null) {
            return null;
        }
        String url = raw.trim();
        if (url.isEmpty()) {
            return null;
        }
        if (url.length() > URL_MAX) {
            throw new MissingFieldException("url is too long");
        }
        return url;
    }

    private static Integer readSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }
}
