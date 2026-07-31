package com.carland.carland_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthUser {
    Long id;
    LocalDateTime createdAt;
    String phoneNumber;
    String status;
    String name;
    String surname;
}
