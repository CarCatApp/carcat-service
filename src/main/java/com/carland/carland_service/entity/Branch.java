package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Partner şubesi. {@code rating} ve {@code ratingCount} yazımda güncellenir, GET DB kolonunu okur.
 * en: Partner branch. {@code rating} / {@code ratingCount} updated on write; GET reads columns.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Partner partner;

    @Column(nullable = false)
    String name;

    String address;

    Double lat;

    Double lng;

    @Column(name = "contact_phone", length = 32)
    String contactPhone;

    @Column(name = "working_hours", length = 512)
    String workingHours;

    @Column(length = 512)
    String photo;

    /**
     * Stored average of non-null {@link Rating#getScore()} values. Null if nobody scored.
     */
    Double rating;

    /**
     * Stored count of {@link Rating} rows for this branch (including comment-only).
     */
    @Builder.Default
    @Column(name = "rating_count")
    Long ratingCount = 0L;

    @OneToMany(mappedBy = "branch")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<Rating> ratings = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    Boolean active = true;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (active == null) {
            active = true;
        }
        if (ratingCount == null) {
            ratingCount = 0L;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
