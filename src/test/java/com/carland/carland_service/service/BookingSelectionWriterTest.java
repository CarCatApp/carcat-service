package com.carland.carland_service.service;

import com.carland.carland_service.entity.Booking;
import com.carland.carland_service.entity.BookingIndividualLine;
import com.carland.carland_service.entity.BookingInspection;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchIndividualService;
import com.carland.carland_service.entity.Calendar;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.IndividualService;
import com.carland.carland_service.entity.Partner;
import com.carland.carland_service.entity.Range;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BookingInspectionRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingSelectionWriterTest {

    @Mock BranchIndividualServiceRepository branchIndividualServiceRepository;
    @Mock BookingIndividualLineRepository lineRepository;
    @Mock BookingInspectionRepository inspectionRepository;

    BookingSelectionWriter writer;
    Branch branch;
    IndividualService oil;

    @BeforeEach
    void setUp() {
        writer = new BookingSelectionWriter(
                branchIndividualServiceRepository, lineRepository, inspectionRepository);
        Partner partner = Partner.builder().id(1L).name("Hyper").active(true).build();
        branch = Branch.builder().id(12L).name("Xeqani").active(true).partner(partner).build();
        oil = IndividualService.builder()
                .id(3L)
                .code("MYF")
                .titleJson("{\"az\":\"Yağ dəyişimi\",\"en\":\"Oil change\"}")
                .active(true)
                .build();
    }

    @Test
    void saveCopiesInspectionAndServiceLine() {
        BranchIndividualService row = BranchIndividualService.builder()
                .branch(branch)
                .individualService(oil)
                .active(true)
                .priceSimple(35)
                .priceComplex(75)
                .build();
        when(branchIndividualServiceRepository.findByBranch_IdAndIndividualService_Id(12L, 3L))
                .thenReturn(Optional.of(row));
        when(inspectionRepository.save(any(BookingInspection.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingSelectionWriter.Priced priced = writer.price(branch, List.of(3L), "  Generator akkumulyatoru doldurmur  ");
        Customer customer = Customer.builder().userId(77L).name("Aysu").surname("İsmayılov").phoneNumber("+994501112233").build();
        Car car = Car.builder().carId(55L).vin("EXFR0XNVBUXU61NHE").plateNumber("50-NE-805").brand("Ford").model("F-150").modelYear(2020).build();
        Calendar calendar = Calendar.builder().day(LocalDate.of(2026, 10, 2)).branch(branch).build();
        Range range = Range.builder()
                .rangeId(8801L)
                .start(OffsetDateTime.parse("2026-10-02T12:00:00Z"))
                .calendar(calendar)
                .build();
        Booking booking = Booking.builder().id(501L).ref("CC-522109").customerUserId(77L).branch(branch).range(range).build();

        BookingInspection saved = writer.save(booking, customer, car, priced);

        assertEquals(3500, priced.priceMin());
        assertEquals(7500, priced.priceMax());
        assertEquals("Generator akkumulyatoru doldurmur", saved.getMessage());
        assertEquals(77L, saved.getCustomerUserId());
        assertEquals("Aysu İsmayılov", saved.getCustomerName());
        assertEquals(501L, saved.getBooking().getId());
        assertEquals(8801L, saved.getRangeId());
        assertEquals(LocalDate.of(2026, 10, 2), saved.getSlotDay());
        assertEquals(12L, saved.getBranchId());
        assertEquals("Xeqani", saved.getBranchName());
        assertEquals("CC-522109", saved.getBookingRef());
        assertEquals("50-NE-805", saved.getPlateNumber());
        ArgumentCaptor<BookingIndividualLine> line = ArgumentCaptor.forClass(BookingIndividualLine.class);
        verify(lineRepository).save(line.capture());
        assertEquals(3L, line.getValue().getIndividualServiceId());
        assertEquals("MYF", line.getValue().getCode());
        assertEquals("Dövri Qulluq + Təmir Xidməti və Yoxlanış",
                BookingKindLabel.of(true, true));
    }

    @Test
    void longIssueIsRejected() {
        assertThrows(MissingFieldException.class, () -> writer.price(branch, List.of(), "x".repeat(501)));
    }
}
