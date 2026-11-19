package com.carland.carland_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimingBeltChoiceRequest {
    private Long carId;
    /** {@code BELT} or {@code CHAIN} */
    private String timingBeltChoice;
}
