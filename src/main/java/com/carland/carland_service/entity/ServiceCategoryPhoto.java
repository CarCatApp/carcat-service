package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: "service_category_photos" tablosu; bir hizmet kategorisinin ikonunu saklar.
 * en: "service_category_photos" table; stores the icon of one service category.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "service_category_photos",
        uniqueConstraints = @UniqueConstraint(name = "uk_service_category_photos_category_id", columnNames = "category_id")
)
public class ServiceCategoryPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;

    @Column(name = "category_id", nullable = false)
    Long categoryId;

    String fileName;
    String fileType;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    byte[] imageData;
}
