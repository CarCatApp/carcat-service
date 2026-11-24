package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBrandChipView;
import com.carland.carland_service.dto.booking.BookingBrandModelGroupView;
import com.carland.carland_service.dto.booking.StaffCompletionCardView;
import com.carland.carland_service.dto.booking.StaffCompletionDraftView;
import com.carland.carland_service.dto.booking.StaffCompletionPackageOption;
import com.carland.carland_service.dto.request.StaffCompleteRequest;
import com.carland.carland_service.dto.request.StaffCompleteRequest.StaffCompleteLine;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.BookingInspection;
import com.carland.carland_service.entity.BookingSelectedService;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchCarePackage;
import com.carland.carland_service.entity.BranchCarePackageItem;
import com.carland.carland_service.entity.BranchCompletion;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.BrandModel;
import com.carland.carland_service.entity.BrandModelService;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.OfferedService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.ServiceEntity;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitPart;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.enums.VisitLineKind;
import com.carland.carland_service.exceptions.ConflictException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.hyper.CarByVin;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BookingInspectionRepository;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BookingSelectedServiceRepository;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
import com.carland.carland_service.repository.BranchCarePackageRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import com.carland.carland_service.repository.BrandModelRepository;
import com.carland.carland_service.repository.BrandModelServiceRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.IndividualServiceRepository;
import com.carland.carland_service.repository.OfferedServiceRepository;
import com.carland.carland_service.repository.ServiceEntityRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.service.webhook.VisitWebhookSupport;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * tr: "Xidməti tamamla": pəncərənin taslağını qurur və doldurulan formu avtomobilin rəqəmsal tarixçəsinə
 *     (visits, source=panel) yazır. Detailed şubede yüzde, Hyper eşlemesiyle değil satırdaki services.id ile güncellenir.
 *     Status dəyişimi StaffBookingService-də qalır. Webhook şubesi bu write metoduna girmez.
 * en: "Complete service": builds the dialog draft and writes the submitted form into the car's digital history
 *     (visits, source=panel). On a detailed branch the percentage is updated from the line's services.id,
 *     not from Hyper name matching. The status change stays in StaffBookingService. A webhook branch never enters write.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCompletionService {

    static final String SOURCE_PANEL = "panel";
    private static final String CURRENCY = "AZN";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** Units the completion form offers in front of the quantity. Stored on the visit part. */
    private static final Set<String> PRODUCT_UNITS = Set.of("ədəd", "litr", "dəst", "kq");
    private static final int TITLE_MAX = 200;
    private static final int NOTE_MAX = 1000;

    private final BranchCarePackageItemRepository carePackageItemRepository;
    private final BookingIndividualLineRepository individualLineRepository;
    private final BookingSelectedServiceRepository selectedServiceRepository;
    private final BookingInspectionRepository inspectionRepository;
    private final BranchIndividualServiceRepository branchIndividualServiceRepository;
    private final IndividualServiceRepository individualServiceRepository;
    private final OfferedServiceRepository offeredServiceRepository;
    private final BrandModelRepository brandModelRepository;
    private final BrandModelServiceRepository brandModelServiceRepository;
    private final CarRepository carRepository;
    private final CarByVin carByVin;
    private final VisitRepository visitRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final ServiceCategoryJson json;
    private final BookingPercentageSnapshot bookingPercentageSnapshot;
    private final VisitWebhookSupport visitWebhookSupport;
    private final DetailedPercentageService detailedPercentageService;
    private final RedisCacheService redisCacheService;
    private final BookingRepository bookingRepository;
    private final BranchCarePackageRepository carePackageRepository;

    /** Package line split: periodic lines become cards, the rest are info lines. */
    private record PackageLines(List<OfferedService> periodic, List<OfferedService> info) {
    }

    /** The package the visit is written with. changed means the booking row must follow. */
    private record PackageChoice(Long id, String name, BigDecimal priceAzn, boolean changed) {
        static PackageChoice kept(Booking booking) {
            return new PackageChoice(
                    booking.getCarePackageId(),
                    blankToNull(booking.getPackageName()),
                    packagePriceAzn(booking),
                    false);
        }

        static PackageChoice cleared(Booking booking) {
            boolean had = booking.getCarePackageId() != null
                    || booking.getPackagePrice() != null
                    || blankToNull(booking.getPackageName()) != null;
            return new PackageChoice(null, null, null, had);
        }
    }

    /** A service line plus the part unit, which lives on visit_parts rather than the line. */
    private record BuiltLine(VisitServiceLine line, String productUnit) {
    }

    private record NextOpen(boolean date, boolean mileage) {
        static NextOpen both() {
            return new NextOpen(true, true);
        }
    }

    // ================= DRAFT =================

    public StaffCompletionDraftView draft(Booking booking, String acceptLanguage) {
        String lang = BookingMineService.langOf(acceptLanguage);
        Branch branch = booking.getBranch();
        Car car = findCar(booking);

        List<StaffCompletionCardView> cards = new ArrayList<>();
        PackageLines pkg = packageLines(booking.getCarePackageId());
        for (OfferedService service : pkg.periodic()) {
            cards.add(packageCard(service, car, lang));
        }

        Map<Long, BranchIndividualService> branchPrices = branchIndividualPrices(branch.getId());
        for (BookingIndividualLine line : individualLineRepository.findByBooking_IdOrderByIdAsc(booking.getId())) {
            IndividualService catalog = individualServiceRepository.findById(line.getIndividualServiceId()).orElse(null);
            BranchIndividualService priced = branchPrices.get(line.getIndividualServiceId());
            String code = catalog == null ? null : catalog.getMaintenanceCode();
            cards.add(individualCard(
                    line.getIndividualServiceId(),
                    text(line.getTitleJson(), lang),
                    code,
                    priced == null ? line.getPriceSimple() : priced.getPriceSimple(),
                    priced == null ? line.getPriceMedium() : priced.getPriceMedium(),
                    priced == null ? line.getPriceComplex() : priced.getPriceComplex(),
                    car));
        }

        // Tekil seçilmiş offered service: periodic → dövri kart (qiymət açıq), digəri → birdəfəlik kart.
        for (BookingSelectedService selected : selectedServiceRepository.findByBooking_IdOrderByIdAsc(booking.getId())) {
            OfferedService service = offeredServiceRepository.findById(selected.getOfferedServiceId()).orElse(null);
            boolean periodic = service != null && Boolean.TRUE.equals(service.getPeriodic());
            String code = periodic && service != null ? service.getMaintenanceCode() : null;
            cards.add(cardBuilder(
                            periodic ? VisitLineKind.INDIVIDUAL.apiValue() : VisitLineKind.ONE_TIME.apiValue(),
                            car,
                            code)
                    .offeredServiceId(selected.getOfferedServiceId())
                    .title(text(selected.getTitleJson(), lang))
                    .maintenanceCode(code)
                    .build());
        }

        String inspectionNote = inspectionNote(booking);
        if (inspectionNote != null) {
            cards.add(cardBuilder(VisitLineKind.INSPECTION.apiValue(), car, null)
                    .title(inspectionNote.length() > TITLE_MAX ? inspectionNote.substring(0, TITLE_MAX) : inspectionNote)
                    .build());
        }

        List<StaffCompletionCardView> catalog = new ArrayList<>();
        for (BranchIndividualService row : branchPrices.values()) {
            IndividualService service = row.getIndividualService();
            if (!Boolean.TRUE.equals(row.getActive()) || service == null || !Boolean.TRUE.equals(service.getActive())) {
                continue;
            }
            catalog.add(individualCard(service.getId(), text(service.getTitleJson(), lang), service.getMaintenanceCode(),
                    row.getPriceSimple(), row.getPriceMedium(), row.getPriceComplex(), car));
        }
        catalog.sort(Comparator.comparing(StaffCompletionCardView::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)));

        return StaffCompletionDraftView.builder()
                .bookingId(booking.getId())
                .ref(booking.getRef())
                .completionMode(BranchCompletion.shown(branch.getCompletionMode()))
                .carMileage(car == null ? null : car.getMileage())
                .carePackageId(booking.getCarePackageId())
                .packageName(blankToNull(booking.getPackageName()))
                .packagePrice(resolvedPackagePrice(booking))
                .packageInfoLines(pkg.info().stream().map(s -> text(s.getTitleJson(), lang)).toList())
                .inspectionNote(inspectionNote)
                .cards(cards)
                .individualCatalog(catalog)
                .packageCatalog(packageCatalog(branch, car, lang))
                .goods(brandModelGroups(branch.getId(), lang))
                .build();
    }

    // ================= WRITE =================

    /**
     * tr: Formu yeni bir panel ziyarəti kimi yazır (əvvəlki qeydlər üzərinə yazılmır — AC15).
     *     Sonra allTimeCost, servis görmüş partnerlər və detailed yüzde güncellenir. carlist commit sonrası düşer.
     * en: Writes the form as a new panel visit (older records are never overwritten — AC15).
     *     Then refreshes allTimeCost, serviced partners and the detailed percentage. The car list is evicted after commit.
     */
    public Visit write(Booking booking, StaffCompleteRequest request, String timezone, String acceptLanguage) {
        String lang = BookingMineService.langOf(acceptLanguage);
        Car car = findCar(booking);
        if (car == null) {
            throw new ConflictException("booking has no car");
        }
        if (request.getMileage() == null || request.getMileage() < 0) {
            throw MissingFieldException.required("mileage");
        }
        Branch branch = booking.getBranch();
        Partner partner = branch.getPartner();
        LocalDate visitDate = request.getDate() != null ? request.getDate() : LocalDate.now(zone(timezone));
        BigDecimal overallPercent = percent(request.getDiscountPercent(), "discountPercent");
        PackageChoice choice = packageChoice(booking, request);
        BigDecimal packagePrice = choice.priceAzn();

        Visit visit = Visit.builder()
                .car(car)
                .source(SOURCE_PANEL)
                .bookingId(booking.getId())
                .branchId(branch.getId())
                .lastServiceDate(visitDate)
                .lastServiceMileage(request.getMileage())
                .invoiceNumber(booking.getRef())
                .serviceCenterId(partner == null ? null : partner.getId())
                .serviceCenterName(partner == null ? branch.getName() : partner.getName())
                .packageName(choice.name())
                .packagePrice(packagePrice)
                .discountPercent(overallPercent)
                .inspectionNote(inspectionNote(booking))
                .notes(clip(request.getNotes(), NOTE_MAX))
                .costCurrency(CURRENCY)
                .finalCostCurrency(CURRENCY)
                .createdAt(LocalDateTime.now())
                .build();

        BigDecimal gross = packagePrice == null ? BigDecimal.ZERO : packagePrice;
        BigDecimal net = gross;
        List<StaffCompleteLine> lines = request.getLines() == null ? List.of() : request.getLines();
        for (int i = 0; i < lines.size(); i++) {
            BuiltLine built = toLine(lines.get(i), i, visitDate, request.getMileage(), lang);
            VisitServiceLine line = built.line();
            visit.addService(line);
            gross = gross.add(line.getCostAmount());
            net = net.add(line.getFinalAmount());
            if (line.getProductName() != null) {
                visit.addPart(VisitPart.builder()
                        .name(line.getProductName())
                        .qty(line.getProductQty())
                        .unit(built.productUnit())
                        .price(line.getProductAmount() == null ? null : productFinalOf(line))
                        .build());
            }
        }
        if (choice.id() != null) {
            for (OfferedService info : packageLines(choice.id()).info()) {
                visit.addService(VisitServiceLine.builder()
                        .kind(VisitLineKind.PACKAGE_INFO.apiValue())
                        .offeredServiceId(info.getId())
                        .serviceName(text(info.getTitleJson(), lang))
                        .doneDate(visitDate)
                        .doneKm(request.getMileage())
                        .costAmount(BigDecimal.ZERO)
                        .costCurrency(CURRENCY)
                        .finalAmount(BigDecimal.ZERO)
                        .build());
            }
        }

        BigDecimal total = overallPercent == null
                ? net
                : net.subtract(net.multiply(overallPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP));
        visit.setCostAmount(money(gross));
        visit.setFinalCostAmount(money(total));
        applyPackageChoice(booking, choice);
        visitRepository.saveAndFlush(visit);

        if (car.getMileage() == null || car.getMileage() < request.getMileage()) {
            // Müştəri tarixi lastMileageUpdateAt burada dəyişmir.
            car.setMileage(request.getMileage().longValue());
        }
        visitWebhookSupport.recalculateAllTimeCost(car);
        refreshServicedPartnerIds(car);
        detailedPercentageService.applyFromVisit(car, booking, visit);
        redisCacheService.evictCarAndHistoryAfterCommit(car);
        log.info("panel visit written | bookingId={} visitId={} carId={} lines={} gross={} total={}",
                booking.getId(), visit.getId(), car.getCarId(), visit.getServices().size(), gross, total);
        return visit;
    }

    private BuiltLine toLine(StaffCompleteLine in, int index, LocalDate visitDate, Integer visitKm, String lang) {
        if (in == null) {
            throw MissingFieldException.required("lines[" + index + "]");
        }
        VisitLineKind kind = VisitLineKind.fromApi(in.getKind());
        if (kind == null || kind == VisitLineKind.PACKAGE_INFO) {
            throw new MissingFieldException("lines[" + index + "].kind is invalid");
        }

        String title = null;
        String maintenanceCode = null;
        if (in.getOfferedServiceId() != null) {
            OfferedService service = offeredServiceRepository.findById(in.getOfferedServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("offered service not found"));
            title = text(service.getTitleJson(), lang);
            maintenanceCode = service.getMaintenanceCode();
        } else if (in.getIndividualServiceId() != null) {
            IndividualService service = individualServiceRepository.findById(in.getIndividualServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("individual service not found"));
            title = text(service.getTitleJson(), lang);
            maintenanceCode = service.getMaintenanceCode();
        }
        if (kind == VisitLineKind.ONE_TIME || kind == VisitLineKind.INSPECTION || title == null) {
            String typed = in.getTitle() == null ? "" : in.getTitle().trim();
            if (typed.isEmpty() && title == null) {
                // AC16: birdəfəlik xidmətdə ad boş qala bilməz.
                throw new MissingFieldException("lines[" + index + "].title is required");
            }
            if (!typed.isEmpty()) {
                title = typed.length() > TITLE_MAX ? typed.substring(0, TITLE_MAX) : typed;
            }
        }
        if (!kind.periodic()) {
            maintenanceCode = null;
        }

        boolean included = kind == VisitLineKind.PACKAGE;
        if (!included && in.getPrice() != null && in.getPrice().signum() < 0) {
            throw new MissingFieldException("lines[" + index + "].price must be >= 0");
        }
        String productName = blankToNull(in.getProductName());
        if (productName == null) {
            throw MissingFieldException.required("lines[" + index + "].productName");
        }
        String productUnit = requiredUnit(in.getProductUnit(), index);
        BigDecimal qty = nonNegative(in.getProductQty(), "lines[" + index + "].productQty");
        if (qty == null || qty.signum() == 0) {
            throw MissingFieldException.required("lines[" + index + "].productQty");
        }
        Long brandModelId = null;
        BigDecimal productGross = null;
        BigDecimal productDiscount = null;
        BigDecimal productFinal = null;
        if (!included) {
            productGross = nonNegative(in.getProductAmount(), "lines[" + index + "].productAmount");
            if (productGross == null) {
                throw MissingFieldException.required("lines[" + index + "].productAmount");
            }
            productDiscount = percent(in.getProductDiscountPercent(), "lines[" + index + "].productDiscountPercent");
            productFinal = nonNegative(in.getProductFinalAmount(), "lines[" + index + "].productFinalAmount");
        }
        if (!included && productName != null) {
            BigDecimal productBase = productGross == null ? BigDecimal.ZERO : productGross;
            if (productFinal == null) {
                productFinal = applyPercent(productBase, productDiscount);
            } else if (productFinal.compareTo(productBase) > 0) {
                throw new ConflictException("lines[" + index + "].productFinalAmount is greater than the amount");
            } else if (productBase.signum() > 0) {
                productDiscount = productBase.subtract(productFinal).multiply(HUNDRED)
                        .divide(productBase, 2, RoundingMode.HALF_UP);
            }
            productGross = productBase;
        } else {
            productFinal = null;
            productDiscount = null;
        }

        if (!included && in.getPrice() == null) {
            throw MissingFieldException.required("lines[" + index + "].price");
        }
        BigDecimal servicePrice = included ? BigDecimal.ZERO : nonNegative(in.getPrice(), "lines[" + index + "].price");
        BigDecimal serviceBase = servicePrice == null ? BigDecimal.ZERO : servicePrice;
        BigDecimal serviceDiscount = included ? null : percent(in.getDiscountPercent(), "lines[" + index + "].discountPercent");
        BigDecimal serviceFinal = included ? BigDecimal.ZERO : nonNegative(in.getFinalAmount(), "lines[" + index + "].finalAmount");
        if (included) {
            serviceFinal = BigDecimal.ZERO;
            serviceDiscount = null;
        } else if (serviceFinal == null) {
            serviceFinal = applyPercent(serviceBase, serviceDiscount);
        } else if (serviceFinal.compareTo(serviceBase) > 0) {
            throw new ConflictException("lines[" + index + "].finalAmount is greater than the amount");
        } else if (serviceBase.signum() > 0) {
            serviceDiscount = serviceBase.subtract(serviceFinal).multiply(HUNDRED)
                    .divide(serviceBase, 2, RoundingMode.HALF_UP);
        }

        BigDecimal productBase = productGross == null ? BigDecimal.ZERO : productGross;
        BigDecimal productNet = productFinal == null ? BigDecimal.ZERO : productFinal;
        BigDecimal gross = serviceBase.add(productBase);
        BigDecimal finalAmount = serviceFinal.add(productNet);

        return new BuiltLine(VisitServiceLine.builder()
                .kind(kind.apiValue())
                .offeredServiceId(in.getOfferedServiceId())
                .individualServiceId(in.getIndividualServiceId())
                .serviceName(title)
                .universalServiceId(maintenanceCode)
                .doneDate(in.getDate() != null ? in.getDate() : visitDate)
                .doneKm(in.getMileage() != null ? in.getMileage() : visitKm)
                .nextServiceDate(kind.periodic() ? in.getNextDate() : null)
                .nextServiceMileage(kind.periodic() ? in.getNextMileage() : null)
                .serviceAmount(money(servicePrice))
                .productGoodId(brandModelId)
                .productName(productName)
                .productQty(qty)
                .productAmount(money(productGross))
                .productDiscountPercent(productDiscount)
                .costAmount(money(gross))
                .costCurrency(CURRENCY)
                .discountPercent(serviceDiscount)
                .finalAmount(money(finalAmount))
                .build(), productUnit);
    }

    // ================= HELPERS =================

    private PackageLines packageLines(Long packageId) {
        List<OfferedService> periodic = new ArrayList<>();
        List<OfferedService> info = new ArrayList<>();
        if (packageId == null) {
            return new PackageLines(periodic, info);
        }
        for (BranchCarePackageItem item : carePackageItemRepository.findByCarePackage_IdOrderByIdAsc(packageId)) {
            OfferedService service = item.getOfferedService();
            if (!Boolean.TRUE.equals(item.getEnabled()) || service == null || !Boolean.TRUE.equals(service.getActive())) {
                continue;
            }
            periodic.add(service);
        }
        return new PackageLines(periodic, info);
    }

    /**
     * tr: syncPackage true ikən formadakı paket booking sətrinin və ziyarətin paketidir.
     *     Eyni id köhnə qiyməti saxlayır. Başqa id kataloqdan gəlir. Null paketi silir.
     * en: When syncPackage is true, the form's package is the booking row and the visit.
     *     The same id keeps the booked price. Another id comes from the catalog. Null removes it.
     */
    private PackageChoice packageChoice(Booking booking, StaffCompleteRequest request) {
        if (!Boolean.TRUE.equals(request.getSyncPackage())) {
            return keptWithCatalogPrice(booking);
        }
        Long id = request.getCarePackageId();
        if (id == null) {
            return PackageChoice.cleared(booking);
        }
        if (id.equals(booking.getCarePackageId())) {
            return keptWithCatalogPrice(booking);
        }
        BranchCarePackage pkg = carePackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("care package not found"));
        Branch branch = booking.getBranch();
        if (!Boolean.TRUE.equals(pkg.getActive())
                || pkg.getBranch() == null
                || branch == null
                || branch.getId() == null
                || !branch.getId().equals(pkg.getBranch().getId())) {
            throw new ResourceNotFoundException("care package not found");
        }
        int manat = pkg.getPrice() == null ? 0 : pkg.getPrice();
        return new PackageChoice(pkg.getId(), blankToNull(pkg.getName()),
                BigDecimal.valueOf(manat).setScale(2, RoundingMode.HALF_UP), true);
    }

    /** Moves the booked quote by the package-price delta and stores the chosen package. */
    private void applyPackageChoice(Booking booking, PackageChoice choice) {
        if (!choice.changed()) {
            return;
        }
        int oldQepik = booking.getPackagePrice() == null ? 0 : booking.getPackagePrice();
        int newQepik = choice.priceAzn() == null
                ? 0
                : choice.priceAzn().movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
        int delta = newQepik - oldQepik;
        if (delta != 0 && booking.getPriceMin() != null) {
            booking.setPriceMin(Math.max(0, booking.getPriceMin() + delta));
        }
        if (delta != 0 && booking.getPriceMax() != null) {
            booking.setPriceMax(Math.max(0, booking.getPriceMax() + delta));
        }
        booking.setCarePackageId(choice.id());
        booking.setPackageName(choice.name());
        booking.setPackagePrice(choice.id() == null ? null : newQepik);
        bookingRepository.save(booking);
    }

    private List<StaffCompletionPackageOption> packageCatalog(Branch branch, Car car, String lang) {
        if (branch == null || branch.getId() == null) {
            return List.of();
        }
        List<BranchCarePackage> rows = carePackageRepository.findByBranch_IdOrderByIdAsc(branch.getId());
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<StaffCompletionPackageOption> out = new ArrayList<>();
        for (BranchCarePackage row : rows) {
            if (!Boolean.TRUE.equals(row.getActive())) {
                continue;
            }
            PackageLines lines = packageLines(row.getId());
            List<StaffCompletionCardView> cards = new ArrayList<>();
            for (OfferedService service : lines.periodic()) {
                cards.add(packageCard(service, car, lang));
            }
            int manat = row.getPrice() == null ? 0 : row.getPrice();
            out.add(StaffCompletionPackageOption.builder()
                    .id(row.getId())
                    .name(row.getName())
                    .price(BigDecimal.valueOf(manat).setScale(2, RoundingMode.HALF_UP))
                    .infoLines(lines.info().stream().map(service -> text(service.getTitleJson(), lang)).toList())
                    .cards(cards)
                    .build());
        }
        return out;
    }

    private StaffCompletionCardView packageCard(OfferedService service, Car car, String lang) {
        return cardBuilder(VisitLineKind.PACKAGE.apiValue(), car, service.getMaintenanceCode())
                .offeredServiceId(service.getId())
                .title(text(service.getTitleJson(), lang))
                .groupTitle(service.getBehavior() == null ? null : text(service.getBehavior().getTitleJson(), lang))
                .maintenanceCode(service.getMaintenanceCode())
                .priceIncluded(true)
                .build();
    }

    private static String requiredUnit(String raw, int index) {
        if (raw == null || raw.isBlank()) {
            throw MissingFieldException.required("lines[" + index + "].productUnit");
        }
        String unit = raw.trim();
        if (!PRODUCT_UNITS.contains(unit)) {
            throw new MissingFieldException("lines[" + index + "].productUnit is invalid");
        }
        return unit;
    }

    private Map<Long, BranchIndividualService> branchIndividualPrices(Long branchId) {
        Map<Long, BranchIndividualService> out = new LinkedHashMap<>();
        for (BranchIndividualService row : branchIndividualServiceRepository.findByBranch_Id(branchId)) {
            if (row.getIndividualService() != null) {
                out.put(row.getIndividualService().getId(), row);
            }
        }
        return out;
    }

    /** Draft product list: Settings → Xidmətlərə uyğun marka və model (with unit prices). */
    private List<BookingBrandModelGroupView> brandModelGroups(Long branchId, String lang) {
        List<BrandModelService> headings = brandModelServiceRepository.findAllByOrderBySortOrderAscIdAsc();
        if (headings.isEmpty()) {
            return List.of();
        }
        Map<Long, List<BookingBrandChipView>> byHeading = new LinkedHashMap<>();
        for (BrandModel model : brandModelRepository.findByBranch_IdOrderByIdAsc(branchId)) {
            if (model.getBrandModelService() == null || model.getBrandModelService().getId() == null) {
                continue;
            }
            String label = model.getName();
            if (model.getSeries() != null && !model.getSeries().isBlank()) {
                label = label + " · " + model.getSeries();
            }
            byHeading
                    .computeIfAbsent(model.getBrandModelService().getId(), id -> new ArrayList<>())
                    .add(BookingBrandChipView.builder()
                            .id(model.getId())
                            .name(label)
                            .price(model.getPrice())
                            .build());
        }
        List<BookingBrandModelGroupView> groups = new ArrayList<>();
        for (BrandModelService heading : headings) {
            List<BookingBrandChipView> brands = byHeading.get(heading.getId());
            groups.add(BookingBrandModelGroupView.builder()
                    .id(heading.getId())
                    .title(text(heading.getTitleJson(), lang))
                    .brands(brands == null ? List.of() : brands)
                    .build());
        }
        return groups;
    }

    private StaffCompletionCardView individualCard(Long id, String title, String maintenanceCode,
                                                   Integer simple, Integer medium, Integer complex, Car car) {
        BigDecimal min = null;
        BigDecimal max = null;
        for (Integer price : new Integer[]{simple, medium, complex}) {
            if (price == null) {
                continue;
            }
            BigDecimal value = BigDecimal.valueOf(price);
            min = min == null || value.compareTo(min) < 0 ? value : min;
            max = max == null || value.compareTo(max) > 0 ? value : max;
        }
        return cardBuilder(VisitLineKind.INDIVIDUAL.apiValue(), car, maintenanceCode)
                .individualServiceId(id)
                .title(title)
                .maintenanceCode(maintenanceCode)
                .priceMin(min)
                .priceMax(max)
                .build();
    }

    private StaffCompletionCardView.StaffCompletionCardViewBuilder cardBuilder(String kind, Car car, String code) {
        NextOpen next = nextOpen(car, code);
        return StaffCompletionCardView.builder()
                .kind(kind)
                .nextDateOpen(next.date())
                .nextMileageOpen(next.mileage());
    }

    /**
     * tr: Bu avtomobilin şablonundakı intervalMonth / intervalKm. İkisi də boşdursa hər ikisi açıq.
     * en: intervalMonth / intervalKm on this car's template. Both open when both are empty.
     */
    private NextOpen nextOpen(Car car, String maintenanceCode) {
        if (car == null || maintenanceCode == null || maintenanceCode.isBlank()) {
            return NextOpen.both();
        }
        Long serviceId = bookingPercentageSnapshot.resolve(car, maintenanceCode);
        if (serviceId == null) {
            return NextOpen.both();
        }
        ServiceEntity service = serviceEntityRepository.findById(serviceId).orElse(null);
        if (service == null) {
            return NextOpen.both();
        }
        boolean date = service.getIntervalMonth() != null && service.getIntervalMonth() > 0;
        boolean km = service.getIntervalKm() != null && service.getIntervalKm() > 0;
        if (!date && !km) {
            return NextOpen.both();
        }
        return new NextOpen(date, km);
    }

    private static BigDecimal productFinalOf(VisitServiceLine line) {
        BigDecimal gross = line.getProductAmount() == null ? BigDecimal.ZERO : line.getProductAmount();
        return applyPercent(gross, line.getProductDiscountPercent());
    }

    private static BigDecimal applyPercent(BigDecimal amount, BigDecimal discount) {
        if (discount == null) {
            return amount;
        }
        return amount.subtract(amount.multiply(discount).divide(HUNDRED, 2, RoundingMode.HALF_UP));
    }

    private String inspectionNote(Booking booking) {
        BookingInspection inspection = inspectionRepository.findByBooking_Id(booking.getId()).orElse(null);
        return inspection == null ? null : clip(inspection.getMessage(), NOTE_MAX);
    }

    private Car findCar(Booking booking) {
        Car car = booking.getCarId() == null ? null : carRepository.findByCarId(booking.getCarId());
        if (car == null && booking.getVin() != null && !booking.getVin().isBlank()) {
            car = carByVin.findOne(booking.getVin());
        }
        return car;
    }

    /**
     * tr: Bookinqdəki qəpik kopyası. Boşdursa şöbə paketinin manat qiyməti.
     * en: The booking's qəpik copy. When that is empty, the branch package price in manat.
     */
    private BigDecimal resolvedPackagePrice(Booking booking) {
        BigDecimal stored = packagePriceAzn(booking);
        if (stored != null && stored.signum() > 0) {
            return stored;
        }
        BigDecimal catalog = catalogManat(booking.getCarePackageId());
        return catalog != null ? catalog : stored;
    }

    /** Same package as booked. A missing booked price is filled from the catalog and written back. */
    private PackageChoice keptWithCatalogPrice(Booking booking) {
        BigDecimal stored = packagePriceAzn(booking);
        if (stored != null && stored.signum() > 0) {
            return PackageChoice.kept(booking);
        }
        BigDecimal catalog = catalogManat(booking.getCarePackageId());
        if (catalog == null || catalog.signum() == 0 || booking.getCarePackageId() == null) {
            return PackageChoice.kept(booking);
        }
        String name = blankToNull(booking.getPackageName());
        if (name == null) {
            BranchCarePackage pkg = carePackageRepository.findById(booking.getCarePackageId()).orElse(null);
            name = pkg == null ? null : blankToNull(pkg.getName());
        }
        return new PackageChoice(booking.getCarePackageId(), name, catalog, true);
    }

    private BigDecimal catalogManat(Long packageId) {
        if (packageId == null) {
            return null;
        }
        BranchCarePackage pkg = carePackageRepository.findById(packageId).orElse(null);
        if (pkg == null || pkg.getPrice() == null) {
            return null;
        }
        return BigDecimal.valueOf(pkg.getPrice()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Booking stores the package price in qəpik. */
    private static BigDecimal packagePriceAzn(Booking booking) {
        if (booking.getPackagePrice() == null || booking.getPackageName() == null || booking.getPackageName().isBlank()) {
            return null;
        }
        return BigDecimal.valueOf(booking.getPackagePrice()).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /**
     * tr: Panel ziyareti sonrası partner ve şube rozet id'leri. Çağıran commit sonrası carlist ve history siler.
     * en: Partner and branch badge ids after a panel visit. The caller drops carlist and history after commit.
     */
    private void refreshServicedPartnerIds(Car car) {
        List<Visit> visits = visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car);
        if (ServicedBadgeIds.applyIfChanged(car, visits)) {
            carRepository.save(car);
        }
    }

    private String text(String titleJson, String lang) {
        return BookingMineService.catalogText(json.read(titleJson), lang);
    }

    private static ZoneId zone(String timezone) {
        try {
            return ZoneId.of(timezone == null || timezone.isBlank() ? StaffBookingService.DEFAULT_TZ : timezone.trim());
        } catch (Exception ex) {
            return ZoneId.of(StaffBookingService.DEFAULT_TZ);
        }
    }

    /** AC17: yalnız rəqəm, mənfi yox. */
    private static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0) {
            throw new MissingFieldException(field + " must be >= 0");
        }
        return value;
    }

    private static BigDecimal percent(BigDecimal value, String field) {
        if (value == null || value.signum() == 0) {
            return null;
        }
        if (value.signum() < 0 || value.compareTo(HUNDRED) > 0) {
            throw new MissingFieldException(field + " must be between 0 and 100");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String clip(String value, int max) {
        String trimmed = blankToNull(value);
        return trimmed == null || trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
