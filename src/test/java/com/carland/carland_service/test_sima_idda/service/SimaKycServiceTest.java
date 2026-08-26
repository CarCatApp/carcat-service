package com.carland.carland_service.test_sima_idda.service;

import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.SimaKycRecord;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.SimaKycRecordRepository;
import com.carland.carland_service.test_sima_idda.config.SimaIddaProperties;
import com.carland.carland_service.test_sima_idda.dto.response.SimaVerifyOutcome;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaApiEnvelope;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaErrorBody;
import com.carland.carland_service.test_sima_idda.dto.sima.SimaIdentityResult;
import com.carland.carland_service.test_sima_idda.feign.SimaFeign;
import com.carland.carland_service.test_sima_idda.hmac.SimaHmacSigner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimaKycServiceTest {

    @Mock SimaFeign simaFeign;
    @Mock CustomerRepository customerRepository;
    @Mock SimaKycRecordRepository simaKycRecordRepository;
    @Mock SimaHmacSigner simaHmacSigner;
    @Mock SimaIddaProperties simaIddaProperties;

    SimaKycService service;
    Customer customer;
    MockMultipartFile photo;

    @BeforeEach
    void setUp() {
        service = new SimaKycService(
                simaFeign,
                customerRepository,
                simaKycRecordRepository,
                new ObjectMapper(),
                simaHmacSigner,
                simaIddaProperties
        );
        customer = Customer.builder()
                .userId(678L)
                .phoneNumber("+994776140999")
                .simaVerified(false)
                .build();
        photo = new MockMultipartFile("photo", "a.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, 1, 2});
    }

    @Test
    void alreadyVerified_skipsSima() {
        customer.setSimaVerified(true);
        customer.setPin("62HJ5KQ");
        customer.setName("A");
        customer.setSurname("B");
        when(customerRepository.findByUserId(678L)).thenReturn(customer);

        SimaVerifyOutcome out = service.verifyCitizen("678", "62HJ5KQ", "AB0668397", null, photo, "az");

        assertEquals(200, out.getHttpStatus());
        assertTrue(out.getBody().isVerified());
        assertEquals("SIMA_ALREADY_VERIFIED", out.getBody().getCode());
        verify(simaFeign, never()).verifyCitizen(any(), any(), any(), any(), any());
    }

    @Test
    void pinTaken_conflictWithoutSima() {
        when(customerRepository.findByUserId(678L)).thenReturn(customer);
        when(customerRepository.findByPinIgnoreCase("62HJ5KQ"))
                .thenReturn(Customer.builder().userId(1L).pin("62HJ5KQ").build());
        when(simaKycRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SimaVerifyOutcome out = service.verifyCitizen("678", "62HJ5KQ", "AB0668397", null, photo, "az");

        assertEquals(409, out.getHttpStatus());
        assertFalse(out.getBody().isVerified());
        assertEquals("PIN_ALREADY_EXISTS", out.getBody().getCode());
        verify(simaFeign, never()).verifyCitizen(any(), any(), any(), any(), any());
    }

    @Test
    void highScores_applyProfileAndPersist() {
        stubCitizenCall();
        when(simaFeign.verifyCitizen(any(), any(), any(), any(), any())).thenReturn(envelope(true, 0.996, 0.999));
        when(simaKycRecordRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(simaKycRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SimaVerifyOutcome out = service.verifyCitizen("678", "62HJ5KQ", "AB0668397", null, photo, "az");

        assertEquals(200, out.getHttpStatus());
        assertTrue(out.getBody().isVerified());
        assertEquals("ARAZ", out.getBody().getName());
        assertTrue(customer.getSimaVerified());
        assertEquals("ARAZ", customer.getName());
        ArgumentCaptor<SimaKycRecord> captor = ArgumentCaptor.forClass(SimaKycRecord.class);
        verify(simaKycRecordRepository).save(captor.capture());
        assertTrue(captor.getValue().isAppliedToProfile());
    }

    @Test
    void lowScore_http200NotVerifiedStillPersisted() {
        stubCitizenCall();
        when(simaFeign.verifyCitizen(any(), any(), any(), any(), any())).thenReturn(envelope(true, 0.85, 0.999));
        when(simaKycRecordRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(simaKycRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SimaVerifyOutcome out = service.verifyCitizen("678", "62HJ5KQ", "AB0668397", null, photo, "en");

        assertEquals(200, out.getHttpStatus());
        assertFalse(out.getBody().isVerified());
        assertEquals("SIMA_SCORE_GATE", out.getBody().getCode());
        assertFalse(Boolean.TRUE.equals(customer.getSimaVerified()));
        verify(customerRepository, never()).save(any());
        verify(simaKycRecordRepository).save(any());
    }

    @Test
    void simaError_mirrorsHttpAndCodes() {
        stubCitizenCall();
        SimaApiEnvelope fail = SimaApiEnvelope.builder()
                .isSuccess(false)
                .error(SimaErrorBody.builder()
                        .httpStatus(400)
                        .errorCode(752)
                        .errorMessage("resim decode edilemedi")
                        .transactionId("t-1")
                        .build())
                .build();
        when(simaFeign.verifyCitizen(any(), any(), any(), any(), any())).thenReturn(fail);
        when(simaKycRecordRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(simaKycRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SimaVerifyOutcome out = service.verifyCitizen("678", "62HJ5KQ", "AB0668397", null, photo, "az");

        assertEquals(400, out.getHttpStatus());
        assertFalse(out.getBody().isVerified());
        assertEquals(752, out.getBody().getSimaResponseCode());
        assertEquals("resim decode edilemedi", out.getBody().getSimaMessage());
        verify(customerRepository, never()).save(any());
    }

    private void stubCitizenCall() {
        when(customerRepository.findByUserId(678L)).thenReturn(customer);
        when(customerRepository.findByPinIgnoreCase(anyString())).thenReturn(null);
        when(simaHmacSigner.signBase64(anyString())).thenReturn("sig");
        when(simaIddaProperties.getSimaIdentifier()).thenReturn("id");
        when(simaIddaProperties.getSimaAuthScheme()).thenReturn("HMACSHA256");
        when(simaIddaProperties.getSimaDeviceInfo()).thenReturn("test");
    }

    private static SimaApiEnvelope envelope(boolean success, double live, double sim) {
        return SimaApiEnvelope.builder()
                .isSuccess(success)
                .result(SimaIdentityResult.builder()
                        .pin("62HJ5KQ")
                        .name("ARAZ")
                        .surname("ELIYEV")
                        .livenessScore(live)
                        .similarityScore(sim)
                        .livenessStatus(true)
                        .similarityStatus(true)
                        .transactionId("100550")
                        .build())
                .build();
    }
}
