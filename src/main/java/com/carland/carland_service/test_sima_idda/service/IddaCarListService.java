package com.carland.carland_service.test_sima_idda.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.test_sima_idda.config.SimaIddaConstants;
import com.carland.carland_service.test_sima_idda.dto.idda.IddaCarItem;
import com.carland.carland_service.test_sima_idda.dto.response.IddaCarListResponse;
import com.carland.carland_service.test_sima_idda.feign.IddaFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * IDDA car-list demo: fetch by FIN, compare VINs to customer's local cars, log matches.
 * Does not insert/update Car rows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IddaCarListService {

    private final IddaFeign iddaFeign;
    private final CustomerRepository customerRepository;

    public IddaCarListResponse getCarsByFin(String userIdHeader, String fin) {
        if (fin == null || fin.isBlank()) {
            throw new IllegalArgumentException("fin query param required");
        }

        Customer customer = requireCustomer(userIdHeader);

        List<IddaCarItem> iddaCars = iddaFeign.getCarsByFin(
                SimaIddaConstants.EXAMPLE_IDDA_PARTNER_CODE,
                SimaIddaConstants.EXAMPLE_IDDA_API_KEY,
                fin
        );
        if (iddaCars == null) {
            iddaCars = Collections.emptyList();
        }

        List<String> localVins = extractLocalVins(customer);
        Set<String> localVinSet = localVins.stream()
                .map(v -> v.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(HashSet::new));

        List<String> matchedVins = new ArrayList<>();
        for (IddaCarItem item : iddaCars) {
            if (item.getVin() == null) {
                continue;
            }
            String vin = item.getVin().toUpperCase(Locale.ROOT);
            if (localVinSet.contains(vin)) {
                matchedVins.add(item.getVin());
                log.info("IDDA VIN match fin={} vin={} plate={}", fin, item.getVin(), item.getPlateNumber());
            } else {
                log.info("IDDA VIN new (not in local list) fin={} vin={}", fin, item.getVin());
            }
        }

        log.info("IDDA compare done fin={} iddaCount={} localCount={} matchedCount={} (no DB car write)",
                fin, iddaCars.size(), localVins.size(), matchedVins.size());

        return IddaCarListResponse.builder()
                .fin(fin)
                .iddaCars(iddaCars)
                .localVins(localVins)
                .matchedVins(matchedVins)
                .note("VIN matches logged only — car list / DB not mutated in test_sima_idda stage")
                .build();
    }

    private List<String> extractLocalVins(Customer customer) {
        List<Car> cars = customer.getCars();
        if (cars == null || cars.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> vins = new ArrayList<>();
        for (Car car : cars) {
            if (car.getVin() != null && !car.getVin().isBlank()) {
                vins.add(car.getVin());
            }
        }
        return vins;
    }

    private Customer requireCustomer(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("X-User-Id header required");
        }
        Customer customer = customerRepository.findByUserId(Long.valueOf(userIdHeader));
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found for userId=" + userIdHeader);
        }
        return customer;
    }
}
