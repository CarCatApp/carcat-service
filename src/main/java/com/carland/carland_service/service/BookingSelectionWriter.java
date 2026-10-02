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
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingIndividualLineRepository;
import com.carland.carland_service.repository.BookingInspectionRepository;
import com.carland.carland_service.repository.BranchIndividualServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * tr: Book'a fərdi xidmət satırları ve təmir kaydı yazar.
 * en: Writes individual-service lines and the inspection record onto a booking.
 */
@Service
@RequiredArgsConstructor
public class BookingSelectionWriter {

    static final int ISSUE_MAX = 500;

    private final BranchIndividualServiceRepository branchIndividualServiceRepository;
    private final BookingIndividualLineRepository lineRepository;
    private final BookingInspectionRepository inspectionRepository;

    public Priced price(Branch branch, List<Long> individualServiceIds, String issue) {
        String message = message(issue);
        List<BranchIndividualService> rows = new ArrayList<>();
        int min = 0;
        int max = 0;
        for (Long id : distinct(individualServiceIds)) {
            BranchIndividualService row = branchIndividualServiceRepository
                    .findByBranch_IdAndIndividualService_Id(branch.getId(), id)
                    .orElseThrow(() -> new ResourceNotFoundException("individual service not found"));
            IndividualService catalog = row.getIndividualService();
            if (!Boolean.TRUE.equals(row.getActive())
                    || catalog == null
                    || !Boolean.TRUE.equals(catalog.getActive())) {
                throw new ResourceNotFoundException("individual service not found");
            }
            int[] qepik = BookingSelectionViews.qepik(row.getPriceSimple(), row.getPriceMedium(), row.getPriceComplex());
            if (qepik != null) {
                min += qepik[0];
                max += qepik[1];
            }
            rows.add(row);
        }
        return new Priced(rows, message, min, max);
    }

    public BookingInspection save(Booking booking, Customer customer, Car car, Priced priced) {
        if (priced == null || booking == null) {
            return null;
        }
        for (BranchIndividualService row : priced.rows) {
            IndividualService catalog = row.getIndividualService();
            lineRepository.save(BookingIndividualLine.builder()
                    .booking(booking)
                    .individualServiceId(catalog.getId())
                    .code(catalog.getCode())
                    .titleJson(catalog.getTitleJson())
                    .priceSimple(row.getPriceSimple())
                    .priceMedium(row.getPriceMedium())
                    .priceComplex(row.getPriceComplex())
                    .build());
        }
        if (priced.message == null) {
            return null;
        }
        Range range = booking.getRange();
        Calendar calendar = range == null ? null : range.getCalendar();
        Branch branch = booking.getBranch();
        Partner partner = branch == null ? null : branch.getPartner();
        return inspectionRepository.save(BookingInspection.builder()
                .booking(booking)
                .customerUserId(customer == null ? booking.getCustomerUserId() : customer.getUserId())
                .customerName(customerName(customer))
                .customerPhone(customer == null ? null : customer.getPhoneNumber())
                .rangeId(range == null ? null : range.getRangeId())
                .slotDay(calendar == null ? null : calendar.getDay())
                .slotStart(range == null ? null : range.getStart())
                .branchId(branch == null ? null : branch.getId())
                .branchName(branch == null ? null : branch.getName())
                .partnerId(partner == null ? null : partner.getId())
                .bookingRef(booking.getRef())
                .message(priced.message)
                .vin(car == null ? booking.getVin() : car.getVin())
                .carId(car == null ? booking.getCarId() : car.getCarId())
                .plateNumber(car == null ? null : car.getPlateNumber())
                .carBrand(car == null ? null : car.getBrand())
                .carModel(car == null ? null : car.getModel())
                .carYear(car == null ? null : car.getModelYear())
                .build());
    }

    static String message(String issue) {
        if (issue == null) {
            return null;
        }
        String trimmed = issue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > ISSUE_MAX) {
            throw new MissingFieldException("issue is too long");
        }
        return trimmed;
    }

    private static List<Long> distinct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                seen.add(id);
            }
        }
        return List.copyOf(seen);
    }

    private static String customerName(Customer customer) {
        if (customer == null) {
            return null;
        }
        String name = customer.getName() == null ? "" : customer.getName().trim();
        String surname = customer.getSurname() == null ? "" : customer.getSurname().trim();
        String joined = (name + " " + surname).trim();
        return joined.isEmpty() ? null : joined;
    }

    public record Priced(List<BranchIndividualService> rows, String message, int priceMin, int priceMax) {}
}
