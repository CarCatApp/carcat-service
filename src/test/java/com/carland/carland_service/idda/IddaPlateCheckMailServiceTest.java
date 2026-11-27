package com.carland.carland_service.idda;

import com.carland.carland_service.service.MailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
class IddaPlateCheckMailServiceTest {

    @Mock MailService mailService;

    @Test
    void code0GoesToTheFourAddresses() {
        IddaPlateCheckMailService service = new IddaPlateCheckMailService(mailService);
        LocalDateTime at = LocalDateTime.of(2026, 11, 27, 11, 6, 11);
        IddaPlateCheck row = IddaPlateCheck.builder()
                .statusCode(0)
                .statusName("Successful")
                .statusMessage("ok <b>")
                .plateNumber("77BU669")
                .plateKey("77BU669")
                .queriedAt(at)
                .requestIdentifier("req-1")
                .querierUserId(7L)
                .countsTowardLimit(true)
                .build();

        service.send(row);

        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(mailService, times(4)).sendPlainMail(to.capture(), eq("IDDA 0 Successful — 77 BU 669"), html.capture());
        assertEquals(List.of(
                "azadisali@gmail.com",
                "niftalizadeaziz@gmail.com",
                "nemat.mirzayev@gmail.com",
                "Nemat.Mirzayev@carcat.app"
        ), to.getAllValues());
        String body = html.getValue();
        assertTrue(body.contains("ok &lt;b&gt;"));
        assertFalse(body.contains("<b>ok"));
        assertTrue(body.contains("req-1"));
        assertTrue(body.contains("bəli"));
        String clock = at.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("Asia/Baku"))
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        assertTrue(body.contains(clock));
        assertFalse(clock.toLowerCase().contains("am"));
        assertFalse(clock.toLowerCase().contains("pm"));
    }

    @Test
    void code4AndCode5AlsoSend() {
        IddaPlateCheckMailService service = new IddaPlateCheckMailService(mailService);
        service.send(IddaPlateCheck.builder().statusCode(4).statusName("ServiceRestrictedByPerson").plateKey("10AA000").build());
        service.send(IddaPlateCheck.builder().statusCode(5).statusName("PinValidationError").plateNumber("99QX927").build());

        verify(mailService, times(4)).sendPlainMail(anyString(), eq("IDDA 4 ServiceRestrictedByPerson"), anyString());
        verify(mailService, times(4)).sendPlainMail(anyString(), eq("IDDA 5 PinValidationError — 99 QX 927"), anyString());
    }

    @Test
    void code2SendsNothing() {
        IddaPlateCheckMailService service = new IddaPlateCheckMailService(mailService);
        service.send(IddaPlateCheck.builder().statusCode(2).statusName("ServiceError").plateKey("77BU669").build());
        service.send(null);
        verify(mailService, never()).sendPlainMail(anyString(), anyString(), anyString());
    }

    @Test
    void oneBadAddressDoesNotStopTheOthers() {
        IddaPlateCheckMailService service = new IddaPlateCheckMailService(mailService);
        doThrow(new RuntimeException("brevo"))
                .when(mailService).sendPlainMail(eq("azadisali@gmail.com"), anyString(), anyString());

        service.send(IddaPlateCheck.builder().statusCode(0).statusName("Successful").plateNumber("77BU669").build());

        verify(mailService, times(4)).sendPlainMail(anyString(), anyString(), anyString());
    }
}
