package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.VisitHistoryItemResponse;
import com.carland.carland_service.dto.response.VisitHistoryResponse;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.Visit;
import com.carland.carland_service.enums.PartnerId;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.VisitRepository;
import com.carland.carland_service.service.HyperPercentageSyncService;
import com.carland.carland_service.service.PartnerLookupService;
import com.carland.carland_service.service.RedisCacheService;
import com.carland.carland_service.service.VisitHistoryService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitHistoryServiceImplBranchTest {

    @Mock BranchRepository branchRepository;
    @Mock CarRepository carRepository;
    @Mock CustomerRepository customerRepository;
    @Mock VisitRepository visitRepository;
    @Mock HyperPercentageSyncService hyperPercentageSyncService;
    @Mock PartnerLookupService partnerLookupService;
    @Mock HyperTokenService hyperTokenService;
    @Mock org.springframework.web.client.RestTemplate restTemplate;
    @Mock RedisCacheService redisCacheService;

    @InjectMocks VisitHistoryServiceImpl service;

    @Test
    void detailedVisitKeepsItsBranchAndHyperVisitKeepsTemporaryBranch() {
        Customer customer = Customer.builder().userId(9L).phoneNumber("0501112233").build();
        Car car = Car.builder().carId(5L).vin("VIN1").customer(customer)
                .servicedPartnerIds(List.of("4", "1")).build();
        Branch xirdalan = Branch.builder().id(22L).name("Auto Zone Xırdalan")
                .address("Xırdalan, Abşeron").lat(40.45).lng(49.75).build();
        Branch babek = Branch.builder().id(10L).name("Hyper Service")
                .address("Babək pr. 43A").lat(40.40).lng(49.85).build();
        Visit panel = Visit.builder().id(1L).car(car).source("panel").branchId(22L)
                .serviceCenterId(4L).serviceCenterName("Auto Zone").build();
        Visit hyper = Visit.builder().id(2L).car(car).hyperRecordId(90L).serviceCenterId(1L)
                .serviceCenterName("HyperService").build();

        when(customerRepository.findByUserIdAndPhoneNumberAndStatus(9L, "0501112233", "ACTIVE")).thenReturn(customer);
        when(carRepository.findByVinAndCustomer("VIN1", customer)).thenReturn(car);
        when(visitRepository.existsByCar(car)).thenReturn(true);
        when(visitRepository.findAllByCarOrderByLastServiceDateDescIdDesc(car)).thenReturn(List.of(panel, hyper));
        when(hyperTokenService.getToken()).thenThrow(new RuntimeException("hyper down"));
        when(partnerLookupService.resolvePartnerId(any(), any())).thenAnswer(inv -> {
            Long stored = inv.getArgument(0);
            PartnerId fallback = inv.getArgument(1);
            return stored != null ? stored : fallback.getId();
        });
        when(partnerLookupService.loadByIds(any())).thenReturn(Map.of());
        when(branchRepository.findAllById(any())).thenReturn(List.of(xirdalan));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(babek));
        when(redisCacheService.getOrLoadHistoryV2(eq("VIN1"), eq("az"), any())).thenAnswer(inv -> {
            Supplier<VisitHistoryResponse> loader = inv.getArgument(2);
            return loader.get();
        });

        VisitHistoryResponse response = ((VisitHistoryService) service)
                .getServiceHistoryByVin("VIN1", "0501112233", "9", "az");

        VisitHistoryItemResponse detailed = response.getItems().get(0);
        assertEquals(22L, detailed.getBranchId());
        assertEquals("Auto Zone Xırdalan", detailed.getBranchName());
        assertEquals("Xırdalan, Abşeron", detailed.getBranchAddress());
        assertEquals(40.45, detailed.getLat());
        assertEquals(49.75, detailed.getLng());
        assertEquals(4L, detailed.getServiceCenterId());

        VisitHistoryItemResponse fromHyper = response.getItems().get(1);
        assertEquals(10L, fromHyper.getBranchId());
        assertEquals("Hyper Service", fromHyper.getBranchName());
        assertEquals("Babək pr. 43A", fromHyper.getBranchAddress());
        assertNull(hyper.getBranchId());
    }
}
