package com.carland.carland_service.repository;

import com.carland.carland_service.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: Admin entity'si için JPA repository; admin kullanıcılarını id, telefon ve duruma göre sorgular.
 * en: JPA repository for the Admin entity; queries admin users by id, phone and status.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    /** tr: Kullanıcı id'sine göre admin bulur. / en: Finds an admin by user id. */
    Admin findByUserId(Long userId);

    /** tr: Telefon numarasına göre admin bulur. / en: Finds an admin by phone number. */
    Admin findByPhoneNumber(String phoneNumber);

    /** tr: Kullanıcı id, telefon ve duruma göre admin bulur. / en: Finds an admin by user id, phone number and status. */
    Admin findByUserIdAndPhoneNumberAndStatus(Long userId, String phoneNumber, String status);
}
