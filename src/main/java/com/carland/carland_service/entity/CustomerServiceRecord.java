package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * tr: "customer_service_records" tablosunu modelleyen entity; müşterinin kendi girdiği araç servis kaydını (servis adı, tarih, km) temsil eder.
 * en: Entity modeling the "customer_service_records" table; represents a car service record entered by the customer (service name, date, km).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "customer_service_records")
public class CustomerServiceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String serviceName;
    String serviceNameAz;
    String serviceNameRu;
    String serviceNameEn;
    String actionType;
    LocalDate doneDate;
    Integer doneKm;
    Long serviceId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    @ToString.Exclude
    Car car;
    String servicedStatus;
}
