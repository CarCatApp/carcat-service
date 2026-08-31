package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * tr: Geri bildirime eklenen resmi binary olarak saklar (mail ekinin DB kopyası).
 * en: Stores the feedback attachment image as binary (DB copy of the mail attachment).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "feedback_photos",
        uniqueConstraints = @UniqueConstraint(name = "uk_feedback_photos_feedback_id", columnNames = "feedback_id")
)
public class FeedbackPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long imageId;

    @Column(name = "feedback_id", nullable = false)
    Long feedbackId;

    @Column(name = "file_name", length = 255)
    String fileName;

    @Column(name = "file_type", length = 64)
    String fileType;

    @Lob
    @JdbcTypeCode(Types.BINARY)
    byte[] imageData;
}
