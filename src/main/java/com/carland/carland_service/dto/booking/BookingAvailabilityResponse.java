package com.carland.carland_service.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingAvailabilityResponse {
    Long branchId;
    String from;
    String to;
    String day;
    String timezone;
    List<String> serviceKeys;
    Integer remainingPercent;
    List<BookingAvailabilityDayView> days;
    List<BookingAvailabilitySlotView> ranges;
}
