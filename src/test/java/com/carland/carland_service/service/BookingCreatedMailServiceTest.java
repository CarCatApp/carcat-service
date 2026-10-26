package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffBookingArrival;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingCreatedMailServiceTest {

    @Mock MailService mailService;

    @Test
    void sendsTheSameLetterToEachFixedAddress() {
        BookingCreatedMailService service = new BookingCreatedMailService(
                mailService, BookingCreatedMailService.DEFAULT_RECIPIENTS);
        StaffBookingArrival arrival = StaffBookingArrival.builder()
                .bookingId(9L)
                .branchId(7L)
                .partnerName("Hyper")
                .branchName("Xəqani")
                .ref("BK9")
                .bookingMode("approval")
                .status("pending")
                .day("26.10.2026")
                .start("09:00")
                .end("09:30")
                .brand("Toyota")
                .model("Camry")
                .vin("VIN1")
                .plateNumber("10-AA-010")
                .customerName("Ali <script>")
                .phone("0501112233")
                .packageName("Yağ")
                .packagePrice(4000)
                .serviceNames(List.of("Filter"))
                .issue("səs")
                .priceMin(12000)
                .priceMax(15000)
                .build();

        service.send(arrival);

        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(mailService, times(4)).sendPlainMail(to.capture(), eq("Yeni rezervasiya BK9 — Xəqani"), html.capture());
        assertEquals(List.of(
                "azadisali@gmail.com",
                "niftalizadeaziz@gmail.com",
                "nemat.mirzayev@gmail.com",
                "Nemat.Mirzayev@carcat.app"
        ), to.getAllValues());
        String body = html.getValue();
        assertTrue(body.contains("Təsdiq gözləyir"));
        assertTrue(body.contains("Təsdiqli"));
        assertTrue(body.contains("09:00–09:30"));
        assertTrue(body.contains("40.00 AZN"));
        assertTrue(body.contains("120.00 AZN – 150.00 AZN"));
        assertTrue(body.contains("Ali &lt;script&gt;"));
        assertFalse(body.contains("<script>"));
    }

    @Test
    void oneBadAddressDoesNotStopTheOthers() {
        BookingCreatedMailService service = new BookingCreatedMailService(mailService, "a@x.com,b@x.com");
        doThrow(new RuntimeException("brevo"))
                .when(mailService).sendPlainMail(eq("a@x.com"), anyString(), anyString());

        service.send(StaffBookingArrival.builder().bookingId(1L).ref("R").branchName("B").build());

        verify(mailService).sendPlainMail(eq("b@x.com"), eq("Yeni rezervasiya R — B"), anyString());
    }

    @Test
    void blankListSendsNothing() {
        BookingCreatedMailService service = new BookingCreatedMailService(mailService, "  ");

        service.send(StaffBookingArrival.builder().bookingId(1L).build());

        verify(mailService, never()).sendPlainMail(anyString(), anyString(), anyString());
    }
}
