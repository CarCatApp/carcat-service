package com.carland.carland_service.dto.commission;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CommissionDetail {

    private final Long id;
    private final String completedAt;
    private final String partnerName;
    private final String branchName;
    private final String vin;
    private final String plate;
    private final String ref;
    private final String statusLabel;
    private final String rate;
    private final boolean pending;
    private final String serviceAmount;
    private final String commission;
    private final String decidedAt;
    private final String outcome;
    private final String earliest;
    private final String snapshot;
    private final String why;
    private final boolean sharedDay;
    private final List<Line> lines;

    @Getter
    @Builder
    public static class Line {
        private final String visit;
        private final String title;
        private final String amount;
    }
}
