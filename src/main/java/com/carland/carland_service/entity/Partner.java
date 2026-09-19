package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: {@code partners} tablosu — Hyper entegrasyon + booking org (HQ) aynı kayıt.
 * en: {@code partners} table — Hyper integration and booking org (HQ) share one row.
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

    /** Display name (e.g. HyperService, AvtoVaz). */
    @Column(nullable = false)
    String name;

    /** Branch / workshop name (e.g. Babək Ekspress). Nullable when unknown. */
    String dealer;

    /** Optional branding asset for mobile (Screen 2 logo). Booking admin "Photo URL" writes here. */
    String logoUrl;

    @Column(name = "contact_phone", length = 32)
    String contactPhone;

    @Column(name = "contact_email", length = 128)
    String contactEmail;

    @Builder.Default
    @Column(nullable = false)
    Boolean active = true;

    /** Integration source (e.g. hyper, avtovaz, carcat). */
    @Column(nullable = false)
    String source;

    /** HMAC secret for partner webhook requests (X-Signature). */
    @ToString.Exclude
    @Column(name = "webhook_secret")
    String webhookSecret;

    /** OAuth client id for outbound API calls to this partner (e.g. Hyper). */
    @ToString.Exclude
    @Column(name = "api_client_id")
    String apiClientId;

    /** OAuth client secret for outbound API calls to this partner. */
    @ToString.Exclude
    @Column(name = "api_client_secret")
    String apiClientSecret;

    /** Auth users.id of the single HQ / partner admin. */
    @Column(name = "hq_user_id", unique = true)
    Long hqUserId;

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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
