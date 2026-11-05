package com.carland.carland_service.digitalhistory;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.entity.VisitPart;
import com.carland.carland_service.entity.VisitServiceLine;
import com.carland.carland_service.idda.IddaPlateCheck;
import com.carland.carland_service.idda.IddaPlateCheckRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import com.carland.carland_service.repository.PartnerRepository;
import com.carland.carland_service.repository.VisitRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalHistoryReportLoaderTest {

    @Mock CarRepository cars;
    @Mock VisitRepository visits;
    @Mock BranchRepository branches;
    @Mock PartnerRepository partners;
    @Mock ColorRepository colors;
    @Mock IddaPlateCheckRepository idda;
    @InjectMocks DigitalHistoryReportLoader loader;

    @Test
    void reportUsesVisitKindsAndSkipsIddaOwner() throws Exception {
        Car car = Car.builder()
                .carId(7L)
                .vin("JTNB23HK5R3164829")
                .brand("Toyota")
                .model("Camry")
                .modelYear(2025)
                .plateNumber("88NM888")
                .mileage(89128L)
                .engineVolume(2487)
                .colorId(3L)
                .bodyType("Minik")
                .build();
        when(cars.findByCarId(7L)).thenReturn(car);
        when(colors.findById(3L)).thenReturn(Optional.of(Color.builder().az("Ağ").color("White").build()));

        IddaPlateCheck restricted = IddaPlateCheck.builder()
                .statusCode(0)
                .statusName("ServiceRestrictedByPerson")
                .vin("JTNB23HK5R3164829")
                .certificateNumber("SHOULD_NOT")
                .engineNumber("SHOULD_NOT")
                .ownerName("SECRET OWNER")
                .build();
        IddaPlateCheck success = IddaPlateCheck.builder()
                .statusCode(0)
                .vin("JTNB23HK5R3164829")
                .certificateNumber("BC481273")
                .certificateGivenDate("03.11.2023")
                .engineNumber("A25A4729361")
                .ownerName("ALSO SECRET")
                .build();
        when(idda.findByVinNewestFirst("JTNB23HK5R3164829")).thenReturn(List.of(restricted, success));

        Branch branch = Branch.builder()
                .id(10L)
                .name("Toyota Bakı Mərkəzi")
                .address("F. Bayramov küç. 26")
                .contactPhone("012 496 70 10")
                .contactEmail("info@toyotabaku.az")
                .build();
        when(branches.findAllById(any())).thenReturn(List.of(branch));

        Visit panel = Visit.builder()
                .bookingId(5L)
                .source("panel")
                .branchId(10L)
                .serviceCenterId(1L)
                .serviceCenterName("Toyota")
                .lastServiceDate(LocalDate.of(2025, 2, 27))
                .lastServiceMileage(89128)
                .packageName("Extra")
                .inspectionNote("səs gəlir")
                .finalCostAmount(new BigDecimal("385.00"))
                .finalCostCurrency("AZN")
                .services(List.of(
                        VisitServiceLine.builder().id(1L).kind("package").serviceName("Yağ dəyişdirilib").build(),
                        VisitServiceLine.builder().id(2L).kind("individual").serviceName("Filtr").productName("Salon filteri").productQty(new BigDecimal("1")).build(),
                        VisitServiceLine.builder().id(3L).kind("inspection").serviceName("Diaqnostika").build()
                ))
                .parts(List.of())
                .build();
        Visit hyper = Visit.builder()
                .source("hyper")
                .serviceCenterName("Hyper Service")
                .lastServiceDate(LocalDate.of(2024, 1, 19))
                .lastServiceMileage(49900)
                .finalCostAmount(new BigDecimal("10.00"))
                .finalCostCurrency("AZN")
                .services(List.of(VisitServiceLine.builder().id(4L).serviceName("Antifriz").build()))
                .parts(List.of(VisitPart.builder().id(8L).name("Antifriz").qty(new BigDecimal("5")).unit("L").build()))
                .build();
        when(visits.findAllByCarOrderByLastServiceDateDescIdDesc(car)).thenReturn(List.of(panel, hyper));

        DigitalHistoryReport report = loader.load(7L);

        assertEquals("BC481273", report.certificateNumber());
        assertEquals("A25A4729361", report.engineNumber());
        assertEquals("Ağ", report.color());
        assertEquals("2 487 sm³", report.engineVolume());
        assertEquals("89 128 km", report.mileage());
        assertEquals(2, report.serviceCount());
        assertEquals("395,00 AZN", report.grandTotal());

        DigitalHistoryReport.VisitBlock panelBlock = report.visits().get(0);
        assertEquals("Dövri qulluq + Təmir və diaqnostika", panelBlock.label());
        assertEquals(List.of("Yağ dəyişdirilib"), panelBlock.packageLines());
        assertEquals(List.of("Filtr · Salon filteri · 1"), panelBlock.individualLines());
        assertEquals("səs gəlir", panelBlock.inspectionNote());
        assertEquals(List.of("Diaqnostika"), panelBlock.inspectionLines());
        assertEquals("Toyota Bakı Mərkəzi", panelBlock.centerName());

        DigitalHistoryReport.VisitBlock hyperBlock = report.visits().get(1);
        assertNull(hyperBlock.label());
        assertTrue(hyperBlock.plainLines().contains("Antifriz"));
        assertTrue(hyperBlock.plainLines().contains("Antifriz · 5 · L"));
        assertTrue(hyperBlock.packageLines().isEmpty());

        String text = pdfText(new DigitalHistoryPdf().render(report));
        assertTrue(text.contains("JTNB23HK5R3164829"));
        assertTrue(text.contains("BC481273"));
        assertTrue(text.contains("Fərdi xidmətlər"));
        assertTrue(text.contains("Diaqnostika"));
        assertTrue(text.contains("Antifriz"));
        assertFalse(text.contains("SECRET"));
        assertFalse(text.contains("SHOULD_NOT"));
    }

    private static String pdfText(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
