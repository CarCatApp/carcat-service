package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingRatingCreateRequest;
import com.carland.carland_service.dto.booking.BookingRatingStats;
import com.carland.carland_service.dto.booking.BookingRatingView;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Rating;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * tr: Ortalama yazımda hesaplanır ({@code branches}/{@code partners} kolonları). GET kolon okur.
 * en: Average is computed on write into branch/partner columns. GET reads those columns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingRatingService {

    private final RatingRepository ratingRepository;
    private final BranchRepository branchRepository;
    private final PartnerRepository partnerRepository;

    @Transactional
    public BookingRatingView add(Long branchId, Long userId, BookingRatingCreateRequest request) {
        if (branchId == null) {
            throw new MissingFieldException("branchId boş ola bilməz");
        }
        if (userId == null) {
            throw MissingFieldException.required("X-User-Id");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Şöbə tapılmadı"));
        Integer score = request == null ? null : request.getScore();
        String text = request == null ? null : blankToNull(request.getText());
        String reaction = request == null ? null : blankToNull(request.getReaction());
        if (score != null && (score < 1 || score > 5)) {
            throw new MissingFieldException("score 1–5 aralığında olmalıdır");
        }
        if (score == null && text == null && reaction == null) {
            throw new MissingFieldException("score, text və ya reaction tələb olunur");
        }
        Rating saved = ratingRepository.save(Rating.builder()
                .branch(branch)
                .userId(userId)
                .score(score)
                .text(text)
                .reaction(reaction)
                .build());
        refreshStoredStats(branch);
        Partner partner = branch.getPartner();
        return BookingRatingView.builder()
                .id(saved.getId())
                .branchId(branchId)
                .partnerId(partner == null ? null : partner.getId())
                .userId(userId)
                .score(saved.getScore())
                .text(saved.getText())
                .reaction(saved.getReaction())
                .createdAt(saved.getCreatedAt())
                .rating(branch.getRating())
                .ratingCount(storedCount(branch.getRatingCount()))
                .partnerRating(partner == null ? null : partner.getRating())
                .partnerRatingCount(partner == null ? 0L : storedCount(partner.getRatingCount()))
                .build();
    }

    @Transactional
    public void recalculateStoredStats() {
        List<Branch> branches = branchRepository.findAllWithPartner();
        if (branches.isEmpty()) {
            return;
        }
        List<Long> ids = branches.stream().map(Branch::getId).toList();
        Map<Long, List<Rating>> byBranch = load(ids).stream()
                .filter(row -> row.getBranch() != null && row.getBranch().getId() != null)
                .collect(Collectors.groupingBy(row -> row.getBranch().getId()));
        for (Branch branch : branches) {
            apply(branch, BookingRatingStats.from(byBranch.getOrDefault(branch.getId(), List.of())));
        }
        branchRepository.saveAll(branches);

        Map<Long, List<Branch>> byPartner = new HashMap<>();
        for (Branch branch : branches) {
            if (branch.getPartner() == null || branch.getPartner().getId() == null) {
                continue;
            }
            byPartner.computeIfAbsent(branch.getPartner().getId(), ignored -> new ArrayList<>()).add(branch);
        }
        for (List<Branch> group : byPartner.values()) {
            Partner partner = group.get(0).getPartner();
            List<Rating> all = new ArrayList<>();
            for (Branch branch : group) {
                all.addAll(byBranch.getOrDefault(branch.getId(), List.of()));
            }
            apply(partner, BookingRatingStats.from(all));
            partnerRepository.save(partner);
        }
        log.info("BOOKING_RATING_STATS_STORED branches={}", branches.size());
    }

    private void refreshStoredStats(Branch branch) {
        BookingRatingStats branchStats = BookingRatingStats.from(load(List.of(branch.getId())));
        apply(branch, branchStats);
        branchRepository.save(branch);
        Partner partner = branch.getPartner();
        if (partner == null || partner.getId() == null) {
            return;
        }
        List<Long> ids = branchRepository.findByPartnerIdOrderByIdAsc(partner.getId()).stream()
                .map(Branch::getId)
                .toList();
        apply(partner, BookingRatingStats.from(load(ids)));
        partnerRepository.save(partner);
    }

    private List<Rating> load(Collection<Long> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = branchIds.stream().filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return ratingRepository.findByBranch_IdIn(ids);
    }

    private static void apply(Branch branch, BookingRatingStats stats) {
        branch.setRating(stats.rating());
        branch.setRatingCount(storedCount(stats.ratingCount()));
    }

    private static void apply(Partner partner, BookingRatingStats stats) {
        partner.setRating(stats.rating());
        partner.setRatingCount(storedCount(stats.ratingCount()));
    }

    static Long storedCount(Long value) {
        return value == null ? 0L : value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
