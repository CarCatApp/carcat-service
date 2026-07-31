package com.carland.carland_service.dto.response;

import lombok.*;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    Long userId;
    String phoneNumber;
    String name;
    String surname;
    String status;
    LocalDate createdAt;

}
