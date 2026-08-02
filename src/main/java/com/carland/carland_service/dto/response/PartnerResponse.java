package com.carland.carland_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tr: PartnerController işlemlerinden dönen yanıt DTO'su; işlem mesajı ve partner verisini (PartnerDataResponse) içerir.
 * en: Response DTO returned by PartnerController operations; contains an operation message and the partner data (PartnerDataResponse).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"message", "partner"})
public class PartnerResponse {
    String message;
    PartnerDataResponse partner;
}
