package com.carland.carland_service.dto.commission;

import com.carland.carland_service.service.commission.CommissionMoney;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class CommissionScreen {

    private final Query query;
    private final List<Row> rows;
    private final Bucket fresh;
    private final Bucket again;
    private final Bucket all;
    private final long pendingCount;
    private final boolean empty;
    private final boolean showGroups;
    private final List<PartnerGroup> groups;
    private final List<Option> partners;
    private final List<Option> branches;
    private final List<RateRow> rates;
    private final int currentPage;
    private final int totalPages;
    private final int windowStart;
    private final int windowEnd;

    @Getter
    @AllArgsConstructor
    public static class Query {
        private final LocalDate from;
        private final LocalDate to;
        private final String preset;
        private final Long partnerId;
        private final Long branchId;
        private final String status;
        private final String search;
        private final String sort;
        private final boolean ascending;
    }

    @Getter
    @AllArgsConstructor
    public static class Row {
        private final Long id;
        private final String completedAt;
        private final String partnerName;
        private final String branchName;
        private final String vin;
        private final String plate;
        private final String status;
        private final String statusLabel;
        private final boolean pending;
        private final String serviceAmount;
        private final String rate;
        private final String commission;
    }

    @Getter
    @AllArgsConstructor
    public static class Bucket {
        private final long count;
        private final BigDecimal service;
        private final BigDecimal commission;

        public String getServiceText() {
            return CommissionMoney.azn(service);
        }

        public String getCommissionText() {
            return CommissionMoney.azn(commission);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class PartnerGroup {
        private final String name;
        private final long count;
        private final BigDecimal service;
        private final BigDecimal newCommission;
        private final BigDecimal oldCommission;
        private final BigDecimal commission;

        public String getServiceText() {
            return CommissionMoney.azn(service);
        }

        public String getNewCommissionText() {
            return CommissionMoney.azn(newCommission);
        }

        public String getOldCommissionText() {
            return CommissionMoney.azn(oldCommission);
        }

        public String getCommissionText() {
            return CommissionMoney.azn(commission);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Option {
        private final Long id;
        private final String label;
        private final boolean selected;
    }

    @Getter
    @AllArgsConstructor
    public static class RateRow {
        private final String partnerName;
        private final String statusLabel;
        private final String percent;
        private final String effectiveFrom;
        private final String createdAt;
        private final String note;
    }
}
