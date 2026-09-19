package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * tr: Partner şubesi (List of Branch). {@code partners} kaydına bağlıdır.
 * en: Partner branch (List of Branch). Belongs to a {@code partners} row.
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

    /** Filled by customer votes later; admin does not set this. */
    Double rating;

    @Builder.Default
    @Column(name = "rating_count")
    Integer ratingCount = 0;

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
            ratingCount = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
