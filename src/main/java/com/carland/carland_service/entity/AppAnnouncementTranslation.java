package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

/**
 * tr: Duyuru metni. {@code (announcement_id, lang)} tekildir. {@code az} admin yazımında zorunludur.
 * en: Announcement copy. {@code (announcement_id, lang)} is unique. {@code az} is required on admin writes.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
@Table(
        name = "app_announcement_translation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_app_announcement_lang",
                columnNames = {"announcement_id", "lang"}
        )
)
public class AppAnnouncementTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    AppAnnouncement announcement;

    @Column(nullable = false, length = 2)
    String lang;

    @Column(nullable = false, length = 60)
    String title;

    @Column(nullable = false, length = 500)
    String body;

    @Column(name = "cta_label", length = 30)
    String ctaLabel;
}
