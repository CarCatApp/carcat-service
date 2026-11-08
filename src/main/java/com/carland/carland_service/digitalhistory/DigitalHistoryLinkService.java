package com.carland.carland_service.digitalhistory;

import com.carland.carland_service.dto.response.DigitalHistoryLinkResponse;
import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.exceptions.UserNotFoundException;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * tr: Sahibin arabası için 5 dakikalık paylaşım adresi üretir. Adresin kendisi QR metnidir.
 * en: Builds a 5-minute share URL for the owner's car. That URL is the QR text.
 */
@Service
public class DigitalHistoryLinkService {

    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final DigitalHistoryTokens tokens;
    private final DigitalHistoryIssuer issuer;
    private final String publicBaseUrl;

    public DigitalHistoryLinkService(
            CustomerRepository customerRepository,
            CarRepository carRepository,
            DigitalHistoryTokens tokens,
            DigitalHistoryIssuer issuer,
            @Value("${digital-history.public-base-url}") String publicBaseUrl) {
        this.customerRepository = customerRepository;
        this.carRepository = carRepository;
        this.tokens = tokens;
        this.issuer = issuer;
        this.publicBaseUrl = publicBaseUrl;
    }

    public DigitalHistoryLinkResponse create(Long carId, String phoneNumber, String userIdHeader, String acceptLanguage) {
        if (carId == null || phoneNumber == null || phoneNumber.isBlank()
                || userIdHeader == null || userIdHeader.isBlank()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(
                Long.valueOf(userIdHeader.trim()), phoneNumber, UserStatus.ACTIVE.name());
        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        Car car = carRepository.findByCarIdAndCustomer(carId, customer);
        if (car == null || car.getCarId() == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        String token = tokens.sign(issuer.issue(car.getCarId(), customer.getNotificationLanguage()));
        return DigitalHistoryLinkResponse.builder()
                .url(base() + DigitalHistoryPaths.PDF + "?jwt=" + token)
                .expiresInSeconds(tokens.shareSeconds())
                .build();
    }

    private String base() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException("digital-history.public-base-url is empty");
        }
        String base = publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
