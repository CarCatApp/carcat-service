package com.carland.carland_service.repository;

import com.carland.carland_service.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: SuperAdmin entity'si için JPA repository; superadmin kullanıcılarını id, telefon ve duruma göre sorgular.
 * en: JPA repository for the SuperAdmin entity; queries superadmin users by id, phone and status.
 */
@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {
    /** tr: Kullanıcı id'sine göre superadmin bulur. / en: Finds a superadmin by user id. */
    SuperAdmin findByUserId(Long userId);

    /** tr: Telefon numarasına göre superadmin bulur. / en: Finds a superadmin by phone number. */
    SuperAdmin findByPhoneNumber(String phoneNumber);

    /** tr: Kullanıcı id, telefon ve duruma göre superadmin bulur. / en: Finds a superadmin by user id, phone number and status. */
    SuperAdmin findByUserIdAndPhoneNumberAndStatus(Long userId, String phoneNumber, String status);
}
