package com.carland.carland_service.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * tr: AppointmentController randevu sorgularında dönen yanıt DTO'su. Randevu tarihi, durumu, servis merkezi ve müşteri bilgilerini içerir.
 * en: Response DTO returned by AppointmentController appointment queries. Contains appointment date, status, auto service and customer info.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    Long id;
    String appointmentDate;
    String status;
    Long autoServiceId;
    String autoServiceName;
    String autoServiceNumber;
    String serviceCategory;
    String customerNumber;
    String customerName;
    String message;
    String appointmentStart;
    String appointmentEnd;
}
