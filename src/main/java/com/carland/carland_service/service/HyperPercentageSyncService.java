package com.carland.carland_service.service;

import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Percentage;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.enums.HyperServiceMapping;
import com.carland.carland_service.enums.PercentageStatus;
import com.carland.carland_service.repository.PercentageRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.carland.carland_service.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Applies Hyper (partner) service history onto a car's percentages.
 *
 * tr: Hyper (partner) servis geçmişini aracın bakım yüzdelerine uygular: her yüzde için en güncel
 *     eşleşen ziyaret satırını bulur, son/sonraki servis tarih-km bilgilerini ve km/ay yüzdelerini
 *     hesaplayıp EDITED_BY_PARTNER durumuyla kaydeder.
 * en: Applies Hyper (partner) service history onto a car's percentages: finds the latest matching
 *     visit line per percentage, computes last/next service date-km values and km/month percentages,
 *     and persists them with the EDITED_BY_PARTNER status.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HyperPercentageSyncService {

    private final PercentageRepository percentageRepository;
    private final VisitRepository visitRepository;
    private final ServiceEntityRepository serviceEntityRepository;

    public record PartnerLineSnapshot(
            LocalDate lastServiceDate,
            Integer lastServiceKm,
            LocalDate nextServiceDate,
            Integer nextServiceKm
    ) {
    }

    /**
     * tr: Verilen ziyaret listesini aracın yüzdelerine uygular (zorla yeniden uygulama yapmaz;
     *     EDITED_BY_PARTNER olan ve daha yeni verisi olmayan yüzdeler atlanır).
     * en: Applies the given list of visits onto the car's percentages (no forced reapply;
     *     percentages already EDITED_BY_PARTNER without newer data are skipped).
     */
    public void syncFromVisits(Car car, List<Visit> visits) {
        syncInternal(car, visits, false);
    }

    /**
     * tr: Tek bir ziyareti zorla yeniden uygulama modunda senkronlar; ziyaret null ise hiçbir şey yapmaz.
     *     Bu servis için daha yeni bir satır varsa uygulamayı atlar.
     * en: Syncs a single visit in forced-reapply mode; does nothing when the visit is null.
     *     Skips applying if a newer line exists for the same service.
     */
    public void syncFromVisit(Car car, Visit visit) {
        if (visit == null) {
            return;
        }
        syncInternal(car, List.of(visit), true);
    }

    /**
     * Read-only: best matching partner visit line for list display (CREATED percentages).
     *
     * tr: Salt-okunur: liste görünümü için (CREATED durumundaki yüzdeler) servise en iyi eşleşen
     *     partner ziyaret satırının son/sonraki servis tarih-km özetini döner; servis adı boşsa
     *     veya kullanılabilir veri yoksa Optional.empty döner.
     * en: Read-only: returns the last/next service date-km snapshot of the best matching partner
     *     visit line for list display (CREATED percentages); returns Optional.empty when the
     *     service name is blank or no usable data exists.
     */
    public Optional<PartnerLineSnapshot> findBestPartnerLineForService(
            Car car,
            String serviceNameEn,
            Long intervalKm,
            Integer intervalMonth
    ) {
        if (!StringUtils.hasText(serviceNameEn)) {
            return Optional.empty();
        }
        List<Visit> visits = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car);
        Optional<PartnerLineSnapshot> snapshot = findLatestMatch(serviceNameEn.trim(), visits)
                .filter(this::hasUsableData)
                .map(match -> toSnapshot(match, intervalKm, intervalMonth));
        log.info("[hist-debug] partner line lookup | carId={} nameEn={} visitCount={} match={}",
                car.getCarId(), serviceNameEn, visits.size(), snapshot.isPresent());
        return snapshot;
    }

    private void syncInternal(Car car, List<Visit> visits, boolean forceReapply) {
        if (visits == null || visits.isEmpty()) {
            return;
        }

        List<Visit> allVisits = forceReapply
                ? visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car)
                : visits;

        List<Percentage> percentages = percentageRepository.findAllByCarId(car.getCarId());
        for (Percentage percentage : percentages) {
            String nameEn = resolveServiceNameEn(percentage);
            if (!StringUtils.hasText(nameEn)) {
                continue;
            }
            if (!StringUtils.hasText(percentage.getServiceNameEn())) {
                percentage.setServiceNameEn(nameEn);
            }

            Optional<HyperServiceMatch> matchOpt = findLatestMatch(nameEn, visits);
            if (matchOpt.isEmpty()) {
                continue;
            }
            HyperServiceMatch match = matchOpt.get();

            if (forceReapply) {
                Optional<HyperServiceMatch> globalBest = findLatestMatch(nameEn, allVisits);
                if (globalBest.isEmpty() || !isSameServiceLine(globalBest.get(), match)) {
                    log.info(
                            "Skipping percentage sync — a newer line exists for this service | carId={}, nameEn={}, recordId={}",
                            car.getCarId(),
                            nameEn,
                            recordIdOf(match.visit())
                    );
                    continue;
                }
            }

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
                    car.getCarId(), nameEn, percentage.getId(), matchedRecordId, forceReapply);
        }
    }

    private String resolveServiceNameEn(Percentage percentage) {
        if (StringUtils.hasText(percentage.getServiceNameEn())) {
            return percentage.getServiceNameEn().trim();
        }
        if (percentage.getServiceId() == null) {
            return null;
        }
        return serviceEntityRepository.findById(percentage.getServiceId())
                .map(ServiceEntity::getNameEn)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(null);
    }

    private PartnerLineSnapshot toSnapshot(HyperServiceMatch match, Long intervalKm, Integer intervalMonth) {
        Visit visit = match.visit();
        VisitServiceLine line = match.line();

        LocalDate lastServiceDate = visit.getLastServiceDate();
        Integer lastServiceKm = visit.getLastServiceMileage();

        Integer nextServiceKm = resolveNextServiceKm(line.getNextServiceMileage(), lastServiceKm, intervalKm);

        LocalDate nextServiceDate = line.getNextServiceDate();
        if (nextServiceDate == null && lastServiceDate != null && intervalMonth != null && intervalMonth > 0) {
            nextServiceDate = lastServiceDate.plusMonths(intervalMonth);
        }

        return new PartnerLineSnapshot(lastServiceDate, lastServiceKm, nextServiceDate, nextServiceKm);
    }

    private boolean isSameServiceLine(HyperServiceMatch a, HyperServiceMatch b) {
        if (!Objects.equals(recordIdOf(a.visit()), recordIdOf(b.visit()))) {
            return false;
        }
        String aUniversal = a.line().getUniversalServiceId();
        String bUniversal = b.line().getUniversalServiceId();
        if (aUniversal != null && bUniversal != null) {
            return aUniversal.trim().equalsIgnoreCase(bUniversal.trim());
        }
        return Objects.equals(a.line().getServiceCode(), b.line().getServiceCode());
    }

    private Optional<HyperServiceMatch> findLatestMatch(String nameEn, List<Visit> visits) {
        HyperServiceMatch best = null;

        for (Visit visit : visits) {
            if (visit.getServices() == null) {
                continue;
            }
            for (VisitServiceLine line : visit.getServices()) {
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
        VisitServiceLine line = match.line();
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
        VisitServiceLine line = match.line();

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

        Integer nextServiceKm = resolveNextServiceKm(line.getNextServiceMileage(), lastServiceKm, intervalKm);
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

    /** Line-level next km from Hyper can be invalid; fall back to template interval when needed. */
    private Integer resolveNextServiceKm(Integer lineNext, Integer lastServiceKm, Long intervalKm) {
        if (lineNext != null && lastServiceKm != null && lineNext > lastServiceKm) {
            return lineNext;
        }
        if (lastServiceKm != null && intervalKm != null && intervalKm > 0) {
            return Math.toIntExact(lastServiceKm + intervalKm);
        }
        return lineNext;
    }

    private String recordIdOf(Visit visit) {
        return visit.getHyperRecordId() != null ? String.valueOf(visit.getHyperRecordId()) : null;
    }

    private record HyperServiceMatch(Visit visit, VisitServiceLine line) {
    }
}
