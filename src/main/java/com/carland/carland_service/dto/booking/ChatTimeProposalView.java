package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTimeProposalView {
    Long id;
    /** pending | accepted | declined | superseded */
    String status;
    /** Current booking hour, 24h. */
    String fromTime;
    /** Proposed hour, 24h. */
    String toTime;
    /** Proposed day, dd.MM. */
    String day;
}
