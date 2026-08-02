package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * tr: Notification entity'si için JPA repository; müşteri bildirimlerini sorgular.
 * en: JPA repository for the Notification entity; queries customer notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** tr: Müşteri id'sine göre bildirimleri listeler. / en: Lists notifications by customer id. */
    List<Notification> findAllByCustomerId(Long userId);

    /** tr: Müşteri id'si ve duruma göre bildirimleri listeler. / en: Lists notifications by customer id and status. */
    List<Notification> findAllByCustomerIdAndStatus(Long userId, String status);
}
