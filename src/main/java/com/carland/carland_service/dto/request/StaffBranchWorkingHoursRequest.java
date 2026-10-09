package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffBranchWorkingHoursRequest {
    String weekdayStart;
    String weekdayEnd;
    String saturdayStart;
    String saturdayEnd;
    String sundayStart;
    String sundayEnd;
}
