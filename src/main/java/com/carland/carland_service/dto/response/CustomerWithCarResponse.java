package com.carland.carland_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * tr: Bir müşteriyi araçlarıyla birlikte döndüren birleşik yanıt DTO'su (müşteri-araç link/listeleme akışı).
 * en: Combined response DTO returning a customer together with their cars (customer-car link/listing flow).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerWithCarResponse {

    private CustomerResponse customerResponse;
    private List<CarResponseForLink> carResponses;


}
