package com.carland.carland_service.dto.commission;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class CommissionExport {

    private final LocalDate from;
    private final LocalDate to;
    private final String filters;
    private final List<Line> lines;
    private final CommissionScreen.Bucket fresh;
    private final CommissionScreen.Bucket again;
    private final CommissionScreen.Bucket all;
    private final long pendingCount;
    private final List<CommissionScreen.PartnerGroup> groups;

    @Getter
    @AllArgsConstructor
    public static class Line {
        private final String completedAt;
        private final String partnerName;
        private final String branchName;
        private final String vin;
        private final String plate;
        private final String statusLabel;
        private final BigDecimal serviceAmount;
        private final BigDecimal ratePercent;
        private final BigDecimal commissionAmount;
        private final boolean pending;
        private final String ref;
    }
}
