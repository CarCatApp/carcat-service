package com.carland.carland_service.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TimeOfferOptions {
    /** The booking's own service. The list is already limited to it. */
    String serviceName;
    List<TimeOfferDay> days;
}
