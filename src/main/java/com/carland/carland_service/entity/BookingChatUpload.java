package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

/**
 * tr: Chat medya yüklemesi (resim/belge). Mesaja bağlanınca message_id dolar.
 * en: Chat media upload (image/file). message_id is set when attached to a message.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "booking_chat_uploads",
        indexes = {
                @Index(name = "idx_chat_uploads_booking", columnList = "booking_id"),
                @Index(name = "uk_chat_uploads_public", columnList = "public_id", unique = true)
        }
)
public class BookingChatUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Client/API uploadId, e.g. upl_9f3a */
    @Column(name = "public_id", nullable = false, length = 40, unique = true)
    String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Booking booking;

    /** image | file */
    @Column(nullable = false, length = 16)
    String kind;

    @Column(name = "file_name", nullable = false, length = 255)
    String fileName;

    @Column(name = "content_type", nullable = false, length = 120)
    String contentType;

    @Column(name = "size_bytes", nullable = false)
    Long sizeBytes;

    Integer width;
    Integer height;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    byte[] data;

    @Column(name = "created_by_role", nullable = false, length = 16)
    String createdByRole;

    @Column(name = "created_by_user_id")
    Long createdByUserId;

    /** Set when a chat message consumes this upload. */
    @Column(name = "message_id")
    Long messageId;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
