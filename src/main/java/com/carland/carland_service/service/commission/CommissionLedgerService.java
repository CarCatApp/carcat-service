package com.carland.carland_service.service.commission;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CommissionCustomerStatus;
import com.carland.carland_service.entity.CommissionEntry;
import com.carland.carland_service.entity.CommissionRate;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CommissionEntryRepository;
import com.carland.carland_service.repository.CommissionRateRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.util.PlateNumbers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * tr: Tamamlanan randevunun komisyon satırını açar. Tutar panelden veya aynı günün Hyper ziyaretinden gelir.
 *     Oran satıra yazılır ve sonra değişmez. carlist / history cache'ine dokunmaz.
 * en: Opens the commission row when a booking is completed. The amount comes from the panel visit or that day's Hyper visit.
 *     The rate is stored on the row and does not change later. Does not touch carlist / history cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommissionLedgerService {

    private final CommissionEntryRepository entries;
    private final CommissionRateRepository rates;
    private final CommissionCustomerStatusService statusService;
    private final VisitRepository visits;
    private final CarRepository cars;

    @Transactional
    public void onCompleted(Booking booking) {
        if (booking == null || booking.getId() == null || entries.existsByBookingId(booking.getId())) {
            return;
        }
        CommissionEntry entry = build(booking);
        entries.saveAndFlush(entry);
        if (entry.getVin() == null) {
            apply(entry, false);
            entries.save(entry);
            return;
        }
        refreshVin(entry.getVin());
    }

    @Transactional
    public void refreshVin(String vin) {
        String key = CommissionVin.normalize(vin);
        if (key == null) {
            return;
        }
        List<CommissionEntry> rows = entries.findByVin(key);
        Map<String, List<CommissionEntry>> groups = rows.stream()
                .collect(Collectors.groupingBy(row -> row.getPartnerId() + "|" + CommissionClock.bakuDate(row.getCompletedAt())));
        for (CommissionEntry row : rows) {
            String group = row.getPartnerId() + "|" + CommissionClock.bakuDate(row.getCompletedAt());
            boolean ambiguous = groups.getOrDefault(group, List.of()).size() > 1;
            apply(row, ambiguous);
        }
        if (!rows.isEmpty()) {
            entries.saveAll(rows);
        }
    }

    private CommissionEntry build(Booking booking) {
        String vin = CommissionVin.normalize(booking.getVin());
        if (vin != null && vin.length() > 32) {
            vin = vin.substring(0, 32);
        }
        CommissionCustomerStatus status = statusService.find(vin);
        if (status == null && vin != null) {
            status = statusService.ensure(booking);
        }
        String customerStatus = status == null ? CommissionStatus.NEW : status.getStatus();
        OffsetDateTime completedAt = OffsetDateTime.now(CommissionClock.BAKU);
        Branch branch = booking.getBranch();
        Partner partner = branch == null ? null : branch.getPartner();
        Long partnerId = partner == null ? null : partner.getId();
        Car car = booking.getCarId() == null ? null : cars.findByCarId(booking.getCarId());
        String plate = car == null ? null : cut(PlateNumbers.format(car.getPlateNumber()), 32);
        return CommissionEntry.builder()
                .bookingId(booking.getId())
                .ref(cut(booking.getRef(), 16))
                .partnerId(partnerId)
                .partnerName(cut(partner == null ? null : partner.getName(), 160))
                .branchId(branch == null ? null : branch.getId())
                .branchName(cut(branch == null ? null : branch.getName(), 160))
                .vin(vin)
                .plate(plate)
                .completedAt(completedAt)
                .customerStatus(CommissionStatus.known(customerStatus) ? customerStatus : CommissionStatus.NEW)
                .statusDecidedAt(status == null ? completedAt : status.getDecidedAt())
                .hyperOutcome(status == null ? CommissionStatusRules.NO_VIN : status.getHyperOutcome())
                .earliestServiceDate(status == null ? null : status.getEarliestServiceDate())
                .hyperSnapshot(status == null ? null : status.getSnapshot())
                .ratePercent(rate(partnerId, customerStatus, CommissionClock.bakuDate(completedAt)))
                .amountState(CommissionEntry.PENDING)
                .build();
    }

    private BigDecimal rate(Long partnerId, String status, LocalDate day) {
        String customerStatus = CommissionStatus.known(status) ? status : CommissionStatus.NEW;
        List<CommissionRate> rows = partnerId == null
                ? List.of()
                : rates.findByPartnerIdAndCustomerStatus(partnerId, customerStatus);
        return CommissionMoney.scale(CommissionRates.pick(rows, day, CommissionStatus.fallback(customerStatus)));
    }

    private void apply(CommissionEntry entry, boolean ambiguous) {
        List<Visit> bookingVisits = visits.findCommissionByBookingId(entry.getBookingId());
        List<Visit> hyper = List.of();
        if (!ambiguous && entry.getVin() != null && entry.getPartnerId() != null) {
            hyper = visits.findCommissionHyperVisits(
                    entry.getVin(), CommissionClock.bakuDate(entry.getCompletedAt()), entry.getPartnerId());
        }
        CommissionAmountResult result = CommissionAmounts.resolve(bookingVisits, hyper, ambiguous);
        if (!result.ready()) {
            entry.setServiceAmount(null);
            entry.setCommissionAmount(null);
            entry.setAmountState(CommissionEntry.PENDING);
            return;
        }
        entry.setServiceAmount(result.serviceAmount());
        entry.setCommissionAmount(CommissionMoney.commission(result.serviceAmount(), entry.getRatePercent()));
        entry.setAmountState(CommissionEntry.READY);
    }

    private static String cut(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
