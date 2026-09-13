package com.carland.carland_service.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminSimaPanel {
    Long authUserId;
    Long customerUserId;
    String phone;
    boolean customerFound;
    boolean simaVerified;
    int dailyUsed;
    int dailyLimit;
    int totalUsed;
    int totalLimit;
    List<AdminSimaAttemptRow> attempts;
}
