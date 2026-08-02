package com.carland.carland_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * tr: "service_histories" tablosunu modelleyen entity; aracın geçmiş servis kaydını (servis adı, tarih, km, servis merkezi, tutar) temsil eder.
 * en: Entity modeling the "service_histories" table; represents a car's past service record (service name, date, km, service center, amount).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "service_histories")
public class ServiceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String serviceName;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    List<String> actionType;
    LocalDate doneDate;
    Integer doneKm;
    String serviceCenter;
    @Column(name = "service_center_id")
    Long serviceCenterId;
    BigDecimal serviceAmount;
    String dealer;
    LocalDate nextServiceDate;
    Integer nextServiceMileage;
    String source;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    @ToString.Exclude
    Car car;

}
