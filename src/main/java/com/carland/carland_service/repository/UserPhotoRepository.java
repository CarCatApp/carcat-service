package com.carland.carland_service.repository;


import com.carland.carland_service.entity.UserPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * tr: UserPhoto entity'si için JPA repository; kullanıcı profil fotoğraflarını sorgular.
 * en: JPA repository for the UserPhoto entity; queries user profile photos.
 */
@Repository
public interface UserPhotoRepository extends JpaRepository<UserPhoto, Long> {

    /** tr: Kullanıcı id'si ve telefonuna göre profil fotoğrafını bulur. / en: Finds the profile photo by user id and phone number. */
    UserPhoto findByUserIdAndUserPhoneNumber(Long userId, String phoneNumber);



}







