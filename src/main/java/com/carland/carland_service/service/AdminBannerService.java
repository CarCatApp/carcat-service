package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminBannerSaveRequest;
import com.carland.carland_service.dto.response.AdminBannerBranchOption;
import com.carland.carland_service.dto.response.AdminBannerRow;
import com.carland.carland_service.dto.response.BannerItem;
import com.carland.carland_service.entity.Banner;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Promo;
import com.carland.carland_service.enums.PartnerId;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BannerRepository;
import com.carland.carland_service.repository.PromoRepository;
import com.carland.carland_service.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * tr: Admin banner. Redis yok. showing=true birden fazla olabilir.
 *     Sıra sortOrder küçükten büyüğe, aynı sayıda id büyük önde.
 *     service branchId ister. adds, info ve promo fotoğraf ister, url boş kalabilir.
 *     promo ayrıca context ve tek promoId ister. Aynı promo ikinci banner'a yazılmaz. lang az, en, ru. Boş eski satır az.
 *     Liste dönmeden expireDate'i Bakü gününden önce olan showing kapanır, sonra liste döner.
 *     Fotoğraf photo:banner:{bannerId}. Yazan replaceBannerPhoto, commit sonrası evict.
 * en: Admin banner. No Redis. More than one row may have showing=true.
 *     Order is sortOrder ascending, then larger id first.
 *     service requires branchId. adds, info and promo require a photo; url may be empty.
 *     promo also requires context and one promoId. The same promo cannot sit on a second banner. lang is az, en, or ru. A blank legacy row is az.
 *     Before a list returns, showing is turned off when expireDate is before today's Baku date.
 *     Photo key photo:banner:{bannerId}. replaceBannerPhoto evicts it after commit.
 */
@Service
@RequiredArgsConstructor
public class AdminBannerService {

    static final int TITLE_MAX = 255;
    static final int DESCRIPTION_MAX = 4000;
    static final int DESCRIPTION_MIN = 3;
    static final int CONTEXT_MAX = 4000;
    static final int URL_MAX = 2000;
    static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private final BannerRepository bannerRepository;
    private final BranchRepository branchRepository;
    private final PhotoService photoService;
    private final PromoRepository promoRepository;

    /**
     * tr: Tek banner. status=active ve showing=true. Yoksa veya kapalıysa 404.
     *     Çağrıdan önce süresi geçen showing kapanır.
     * en: One banner. status=active and showing=true. Missing or closed is 404.
     *     A passed expire date turns showing off before the read.
     */
    @Transactional
    public BannerItem visibleById(Long id) {
        closeExpired();
        Banner banner = bannerRepository.findById(id == null ? -1L : id)
                .orElseThrow(() -> new ResourceNotFoundException("banner not found"));
        if (!Banner.ACTIVE.equals(banner.getStatus()) || !Boolean.TRUE.equals(banner.getShowing())) {
            throw new ResourceNotFoundException("banner not found");
        }
        return toItem(banner);
    }

    @Transactional
    public List<BannerItem> visible(String lang) {
        closeExpired();
        String wanted = AppAnnouncementHeaders.language(lang);
        return bannerRepository.findVisible(Banner.ACTIVE).stream()
                .filter(banner -> wanted.equals(storedLang(banner)))
                .map(this::toItem)
                .toList();
    }

    /**
     * tr: Köhnə Hyper müştəri. Başqa servisin branchId'si düşer. branchId boş olan adds/info kalır.
     * en: Old Hyper customer. A branchId that is not Hyper is dropped. adds/info with an empty branchId stay.
     */
    @Transactional
    public List<BannerItem> visibleForOldHyper(String lang) {
        Set<Long> hyperBranchIds = new HashSet<>();
        List<Branch> hyperBranches = branchRepository.findByPartnerIdOrderByIdAsc(PartnerId.HYPER.getId());
        if (hyperBranches != null) {
            for (Branch branch : hyperBranches) {
                if (branch != null && branch.getId() != null) {
                    hyperBranchIds.add(branch.getId());
                }
            }
        }
        closeExpired();
        String wanted = AppAnnouncementHeaders.language(lang);
        return bannerRepository.findVisible(Banner.ACTIVE).stream()
                .filter(banner -> banner.getBranchId() == null || hyperBranchIds.contains(banner.getBranchId()))
                .filter(banner -> wanted.equals(storedLang(banner)))
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public List<AdminBannerRow> active() {
        closeExpired();
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
                .context(readContext(type, body == null ? null : body.getContext()))
                .promoId(readPromoId(type, body == null ? null : body.getPromoId(), null))
                .lang(readLang(body == null ? null : body.getLang()))
                .expireDate(body == null ? null : body.getExpireDate())
                .type(type)
                .branchId(readBranchId(type, body))
                .url(readUrl(type, body == null ? null : body.getUrl()))
                .sortOrder(readSortOrder(body == null ? null : body.getSortOrder()))
                .showing(false)
                .status(Banner.ACTIVE)
                .build();
        banner = bannerRepository.save(banner);
        linkPromo(banner, null);
        if (needsPhoto(type)) {
            photoService.replaceBannerPhoto(file, banner.getId());
        }
        return toRow(banner);
    }

    @Transactional
    public AdminBannerRow replacePhoto(Long id, MultipartFile file) {
        Banner banner = requireBanner(id);
        if (!needsPhoto(storedType(banner))) {
            throw new InvalidStatusException("banner photo is only for adds, info and promo");
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
        boolean text = body.getTitle() != null || body.getDescription() != null || body.getContext() != null
                || body.getPromoId() != null || body.getBranchId() != null || body.getSortOrder() != null
                || body.getUrl() != null || body.getLang() != null || body.getExpireDate() != null;
        if (text) {
            String type = storedType(banner);
            Long previousPromoId = banner.getPromoId();
            banner.setTitle(readTitle(body));
            banner.setDescription(readDescription(body));
            banner.setContext(readContext(type, body.getContext()));
            if (body.getLang() != null) {
                banner.setLang(readLang(body.getLang()));
            }
            banner.setExpireDate(body.getExpireDate());
            if (body.getSortOrder() != null) {
                banner.setSortOrder(body.getSortOrder());
            }
            if (Banner.SERVICE.equals(type)) {
                banner.setBranchId(readBranchId(type, body));
                banner.setUrl(null);
                banner.setPromoId(null);
            } else {
                banner.setBranchId(null);
                banner.setPromoId(readPromoId(type, body.getPromoId(), banner.getId()));
                if (body.getUrl() != null) {
                    banner.setUrl(readUrl(type, body.getUrl()));
                }
            }
            bannerRepository.save(banner);
            linkPromo(banner, previousPromoId);
            return toRow(banner);
        } else if (body.getShowing() != null) {
            if (Boolean.TRUE.equals(body.getShowing()) && isExpired(banner.getExpireDate(), LocalDate.now(BAKU))) {
                throw new InvalidStatusException("expireDate has passed");
            }
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
                .lang(storedLang(banner))
                .title(banner.getTitle())
                .description(banner.getDescription())
                .context(banner.getContext())
                .promoId(banner.getPromoId())
                .expireDate(banner.getExpireDate())
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
                .lang(storedLang(banner))
                .title(banner.getTitle())
                .description(banner.getDescription())
                .context(banner.getContext())
                .promoId(banner.getPromoId())
                .expireDate(banner.getExpireDate())
                .branchId(banner.getBranchId())
                .url(banner.getUrl())
                .sortOrder(banner.getSortOrder())
                .showing(Boolean.TRUE.equals(banner.getShowing()))
                .status(banner.getStatus())
                .build();
    }

    private void closeExpired() {
        LocalDate today = LocalDate.now(BAKU);
        List<Banner> expired = bannerRepository.findExpiredShowing(Banner.ACTIVE, today);
        if (expired == null || expired.isEmpty()) {
            return;
        }
        for (Banner banner : expired) {
            banner.setShowing(false);
        }
        bannerRepository.saveAll(expired);
        bannerRepository.flush();
    }

    static boolean isExpired(LocalDate expireDate, LocalDate today) {
        return expireDate != null && expireDate.isBefore(today);
    }

    public static LocalDate parseExpireDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeException ex) {
            throw new InvalidStatusException("expireDate must be yyyy-MM-dd");
        }
    }

    static String storedType(Banner banner) {
        if (banner.getType() == null || banner.getType().isBlank()) {
            return Banner.SERVICE;
        }
        return banner.getType();
    }

    static String storedLang(Banner banner) {
        if (banner.getLang() == null || banner.getLang().isBlank()) {
            return Banner.AZ;
        }
        return banner.getLang();
    }

    private static String readLang(String raw) {
        if (raw == null || raw.isBlank()) {
            return Banner.AZ;
        }
        String lang = raw.trim().toLowerCase(Locale.ROOT);
        if (!Banner.AZ.equals(lang) && !Banner.EN.equals(lang) && !Banner.RU.equals(lang)) {
            throw new InvalidStatusException("lang must be az, en or ru");
        }
        return lang;
    }

    static boolean needsPhoto(String type) {
        return Banner.ADDS.equals(type) || Banner.INFO.equals(type) || Banner.PROMO.equals(type);
    }

    private static String readType(String raw) {
        if (raw == null || raw.isBlank()) {
            return Banner.SERVICE;
        }
        String type = raw.trim().toLowerCase(Locale.ROOT);
        if (!Banner.SERVICE.equals(type) && !Banner.ADDS.equals(type)
                && !Banner.INFO.equals(type) && !Banner.PROMO.equals(type)) {
            throw new InvalidStatusException("type must be service, adds, info or promo");
        }
        return type;
    }

    private Long readPromoId(String type, Long promoId, Long selfId) {
        if (!Banner.PROMO.equals(type)) {
            return null;
        }
        if (promoId == null) {
            throw MissingFieldException.required("promoId");
        }
        if (!promoRepository.existsById(promoId)) {
            throw new ResourceNotFoundException("promo not found");
        }
        bannerRepository.findByPromoId(promoId).ifPresent(other -> {
            if (selfId == null || !selfId.equals(other.getId())) {
                throw new AlreadyExistsException("promo is already linked to a banner");
            }
        });
        return promoId;
    }

    private void linkPromo(Banner banner, Long previousPromoId) {
        Long next = banner.getPromoId();
        if (previousPromoId != null && !previousPromoId.equals(next)) {
            promoRepository.findById(previousPromoId).ifPresent(old -> {
                if (banner.getId() != null && banner.getId().equals(old.getBannerId())) {
                    old.setBannerId(null);
                    promoRepository.save(old);
                }
            });
        }
        if (next == null) {
            return;
        }
        promoRepository.findById(next).ifPresent(promo -> {
            promo.setBannerId(banner.getId());
            promoRepository.save(promo);
        });
    }

    private static String readContext(String type, String raw) {
        if (!Banner.PROMO.equals(type)) {
            return null;
        }
        if (raw == null || raw.isBlank()) {
            throw MissingFieldException.required("context");
        }
        String context = raw.trim();
        if (context.length() > CONTEXT_MAX) {
            throw new MissingFieldException("context is too long");
        }
        return context;
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
