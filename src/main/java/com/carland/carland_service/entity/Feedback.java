package com.carland.carland_service.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;


/**
 * tr: "feedbacks" tablosunu modelleyen entity; müşterinin gönderdiği geri bildirimi (tip, konu, açıklama, puan) saklar.
 * en: Entity modeling the "feedbacks" table; stores feedback submitted by a customer (type, subject, description, rating).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    Long feedbackId;
    String type;
    String subject;
    String description;
    Integer rating;
    Long customerId;
    String customerPhone;
    /** Server clock when the row was inserted. Null on older rows. Admin shows Asia/Baku (+4). */
    @Column(name = "created_at")
    LocalDateTime createdAt;

    @PrePersist
    void stampCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

