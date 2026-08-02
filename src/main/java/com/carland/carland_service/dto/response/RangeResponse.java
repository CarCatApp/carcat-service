package com.carland.carland_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * tr: Takvimdeki tek bir randevu zaman aralığını (başlangıç/bitiş, durum, boş kontenjan) döndüren yanıt DTO'su; CalendarResponse içinde kullanılır.
 * en: Response DTO returning a single appointment time slot in the calendar (start/end, status, free count); used inside CalendarResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RangeResponse {
    Long rangeId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    LocalTime start;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    LocalTime end;
    String status;
    String message;
    Integer freeCount;
    List<AppointmentResponse> appointmentResponses;
}
