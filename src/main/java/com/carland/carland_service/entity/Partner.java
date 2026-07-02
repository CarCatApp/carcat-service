package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

    /** Optional branding asset for mobile (Screen 2 logo). */
    String logoUrl;

    @Builder.Default
    @Column(nullable = false)
    Boolean active = true;

    /** Integration source (e.g. hyper, avtovaz). */
    @Column(nullable = false)
    String source;

    /** HMAC secret for partner webhook requests (X-Signature). */
    @Column(name = "webhook_secret")
    String webhookSecret;

    /** OAuth client id for outbound API calls to this partner (e.g. Hyper). */
    @Column(name = "api_client_id")
    String apiClientId;

    /** OAuth client secret for outbound API calls to this partner. */
    @Column(name = "api_client_secret")
    String apiClientSecret;
}
