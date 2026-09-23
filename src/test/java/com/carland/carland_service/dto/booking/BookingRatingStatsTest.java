package com.carland.carland_service.dto.booking;

import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Rating;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingRatingStatsTest {

    @Test
    void emptyWhenNoRows() {
        BookingRatingStats stats = BookingRatingStats.from(List.of());
        assertNull(stats.rating());
        assertEquals(0L, stats.ratingCount());
    }

    @Test
    void countIsRowCountAverageIsRounded() {
        Branch branch = Branch.builder().id(7L).build();
        Rating a = Rating.builder().branch(branch).score(5).build();
        Rating b = Rating.builder().branch(branch).score(4).build();
        Rating c = Rating.builder().branch(branch).score(5).build();
        BookingRatingStats stats = BookingRatingStats.from(List.of(a, b, c));
        assertEquals(3L, stats.ratingCount());
        assertEquals(4.7, stats.rating());
    }

    @Test
    void averageIgnoresNullScoresCountIncludesComments() {
        Branch branch = Branch.builder().id(7L).build();
        Rating scoredA = Rating.builder().branch(branch).score(5).build();
        Rating scoredB = Rating.builder().branch(branch).score(4).build();
        Rating commentOnly = Rating.builder().branch(branch).score(null).text("ok").build();
        BookingRatingStats stats = BookingRatingStats.from(List.of(scoredA, scoredB, commentOnly));
        assertEquals(3L, stats.ratingCount());
        assertEquals(4.5, stats.rating());
    }
}
