package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

/**
 * tr: Owner-app rezervasyonu. slot = ranges.range_id. Para qəpik.
 * en: Owner-app booking. slot = ranges.range_id. Money in qepik.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_branch_status", columnList = "branch_id, status"),
                @Index(name = "idx_bookings_range", columnList = "range_id"),
                @Index(name = "idx_bookings_customer", columnList = "customer_user_id")
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 16)
    String ref;

    @Column(name = "customer_user_id", nullable = false)
    Long customerUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "range_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Range range;

    @Column(nullable = false, length = 16)
    String status;

    /**
     * tr: Partner panelində gizlədilib. Status, yer və müştəri tərəfi dəyişmir. Köhnə sətir null qala bilər.
     * en: Hidden from the partner panel. Status, the place and the customer side stay the same. Old rows may stay null.
     */
    @Builder.Default
    @Column(name = "staff_hidden")
    Boolean staffHidden = false;

    @Column(name = "price_min")
    Integer priceMin;

    @Column(name = "price_max")
    Integer priceMax;

    @Column(nullable = false, length = 8)
    @Builder.Default
    String currency = "AZN";

    @Column(length = 32)
    String vin;

    @Column(name = "car_id")
    Long carId;

    /** Şubenin care paketi. İçerik satırları cevapta yok; ad ve fiyat kopyası. */
    @Column(name = "care_package_id")
    Long carePackageId;

    @Column(name = "package_name", length = 80)
    String packageName;

    /** Care paket fiyatının qəpik kopyası. Aralık yok, min ve max aynı değer. */
    @Column(name = "package_price")
    Integer packagePrice;

    @Column(name = "cancel_reason_code", length = 64)
    String cancelReasonCode;

    @Column(name = "cancel_note", length = 500)
    String cancelNote;

    @Column(name = "pending_expires_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime pendingExpiresAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime updatedAt;

    /**
     * tr: Partner sorğunu qəbul edəndə. Ani növbədə bu, yaranma anıdır. Köhnə sətir boş qala bilər.
     * en: When the partner accepts the request. On an instant queue this is the creation instant. Old rows may stay empty.
     */
    @Column(name = "accepted_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime acceptedTime;

    /**
     * tr: Xidmət tamamlananda. Köhnə sətir boş qala bilər.
     * en: When the service is completed. Old rows may stay empty.
     */
    @Column(name = "completed_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime completedTime;

    /**
     * tr: Partner rədd edəndə, ya da yer dolunca gözləyən rədd olunanda. Ləğv və gəlmədi bunu yazmır.
     * en: When the partner rejects, or a waiting request is rejected because the places are full. Cancel and no-show leave this empty.
     */
    @Column(name = "rejected_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime rejectedTime;

    /** tr: Boşdursa yazır. Sonrakı keçid köhnə saatı saxlayır. en: Writes while empty. A later transition keeps the earlier stamp. */
    public void stampAccepted(OffsetDateTime when) {
        if (acceptedTime == null) {
            acceptedTime = when == null ? OffsetDateTime.now() : when;
        }
    }

    /** tr: Boşdursa yazır. en: Writes while empty. */
    public void stampCompleted(OffsetDateTime when) {
        if (completedTime == null) {
            completedTime = when == null ? OffsetDateTime.now() : when;
        }
    }

    /** tr: Boşdursa yazır. en: Writes while empty. */
    public void stampRejected(OffsetDateTime when) {
        if (rejectedTime == null) {
            rejectedTime = when == null ? OffsetDateTime.now() : when;
        }
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (currency == null) {
            currency = "AZN";
        }
        if (staffHidden == null) {
            staffHidden = false;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
