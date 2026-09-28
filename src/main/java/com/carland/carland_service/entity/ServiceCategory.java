package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * tr: Tüm şubeler için ortak hizmet kategorisi (partner panelindeki "Xidmetler" kartları). Başlıklar az/en/ru JSON.
 * en: Service category shared by all branches (the "Xidmetler" cards in the partner panel). Titles are az/en/ru JSON.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "service_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_service_categories_code", columnNames = "code")
)
public class ServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** routine | repair_inspection | ... ; ^[a-z0-9_]+$ */
    @Column(nullable = false, length = 64)
    String code;

    /** {"az":"...","en":"...","ru":"..."} */
    @Column(name = "title_json", nullable = false, length = 1024)
    String titleJson;

    @Column(name = "description_json", length = 2048)
    String descriptionJson;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;

    /** true: partner-ui'da "Ac" ile detay sayfası */
    @Column(nullable = false)
    @Builder.Default
    Boolean openable = false;

    /** false: şube kapatamaz, hep aktif */
    @Column(nullable = false)
    @Builder.Default
    Boolean toggleable = true;

    /** global aç/kapa; false ise hiçbir şubeye listelenmez */
    @Column(nullable = false)
    @Builder.Default
    Boolean active = true;

    /** virgüllü BranchService.serviceKey listesi, örn. "dir:repair,dir:inspection" */
    @Column(name = "direction_keys", length = 256)
    String directionKeys;
}
