package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;

/**
 * tr: Booking sohbeti (CRCT-286). Metin + resim/belge.
 * en: Booking chat (CRCT-286). Text + image/file.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "booking_messages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_booking_messages_client",
                        columnNames = {"booking_id", "client_message_id"})
        },
        indexes = {
                @Index(name = "idx_booking_messages_booking_created", columnList = "booking_id, created_at, id")
        }
)
public class BookingMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Booking booking;

    /** Client UUID / timestamp_seq — idempotent send. Legacy rows may be null. */
    @Column(name = "client_message_id", length = 64)
    String clientMessageId;

    /** customer | branch | system */
    @Column(name = "from_role", nullable = false, length = 16)
    String fromRole;

    /** Writer user id when customer/branch; null for system. */
    @Column(name = "author_user_id")
    Long authorUserId;

    /** text | image | file | system */
    @Column(nullable = false, length = 16)
    @Builder.Default
    String kind = "text";

    /** Text body; for media may be empty or caption. */
    @Column(length = 2000)
    String body;

    /** booking_chat_uploads.public_id when kind is image|file */
    @Column(name = "upload_id", length = 40)
    String uploadId;

    @Column(name = "file_name", length = 255)
    String fileName;

    @Column(name = "content_type", length = 120)
    String contentType;

    @Column(name = "size_bytes")
    Long sizeBytes;

    Integer width;
    Integer height;

    /** sent | delivered | seen */
    @Column(nullable = false, length = 16)
    @Builder.Default
    String delivery = "sent";

    @Builder.Default
    @Column(nullable = false)
    Boolean rebook = false;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (kind == null || kind.isBlank()) {
            kind = "text";
        }
        if (delivery == null || delivery.isBlank()) {
            delivery = "sent";
        }
        if (rebook == null) {
            rebook = false;
        }
        if (body == null) {
            body = "";
        }
    }
}
