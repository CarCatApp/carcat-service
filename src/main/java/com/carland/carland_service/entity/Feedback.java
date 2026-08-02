package com.carland.carland_service.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


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
}

