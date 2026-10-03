package com.carland.carland_service.controller;

import com.carland.carland_service.dto.response.GeneratePhotoResponse;
import com.carland.carland_service.dto.response.PhotoResponse;
import com.carland.carland_service.enums.CarPhotoStatus;
import com.carland.carland_service.security.BookingStaffRequestAuth;
import com.carland.carland_service.service.PhotoService;
import com.carland.carland_service.service.StaffMediaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * tr: Fotoğraf REST controller'ı; araç fotoğrafı, partner logosu/rozet logosu, kullanıcı profil fotoğrafı
 *     bakım kalemi (percentage), hizmet kategorisi ve offered service ikonları için yükleme/getirme uçlarını sunar.
 * en: REST controller for photos; exposes upload/fetch for car photos, partner logos/badge logos,
 *     user profile pictures, maintenance-item (percentage) icons, service-category icons, and offered-service icons.
 */
@RestController
@RequestMapping("/api/v1/photo")
@RequiredArgsConstructor

public class PhotoController {

    private final PhotoService photoService;
    private final StaffMediaService staffMediaService;
    private final BookingStaffRequestAuth bookingStaffRequestAuth;

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
     * tr: OpenAI ile araç fotoğrafı üretir. İş kuyruğa girdiyse pending (202); AI foto zaten uyumluysa ready (200).
     * en: Starts AI generation. pending (202) when queued; ready (200) when the existing AI photo still matches.
     */
    @PostMapping(value = "/for/car/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeneratePhotoResponse> generateCarPhoto(@RequestParam("carId") Long carId,
                                                                  @RequestHeader("role") String role,
                                                                  @RequestHeader("phoneNumber") String phoneNumber,
                                                                  @RequestHeader("X-User-Id") String userIdHeader,
                                                                  @RequestHeader("X-Client-Timezone") String timezone,
                                                                  @RequestHeader("Accept-Language") String acceptLanguage) {
        GeneratePhotoResponse body = photoService.generateCarPhoto(
                carId, role, phoneNumber, userIdHeader, timezone, acceptLanguage);
        if (CarPhotoStatus.READY.equalsIgnoreCase(body.getPhotoStatus())) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.accepted().body(body);
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
     * tr: Bakım kalemi ikonunu döner (services.id). Kendi fotosu yoksa empty-state; ikisi de yoksa 404.
     *     Flutter: GET /service/percentages içindeki serviceId. role / X-User-Id gerekmez (Kong JWT yeter).
     * en: Returns the maintenance-item icon (services.id). Falls back to empty-state; 404 when both missing.
     *     Flutter uses serviceId from GET /service/percentages. No role / X-User-Id (Kong JWT is enough).
     */
    @GetMapping(value = "/for/percentage/get", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getPercentagePhoto(@RequestParam("serviceId") Long serviceId) {
        return photoService.getPercentagePhoto(serviceId);
    }

    /**
     * tr: Bakım kalemi ikonunu yükler; eski byte silinir. Postman: form-data key = file, query serviceId.
     * en: Uploads the maintenance-item icon; replaces existing bytes. Postman: form-data key = file, query serviceId.
     */
    @PostMapping(value = "/for/percentage/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadPercentagePhoto(@RequestPart("file") MultipartFile file,
                                               @RequestParam("serviceId") Long serviceId) {
        return photoService.uploadPercentagePhoto(file, serviceId);
    }

    /**
     * tr: Offered service ikonunu döner. Foto yoksa 404. role / X-User-Id gerekmez.
     * en: Returns the offered-service icon. 404 when missing. No role / X-User-Id.
     */
    @GetMapping(value = "/for/offered-service/get", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getOfferedServicePhoto(@RequestParam("offeredServiceId") Long offeredServiceId) {
        return photoService.getOfferedServicePhoto(offeredServiceId);
    }

    /**
     * tr: Offered service ikonunu yükler; eski byte silinir. Postman: form-data key = file, query offeredServiceId.
     * en: Uploads the offered-service icon; replaces existing bytes. Postman: form-data key = file, query offeredServiceId.
     */
    @PostMapping(value = "/for/offered-service/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadOfferedServicePhoto(@RequestPart("file") MultipartFile file,
                                                   @RequestParam("offeredServiceId") Long offeredServiceId) {
        return photoService.uploadOfferedServicePhoto(file, offeredServiceId);
    }

    /**
     * tr: Empty-state (placeholder) ikonunu döner; yoksa 404.
     * en: Returns the empty-state placeholder icon; 404 when missing.
     */
    @GetMapping(value = "/for/percentage/get-empty", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getPercentageEmptyPhoto() {
        return photoService.getPercentageEmptyPhoto();
    }

    /**
     * tr: Empty-state ikonunu yükler; eski byte silinir.
     * en: Uploads the empty-state placeholder; replaces existing bytes.
     */
    @PostMapping(value = "/for/percentage/upload-empty", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadPercentageEmptyPhoto(@RequestPart("file") MultipartFile file) {
        return photoService.uploadPercentageEmptyPhoto(file);
    }

    /**
     * tr: Hizmet kategorisi ikonunu döner. Foto yoksa 404 (empty-state yok). role / X-User-Id gerekmez.
     * en: Returns the service-category icon. 404 when the photo is missing (no empty-state). No role / X-User-Id.
     */
    @GetMapping(value = "/for/service-category/get", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getServiceCategoryPhoto(@RequestParam("categoryId") Long categoryId) {
        return photoService.getServiceCategoryPhoto(categoryId);
    }

    /**
     * tr: Hizmet kategorisi ikonunu yükler. Postman: form-data key = file, query categoryId.
     * en: Uploads the service-category icon. Postman: form-data key = file, query categoryId.
     */
    @PostMapping(value = "/for/service-category/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadServiceCategoryPhoto(@RequestPart("file") MultipartFile file,
                                                    @RequestParam("categoryId") Long categoryId) {
        return photoService.uploadServiceCategoryPhoto(file, categoryId);
    }

    /**
     * tr: Hizmet kategorisi ikonunu siler.
     * en: Deletes the service-category icon.
     */
    @DeleteMapping(value = "/for/service-category/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse deleteServiceCategoryPhoto(@RequestParam("categoryId") Long categoryId) {
        return photoService.deleteServiceCategoryPhoto(categoryId);
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

    /**
     * tr: Şubenin tek fotoğrafını yükler. Partner admin reddedilir. Partner logosu değildir.
     * en: Uploads the branch's single photo. Partner admin is rejected. This is not the partner logo.
     */
    @PostMapping(value = "/for/branch/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadBranchPhoto(HttpServletRequest request,
                                           @RequestPart("file") MultipartFile file,
                                           @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return staffMediaService.uploadBranchPhoto(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                file,
                acceptLanguage);
    }

    /**
     * tr: Şube fotoğrafını döner. Staff token gerekir.
     * en: Returns the branch photo. A staff token is required.
     */
    @GetMapping(value = "/for/branch/get/{branchId}", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getBranchPhoto(HttpServletRequest request,
                                                 @PathVariable("branchId") Long branchId,
                                                 @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return staffMediaService.getBranchPhoto(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                branchId,
                acceptLanguage);
    }

    /**
     * tr: Giriş yapan staff'ın fotoğrafını yükler ve cache'i commit sonrası düşürür.
     * en: Uploads the signed-in staff photo and evicts the cache after commit.
     */
    @PostMapping(value = "/for/staff/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PhotoResponse uploadStaffPhoto(HttpServletRequest request,
                                          @RequestPart("file") MultipartFile file,
                                          @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return staffMediaService.uploadStaffPhoto(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                file,
                acceptLanguage);
    }

    /**
     * tr: Giriş yapan staff'ın fotoğrafını döner. Cache miss olursa DB'den dolar.
     * en: Returns the signed-in staff photo. A cache miss loads from the database.
     */
    @GetMapping(value = "/for/staff/me", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> getStaffPhoto(HttpServletRequest request,
                                                @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return staffMediaService.getStaffPhoto(
                bookingStaffRequestAuth.userId(request),
                bookingStaffRequestAuth.mustChangePassword(request),
                acceptLanguage);
    }
}
