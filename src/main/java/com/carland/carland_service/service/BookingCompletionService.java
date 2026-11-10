package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.BookingBrandChipView;
import com.carland.carland_service.dto.booking.BookingBrandModelGroupView;
import com.carland.carland_service.dto.booking.StaffCompletionCardView;
import com.carland.carland_service.dto.booking.StaffCompletionDraftView;
import com.carland.carland_service.dto.request.StaffCompleteRequest;
import com.carland.carland_service.dto.request.StaffCompleteRequest.StaffCompleteLine;
import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.BookingInspection;
import com.carland.carland_service.entity.BookingSelectedService;
import com.carland.carland_service.entity.Branch;
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
import com.carland.carland_service.repository.BookingSelectedServiceRepository;
import com.carland.carland_service.repository.BranchCarePackageItemRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /** Package line split: periodic lines become cards, the rest are info lines. */
    private record PackageLines(List<OfferedService> periodic, List<OfferedService> info) {
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
        PackageLines pkg = packageLines(booking);
        for (OfferedService service : pkg.periodic()) {
            cards.add(cardBuilder(VisitLineKind.PACKAGE.apiValue(), car, service.getMaintenanceCode())
                    .offeredServiceId(service.getId())
                    .title(text(service.getTitleJson(), lang))
                    .groupTitle(service.getBehavior() == null ? null : text(service.getBehavior().getTitleJson(), lang))
                    .maintenanceCode(service.getMaintenanceCode())
                    .priceIncluded(true)
                    .build());
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
                .packageName(blankToNull(booking.getPackageName()))
                .packagePrice(packagePriceAzn(booking))
                .packageInfoLines(pkg.info().stream().map(s -> text(s.getTitleJson(), lang)).toList())
                .inspectionNote(inspectionNote)
                .cards(cards)
                .individualCatalog(catalog)
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
        BigDecimal packagePrice = packagePriceAzn(booking);

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
                .packageName(blankToNull(booking.getPackageName()))
                .packagePrice(packagePrice)
                .discountPercent(overallPercent)
                .inspectionNote(inspectionNote(booking))
                .notes(clip(request.getNotes(), NOTE_MAX))
                .costCurrency(CURRENCY)
                .finalCostCurrency(CURRENCY)
                .createdAt(LocalDateTime.now())
                .build();

        Map<Long, BrandModel> models = brandModelsById(branch.getId());
        BigDecimal gross = packagePrice == null ? BigDecimal.ZERO : packagePrice;
        BigDecimal net = gross;
        List<StaffCompleteLine> lines = request.getLines() == null ? List.of() : request.getLines();
        for (int i = 0; i < lines.size(); i++) {
            VisitServiceLine line = toLine(lines.get(i), i, visitDate, request.getMileage(), models, lang);
            visit.addService(line);
            gross = gross.add(line.getCostAmount());
            net = net.add(line.getFinalAmount());
            if (line.getProductName() != null) {
                visit.addPart(VisitPart.builder()
                        .name(line.getProductName())
                        .qty(line.getProductQty())
                        .price(line.getProductAmount() == null ? null : productFinalOf(line))
                        .build());
            }
        }
        for (OfferedService info : packageLines(booking).info()) {
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

        BigDecimal total = overallPercent == null
                ? net
                : net.subtract(net.multiply(overallPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP));
        visit.setCostAmount(money(gross));
        visit.setFinalCostAmount(money(total));
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

    private VisitServiceLine toLine(StaffCompleteLine in, int index, LocalDate visitDate, Integer visitKm,
                                    Map<Long, BrandModel> models, String lang) {
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
        String productName = blankToNull(in.getProductName());
        BigDecimal qty = nonNegative(in.getProductQty(), "lines[" + index + "].productQty");
        BigDecimal productGross = included ? null : nonNegative(in.getProductAmount(), "lines[" + index + "].productAmount");
        Long brandModelId = in.getBrandModelId();
        if (brandModelId != null) {
            BrandModel model = models.get(brandModelId);
            if (model == null) {
                throw new ResourceNotFoundException("brand model not found");
            }
            if (productName == null) {
                productName = brandLabel(model, lang);
            }
            if (!included && productGross == null && model.getPrice() != null) {
                productGross = model.getPrice().multiply(qty == null ? BigDecimal.ONE : qty);
            }
        }
        if (productName == null) {
            productGross = null;
            qty = null;
            brandModelId = null;
        } else if (qty == null) {
            qty = BigDecimal.ONE;
        }

        BigDecimal productDiscount = included
                ? null
                : percent(in.getProductDiscountPercent(), "lines[" + index + "].productDiscountPercent");
        BigDecimal productFinal = included
                ? null
                : nonNegative(in.getProductFinalAmount(), "lines[" + index + "].productFinalAmount");
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

        return VisitServiceLine.builder()
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
                .build();
    }

    // ================= HELPERS =================

    private PackageLines packageLines(Booking booking) {
        List<OfferedService> periodic = new ArrayList<>();
        List<OfferedService> info = new ArrayList<>();
        if (booking.getCarePackageId() == null) {
            return new PackageLines(periodic, info);
        }
        for (BranchCarePackageItem item : carePackageItemRepository.findByCarePackage_IdOrderByIdAsc(booking.getCarePackageId())) {
            OfferedService service = item.getOfferedService();
            if (!Boolean.TRUE.equals(item.getEnabled()) || service == null || !Boolean.TRUE.equals(service.getActive())) {
                continue;
            }
            (Boolean.TRUE.equals(service.getPeriodic()) ? periodic : info).add(service);
        }
        return new PackageLines(periodic, info);
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

    private Map<Long, BrandModel> brandModelsById(Long branchId) {
        Map<Long, BrandModel> out = new HashMap<>();
        for (BrandModel model : brandModelRepository.findByBranch_IdOrderByIdAsc(branchId)) {
            out.put(model.getId(), model);
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

    private String brandLabel(BrandModel model, String lang) {
        BrandModelService heading = model.getBrandModelService();
        String head = heading == null ? null : text(heading.getTitleJson(), lang);
        String name = model.getName();
        if (model.getSeries() != null && !model.getSeries().isBlank()) {
            name = name + " · " + model.getSeries();
        }
        return head == null || head.isBlank() ? name : head + " · " + name;
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
