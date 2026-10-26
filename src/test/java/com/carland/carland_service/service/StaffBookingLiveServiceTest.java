package com.carland.carland_service.service;

import com.carland.carland_service.dto.booking.StaffBookingArrival;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StaffBookingLiveServiceTest {

    @Mock BookingCreatedMailService mail;

    @Test
    void createNotifiesOpsEvenWhenNoPanelIsOpen() {
        StaffBookingLiveService live = new StaffBookingLiveService(new ObjectMapper(), mail);
        StaffBookingArrival arrival = StaffBookingArrival.builder().bookingId(4L).branchId(7L).ref("BK4").build();

        live.publishAfterCommit(arrival);

        verify(mail).send(arrival);
    }

    @Test
    void missingBookingDoesNotMail() {
        StaffBookingLiveService live = new StaffBookingLiveService(new ObjectMapper(), mail);

        live.publishAfterCommit(StaffBookingArrival.builder().branchId(7L).build());

        verify(mail, never()).send(org.mockito.ArgumentMatchers.any());
    }
}
