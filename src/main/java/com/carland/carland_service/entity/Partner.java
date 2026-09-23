package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: {@code partners} tablosu — booking HQ + entegrasyon kaydı (secret kolon yok; env).
 * en: {@code partners} table — booking HQ + integration row (no secret columns; env).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "partners", uniqueConstraints = {
        @UniqueConstraint(name = "uk_partners_name_source", columnNames = {"name", "source"})
})
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    String logoUrl;

    @Column(name = "contact_phone", length = 32)
    String contactPhone;

    @Column(name = "contact_email", length = 128)
    String contactEmail;

    @Builder.Default
    @Column(nullable = false)
    Boolean active = true;

    @Column(nullable = false)
    String source;

    @Column(name = "hq_user_id", unique = true)
    Long hqUserId;

    Double rating;

    @Builder.Default
    @Column(name = "rating_count")
    Long ratingCount = 0L;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "partner")
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<Branch> branches = new ArrayList<>();

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
