package com.carland.carland_service.dto.response;

import lombok.*;

/**
 * tr: AutoServiceController işlemlerinden (oluşturma/güncelleme/silme) sonra dönen basit mesaj yanıtı DTO'su.
 * en: Simple message response DTO returned after AutoServiceController operations (create/update/delete).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoServiceResponse {
    private String message;

}
