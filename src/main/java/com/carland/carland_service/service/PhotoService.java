package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.GeneratePhotoResponse;
import com.carland.carland_service.dto.response.PhotoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * tr: Araç, kullanıcı profili ve partner fotoğraflarının/logolarının yüklenmesi, getirilmesi ve silinmesi için sözleşmedir.
 * en: Contract for uploading, retrieving, and deleting car, user profile, and partner photos/logos.
 */
public interface PhotoService {

    /**
     * tr: Araca fotoğraf yükler ve sonucu döner.
     * en: Uploads a photo for the car and returns the result.
     */
    PhotoResponse uploadCarPhoto(MultipartFile file, Long carId, String role, String phoneNumber, String userIdHeader,
                                 String timezone, String acceptLanguage);

    /**
     * tr: Aracın fotoğrafını siler ve sonucu döner.
     * en: Deletes the car's photo and returns the result.
     */
    PhotoResponse deleteCarPhoto(String role, Long carId, String phoneNumber, String userIdHeader, String timezone,
                                 String acceptLanguage);


    /**
     * tr: Kullanıcının profil fotoğrafını yükler ve sonucu döner.
     * en: Uploads the user's profile picture and returns the result.
     */
    PhotoResponse uploadUserPP(MultipartFile file, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);

    /**
     * tr: Kullanıcının profil fotoğrafını siler ve sonucu döner.
     * en: Deletes the user's profile picture and returns the result.
     */
    PhotoResponse deleteUserPP(String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: AI ile araç fotoğrafı üretir. OpenAI kuyruğa girdiyse pending; mevcut AI foto prompt ile uyumluysa ready.
     * en: Starts AI generation. pending when OpenAI is queued; ready when the current AI photo still matches the prompt.
     */
    GeneratePhotoResponse generateCarPhoto(Long carId, String role, String phoneNumber, String userIdHeader,
                                           String timezone, String acceptLanguage);

    /**
     * tr: Aracın fotoğrafını v2 olarak döner: gövde byte, status/source header.
     * en: Returns the car photo for v2: body = bytes, status/source on headers.
     */
    ResponseEntity<byte[]> getCarPhotoV2(String role, Long carId, String phoneNumber, String userIdHeader,
                                         String timezone, String acceptLanguage);

    /**
     * tr: Aracın fotoğrafını byte dizisi olarak döner.
     * en: Returns the car's photo as a byte array.
     */
    ResponseEntity<byte[]> getCarPhoto(String role, Long carId, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Kullanıcının profil fotoğrafını byte dizisi olarak döner.
     * en: Returns the user's profile picture as a byte array.
     */
    ResponseEntity<byte[]> getUserPP(String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage);


    /**
     * tr: Partner logosunu yükler ve sonucu döner.
     * en: Uploads the partner logo and returns the result.
     */
    PhotoResponse uploadPartnerPhoto(MultipartFile file, Long partnerId);

    /**
     * tr: Partner logosunu byte dizisi olarak döner.
     * en: Returns the partner logo as a byte array.
     */
    ResponseEntity<byte[]> getPartnerPhotoById(Long partnerId);

    /**
     * tr: Partner rozet (badge) logosunu yükler ve sonucu döner.
     * en: Uploads the partner badge logo and returns the result.
     */
    PhotoResponse uploadPartnerBadgeLogo(MultipartFile file, Long partnerId);

    /**
     * tr: Partner rozet (badge) logosunu byte dizisi olarak döner.
     * en: Returns the partner badge logo as a byte array.
     */
    ResponseEntity<byte[]> getPartnerBadgeLogoById(Long partnerId);

    /**
     * tr: Sahiplik kontrolü olmadan aracın fotoğrafını siler (admin/temizlik amaçlı) ve sonucu döner.
     * en: Deletes the car's photo without ownership checks (admin/cleanup use) and returns the result.
     */
    PhotoResponse deleteOtherCarPhoto(Long carId, String acceptLanguage);

    /**
     * tr: Servis kalemi ikonunu yükler. Satır varsa fileName/fileType/imageData güncellenir; yoksa INSERT.
     * en: Uploads the maintenance-item icon. Updates fileName/fileType/imageData in place when a row exists.
     */
    PhotoResponse uploadPercentagePhoto(MultipartFile file, Long serviceId);

    /**
     * tr: Servis kalemi ikonunu döner; yoksa empty-state. İkisi de yoksa 404.
     * en: Returns the maintenance-item icon, or the empty-state placeholder. 404 when both are missing.
     */
    ResponseEntity<byte[]> getPercentagePhoto(Long serviceId);

    /**
     * tr: Empty-state ikonunu yükler. Satır varsa fileName/fileType/imageData güncellenir; yoksa INSERT.
     * en: Uploads the empty-state icon. Updates fileName/fileType/imageData in place when a row exists.
     */
    PhotoResponse uploadPercentageEmptyPhoto(MultipartFile file);

    /**
     * tr: Empty-state ikonunu döner; yoksa 404.
     * en: Returns the empty-state placeholder icon; 404 when missing.
     */
    ResponseEntity<byte[]> getPercentageEmptyPhoto();

}
