package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * tr: Admin banner. Şube satırına bağlı değil; branchId elle de yazılır.
 *     Silme satırı silmez, status=deactive olur. Liste yalnız active okur.
 *     active geri dönüşü admin ekranında yok; DB'de elle SQL.
 * en: Admin banner. branchId is stored on its own and can be typed by hand.
 *     Delete sets status=deactive. The list reads active only.
 *     Turning a row active again is a manual SQL update.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "banners")
public class Banner {

    public static final String ACTIVE = "active";
    public static final String DEACTIVE = "deactive";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false, length = 4000)
    String description;

    @Column(name = "branch_id", nullable = false)
    Long branchId;

    @Builder.Default
    @Column(nullable = false)
    Boolean showing = false;

    @Builder.Default
    @Column(nullable = false, length = 16)
    String status = ACTIVE;

    @PrePersist
    void onCreate() {
        if (showing == null) {
            showing = false;
        }
        if (status == null || status.isBlank()) {
            status = ACTIVE;
        }
    }
}
