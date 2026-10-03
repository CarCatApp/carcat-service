package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * tr: Bir davranış grubunun altındaki xidmət satırı. Başlıklar az/en/ru JSON.
 * en: One service line under a behavior group. Titles are az/en/ru JSON.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "offered_services")
public class OfferedService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "behavior_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    ServiceBehavior behavior;

    /** {"az":"...","en":"...","ru":"..."} */
    @Column(name = "title_json", nullable = false, length = 1024)
    String titleJson;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    Boolean active = true;

    /** Paket ekranındaki grup. Fərdi xidmət filteri değil. Mevcut satırda boş kalabilir. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_service_filter_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    OfferedServiceFilter filter;
}
