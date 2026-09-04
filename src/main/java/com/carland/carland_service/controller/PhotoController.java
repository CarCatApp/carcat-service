package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.GeneratePhotoResponse;
import com.carland.carland_service.dto.response.PhotoResponse;
import com.carland.carland_service.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * tr: Fotoğraf REST controller'ı; araç fotoğrafı, partner logosu/rozet logosu ve kullanıcı profil fotoğrafı için yükleme, getirme ve silme uçlarını sunar.
 * en: REST controller for photos; exposes upload, fetch, and delete endpoints for car photos, partner logos/badge logos, and user profile pictures.
 */
@RestController
@RequestMapping("/api/v1/photo")
@RequiredArgsConstructor

public class PhotoController {

    private final PhotoService photoService;

    /**
     * tr: Multipart "file" bölümündeki fotoğrafı verilen carId'ye ait araca yükler ve sonucu döner.
     * en: Uploads the photo from the multipart "file" part to the car identified by carId and returns the result.
     */
    @PostMapping(value = "/for/car/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadCarPhoto(@RequestPart("file") MultipartFile file,
                                        @RequestParam("carId") Long carId,
                                        @RequestHeader("role") String role,
                                        @RequestHeader("phoneNumber") String phoneNumber,
                                        @RequestHeader("X-User-Id") String userIdHeader,
                                        @RequestHeader("X-Client-Timezone") String timezone,
                                        @RequestHeader("Accept-Language") String acceptLanguage) {
        return photoService.uploadCarPhoto(file, carId, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: OpenAI ile araç fotoğrafı üretimini başlatır; 202 + pending. Upload path'ine dokunmaz.
     * en: Starts AI generation of the car photo; 202 + pending. Does not replace the upload path.
     */
    @PostMapping(value = "/for/car/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeneratePhotoResponse> generateCarPhoto(@RequestParam("carId") Long carId,
                                                                  @RequestHeader("role") String role,
                                                                  @RequestHeader("phoneNumber") String phoneNumber,
                                                                  @RequestHeader("X-User-Id") String userIdHeader,
                                                                  @RequestHeader("X-Client-Timezone") String timezone,
                                                                  @RequestHeader("Accept-Language") String acceptLanguage) {
        return ResponseEntity.accepted().body(
                photoService.generateCarPhoto(carId, role, phoneNumber, userIdHeader, timezone, acceptLanguage));
    }


    /**
     * tr: Verilen carId'ye ait aracın fotoğrafını, çağıran kullanıcının rol/kimlik bilgilerine göre siler ve sonucu döner.
     * en: Deletes the photo of the car identified by carId, based on the caller's role/identity headers, and returns the result.
     */
    @DeleteMapping("/for/car/delete")
    public PhotoResponse deleteCarPhoto(@RequestHeader("role") String role,
                                        @RequestParam("carId") Long carId,
                                        @RequestHeader("phoneNumber") String phoneNumber,
                                        @RequestHeader("X-User-Id") String userIdHeader,
                                        @RequestHeader("X-Client-Timezone") String timezone,
                                        @RequestHeader("Accept-Language") String acceptLanguage) {
        return photoService.deleteCarPhoto(role, carId, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Verilen carId'ye ait aracın fotoğrafını sahiplik kontrolü olmadan siler (yönetimsel/diğer kullanım) ve sonucu döner.
     * en: Deletes the photo of the car identified by carId without an ownership check (administrative/other use) and returns the result.
     */
    @DeleteMapping("/for/car/delete/other")
    public PhotoResponse deleteOtherCarPhoto(@RequestParam("carId") Long carId,
                                             @RequestHeader("Accept-Language") String acceptLanguage) {
        return photoService.deleteOtherCarPhoto(carId, acceptLanguage);
    }

    /**
     * tr: Verilen carId'ye ait aracın fotoğrafını byte dizisi olarak döner.
     * en: Returns the photo of the car identified by carId as a byte array.
     */
    @GetMapping(value = "/for/car/get", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getCarPhoto(
            @RequestHeader("role") String role,
            @RequestParam("carId") Long carId,
            @RequestHeader("phoneNumber") String phoneNumber,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-Client-Timezone") String timezone,
            @RequestHeader("Accept-Language") String acceptLanguage) {

        return photoService.getCarPhoto(role, carId, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Multipart "file" bölümündeki fotoğrafı verilen partnerId'ye ait partnere yükler ve sonucu döner.
     * en: Uploads the photo from the multipart "file" part to the partner identified by partnerId and returns the result.
     */
    @PostMapping(value = "/for/partner/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadPartnerPhoto(@RequestPart("file") MultipartFile file,
                                            @RequestParam("partnerId") Long partnerId) {
        return photoService.uploadPartnerPhoto(file, partnerId);
    }

    /**
     * tr: Path'teki partnerId'ye ait partner fotoğrafını byte dizisi olarak döner.
     * en: Returns the partner photo for the partnerId in the path as a byte array.
     */
    @GetMapping(value = "/for/partner/get/{partnerId}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getPartnerPhotoById(@PathVariable("partnerId") Long partnerId) {
        return photoService.getPartnerPhotoById(partnerId);
    }

    /**
     * tr: Multipart "file" bölümündeki rozet logosunu verilen partnerId'ye ait partnere yükler ve sonucu döner.
     * en: Uploads the badge logo from the multipart "file" part to the partner identified by partnerId and returns the result.
     */
    @PostMapping(value = "/for/partner/badge-logo/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadPartnerBadgeLogo(@RequestPart("file") MultipartFile file,
                                                @RequestParam("partnerId") Long partnerId) {
        return photoService.uploadPartnerBadgeLogo(file, partnerId);
    }

    /**
     * tr: Path'teki partnerId'ye ait partner rozet logosunu byte dizisi olarak döner.
     * en: Returns the partner badge logo for the partnerId in the path as a byte array.
     */
    @GetMapping(value = "/for/partner/badge-logo/get/{partnerId}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getPartnerBadgeLogoById(@PathVariable("partnerId") Long partnerId) {
        return photoService.getPartnerBadgeLogoById(partnerId);
    }


    /**
     * tr: Multipart "file" bölümündeki fotoğrafı, header'lardan belirlenen kullanıcının profil fotoğrafı olarak yükler ve sonucu döner.
     * en: Uploads the photo from the multipart "file" part as the profile picture of the user resolved from the headers and returns the result.
     */
    @PostMapping(value = "/for/user/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadUserPP(@RequestPart("file") MultipartFile file,
                                      @RequestHeader("role") String role,
                                      @RequestHeader("phoneNumber") String phoneNumber,
                                      @RequestHeader("X-User-Id") String userIdHeader,
                                      @RequestHeader("X-Client-Timezone") String timezone,
                                      @RequestHeader("Accept-Language") String acceptLanguage) {
        return photoService.uploadUserPP(file, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Header'lardan belirlenen kullanıcının profil fotoğrafını siler ve sonucu döner.
     * en: Deletes the profile picture of the user resolved from the headers and returns the result.
     */
    @DeleteMapping("/for/user/delete")
    public PhotoResponse deletePP(@RequestHeader("role") String role,
                                  @RequestHeader("phoneNumber") String phoneNumber,
                                  @RequestHeader("X-User-Id") String userIdHeader,
                                  @RequestHeader("X-Client-Timezone") String timezone,
                                  @RequestHeader("Accept-Language") String acceptLanguage) {
        return photoService.deleteUserPP(role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }

    /**
     * tr: Header'lardan belirlenen kullanıcının profil fotoğrafını byte dizisi olarak döner.
     * en: Returns the profile picture of the user resolved from the headers as a byte array.
     */
    @GetMapping(value = "/for/user/get", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getProfilePicture(
            @RequestHeader("role") String role,
            @RequestHeader("phoneNumber") String phoneNumber,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-Client-Timezone") String timezone,
            @RequestHeader("Accept-Language") String acceptLanguage) {

        return photoService.getUserPP(role, phoneNumber, userIdHeader, timezone, acceptLanguage);
    }


}
