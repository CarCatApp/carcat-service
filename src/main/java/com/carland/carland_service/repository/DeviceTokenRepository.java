package com.carland.carland_service.repository;

import com.carland.carland_service.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * tr: Cinsiyet + marka + yakıt AND filtresine uyan, token'ı olan kullanıcılar (tek satır / kullanıcı).
     * en: Users with a token matching gender + brand + fuel AND filters (one row per user).
     */
    @Query("""
            SELECT dt FROM DeviceToken dt
            WHERE EXISTS (
              SELECT 1 FROM Customer c
              WHERE c.userId = dt.userId
                AND (:gender IS NULL OR UPPER(c.gender) = :gender)
                AND (
                  (:brand IS NULL AND :engineTypeId IS NULL)
                  OR EXISTS (
                    SELECT 1 FROM Car car
                    WHERE car.customer.userId = c.userId
                      AND (:brand IS NULL OR LOWER(car.brand) = :brand)
                      AND (
                        :engineTypeId IS NULL
                        OR car.engineTypeId = :engineTypeId
                        OR (:engineTypeName IS NOT NULL AND LOWER(car.engineType) = :engineTypeName)
                      )
                  )
                )
            )
            """)
    List<DeviceToken> findAudience(
            @Param("gender") String gender,
            @Param("brand") String brand,
            @Param("engineTypeId") Long engineTypeId,
            @Param("engineTypeName") String engineTypeName
    );

    @Query("""
            SELECT COUNT(dt) FROM DeviceToken dt
            WHERE EXISTS (
              SELECT 1 FROM Customer c
              WHERE c.userId = dt.userId
                AND (:gender IS NULL OR UPPER(c.gender) = :gender)
                AND (
                  (:brand IS NULL AND :engineTypeId IS NULL)
                  OR EXISTS (
                    SELECT 1 FROM Car car
                    WHERE car.customer.userId = c.userId
                      AND (:brand IS NULL OR LOWER(car.brand) = :brand)
                      AND (
                        :engineTypeId IS NULL
                        OR car.engineTypeId = :engineTypeId
                        OR (:engineTypeName IS NOT NULL AND LOWER(car.engineType) = :engineTypeName)
                      )
                  )
                )
            )
            """)
    long countAudience(
            @Param("gender") String gender,
            @Param("brand") String brand,
            @Param("engineTypeId") Long engineTypeId,
            @Param("engineTypeName") String engineTypeName
    );
}
