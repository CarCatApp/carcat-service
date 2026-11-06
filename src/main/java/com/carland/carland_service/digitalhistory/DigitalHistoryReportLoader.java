package com.carland.carland_service.digitalhistory;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CarPhoto;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.enums.CarPhotoStatus;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.idda.IddaPlateCheck;
import com.carland.carland_service.idda.IddaPlateCheckRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CarPhotoRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.util.PlateNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * tr: PDF verisini okur. Hyper'a gitmez, satır yazmaz, {@code carlist} silmez.
 *     Sayılan qeyd tamamlanmış booking (panel) və Hyper ziyarətidir. Servis mərkəzi sayı branch-dır.
 *     Hazır fotoşəkil bayt olaraq alınır; Redis yazılmaz.
 *     Şəhadətnamə və mühərrik nömrəsi, VIN'in son başarılı IDDA kaydından gelir. Sahip adı alınmaz.
 * en: Reads the PDF data. Does not call Hyper, does not write rows, does not drop {@code carlist}.
 *     A counted record is a completed booking (panel) or a Hyper visit. The center count is branches.
 *     A ready photo is taken as bytes; Redis is not written.
 *     Certificate and engine number come from the VIN's latest successful IDDA row. The owner name is not read.
 */
@Service
@RequiredArgsConstructor
public class DigitalHistoryReportLoader {

    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private final CarRepository carRepository;
    private final VisitRepository visitRepository;
    private final BranchRepository branchRepository;
    private final PartnerRepository partnerRepository;
    private final ColorRepository colorRepository;
    private final IddaPlateCheckRepository iddaPlateCheckRepository;
    private final CarPhotoRepository carPhotoRepository;

    @Transactional(readOnly = true)
    public DigitalHistoryReport load(long carId) {
        Car car = carRepository.findByCarId(carId);
        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang("az"));
        }
        List<Visit> visits = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car).stream()
                .filter(DigitalHistoryReportLoader::counted)
                .toList();
        for (Visit visit : visits) {
            if (visit.getServices() != null) {
                visit.getServices().size();
            }
            if (visit.getParts() != null) {
                visit.getParts().size();
            }
        }

        Map<Long, Branch> branches = branches(visits);
        Map<Long, Partner> partners = partners(visits);
        List<DigitalHistoryReport.VisitBlock> blocks = new ArrayList<>();
        Map<String, CenterAcc> centers = new LinkedHashMap<>();
        BigDecimal grand = BigDecimal.ZERO;
        boolean anyAmount = false;
        String grandCurrency = null;
        boolean mixed = false;
        LocalDate newest = null;
        LocalDate oldest = null;

        for (Visit visit : visits) {
            Branch branch = visit.getBranchId() == null ? null : branches.get(visit.getBranchId());
            Partner partner = partner(visit, branch, partners);
            blocks.add(DigitalHistoryVisits.toBlock(visit, branch, partner));
            accumulate(centers, visit, branch, partner);
            if (visit.getFinalCostAmount() != null) {
                anyAmount = true;
                grand = grand.add(visit.getFinalCostAmount());
                String currency = visit.getFinalCostCurrency();
                if (currency != null && !currency.isBlank()) {
                    if (grandCurrency == null) {
                        grandCurrency = currency.trim();
                    } else if (!grandCurrency.equalsIgnoreCase(currency.trim())) {
                        mixed = true;
                    }
                }
            }
            LocalDate day = visit.getLastServiceDate();
            if (day != null) {
                if (newest == null || day.isAfter(newest)) {
                    newest = day;
                }
                if (oldest == null || day.isBefore(oldest)) {
                    oldest = day;
                }
            }
        }

        IddaFields idda = idda(car.getVin());
        String period = period(oldest, newest);
        return new DigitalHistoryReport(
                DigitalHistoryFormat.day(LocalDate.now(BAKU)),
                DigitalHistoryFormat.text(car.getBrand()),
                DigitalHistoryFormat.text(car.getModel()),
                DigitalHistoryFormat.joinSpace(car.getBrand(), car.getModel()),
                DigitalHistoryFormat.text(PlateNumbers.format(car.getPlateNumber())),
                DigitalHistoryFormat.text(car.getVin()),
                car.getModelYear() == null ? "" : car.getModelYear().toString(),
                color(car),
                DigitalHistoryFormat.volume(car.getEngineVolume()),
                idda.engineNumber(),
                DigitalHistoryFormat.text(car.getBodyType()),
                idda.certificateNumber(),
                idda.certificateDate(),
                DigitalHistoryFormat.km(car.getMileage()),
                DigitalHistoryFormat.day(newest),
                visits.size(),
                period,
                centers.size(),
                List.copyOf(blocks),
                anyAmount ? DigitalHistoryFormat.total(grand, grandCurrency, mixed) : "",
                centers.values().stream().map(CenterAcc::toBlock).toList(),
                photo(car.getCarId())
        );
    }

    private Map<Long, Branch> branches(List<Visit> visits) {
        List<Long> ids = visits.stream()
                .map(Visit::getBranchId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Branch> byId = new HashMap<>();
        for (Branch branch : branchRepository.findAllById(ids)) {
            byId.put(branch.getId(), branch);
        }
        return byId;
    }

    private Map<Long, Partner> partners(List<Visit> visits) {
        List<Long> ids = visits.stream()
                .map(Visit::getServiceCenterId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Partner> byId = new HashMap<>();
        for (Partner partner : partnerRepository.findAllById(ids)) {
            byId.put(partner.getId(), partner);
        }
        return byId;
    }

    private static Partner partner(Visit visit, Branch branch, Map<Long, Partner> partners) {
        if (branch != null && branch.getPartner() != null) {
            return branch.getPartner();
        }
        if (visit.getServiceCenterId() == null) {
            return null;
        }
        return partners.get(visit.getServiceCenterId());
    }

    /**
     * tr: Tamamlanmış booking (panel və ya bookingId) və Hyper ziyarəti. Başqa mənbə sayılmır.
     * en: A completed booking (panel or a booking id) and a Hyper visit. Any other source is left out.
     */
    private static boolean counted(Visit visit) {
        String source = visit.getSource();
        if (visit.getBookingId() != null || "panel".equalsIgnoreCase(source)) {
            return true;
        }
        if (source == null || source.isBlank()) {
            return true;
        }
        String normalized = source.toLowerCase(Locale.ROOT);
        return "hyper".equals(normalized) || "hyperservice".equals(normalized);
    }

    private byte[] photo(Long carId) {
        if (carId == null) {
            return null;
        }
        CarPhoto stored = carPhotoRepository.findByCarId(carId);
        if (stored == null || stored.getImageData() == null || stored.getImageData().length == 0) {
            return null;
        }
        if (!CarPhotoStatus.isReady(stored.getPhotoStatus())) {
            return null;
        }
        return stored.getImageData();
    }

    private static void accumulate(Map<String, CenterAcc> centers, Visit visit, Branch branch, Partner partner) {
        if (branch == null) {
            return;
        }
        String key = "b:" + branch.getId();
        CenterAcc acc = centers.computeIfAbsent(key, ignored -> new CenterAcc(
                DigitalHistoryVisits.centerName(visit, branch, partner),
                DigitalHistoryFormat.text(branch.getAddress()),
                DigitalHistoryVisits.phone(branch, partner),
                DigitalHistoryVisits.email(branch, partner)
        ));
        acc.visits++;
        if (visit.getFinalCostAmount() != null) {
            acc.anyAmount = true;
            acc.total = acc.total.add(visit.getFinalCostAmount());
            String currency = visit.getFinalCostCurrency();
            if (currency != null && !currency.isBlank()) {
                if (acc.currency == null) {
                    acc.currency = currency.trim();
                } else if (!acc.currency.equalsIgnoreCase(currency.trim())) {
                    acc.mixed = true;
                }
            }
        }
    }

    private static String period(LocalDate oldest, LocalDate newest) {
        String start = DigitalHistoryFormat.day(oldest);
        String end = DigitalHistoryFormat.day(newest);
        if (start.isEmpty()) {
            return end;
        }
        if (end.isEmpty() || start.equals(end)) {
            return start;
        }
        return start + " – " + end;
    }

    private String color(Car car) {
        if (car.getColorId() != null) {
            String named = colorRepository.findById(car.getColorId())
                    .map(color -> color.nameForLang("az"))
                    .map(DigitalHistoryFormat::text)
                    .orElse("");
            if (!named.isEmpty()) {
                return named;
            }
        }
        return DigitalHistoryFormat.text(car.getIddaColor());
    }

    /**
     * tr: Yeniden eskiye. İlk başarılı kayıt. Sahip adı bu metoda girmez.
     * en: Newest first. The first successful row. The owner name does not enter this method.
     */
    private IddaFields idda(String vin) {
        if (vin == null || vin.isBlank()) {
            return IddaFields.EMPTY;
        }
        List<IddaPlateCheck> rows = iddaPlateCheckRepository.findByVinNewestFirst(
                vin.trim().toUpperCase(Locale.ROOT));
        for (IddaPlateCheck row : rows) {
            if (!row.isVehicleSuccess()) {
                continue;
            }
            return new IddaFields(
                    DigitalHistoryFormat.text(row.getCertificateNumber()),
                    DigitalHistoryFormat.text(row.getCertificateGivenDate()),
                    DigitalHistoryFormat.text(row.getEngineNumber())
            );
        }
        return IddaFields.EMPTY;
    }

    private record IddaFields(String certificateNumber, String certificateDate, String engineNumber) {
        private static final IddaFields EMPTY = new IddaFields("", "", "");
    }

    private static final class CenterAcc {
        private final String name;
        private final String address;
        private final String phone;
        private final String email;
        private int visits;
        private BigDecimal total = BigDecimal.ZERO;
        private boolean anyAmount;
        private String currency;
        private boolean mixed;

        private CenterAcc(String name, String address, String phone, String email) {
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.email = email;
        }

        private DigitalHistoryReport.CenterBlock toBlock() {
            return new DigitalHistoryReport.CenterBlock(
                    name,
                    address,
                    phone,
                    email,
                    visits,
                    anyAmount ? DigitalHistoryFormat.total(total, currency, mixed) : ""
            );
        }
    }
}
