package com.carland.carland_service.dto.response;

import lombok.Data;

@Data
public class AdminAuthLoginResponse {

    String accessToken;
    String refreshToken;
    String role;
    Long userId;
    String phoneNumber;
    String name;
    String surname;
}
