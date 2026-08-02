package com.carland.carland_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

/**
 * tr: "maintenance_templates" tablosunu modelleyen entity; motor tipine bağlı bakım şablonunu ve içerdiği servis tanımlarını temsil eder.
 * en: Entity modeling the "maintenance_templates" table; represents an engine-type-based maintenance template and the service definitions it contains.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "maintenance_templates")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    /**
     * One EngineType has exactly one MaintenanceTemplate.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engine_type_id", nullable = false, unique = true)
    EngineType engineType;

    @OneToMany(mappedBy = "maintenanceTemplate", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    @ToString.Exclude
    List<ServiceEntity> services = new ArrayList<>();


}
