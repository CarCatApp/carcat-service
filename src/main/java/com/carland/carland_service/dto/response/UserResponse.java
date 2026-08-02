package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: UserController kullanıcı işlemlerinden sonra dönen basit mesaj yanıtı DTO'su.
 * en: Simple message response DTO returned after UserController user operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    String message;
}
