package com.carland.carland_service.idda;

import com.carland.carland_service.digitalfinance.DigitalFinanceClient;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.dto.response.CarResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.repository.BrandRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.ModelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IddaPlateCheckServiceTest {

    @Mock DigitalFinanceClient digitalFinanceClient;
    @Mock IddaPlateCheckRepository iddaPlateCheckRepository;
    @Mock CustomerRepository customerRepository;
    @Mock CarRepository carRepository;
    @Mock BrandRepository brandRepository;
    @Mock ModelRepository modelRepository;

    IddaPlateCheckService service;

    @BeforeEach
    void setUp() {
        service = new IddaPlateCheckService(
                digitalFinanceClient,
                iddaPlateCheckRepository,
                customerRepository,
                carRepository,
                brandRepository,
                modelRepository,
                new ObjectMapper());
    }

    @Test
    void adjacentAaPlateIsRejectedBeforeIdda() throws Exception {
        for (String plate : List.of("10AA000", "10-AA-000", "10 AA 000", "99aa123", "10\u0410\u0410000")) {
            InvalidStatusException ex = assertThrows(InvalidStatusException.class,
                    () -> service.check(plate, "050", "7", "az"));
            assertEquals("AA qeydiyyat nişanı icazəsi yoxdur", ex.getMessage());
        }
        InvalidStatusException en = assertThrows(InvalidStatusException.class,
                () -> service.check("10AA000", "050", "7", "en"));
        assertEquals("AA registration plates are not allowed", en.getMessage());
        verify(digitalFinanceClient, never()).exchange(anyString(), anyString(), any(), any(), any());
        verify(iddaPlateCheckRepository, never())
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(anyString(), any());
        verify(customerRepository, never()).findByUserIdAndPhoneNumberAndStatus(any(), any(), any());
    }

    @Test
    void code4DoesNotCountAndIsNotServedFromCache() throws Exception {
        customer();
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(anyString(), any()))
                .thenReturn(List.of());
        when(digitalFinanceClient.exchange(eq("GET"), anyString(), any(), anyString(), any()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", code4()));

        CarResponse response = service.check("77-BU-669", "050", "7", "az");

        assertEquals("77 BU 669", response.getPlateNumber());
        assertEquals(4, response.getIddaStatusCode());
        assertEquals("13e79e85-2e94-4f81-932a-a7b3cccbfb70", response.getRequestIdentifier());
        assertEquals("limit", response.getMessage());
        assertEquals(Boolean.TRUE, response.getIsRestrict());
        assertNull(response.getVin());
        assertNull(response.getBrand());
        assertNull(response.getCarId());
        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(response));
        assertEquals("77 BU 669", json.path("plateNumber").asText());
        assertTrue(json.path("isRestrict").asBoolean());
        assertFalse(json.has("restrict"));
        assertFalse(Boolean.TRUE.equals(response.getFromIdda()));
        ArgumentCaptor<IddaPlateCheck> saved = ArgumentCaptor.forClass(IddaPlateCheck.class);
        verify(iddaPlateCheckRepository).save(saved.capture());
        assertFalse(saved.getValue().isCountsTowardLimit());
        assertEquals("77BU669", saved.getValue().getPlateKey());

        service.check("77BU669", "050", "7", "az");
        verify(digitalFinanceClient, times(2)).exchange(eq("GET"), anyString(), any(), anyString(), any());
        verify(carRepository, never()).findAllByVinIgnoreCase(anyString());
    }

    @Test
    void permissionDeniedHidesIddaPayloadAndTheStoredCar() throws Exception {
        customer();
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(eq("77BU669"), any()))
                .thenReturn(List.of(successRow(7L)));
        when(digitalFinanceClient.exchange(eq("GET"), anyString(), any(), anyString(), any()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", code4WithCar()));
        when(iddaPlateCheckRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CarResponse response = service.check("77BU669", "050", "7", "az");

        assertEquals(Boolean.TRUE, response.getIsRestrict());
        assertEquals(4, response.getIddaStatusCode());
        assertNull(response.getVin());
        assertNull(response.getBrand());
        assertNull(response.getModel());
        assertNull(response.getCarId());
        assertNull(response.getCustomerId());
        assertNull(response.getMileage());
        verify(carRepository, never()).findAllByVinIgnoreCase(anyString());
    }

    @Test
    void liveSuccessIsOpenAndTheSixthQueryIsRestrictedWithoutIdda() throws Exception {
        customer();
        List<IddaPlateCheck> four = List.of(successRow(7L), successRow(7L), successRow(7L), successRow(7L));
        List<IddaPlateCheck> five = List.of(successRow(7L), successRow(7L), successRow(7L), successRow(7L), successRow(7L));
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(eq("77BU669"), any()))
                .thenReturn(four)
                .thenReturn(five);
        when(brandRepository.existsByIsnew(".")).thenReturn(false);
        when(brandRepository.findAll()).thenReturn(List.of());
        when(modelRepository.findAll()).thenReturn(List.of());
        when(digitalFinanceClient.exchange(eq("GET"), anyString(), any(), anyString(), any()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", successJson()));
        when(iddaPlateCheckRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CarResponse live = service.check("77BU669", "050", "7", "az");
        CarResponse capped = service.check("77BU669", "050", "7", "az");

        assertEquals("19XZE4F54NE012640", live.getVin());
        assertEquals(Boolean.FALSE, live.getIsRestrict());
        assertEquals("77 BU 669", capped.getPlateNumber());
        assertEquals(Boolean.TRUE, capped.getIsRestrict());
        assertNull(capped.getVin());
        assertNull(capped.getBrand());
        assertNull(capped.getCarId());
        assertEquals(MessagesLangValues.PLATE_QUERY_LIMIT.getMessageByLang("az"), capped.getMessage());
        verify(digitalFinanceClient, times(1)).exchange(eq("GET"), anyString(), any(), anyString(), any());
        verify(carRepository, times(1)).findAllByVinIgnoreCase(anyString());
    }

    @Test
    void timeoutDoesNotSaveAndIsRestricted() throws Exception {
        customer();
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(anyString(), any()))
                .thenReturn(List.of());
        when(digitalFinanceClient.exchange(eq("GET"), anyString(), any(), anyString(), any()))
                .thenThrow(new IOException("timed out"));

        CarResponse response = service.check("77BU669", "050", "7", "az");

        assertEquals(Boolean.TRUE, response.getIsRestrict());
        assertNull(response.getVin());
        assertEquals(MessagesLangValues.IDDA_UNAVAILABLE.getMessageByLang("az"), response.getMessage());
        verify(iddaPlateCheckRepository, never()).save(any());
        verify(carRepository, never()).findAllByVinIgnoreCase(anyString());
    }

    @Test
    void guideUserOnDemoPlateSkipsTheFullCapAndDoesNotCount() throws Exception {
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(55L, "050", "ACTIVE"))
                .thenReturn(Customer.builder().userId(55L).name("N").surname("M").phoneNumber("050").build());
        List<IddaPlateCheck> ten = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ten.add(successRow(55L));
        }
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(eq("77BU669"), any()))
                .thenReturn(ten);
        when(brandRepository.existsByIsnew(".")).thenReturn(false);
        when(brandRepository.findAll()).thenReturn(List.of());
        when(modelRepository.findAll()).thenReturn(List.of());
        when(digitalFinanceClient.exchange(eq("GET"), anyString(), any(), anyString(), any()))
                .thenReturn(new DigitalFinanceClient.Upstream(200, "application/json", successJson()));
        when(iddaPlateCheckRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CarResponse response = service.check("77-BU-669", "050", "55", "az");

        assertEquals(Boolean.FALSE, response.getIsRestrict());
        assertEquals("19XZE4F54NE012640", response.getVin());
        assertEquals(Boolean.TRUE, response.getFromIdda());
        ArgumentCaptor<IddaPlateCheck> saved = ArgumentCaptor.forClass(IddaPlateCheck.class);
        verify(iddaPlateCheckRepository).save(saved.capture());
        assertFalse(saved.getValue().isCountsTowardLimit());
        verify(digitalFinanceClient, times(1)).exchange(eq("GET"), anyString(), any(), anyString(), any());
    }

    @Test
    void eleventhPlateQueryIsRestrictedAndHidesTheStoredCar() throws Exception {
        customer();
        List<IddaPlateCheck> ten = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ten.add(successRow(1L));
        }
        IddaPlateCheck newest = successRow(2L);
        newest.setVin("NEWERVIN000000001");
        ten.add(0, newest);
        when(iddaPlateCheckRepository
                .findByPlateKeyAndCountsTowardLimitTrueAndQueriedAtAfterOrderByQueriedAtDesc(eq("77BU669"), any()))
                .thenReturn(ten);

        CarResponse response = service.check("77BU669", "050", "7", "az");

        assertEquals("77 BU 669", response.getPlateNumber());
        assertNull(response.getVin());
        assertNull(response.getBrand());
        assertNull(response.getCarId());
        assertEquals(Boolean.TRUE, response.getIsRestrict());
        assertEquals(MessagesLangValues.PLATE_QUERY_LIMIT.getMessageByLang("az"), response.getMessage());
        verify(digitalFinanceClient, never()).exchange(anyString(), anyString(), any(), any(), any());
        verify(carRepository, never()).findAllByVinIgnoreCase(anyString());
    }

    private void customer() {
        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(7L, "050", "ACTIVE"))
                .thenReturn(Customer.builder().userId(7L).name("A").surname("B").phoneNumber("050").build());
    }

    private static IddaPlateCheck successRow(Long userId) {
        return IddaPlateCheck.builder()
                .id(userId)
                .plateKey("77BU669")
                .plateNumber("77BU669")
                .vin("19XZE4F54NE012640")
                .brand("Honda")
                .model("Insight")
                .querierUserId(userId)
                .queriedAt(LocalDateTime.now().minusDays(1))
                .countsTowardLimit(true)
                .statusCode(0)
                .statusName("Successful")
                .build();
    }

    private static byte[] successJson() {
        return """
                {"RequestIdentifier":"8ec23951-d0ae-4d7d-a6c1-0b9eb1bbff0f","Status":{"Name":"Successful","Code":0,"Message":""},"Response":{"Certificate":{"Number":"BA535286","GivenDate":"25.11.2025"},"Car":{"NumberPlate":"77BU669","Mark":"HONDA INSIGHT MINIK","BodyNumber":"19XZE4F54NE012640","ManufactoringYear":"2022","Color":"Gümüşü metallik","EngineCapacity":"1496","EngineNumber":"3513075","Type":{"Id":3,"Description":"MINIK"}}}}
                """.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] code4() {
        return """
                {"RequestIdentifier":"13e79e85-2e94-4f81-932a-a7b3cccbfb70","Status":{"Name":"ServiceRestrictedByPerson","Code":4,"Message":"limit"},"Response":null}
                """.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] code4WithCar() {
        return """
                {"RequestIdentifier":"13e79e85-2e94-4f81-932a-a7b3cccbfb70","Status":{"Name":"ServiceRestrictedByPerson","Code":4,"Message":"limit"},"Response":{"Car":{"NumberPlate":"77BU669","Mark":"HONDA INSIGHT MINIK","BodyNumber":"19XZE4F54NE012640","ManufactoringYear":"2022","Color":"Gümüşü metallik","EngineCapacity":"1496"}}}
                """.getBytes(StandardCharsets.UTF_8);
    }
}
