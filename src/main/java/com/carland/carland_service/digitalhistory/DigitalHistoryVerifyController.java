package com.carland.carland_service.digitalhistory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * tr: {@code {public-base-url}/verify/{nömrə}}. Login yoxdur. Kong bu yolu JWT-siz buraxır.
 * en: {@code {public-base-url}/verify/{number}}. No login. Kong lets this path through without JWT.
 */
@RestController
public class DigitalHistoryVerifyController {

    private final DigitalHistoryIssuer issuer;
    private final DigitalHistoryPdf pdf;
    private final String publicBaseUrl;

    public DigitalHistoryVerifyController(
            DigitalHistoryIssuer issuer,
            DigitalHistoryPdf pdf,
            @Value("${digital-history.public-base-url}") String publicBaseUrl) {
        this.issuer = issuer;
        this.pdf = pdf;
        this.publicBaseUrl = publicBaseUrl;
    }

    @GetMapping({"/verify/{code}", "/api/v1/car/digital-history/verify/{code}"})
    public ResponseEntity<String> page(@PathVariable String code) {
        return issuer.findByCode(code)
                .map(report -> ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .header(HttpHeaders.CACHE_CONTROL, "no-store")
                        .body(DigitalHistoryVerifyHtml.page(report, DigitalHistoryPaths.verifyUrl(publicBaseUrl, code) + "/pdf")))
                .orElseGet(() -> ResponseEntity.status(404)
                        .contentType(MediaType.TEXT_HTML)
                        .body(DigitalHistoryVerifyHtml.missing()));
    }

    @GetMapping({"/verify/{code}/pdf", "/api/v1/car/digital-history/verify/{code}/pdf"})
    public ResponseEntity<?> file(@PathVariable String code) {
        return issuer.findByCode(code)
                .<ResponseEntity<?>>map(report -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"digital-history.pdf\"")
                        .header(HttpHeaders.CACHE_CONTROL, "no-store")
                        .body(pdf.render(report)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .contentType(MediaType.TEXT_HTML)
                        .body(DigitalHistoryVerifyHtml.missing()));
    }
}
