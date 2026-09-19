package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAuditRequest {
    String action;
    String actor;
    Long userId;
    Long partnerId;
    Long branchId;
    String phoneNumber;
    String role;
    String detail;
    Boolean success;
}
