package com.carland.carland_service.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRatingView {
    Long id;
    Long branchId;
    Long partnerId;
    Long userId;
    Integer score;
    String text;
    String reaction;
    LocalDateTime createdAt;
    Double rating;
    Long ratingCount;
    Double partnerRating;
    Long partnerRatingCount;
}
