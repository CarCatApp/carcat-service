package com.carland.carland_service.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Admin promo kodu. code büyük/küçük harf duymaz (codeKey). expired elle açılır.
 *     Silinmez; status deactive olur. Şubeler promo_branches.
 * en: Admin promo code. code ignores case (codeKey). expired is a manual switch.
 *     Rows are not deleted; status becomes deactive. Branches live in promo_branches.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "promos")
public class Promo {

    public static final String ACTIVE = "active";
    public static final String DEACTIVE = "deactive";
    public static final String PERCENTAGE = "percentage";
    public static final String AMOUNT = "amount";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 255)
    String name;

    @Column(nullable = false, length = 4000)
    String description;

    @Column(nullable = false, length = 64)
    String code;

    /** tr: code trim + lower. Tekil. / en: Trimmed lower code. Unique. */
    @Column(name = "code_key", nullable = false, unique = true, length = 64)
    String codeKey;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Column(name = "discount_type", nullable = false, length = 16)
    String discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    BigDecimal discountValue;

    @Builder.Default
    @Column(nullable = false, length = 16)
    String status = ACTIVE;

    @Builder.Default
    @Column(nullable = false)
    Boolean expired = false;

    /** tr: Bu promo'ya bağlı tek banner. banners.id. / en: The one banner linked to this promo. banners.id. */
    @Column(name = "banner_id", unique = true)
    Long bannerId;

    @Builder.Default
    @OneToMany(mappedBy = "promo", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<PromoBranch> branches = new ArrayList<>();

    /**
     * tr: Mobil kod bugün Bakü gününde kullanılabilir mi. status active, expired kapalı, tarih aralığı dahil.
     * en: Whether the mobile code can be used on this Baku date. status active, expired off, dates inclusive.
     */
    public boolean usableOn(LocalDate today) {
        if (!ACTIVE.equals(status) || Boolean.TRUE.equals(expired) || today == null) {
            return false;
        }
        if (startDate == null || endDate == null) {
            return false;
        }
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }
}
