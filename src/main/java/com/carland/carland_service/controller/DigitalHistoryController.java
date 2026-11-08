package com.carland.carland_service.controller;

import com.carland.carland_service.digitalhistory.DigitalHistoryIssuer;
import com.carland.carland_service.digitalhistory.DigitalHistoryLinkService;
import com.carland.carland_service.digitalhistory.DigitalHistoryPdf;
import com.carland.carland_service.digitalhistory.DigitalHistoryTokenException;
import com.carland.carland_service.digitalhistory.DigitalHistoryTokens;
import com.carland.carland_service.dto.response.DigitalHistoryLinkResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * tr: Sahip paylaşım adresi üretir. QR o adresi taşır. Tıklayan tarayıcı PDF'i açar.
 *     PDF isteğinde Authorization yoktur; sır query'deki {@code jwt} değeridir.
 * en: The owner mints a share URL. The QR carries that URL. A tap opens the PDF in the browser.
 *     The PDF request has no Authorization header; the secret is the {@code jwt} query value.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/car")
@RequiredArgsConstructor
public class DigitalHistoryController {

    private final DigitalHistoryLinkService linkService;
    private final DigitalHistoryTokens tokens;
    private final DigitalHistoryIssuer issuer;
    private final DigitalHistoryPdf pdf;

    @Operation(summary = "Digital history share URL for a logo QR")
    @PostMapping("/{carId}/digital-history/link")
    public DigitalHistoryLinkResponse link(
            @PathVariable Long carId,
            @RequestHeader("Authorization") String token,
            @RequestHeader("phoneNumber") String phoneNumber,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        log.info("POST /car/{}/digital-history/link | userId={}", carId, userIdHeader);
        return linkService.create(carId, phoneNumber, userIdHeader, acceptLanguage);
    }

    @Operation(summary = "Digital history PDF opened from the share link")
    @GetMapping("/digital-history")
    public ResponseEntity<?> pdf(@RequestParam(name = "jwt", required = false) String jwt) {
        final long issueId;
        try {
            issueId = tokens.parseIssueId(jwt);
        } catch (DigitalHistoryTokenException ex) {
            log.warn("digital history pdf rejected | expired={}", ex.isExpired());
            String message = ex.isExpired() ? "Keçidin müddəti bitib" : "Keçid etibarsızdır";
            return ResponseEntity.status(401)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", message));
        }
        return issuer.find(issueId)
                .<ResponseEntity<?>>map(this::pdfResponse)
                .orElseGet(() -> {
                    log.warn("digital history pdf missing | issueId={}", issueId);
                    return ResponseEntity.status(401)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(Map.of("error", "Keçid etibarsızdır"));
                });
    }

    private ResponseEntity<byte[]> pdfResponse(com.carland.carland_service.digitalhistory.DigitalHistoryReport report) {
        log.info("GET /car/digital-history | report={}", report.reportNo());
        byte[] body = pdf.render(report);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"digital-history.pdf\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }
}
