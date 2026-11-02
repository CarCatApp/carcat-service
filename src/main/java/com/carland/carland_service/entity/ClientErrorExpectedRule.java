package com.carland.carland_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: apiError üçün "bu gözləniləndir" qaydası. Cədvəl boş başlayır, açılışda seed yoxdur.
 *     endpoint mobilin göndərdiyi maskalı path ilə eyni sətir olmalıdır.
 * en: Marks an apiError as expected. The table starts empty; nothing is seeded on boot.
 *     endpoint must equal the masked path the mobile client already sends.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "client_error_expected_rules")
public class ClientErrorExpectedRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endpoint_pattern", nullable = false, length = 500)
    private String endpointPattern;

    @Column(name = "status_code")
    private Integer statusCode;

    /** tr: Status yoxdursa (timeout) Dio tipi. Boşdursa həmin ünvanda bütün timeouts. */
    @Column(length = 40)
    private String signal;

    @Column(nullable = false)
    private boolean expected;
}
