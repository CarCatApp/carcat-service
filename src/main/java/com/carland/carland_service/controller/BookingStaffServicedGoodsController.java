package com.carland.carland_service.controller;

import com.carland.carland_service.dto.booking.BookingBrandModelsResponse;
import com.carland.carland_service.dto.request.StaffServicedGoodSaveRequest;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.BranchServicedGoodsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BookingStaffServicedGoodsController {

    private final BookingStaffRequestAuth bookingStaffRequestAuth;
    private final BranchServicedGoodsService branchServicedGoodsService;

    @GetMapping("/api/v1/booking/staff/branch/serviced-goods")
    public BookingBrandModelsResponse list(
            HttpServletRequest request,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchServicedGoodsService.list(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                acceptLanguage);
    }

    @PostMapping("/api/v1/booking/staff/branch/serviced-goods/{headingId}")
    public BookingBrandModelsResponse add(
            HttpServletRequest request,
            @PathVariable Long headingId,
            @RequestBody StaffServicedGoodSaveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchServicedGoodsService.add(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                headingId,
                body,
                acceptLanguage);
    }

    @PatchMapping("/api/v1/booking/staff/branch/serviced-goods/{headingId}/{goodId}")
    public BookingBrandModelsResponse update(
            HttpServletRequest request,
            @PathVariable Long headingId,
            @PathVariable Long goodId,
            @RequestBody StaffServicedGoodSaveRequest body,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchServicedGoodsService.update(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                headingId,
                goodId,
                body,
                acceptLanguage);
    }

    @DeleteMapping("/api/v1/booking/staff/branch/serviced-goods/{headingId}/{goodId}")
    public BookingBrandModelsResponse delete(
            HttpServletRequest request,
            @PathVariable Long headingId,
            @PathVariable Long goodId,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return branchServicedGoodsService.delete(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                headingId,
                goodId,
                acceptLanguage);
    }
}
