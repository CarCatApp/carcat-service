package com.carland.carland_service.service;

import com.carland.carland_service.dto.request.AdminPromoSaveRequest;
import com.carland.carland_service.dto.response.AdminBannerBranchOption;
import com.carland.carland_service.dto.response.AdminPromoRow;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Promo;
import com.carland.carland_service.entity.PromoBranch;
import com.carland.carland_service.exceptions.AlreadyExistsException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.PromoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * tr: Admin promo. Redis yok. Kod codeKey ile tekil, karşılaştırma büyük/küçük harf duymaz.
 *     En az bir şube. percentage 1–100 tam sayı. amount 0'dan büyük, en fazla 2 kuruş.
 *     expired elle. Mobil kullanılabilirlik Promo.usableOn: active, expired kapalı, Bakü günü aralıkta.
 * en: Admin promo. No Redis. codeKey is unique; comparison ignores case.
 *     At least one branch. percentage is a whole number 1–100. amount is above 0, at most 2 decimals.
 *     expired is manual. Mobile use is Promo.usableOn: active, expired off, Baku date inside the range.
 */
@Service
@RequiredArgsConstructor
public class AdminPromoService {

    static final int NAME_MAX = 255;
    static final int DESCRIPTION_MAX = 4000;
    static final int CODE_MAX = 64;

    private final PromoRepository promoRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<AdminPromoRow> all() {
        return promoRepository.findAllByOrderByIdDesc().stream()
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
    public AdminPromoRow create(AdminPromoSaveRequest body) {
        Promo promo = Promo.builder()
                .name(readName(body))
                .description(readDescription(body))
                .code(readCode(body))
                .codeKey(codeKey(body == null ? null : body.getCode()))
                .startDate(readStart(body))
                .endDate(readEnd(body))
                .discountType(readType(body == null ? null : body.getDiscountType()))
                .status(readStatus(body == null ? null : body.getStatus(), true))
                .expired(Boolean.TRUE.equals(body == null ? null : body.getExpired()))
                .branches(new ArrayList<>())
                .build();
        promo.setDiscountValue(readValue(promo.getDiscountType(), body == null ? null : body.getDiscountValue()));
        ensureCodeFree(promo.getCodeKey(), null);
        replaceBranches(promo, readBranchIds(body == null ? null : body.getBranchIds()));
        return toRow(promoRepository.save(promo));
    }

    @Transactional
    public AdminPromoRow update(Long id, AdminPromoSaveRequest body) {
        Promo promo = promoRepository.findById(id == null ? -1L : id)
                .orElseThrow(() -> new ResourceNotFoundException("promo not found"));
        if (body == null) {
            throw new MissingFieldException("body is required");
        }
        String code = readCode(body);
        String key = codeKey(code);
        ensureCodeFree(key, promo.getId());
        promo.setName(readName(body));
        promo.setDescription(readDescription(body));
        promo.setCode(code);
        promo.setCodeKey(key);
        promo.setStartDate(readStart(body));
        promo.setEndDate(readEnd(body));
        String type = readType(body.getDiscountType());
        promo.setDiscountType(type);
        promo.setDiscountValue(readValue(type, body.getDiscountValue()));
        promo.setStatus(readStatus(body.getStatus(), false));
        promo.setExpired(Boolean.TRUE.equals(body.getExpired()));
        replaceBranches(promo, readBranchIds(body.getBranchIds()));
        return toRow(promoRepository.save(promo));
    }

    /**
     * tr: Kalan şube aynı satır kalır. Listeyi silip aynı (promo, şube) için yeni satır açmak
     *     uk_promo_branch'i kırar: Hibernate INSERT'i DELETE'ten önce yazar (23505).
     * en: A kept branch stays the same row. Clearing and inserting the same (promo, branch)
     *     breaks uk_promo_branch: Hibernate flushes INSERT before DELETE (23505).
     */
    private void replaceBranches(Promo promo, List<Long> branchIds) {
        if (promo.getBranches() == null) {
            promo.setBranches(new ArrayList<>());
        }
        Map<Long, PromoBranch> existing = new LinkedHashMap<>();
        for (PromoBranch link : promo.getBranches()) {
            if (link != null && link.getBranchId() != null) {
                existing.putIfAbsent(link.getBranchId(), link);
            }
        }
        List<PromoBranch> next = new ArrayList<>();
        for (Long branchId : branchIds) {
            PromoBranch link = existing.remove(branchId);
            if (link == null) {
                link = PromoBranch.builder()
                        .promo(promo)
                        .branchId(branchId)
                        .build();
            }
            next.add(link);
        }
        promo.getBranches().clear();
        promo.getBranches().addAll(next);
    }

    private void ensureCodeFree(String key, Long selfId) {
        promoRepository.findByCodeKey(key).ifPresent(other -> {
            if (selfId == null || !selfId.equals(other.getId())) {
                throw new AlreadyExistsException("code already exists");
            }
        });
    }

    private AdminPromoRow toRow(Promo promo) {
        List<Long> branchIds = new ArrayList<>();
        if (promo.getBranches() != null) {
            for (PromoBranch link : promo.getBranches()) {
                if (link != null && link.getBranchId() != null) {
                    branchIds.add(link.getBranchId());
                }
            }
        }
        List<Branch> found = branchIds.isEmpty() ? List.of() : branchRepository.findAllById(branchIds);
        List<AdminBannerBranchOption> branches = new ArrayList<>();
        for (Long branchId : branchIds) {
            String name = "";
            if (found != null) {
                for (Branch branch : found) {
                    if (branch != null && branchId.equals(branch.getId())) {
                        name = branch.getName() == null ? "" : branch.getName();
                        break;
                    }
                }
            }
            branches.add(AdminBannerBranchOption.builder().id(branchId).name(name).build());
        }
        return AdminPromoRow.builder()
                .id(promo.getId())
                .bannerId(promo.getBannerId())
                .name(promo.getName())
                .description(promo.getDescription())
                .code(promo.getCode())
                .startDate(promo.getStartDate())
                .endDate(promo.getEndDate())
                .discountType(promo.getDiscountType())
                .discountValue(promo.getDiscountValue())
                .status(promo.getStatus())
                .expired(Boolean.TRUE.equals(promo.getExpired()))
                .branchIds(branchIds)
                .branches(branches)
                .build();
    }

    private List<Long> readBranchIds(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            throw MissingFieldException.required("branchIds");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : raw) {
            if (id == null) {
                throw MissingFieldException.required("branchIds");
            }
            ids.add(id);
        }
        for (Long id : ids) {
            if (!branchRepository.existsById(id)) {
                throw new ResourceNotFoundException("branch not found");
            }
        }
        return new ArrayList<>(ids);
    }

    private static String readName(AdminPromoSaveRequest body) {
        if (body == null || body.getName() == null || body.getName().isBlank()) {
            throw MissingFieldException.required("name");
        }
        String name = body.getName().trim();
        if (name.length() > NAME_MAX) {
            throw new MissingFieldException("name is too long");
        }
        return name;
    }

    private static String readDescription(AdminPromoSaveRequest body) {
        if (body == null || body.getDescription() == null || body.getDescription().isBlank()) {
            throw MissingFieldException.required("description");
        }
        String description = body.getDescription().trim();
        if (description.length() > DESCRIPTION_MAX) {
            throw new MissingFieldException("description is too long");
        }
        return description;
    }

    private static String readCode(AdminPromoSaveRequest body) {
        if (body == null || body.getCode() == null || body.getCode().isBlank()) {
            throw MissingFieldException.required("code");
        }
        String code = body.getCode().trim();
        if (code.length() > CODE_MAX) {
            throw new MissingFieldException("code is too long");
        }
        return code;
    }

    static String codeKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static LocalDate readStart(AdminPromoSaveRequest body) {
        if (body == null || body.getStartDate() == null) {
            throw MissingFieldException.required("startDate");
        }
        return body.getStartDate();
    }

    private static LocalDate readEnd(AdminPromoSaveRequest body) {
        LocalDate start = readStart(body);
        if (body.getEndDate() == null) {
            throw MissingFieldException.required("endDate");
        }
        if (body.getEndDate().isBefore(start)) {
            throw new InvalidStatusException("endDate is before startDate");
        }
        return body.getEndDate();
    }

    private static String readType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw MissingFieldException.required("discountType");
        }
        String type = raw.trim().toLowerCase(Locale.ROOT);
        if (!Promo.PERCENTAGE.equals(type) && !Promo.AMOUNT.equals(type)) {
            throw new InvalidStatusException("discountType must be percentage or amount");
        }
        return type;
    }

    private static String readStatus(String raw, boolean create) {
        if (raw == null || raw.isBlank()) {
            if (create) {
                return Promo.ACTIVE;
            }
            throw MissingFieldException.required("status");
        }
        String status = raw.trim().toLowerCase(Locale.ROOT);
        if (!Promo.ACTIVE.equals(status) && !Promo.DEACTIVE.equals(status)) {
            throw new InvalidStatusException("status must be active or deactive");
        }
        return status;
    }

    private static BigDecimal readValue(String type, BigDecimal raw) {
        if (raw == null) {
            throw MissingFieldException.required("discountValue");
        }
        if (Promo.PERCENTAGE.equals(type)) {
            try {
                BigDecimal whole = raw.stripTrailingZeros();
                if (whole.scale() > 0) {
                    throw new InvalidStatusException("percentage must be a whole number");
                }
                int value = whole.intValueExact();
                if (value < 1 || value > 100) {
                    throw new InvalidStatusException("percentage must be from 1 to 100");
                }
                return BigDecimal.valueOf(value).setScale(2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException ex) {
                throw new InvalidStatusException("percentage must be a whole number");
            }
        }
        if (raw.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidStatusException("amount must be above 0");
        }
        try {
            return raw.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new InvalidStatusException("amount has at most 2 decimals");
        }
    }
}
