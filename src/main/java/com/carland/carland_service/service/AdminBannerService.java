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

import java.util.Comparator;
import java.util.List;

/**
 * tr: Admin banner yazımı. Şube varlığı kontrol edilmez; title ve branchId birbirinden bağımsızdır.
 *     Redis yok. Mobil GET yalnız showing=true ve status=active okur.
 * en: Admin banner writes. Branch existence is not checked; title and branchId are independent.
 *     No Redis. The mobile GET reads showing=true and status=active only.
 */
@Service
@RequiredArgsConstructor
public class AdminBannerService {

    static final int TITLE_MAX = 255;
    static final int DESCRIPTION_MAX = 4000;
    static final int DESCRIPTION_MIN = 3;

    private final BannerRepository bannerRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BannerItem> visible() {
        return bannerRepository.findByStatusAndShowingOrderByIdDesc(Banner.ACTIVE, true).stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBannerRow> active() {
        return bannerRepository.findByStatusOrderByIdDesc(Banner.ACTIVE).stream()
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
        Banner banner = Banner.builder()
                .title(readTitle(body))
                .description(readDescription(body))
                .branchId(readBranchId(body))
                .showing(false)
                .status(Banner.ACTIVE)
                .build();
        return toRow(bannerRepository.save(banner));
    }

    @Transactional
    public AdminBannerRow update(Long id, AdminBannerSaveRequest body) {
        if (id == null) {
            throw new ResourceNotFoundException("banner not found");
        }
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("banner not found"));
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
        boolean text = body.getTitle() != null || body.getDescription() != null || body.getBranchId() != null;
        if (text) {
            banner.setTitle(readTitle(body));
            banner.setDescription(readDescription(body));
            banner.setBranchId(readBranchId(body));
        } else if (body.getShowing() != null) {
            banner.setShowing(body.getShowing());
        } else {
            throw MissingFieldException.required("title");
        }
        return toRow(bannerRepository.save(banner));
    }

    private BannerItem toItem(Banner banner) {
        return BannerItem.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .description(banner.getDescription())
                .branchId(banner.getBranchId())
                .showing(Boolean.TRUE.equals(banner.getShowing()))
                .status(banner.getStatus())
                .build();
    }

    private AdminBannerRow toRow(Banner banner) {
        return AdminBannerRow.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .description(banner.getDescription())
                .branchId(banner.getBranchId())
                .showing(Boolean.TRUE.equals(banner.getShowing()))
                .status(banner.getStatus())
                .build();
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

    private static Long readBranchId(AdminBannerSaveRequest body) {
        if (body == null || body.getBranchId() == null) {
            throw MissingFieldException.required("branchId");
        }
        return body.getBranchId();
    }
}
