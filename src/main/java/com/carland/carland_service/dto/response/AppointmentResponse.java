package com.carland.carland_service.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * tr: Randevu yanıtı: tarih, durum, şube ve müşteri.
 * en: Appointment response: date, status, branch and customer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    Long id;
    String appointmentDate;
    String status;
    Long branchId;
    String branchName;
    String branchPhone;
    String serviceCategory;
    String customerNumber;
    String customerName;
    String message;
    String appointmentStart;
    String appointmentEnd;
}
