package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.v2.ServiceHistoryV2;
import com.carland.carland_service.dto.response.v2.Visit;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.enums.HyperServiceMapping;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Applies Hyper (partner) service history onto a car's percentages.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Match: Hyper {@code universalServiceId} (String) is translated to our {@code name_en}
 *       via {@link HyperServiceMapping}; the percentage is found by {@code serviceNameEn}.</li>
 *   <li>Precedence: partner data is authoritative. CREATED and EDITED_BY_CUSTOMER are
 *       overwritten and become {@link PercentageStatus#EDITED_BY_PARTNER} (locked).
 *       An already partner-locked row is only updated by a strictly newer Hyper record.</li>
 *   <li>Idempotent: re-applying the same Hyper record is a no-op.</li>
 *   <li>Next service: when Hyper sends {@code nextServiceDate} / {@code nextServiceMileage} on the
 *       matched line, those values are applied as-is. Otherwise {@code lastService + intervalKm/intervalMonth}
 *       from the percentage template is used.</li>
 *   <li>Webhook ingest/update: percentages are refreshed only when the visit is at least as recent as
 *       all other visits on the car (date &gt;= max existing date and mileage &gt;= max existing mileage).</li>
 *   <li>Unmatched / never-serviced records are skipped silently (no error).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HyperPercentageSyncService {

    private final PercentageRepository percentageRepository;
    private final VisitRepository visitRepository;

    public void syncFromVisits(Car car, List<Visit> visits) {
        syncInternal(car, visits, false);
    }

    /**
     * Re-applies partner visit data onto matching percentages after a successful webhook create/update.
     * Unlike {@link #syncFromVisits}, re-syncs even when the same partner record was applied before.
     */
    public void syncFromVisit(Car car, Visit visit) {
        if (visit == null) {
            return;
        }
        if (!isEligibleForPercentageSync(car, visit)) {
            log.info("Skipping percentage sync — visit is older than existing visits | carId={}, recordId={}, date={}, mileage={}",
                    car.getCarId(), visit.getHyperRecordId(), visit.getLastServiceDate(), visit.getLastServiceMileage());
            return;
        }
        syncInternal(car, List.of(visit), true);
    }

    /**
     * Returns {@code true} when the visit is recent enough to refresh percentages.
     * Skips when another visit has a newer date or higher mileage (backfilled historical visits).
     */
    boolean isEligibleForPercentageSync(Car car, Visit candidate) {
        LocalDate maxOtherDate = null;
        Integer maxOtherMileage = null;

        for (Visit other : visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car)) {
            if (isSameVisit(other, candidate)) {
                continue;
            }
            LocalDate otherDate = other.getLastServiceDate();
            if (otherDate != null && (maxOtherDate == null || otherDate.isAfter(maxOtherDate))) {
                maxOtherDate = otherDate;
            }
            Integer otherMileage = other.getLastServiceMileage();
            if (otherMileage != null && (maxOtherMileage == null || otherMileage > maxOtherMileage)) {
                maxOtherMileage = otherMileage;
            }
        }

        LocalDate candidateDate = candidate.getLastServiceDate();
        Integer candidateMileage = candidate.getLastServiceMileage();

        if (maxOtherDate != null && candidateDate != null && candidateDate.isBefore(maxOtherDate)) {
            return false;
        }
        if (maxOtherMileage != null && candidateMileage != null && candidateMileage < maxOtherMileage) {
            return false;
        }
        return true;
    }

    private boolean isSameVisit(Visit a, Visit b) {
        if (a.getId() != null && Objects.equals(a.getId(), b.getId())) {
            return true;
        }
        return a.getHyperRecordId() != null && Objects.equals(a.getHyperRecordId(), b.getHyperRecordId());
    }

    private void syncInternal(Car car, List<Visit> visits, boolean forceReapply) {
        if (visits == null || visits.isEmpty()) {
            return;
        }

        List<Percentage> percentages = percentageRepository.findAllByCarId(car.getCarId());
        for (Percentage percentage : percentages) {
            if (percentage.getServiceNameEn() == null || percentage.getServiceNameEn().isBlank()) {
                continue;
            }

            Optional<HyperServiceMatch> matchOpt = findLatestMatch(percentage.getServiceNameEn(), visits);
            if (matchOpt.isEmpty()) {
                continue;
            }
            HyperServiceMatch match = matchOpt.get();

            if (!hasUsableData(match)) {
                continue;
            }

            PercentageStatus current = PercentageStatus.fromStored(percentage.getStatus());
            String matchedRecordId = recordIdOf(match.visit());

            if (!forceReapply && current == PercentageStatus.EDITED_BY_PARTNER) {
                boolean sameRecord = matchedRecordId != null
                        && matchedRecordId.equals(percentage.getPartnerRecordId());
                if (sameRecord || !isNewerThanApplied(match.visit(), percentage)) {
                    continue;
                }
            }

            applyPartnerData(car, percentage, match, matchedRecordId);
            percentageRepository.save(percentage);
            log.info("Synced percentage from Hyper | carId={}, nameEn={}, percentageId={}, recordId={}, forced={}",
                    car.getCarId(), percentage.getServiceNameEn(), percentage.getId(), matchedRecordId, forceReapply);
        }
    }

    private Optional<HyperServiceMatch> findLatestMatch(String nameEn, List<Visit> visits) {
        HyperServiceMatch best = null;

        for (Visit visit : visits) {
            if (visit.getServices() == null) {
                continue;
            }
            for (ServiceHistoryV2 line : visit.getServices()) {
                if (!HyperServiceMapping.matches(line.getUniversalServiceId(), nameEn)) {
                    continue;
                }
                if (best == null || isNewerVisit(visit, best.visit())) {
                    best = new HyperServiceMatch(visit, line);
                }
            }
        }

        return Optional.ofNullable(best);
    }

    private boolean hasUsableData(HyperServiceMatch match) {
        Visit visit = match.visit();
        ServiceHistoryV2 line = match.line();
        return visit.getLastServiceDate() != null
                || visit.getLastServiceMileage() != null
                || line.getNextServiceDate() != null
                || line.getNextServiceMileage() != null;
    }

    private boolean isNewerVisit(Visit candidate, Visit currentBest) {
        LocalDate candidateDate = candidate.getLastServiceDate();
        LocalDate bestDate = currentBest.getLastServiceDate();

        if (candidateDate == null && bestDate == null) {
            return compareById(candidate, currentBest) > 0;
        }
        if (candidateDate == null) {
            return false;
        }
        if (bestDate == null) {
            return true;
        }

        int dateCompare = candidateDate.compareTo(bestDate);
        if (dateCompare != 0) {
            return dateCompare > 0;
        }
        return compareById(candidate, currentBest) > 0;
    }

    private int compareById(Visit a, Visit b) {
        if (a.getId() == null || b.getId() == null) {
            return 0;
        }
        return Long.compare(a.getId(), b.getId());
    }

    /** True when the matched visit is strictly newer than what is currently stored on the percentage. */
    private boolean isNewerThanApplied(Visit visit, Percentage percentage) {
        LocalDate visitDate = visit.getLastServiceDate();
        LocalDate appliedDate = percentage.getLastServiceDate();

        if (visitDate == null) {
            return false;
        }
        if (appliedDate == null) {
            return true;
        }
        int cmp = visitDate.compareTo(appliedDate);
        if (cmp != 0) {
            return cmp > 0;
        }
        Integer visitKm = visit.getLastServiceMileage();
        Integer appliedKm = percentage.getLastServiceKm();
        if (visitKm == null) {
            return false;
        }
        return appliedKm == null || visitKm > appliedKm;
    }

    private void applyPartnerData(Car car, Percentage percentage, HyperServiceMatch match, String matchedRecordId) {
        Visit visit = match.visit();
        ServiceHistoryV2 line = match.line();

        LocalDate lastServiceDate = visit.getLastServiceDate();
        Integer lastServiceKm = visit.getLastServiceMileage();

        if (lastServiceDate != null) {
            percentage.setLastServiceDate(lastServiceDate);
        }
        if (lastServiceKm != null) {
            percentage.setLastServiceKm(lastServiceKm);
        }

        Long intervalKm = percentage.getIntervalKm();
        Integer intervalMonth = percentage.getIntervalMonth();

        Integer nextServiceKm = line.getNextServiceMileage();
        if (nextServiceKm == null && lastServiceKm != null && intervalKm != null && intervalKm > 0) {
            nextServiceKm = Math.toIntExact(lastServiceKm + intervalKm);
        }
        if (nextServiceKm != null) {
            percentage.setNextServiceKm(nextServiceKm);
        }

        LocalDate nextServiceDate = line.getNextServiceDate();
        if (nextServiceDate == null && lastServiceDate != null && intervalMonth != null && intervalMonth > 0) {
            nextServiceDate = lastServiceDate.plusMonths(intervalMonth);
        }
        if (nextServiceDate != null) {
            percentage.setNextServiceDate(nextServiceDate);
        }

        Long carMileage = car.getMileage();
        if (lastServiceKm != null && nextServiceKm != null && carMileage != null) {
            long totalKm = nextServiceKm - lastServiceKm;
            long remainingKmRaw = nextServiceKm - carMileage;
            percentage.setRemainingKm((int) Math.max(remainingKmRaw, 0));
            if (totalKm > 0) {
                int kmPct = (int) Math.round((remainingKmRaw * 100.0) / totalKm);
                percentage.setKmPercentage(Math.max(0, Math.min(100, kmPct)));
            } else {
                percentage.setKmPercentage(0);
            }
        }

        if (lastServiceDate != null && nextServiceDate != null) {
            long lastDay = lastServiceDate.toEpochDay();
            long nextDay = nextServiceDate.toEpochDay();
            long nowDay = LocalDate.now().toEpochDay();
            long totalDays = nextDay - lastDay;
            long remainingDays = Math.max(nextDay - nowDay, 0);
            if (totalDays > 0) {
                int monthPct = (int) Math.round((remainingDays * 100.0) / totalDays);
                percentage.setMonthPercentage(Math.max(0, Math.min(100, monthPct)));
            } else {
                percentage.setMonthPercentage(0);
            }
            percentage.setRemainingMonths(nextServiceDate);
        }

        percentage.setStatus(PercentageStatus.EDITED_BY_PARTNER.name());
        percentage.setPartnerRecordId(matchedRecordId);
        percentage.setLastPartnerSyncAt(LocalDateTime.now());

        log.info("Applied Hyper partner data | carId={}, serviceName={}, lastKm={}, lastDate={}, nextKm={} (hyper={}), nextDate={} (hyper={}), intervalKm={}, intervalMonth={}",
                car.getCarId(), percentage.getServiceNameEn(), lastServiceKm, lastServiceDate,
                nextServiceKm, line.getNextServiceMileage() != null,
                nextServiceDate, line.getNextServiceDate() != null,
                intervalKm, intervalMonth);
    }

    private String recordIdOf(Visit visit) {
        return visit.getHyperRecordId() != null ? String.valueOf(visit.getHyperRecordId()) : null;
    }

    private record HyperServiceMatch(Visit visit, ServiceHistoryV2 line) {
    }
}
