package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.BulkPushRequest;
import com.carland.carland_service.dto.request.DeviceTokenRequest;
import com.carland.carland_service.dto.response.BulkPushResponse;
import com.carland.carland_service.dto.response.DeviceResponse;
import com.carland.carland_service.entity.Customer;
import com.carland.carland_service.entity.DeviceToken;
import com.carland.carland_service.entity.Notification;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.CustomerRepository;
import com.carland.carland_service.repository.DeviceTokenRepository;
import com.carland.carland_service.repository.NotificationRepository;
import com.carland.carland_service.service.DeviceTokenService;
import com.carland.carland_service.service.PushNotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * tr: Cihaz push token yönetiminin implementasyonudur: token kaydetme/güncelleme (kullanıcı başına tek
 *     token kuralıyla, çakışan token'ı temizleyerek) ve müşterilere toplu push bildirimi gönderme.
 * en: Implementation of device push token management: saving/updating tokens (one token per user,
 *     cleaning up conflicting tokens) and sending bulk push notifications to customers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final PushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;

    /**
     * tr: Cihaz token'ını kaydeder veya günceller: aynı token başka kullanıcıya kayıtlıysa eski kaydı siler;
     *     kullanıcının mevcut token'ı aynıysa değişiklik yapmaz, farklıysa günceller, yoksa yeni kayıt oluşturur.
     *     Sonucu mesajlı DeviceResponse olarak döner.
     * en: Saves or updates the device token: deletes the old record when the same token belongs to another user;
     *     leaves it unchanged when the user's current token matches, updates it when different, or creates a new
     *     record otherwise. Returns the outcome as a DeviceResponse with a message.
     */
    @Transactional
    @Override
    public DeviceResponse saveOrUpdateToken(DeviceTokenRequest requestToken) {
        DeviceToken tokenConflict = deviceTokenRepository.findByDeviceToken(requestToken.getDeviceToken());
        if (tokenConflict != null && !tokenConflict.getUserId().equals(requestToken.getUserId())) {
            deviceTokenRepository.delete(tokenConflict);
        }

        DeviceToken existing = deviceTokenRepository.findByUserId(requestToken.getUserId());

        if (existing != null) {
            if (existing.getDeviceToken().equals(requestToken.getDeviceToken())) {
                return DeviceResponse.builder()
                        .message("Device token already up to date.")
                        .build();
            } else {
                existing.setDeviceToken(requestToken.getDeviceToken());
                existing.setPlatform(requestToken.getPlatform());
                deviceTokenRepository.save(existing);
                return DeviceResponse.builder()
                        .message("Device token updated successfully.")
                        .build();
            }
        } else {
            DeviceToken newToken = DeviceToken.builder()
                    .userId(requestToken.getUserId())
                    .deviceToken(requestToken.getDeviceToken())
                    .platform(requestToken.getPlatform())
                    .build();
            deviceTokenRepository.save(newToken);
            return DeviceResponse.builder()
                    .message("Device token saved successfully.")
                    .build();
        }
    }

    /**
     * tr: Verilen müşteri id listesindeki cihazlara toplu push gönderir; her başarılı gönderim için
     *     Notification kaydı oluşturur ve toplam/başarılı/başarısız sayılarını döner. Eksik alanlarda
     *     MissingFieldException; başlık 100 veya mesaj 300 karakteri aşarsa InvalidStatusException fırlatır.
     *     Tekil gönderim hataları yutulur ve sadece loglanır.
     * en: Sends a bulk push to the devices of the given customer id list; creates a Notification record per
     *     successful send and returns total/success/failed counts. Throws MissingFieldException on missing
     *     fields and InvalidStatusException when the title exceeds 100 or the body exceeds 300 characters.
     *     Individual send failures are swallowed and only logged.
     */
    @Override
    public BulkPushResponse sendBulk(BulkPushRequest bulkRequest, String acceptLanguage) {

        if (bulkRequest == null || bulkRequest.getCustomerIdList() == null || bulkRequest.getCustomerIdList().isEmpty()
                || bulkRequest.getTitle() == null || bulkRequest.getBody() == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        if (bulkRequest.getTitle().length() > 100) {
            throw new InvalidStatusException("Başlıq mətni 100 simvolu keçə bilməz");
        }

        if (bulkRequest.getBody().length() > 300) {
            throw new InvalidStatusException("Mesaj mətni 300 simvolu keçə bilməz");
        }

        int totalItemCount = bulkRequest.getCustomerIdList().size();

        List<DeviceToken> deviceTokens = deviceTokenRepository.findAllByUserIdIn(bulkRequest.getCustomerIdList());

        int successItemCount = 0;

        for (DeviceToken deviceToken : deviceTokens) {
            try {
                Notification notification = Notification.builder()
                        .created(LocalDate.now())
                        .customerId(deviceToken.getUserId())
                        .notificationText(bulkRequest.getBody())
                        .title(bulkRequest.getTitle())
                        .status("ACTIVE")
                        .isRead(false)
                        .type("BULK")
                        .build();

                notificationRepository.save(notification);

                pushNotificationService.send(bulkRequest.getTitle(), bulkRequest.getBody(), deviceToken.getDeviceToken());
                successItemCount++;
            } catch (Exception e) {
                log.error("Push gönderilemedi. userId={}, token={}", deviceToken.getUserId(), deviceToken.getDeviceToken(), e);
            }
        }

        int failedItemCount = totalItemCount - successItemCount;

        return BulkPushResponse.builder()
                .message("Bulk notification processed")
                .totalItemCount(totalItemCount)
                .successItemCount(successItemCount)
                .failedItemCount(failedItemCount)
                .build();
    }


}


