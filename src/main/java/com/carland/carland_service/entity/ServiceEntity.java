package com.carland.carland_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;

/**
 * tr: "services" tablosunu modelleyen entity; bakım şablonuna bağlı servis tanımını (çok dilli ad, bakım aralığı km/ay) temsil eder.
 * en: Entity modeling the "services" table; represents a service definition tied to a maintenance template (multilingual name, interval in km/months).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "services")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String serviceName;//    service_item_id

    String actionType;//    category

    String nameAz;//    name_az

    String nameEn;//    name_en

    String nameRu;//    name_ru

    Long intervalKm;//    standard interval km

    Integer intervalMonth;  //    standard interval time

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean important;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    MaintenanceTemplate maintenanceTemplate;
}
