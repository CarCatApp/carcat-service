package com.carland.carland_service.digitalhistory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * tr: Paylaşım yeni nömrə və snapshot yazır. PDF və yoxlama səhifəsi həmin mətni oxuyur.
 * en: A share writes a new number and a snapshot. The PDF and the verify page read that text.
 */
@Service
@RequiredArgsConstructor
public class DigitalHistoryIssuer {

    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final DigitalHistoryReportLoader loader;
    private final DigitalHistoryIssueRepository reports;
    private final DigitalHistoryCodes codes;
    private final ObjectMapper mapper;

    public long issue(long carId, String notificationLanguage) {
        String lang = DigitalHistoryLang.normalize(notificationLanguage);
        DigitalHistoryReport report = loader.load(carId, lang);
        ZonedDateTime now = ZonedDateTime.now(BAKU);
        String stamp = STAMP.format(now);
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = codes.next(now.getYear());
            if (reports.existsByCode(code)) {
                continue;
            }
            DigitalHistoryIssue row = new DigitalHistoryIssue();
            row.setCode(code);
            row.setCarId(carId);
            row.setIssuedAt(now.toLocalDateTime());
            row.setLang(lang);
            row.setBody(mapper.convertValue(DigitalHistorySnapshot.from(report.withIssue(code, stamp)), MAP));
            try {
                return reports.save(row).getId();
            } catch (DataIntegrityViolationException ex) {
                // başqa paylaşım eyni kodu yazdı
            }
        }
        throw new IllegalStateException("digital history code");
    }

    public Optional<DigitalHistoryReport> find(long id) {
        return reports.findById(id).map(this::report);
    }

    public Optional<DigitalHistoryReport> findByCode(String code) {
        if (!DigitalHistoryCodes.matches(code)) {
            return Optional.empty();
        }
        return reports.findByCode(code).map(this::report);
    }

    private DigitalHistoryReport report(DigitalHistoryIssue row) {
        DigitalHistorySnapshot snapshot = mapper.convertValue(row.getBody(), DigitalHistorySnapshot.class);
        LocalDateTime issued = row.getIssuedAt();
        byte[] photo = loader.readyPhoto(row.getCarId());
        DigitalHistoryReport report = snapshot.toReport(photo);
        if (report.issuedAt() == null || report.issuedAt().isBlank()) {
            String stamp = issued == null ? "" : STAMP.format(issued);
            return report.withIssue(row.getCode(), stamp);
        }
        return report;
    }
}
