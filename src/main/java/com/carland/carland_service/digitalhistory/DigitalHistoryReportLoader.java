package com.carland.carland_service.digitalhistory;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchServicedGood;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CarPhoto;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.enums.BodyTypeTranslation;
import com.carland.carland_service.enums.CarPhotoStatus;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.PartnerId;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.idda.IddaPlateCheck;
import com.carland.carland_service.idda.IddaPlateCheckRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.BranchServicedGoodRepository;
import com.carland.carland_service.repository.CarPhotoRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.service.ServiceCategoryJson;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * tr: PDF verisini okur. Hyper'a gitmez, satır yazmaz, {@code carlist} silmez.
 *     Sayılan qeyd tamamlanmış booking (panel) və Hyper ziyarətidir.
 *     Hyper ziyarətində şube yoxdursa bir Hyper şubesi sayılır. Dealer ayrımı sonra.
 *     Hazır fotoşəkil bayt olaraq alınır; Redis yazılmaz.
 *     Şəhadətnamə və mühərrik nömrəsi, VIN'in son başarılı IDDA kaydından gelir. Sahip adı alınmaz.
 * en: Reads the PDF data. Does not call Hyper, does not write rows, does not drop {@code carlist}.
 *     A counted record is a completed booking (panel) or a Hyper visit.
 *     A Hyper visit with no branch counts as one Hyper center. Dealer split comes later.
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
    private final OfferedServiceRepository offeredServiceRepository;
    private final IndividualServiceRepository individualServiceRepository;
    private final BranchServicedGoodRepository branchServicedGoodRepository;
    private final ServiceCategoryJson titles;

    @Transactional(readOnly = true)
    public DigitalHistoryReport load(long carId) {
        return load(carId, "az");
    }

    @Transactional(readOnly = true)
    public DigitalHistoryReport load(long carId, String acceptLanguage) {
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

        String lang = DigitalHistoryLang.normalize(acceptLanguage);
        Map<Long, Branch> branches = branches(visits);
        Map<Long, Partner> partners = partners(visits);
        DigitalHistoryVisits.Names names = names(visits, lang);
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
            blocks.add(DigitalHistoryVisits.toBlock(visit, branch, partner, names));
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
        DigitalHistoryCopy copy = DigitalHistoryCopy.of(lang);
        String body = BodyTypeTranslation.translate(car.getBodyType(), lang);
        DigitalHistorySpecs.Input spec = new DigitalHistorySpecs.Input(
                DigitalHistorySpecs.platePath(car),
                idda.certificateNumber(),
                idda.certificateDate(),
                DigitalHistoryFormat.text(PlateNumbers.format(car.getPlateNumber())),
                DigitalHistoryFormat.joinSpace(car.getBrand(), car.getModel()),
                DigitalHistoryFormat.text(car.getVin()),
                car.getModelYear() == null ? "" : car.getModelYear().toString(),
                color(car, lang),
                DigitalHistoryFormat.volume(car.getEngineVolume()),
                idda.engineNumber(),
                DigitalHistoryFormat.text(body)
        );
        return new DigitalHistoryReport(
                DigitalHistoryFormat.day(LocalDate.now(BAKU)),
                DigitalHistoryFormat.text(car.getBrand()),
                DigitalHistoryFormat.text(car.getModel()),
                spec.brandModel(),
                spec.plateNumber(),
                spec.vin(),
                spec.year(),
                spec.color(),
                spec.volume(),
                spec.engineNumber(),
                spec.bodyType(),
                spec.certificateNumber(),
                spec.certificateDate(),
                DigitalHistoryFormat.km(car.getMileage()),
                DigitalHistoryFormat.day(newest),
                visits.size(),
                period,
                centers.size(),
                List.copyOf(blocks),
                anyAmount ? DigitalHistoryFormat.total(grand, grandCurrency, mixed) : "",
                centers.values().stream().map(CenterAcc::toBlock).toList(),
                "",
                "",
                lang,
                mileageUpdatedOn(car),
                cities(centers),
                DigitalHistorySpecs.note(spec, copy),
                DigitalHistorySpecs.rows(spec, copy),
                photo(car.getCarId())
        );
    }

    private static String mileageUpdatedOn(Car car) {
        if (car.getLastMileageUpdateAt() == null) {
            return "";
        }
        return DigitalHistoryFormat.day(car.getLastMileageUpdateAt().toLocalDate());
    }

    private static String cities(Map<String, CenterAcc> centers) {
        Set<String> cities = new LinkedHashSet<>();
        for (CenterAcc center : centers.values()) {
            if (center.city != null && !center.city.isBlank()) {
                cities.add(center.city);
            }
        }
        return String.join(" · ", cities);
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

    private static boolean hyper(Visit visit) {
        if (PartnerId.HYPER.getId().equals(visit.getServiceCenterId())) {
            return true;
        }
        String source = visit.getSource();
        if (source == null || source.isBlank()) {
            return visit.getBookingId() == null;
        }
        String normalized = source.trim().toLowerCase(Locale.ROOT);
        return "hyper".equals(normalized) || "hyperservice".equals(normalized);
    }

    @Transactional(readOnly = true)
    public byte[] readyPhoto(Long carId) {
        return photo(carId);
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

    /**
     * tr: Şubesi olan ziyaret o şubeye yazılır. Şubesiz Hyper ziyaretlerin hepsi tek Hyper şubesidir.
     * en: A visit with a branch is filed there. Every branchless Hyper visit shares one Hyper center.
     */
    private static void accumulate(Map<String, CenterAcc> centers, Visit visit, Branch branch, Partner partner) {
        if (branch == null && !hyper(visit)) {
            return;
        }
        String key = branch == null ? "hyper" : "b:" + branch.getId();
        CenterAcc acc = centers.computeIfAbsent(key, ignored -> new CenterAcc(
                DigitalHistoryVisits.centerName(visit, branch, partner),
                branch != null ? DigitalHistoryFormat.text(branch.getAddress())
                        : partner == null ? "" : DigitalHistoryFormat.text(partner.getAddress()),
                DigitalHistoryVisits.phone(branch, partner),
                DigitalHistoryVisits.email(branch, partner),
                branch != null ? DigitalHistoryFormat.text(branch.getCity())
                        : partner == null ? "" : DigitalHistoryFormat.text(partner.getCity())
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

    private String color(Car car, String lang) {
        if (car.getColorId() != null) {
            String named = colorRepository.findById(car.getColorId())
                    .map(color -> color.nameForLang(lang))
                    .map(DigitalHistoryFormat::text)
                    .orElse("");
            if (!named.isEmpty()) {
                return named;
            }
        }
        return DigitalHistoryFormat.text(car.getIddaColor());
    }

    private DigitalHistoryVisits.Names names(List<Visit> visits, String lang) {
        Set<Long> offeredIds = new java.util.HashSet<>();
        Set<Long> individualIds = new java.util.HashSet<>();
        Set<Long> goodIds = new java.util.HashSet<>();
        for (Visit visit : visits) {
            if (visit.getServices() == null) {
                continue;
            }
            for (VisitServiceLine line : visit.getServices()) {
                if (line.getOfferedServiceId() != null) {
                    offeredIds.add(line.getOfferedServiceId());
                }
                if (line.getIndividualServiceId() != null) {
                    individualIds.add(line.getIndividualServiceId());
                }
                if (line.getProductGoodId() != null) {
                    goodIds.add(line.getProductGoodId());
                }
            }
        }
        Map<Long, OfferedService> offered = offeredIds.isEmpty()
                ? Map.of()
                : map(offeredServiceRepository.findAllById(offeredIds), OfferedService::getId);
        Map<Long, IndividualService> individual = individualIds.isEmpty()
                ? Map.of()
                : map(individualServiceRepository.findAllById(individualIds), IndividualService::getId);
        Map<Long, BranchServicedGood> goods = goodIds.isEmpty()
                ? Map.of()
                : map(branchServicedGoodRepository.findAllById(goodIds), BranchServicedGood::getId);
        return new DigitalHistoryVisits.Names() {
            @Override
            public String service(VisitServiceLine line) {
                if (line.getOfferedServiceId() != null) {
                    OfferedService row = offered.get(line.getOfferedServiceId());
                    String title = row == null ? "" : catalog(row.getTitleJson(), lang);
                    if (!title.isEmpty()) {
                        return title;
                    }
                }
                if (line.getIndividualServiceId() != null) {
                    IndividualService row = individual.get(line.getIndividualServiceId());
                    String title = row == null ? "" : catalog(row.getTitleJson(), lang);
                    if (!title.isEmpty()) {
                        return title;
                    }
                }
                return line.getServiceName();
            }

            @Override
            public String product(VisitServiceLine line) {
                if (line.getProductGoodId() != null) {
                    BranchServicedGood good = goods.get(line.getProductGoodId());
                    if (good != null) {
                        String heading = good.getBrandModelService() == null
                                ? ""
                                : catalog(good.getBrandModelService().getTitleJson(), lang);
                        String brand = DigitalHistoryFormat.text(good.getName());
                        if (!heading.isEmpty() && !brand.isEmpty()) {
                            return heading + " · " + brand;
                        }
                        if (!heading.isEmpty()) {
                            return heading;
                        }
                        if (!brand.isEmpty()) {
                            return brand;
                        }
                    }
                }
                return line.getProductName();
            }
        };
    }

    private String catalog(String titleJson, String lang) {
        return DigitalHistoryFormat.text(catalogText(titles.read(titleJson), lang));
    }

    private static String catalogText(Map<String, String> titles, String lang) {
        if (titles == null || titles.isEmpty()) {
            return "";
        }
        String picked = titles.get(lang);
        if (picked != null && !picked.isBlank()) {
            return picked;
        }
        for (String key : List.of("az", "en", "ru")) {
            String value = titles.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static <T> Map<Long, T> map(Iterable<T> rows, java.util.function.Function<T, Long> id) {
        Map<Long, T> byId = new HashMap<>();
        for (T row : rows) {
            Long key = id.apply(row);
            if (key != null) {
                byId.put(key, row);
            }
        }
        return byId;
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
        private final String city;
        private int visits;
        private BigDecimal total = BigDecimal.ZERO;
        private boolean anyAmount;
        private String currency;
        private boolean mixed;

        private CenterAcc(String name, String address, String phone, String email, String city) {
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.email = email;
            this.city = city;
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
