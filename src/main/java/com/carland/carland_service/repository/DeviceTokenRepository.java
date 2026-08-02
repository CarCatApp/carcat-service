package com.carland.carland_service.repository;

import com.carland.carland_service.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: DeviceToken entity'si için JPA repository; push bildirim cihaz token'larını sorgular.
 * en: JPA repository for the DeviceToken entity; queries push notification device tokens.
 */
@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    /** tr: Kullanıcı id'sine göre cihaz token'ını bulur. / en: Finds a device token by user id. */
    DeviceToken findByUserId(Long userId);

    /** tr: Token değerine göre cihaz token kaydını bulur. / en: Finds a device token record by token value. */
    DeviceToken findByDeviceToken(String deviceToken);

    /** tr: Verilen kullanıcı id listesine ait cihaz token'larını listeler. / en: Lists device tokens for the given list of user ids. */
    List<DeviceToken> findAllByUserIdIn(List<Long> customerIdList);

}
