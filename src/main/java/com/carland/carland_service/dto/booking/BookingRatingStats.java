package com.carland.carland_service.dto.booking;

import com.carland.carland_service.entity.Rating;

import java.util.Collection;
import java.util.List;

/**
 * tr: API {@code ratingCount} = bütün yorum satırları. {@code rating} = yalnızca score'u dolu satırların ortalaması
 *     (200 yorum, 180 puan → toplam / 180). Partner = o partnerin tüm şube yorumları, aynı kural.
 * en: {@code ratingCount} = all review rows. {@code rating} = average of non-null scores only
 *     (200 reviews, 180 scores → sum / 180). Partner uses every branch review of that partner.
 */
public record BookingRatingStats(Double rating, Long ratingCount) {

    public static BookingRatingStats empty() {
        return new BookingRatingStats(null, 0L);
    }

    public static BookingRatingStats from(Collection<Rating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return empty();
        }
        long count = ratings.size();
        List<Integer> scores = ratings.stream()
                .map(Rating::getScore)
                .filter(score -> score != null)
                .toList();
        if (scores.isEmpty()) {
            return new BookingRatingStats(null, count);
        }
        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0d);
        double rounded = Math.round(avg * 10d) / 10d;
        return new BookingRatingStats(rounded, count);
    }
}
