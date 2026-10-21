package com.carland.carland_service.digitalfinance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * tr: PO gösterimi için Digital Finance bilgi servisleri. Cevap gövdesi upstream JSON'unun aynısıdır.
 *     Bu controller silinince çağrı DigitalFinanceClient üzerinden kalır.
 * en: Digital Finance info services for the PO demo. The response body is the upstream JSON unchanged.
 *     When this controller is removed, calls stay on DigitalFinanceClient.
 */
@RestController
@RequestMapping("/api/v1/digital-finance")
@RequiredArgsConstructor
@Tag(name = "digital-finance", description = "Pass-through to Digital Finance prod. Response body is the upstream JSON.")
public class DigitalFinanceController {

    private final DigitalFinanceClient digitalFinanceClient;

    @GetMapping("/info/balance")
    @Operation(summary = "Balance", description = "GET /api/v1/Info/Balance. Body is the upstream JSON.")
    public void balance(
            @RequestHeader(value = "RequestIdentifier", required = false) String requestIdentifier,
            HttpServletResponse response) throws IOException {
        digitalFinanceClient.forward("GET", "/api/v1/Info/Balance", null, requestIdentifier, response);
    }

    @PostMapping("/info/expenses")
    @Operation(summary = "Expenses", description = "POST /api/v1/Info/Expenses. Request and response JSON are passed through.")
    public void expenses(
            @RequestHeader(value = "RequestIdentifier", required = false) String requestIdentifier,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        digitalFinanceClient.forward(
                "POST", "/api/v1/Info/Expenses", request.getInputStream().readAllBytes(), requestIdentifier, response);
    }

    @PostMapping("/info/payments")
    @Operation(summary = "Payments", description = "POST /api/v1/Info/Payments. Request and response JSON are passed through.")
    public void payments(
            @RequestHeader(value = "RequestIdentifier", required = false) String requestIdentifier,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        digitalFinanceClient.forward(
                "POST", "/api/v1/Info/Payments", request.getInputStream().readAllBytes(), requestIdentifier, response);
    }

    @GetMapping("/din/vehicle/{vehicleNumber}")
    @Operation(summary = "Vehicle by plate", description = "GET /api/v1/DIN/vehicle/{vehicleNumber}. Body is the upstream JSON.")
    public void vehicleByPlate(
            @PathVariable String vehicleNumber,
            @RequestHeader(value = "RequestIdentifier", required = false) String requestIdentifier,
            @RequestHeader(value = "RequestPurposeId", required = false) String requestPurposeId,
            HttpServletResponse response) throws IOException {
        String plate = URLEncoder.encode(vehicleNumber, StandardCharsets.UTF_8).replace("+", "%20");
        digitalFinanceClient.forward(
                "GET", "/api/v1/DIN/vehicle/" + plate, null, requestIdentifier, requestPurposeId, response);
    }

    @PostMapping("/din/vehicle")
    @Operation(summary = "Vehicle by certificate", description = "POST /api/v1/DIN/vehicle. CertNumber, IssueDate, Otp, OtpIssueDate pass through.")
    public void vehicleByCertificate(
            @RequestHeader(value = "RequestIdentifier", required = false) String requestIdentifier,
            @RequestHeader(value = "RequestPurposeId", required = false) String requestPurposeId,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        digitalFinanceClient.forward(
                "POST",
                "/api/v1/DIN/vehicle",
                request.getInputStream().readAllBytes(),
                requestIdentifier,
                requestPurposeId,
                response);
    }
}
