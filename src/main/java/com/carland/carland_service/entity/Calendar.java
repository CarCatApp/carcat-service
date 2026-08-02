package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: "calendars" tablosunu modelleyen entity; bir servis merkezinin günlük randevu takvimini ve zaman aralıklarını (Range) temsil eder.
 * en: Entity modeling the "calendars" table; represents an auto service center's daily appointment calendar and its time ranges (Range).
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "calendars")
public class Calendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long calendarId;

    LocalDate day;

    @Column(name = "start_time")
    OffsetDateTime start;

    @Column(name = "end_time")
    OffsetDateTime end;
    String serviceCategory;
    String status;
    Integer rangeMinutes;

    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Range> timeRanges = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "auto_service_id", nullable = false)
    AutoService autoService;
}

