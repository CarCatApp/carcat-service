package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.response.GeneratePhotoResponse;
import com.carland.carland_service.dto.response.PhotoResponse;
import com.carland.carland_service.entity.*;
import com.carland.carland_service.enums.CarPhotoSource;
import com.carland.carland_service.enums.CarPhotoStatus;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.enums.UserStatus;
import com.carland.carland_service.exceptions.*;
import com.carland.carland_service.repository.*;
import com.carland.carland_service.service.CarAiPhotoGenerateLock;
import com.carland.carland_service.service.CarAiPhotoWorker;
import com.carland.carland_service.service.PhotoService;
import com.carland.carland_service.service.RedisCacheService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;


/**
 * tr: Araç, kullanıcı profil ve partner fotoğraflarını (logo ve badge logo dahil) yöneten servis; fotoğraf yükleme, getirme ve silme işlemlerini yapar. Yüklemelerde Tika ile içerik tipini doğrular ve dosya adında path traversal saldırısı kontrolü yapar.
 * en: Service managing car, user profile and partner photos (including logo and badge logo); handles upload, retrieval and deletion. On uploads it verifies the content type with Tika and checks the file name for path traversal attacks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhotoServiceImpl implements PhotoService {

    private final CarPhotoRepository carPhotoRepository;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerPhotoRepository partnerPhotoRepository;
    private final BannerRepository bannerRepository;
    private final BannerPhotoRepository bannerPhotoRepository;
    private final BranchBadgeLogoRepository branchBadgeLogoRepository;
    private final BranchRepository branchRepository;
    private final PercentagePhotoRepository percentagePhotoRepository;
    private final PercentageEmptyPhotoRepository percentageEmptyPhotoRepository;
    private final ServiceCategoryPhotoRepository serviceCategoryPhotoRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final OfferedServicePhotoRepository offeredServicePhotoRepository;
    private final OfferedServiceRepository offeredServiceRepository;
    private final RedisCacheService redisCacheService;
    private final BrandRepository brandRepository;
    private final BrandLogoRepository brandLogoRepository;
    private final CarAiPhotoWorker carAiPhotoWorker;

    /**
     * tr: Verilen carId'ye ait araç fotoğrafını uygun Content-Type ile byte dizisi olarak döner. Header'lar eksikse MissingFieldException, fotoğraf yoksa ResourceNotFoundException fırlatır.
     * en: Returns the car photo for the given carId as a byte array with the proper Content-Type. Throws MissingFieldException if headers are missing and ResourceNotFoundException if the photo does not exist.
     */
    @Override
    public ResponseEntity<byte[]> getCarPhoto(
            String role,
            Long carId,
            String phoneNumber,
            String userIdHeader,
            String timezone,
            String acceptLanguage) {

        if (role == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(
                    MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        ResponseEntity<byte[]> cached = redisCacheService.getCarPhoto(carId);
        if (cached != null) {
            return cached;
        }

        CarPhoto carPhoto = carPhotoRepository.findByCarId(carId);

        if (carPhoto == null) {
            throw new ResourceNotFoundException(
                    MessagesLangValues.CAR_PHOTO_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        String fileType = carPhoto.getFileType();
        log.info("file type ============================== {}", fileType);

        if (fileType == null || fileType.isBlank()) {
            fileType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        if (!fileType.contains("/")) {
            fileType = "image/" + fileType.toLowerCase();
        }

        MediaType mediaType = MediaType.parseMediaType(fileType);
        log.info("media type file type ================= {}", mediaType.getType());
        byte[] bytes = carPhoto.getImageData() == null ? new byte[0] : carPhoto.getImageData();
        if (bytes.length > 0 && !CarPhotoStatus.isPending(carPhoto.getPhotoStatus())) {
            redisCacheService.putCarPhoto(carId, mediaType, bytes);
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(bytes);
    }

    @Override
    public ResponseEntity<byte[]> getCarPhotoV2(String role, Long carId, String phoneNumber, String userIdHeader,
                                                String timezone, String acceptLanguage) {
        if (role == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(
                    MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        CarPhoto carPhoto = carPhotoRepository.findByCarId(carId);
        if (carPhoto == null) {
            throw new ResourceNotFoundException(
                    MessagesLangValues.CAR_PHOTO_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        String status = carPhoto.getPhotoStatus();
        if (status == null || status.isBlank()) {
            status = (carPhoto.getImageData() != null && carPhoto.getImageData().length > 0)
                    ? CarPhotoStatus.READY
                    : CarPhotoStatus.FAILED;
        }
        String source = carPhoto.getPhotoSource();
        if (source == null || source.isBlank()) {
            source = (carPhoto.getImageData() != null && carPhoto.getImageData().length > 0)
                    ? CarPhotoSource.USER
                    : CarPhotoSource.DEFAULT;
        }

        String fileType = carPhoto.getFileType();
        if (fileType == null || fileType.isBlank()) {
            fileType = "image/webp";
        }
        if (!fileType.contains("/")) {
            fileType = "image/" + fileType.toLowerCase();
        }
        MediaType mediaType = MediaType.parseMediaType(fileType);
        byte[] bytes = carPhoto.getImageData() == null ? new byte[0] : carPhoto.getImageData();
        boolean ready = CarPhotoStatus.READY.equalsIgnoreCase(status);
        log.info("photo v2 | carId={} status={} storedBytes={} sentBytes={}",
                carId, status, bytes.length, ready ? bytes.length : 0);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("X-Photo-Status", status)
                .header("X-Photo-Source", source)
                .body(ready ? bytes : new byte[0]);
    }

    @Override
    @Transactional
    public GeneratePhotoResponse generateCarPhoto(Long carId, String role, String phoneNumber, String userIdHeader,
                                                  String timezone, String acceptLanguage) {
        if (carId == null || role == null || phoneNumber == null || userIdHeader == null || acceptLanguage == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        if (!role.equals(UserRoles.USER.name())) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }

        Car car = requireOwnedCar(carId, phoneNumber, userIdHeader, acceptLanguage);
        CarPhoto photo = carPhotoRepository.findByCarId(carId);
        int storedBytes = photo == null || photo.getImageData() == null ? 0 : photo.getImageData().length;
        log.info("generate restart | carId={} previousStatus={} storedBytes={}",
                carId, photo == null ? null : photo.getPhotoStatus(), storedBytes);

        if (photo == null) {
            photo = CarPhoto.builder()
                    .carId(carId)
                    .fileName("car " + carId + " image")
                    .fileType("webp")
                    .build();
        }
        String attemptKey = "gen:" + UUID.randomUUID();
        photo.setPhotoStatus(CarPhotoStatus.PENDING);
        photo.setImageData(null);
        photo.setFileType("webp");
        photo.setFileName("car " + carId + " image");
        photo.setPhotoSource(null);
        photo.setPromptKey(attemptKey);
        carPhotoRepository.save(photo);
        LocalDateTime now = LocalDateTime.now();
        car.setAiPhotoLastGenerateAt(now);
        car.setAiPhotoGenerateLockedUntil(CarAiPhotoGenerateLock.lockedUntilFrom(now));
        carRepository.save(car);
        redisCacheService.evictCarPhoto(carId);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        enqueueGenerateAfterCommit(carId, userIdHeader, attemptKey);
        return pendingResponse(car, acceptLanguage, photo.getPhotoSource());
    }

    /**
     * tr: Kullanıcının profil fotoğrafını userId + telefon numarasıyla bulup uygun Content-Type ile döner. Header'lar eksikse MissingFieldException, fotoğraf yoksa ResourceNotFoundException fırlatır.
     * en: Finds the user's profile photo by userId + phone number and returns it with the proper Content-Type. Throws MissingFieldException if headers are missing and ResourceNotFoundException if the photo does not exist.
     */
    @Override
    public ResponseEntity<byte[]> getUserPP(String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {
        if (role == null || phoneNumber == null || userIdHeader == null) {
            throw new MissingFieldException(
                    MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        ResponseEntity<byte[]> cached = redisCacheService.getUserPhoto(userIdHeader);
        if (cached != null) {
            return cached;
        }

        UserPhoto userPhoto = userPhotoRepository.findByUserIdAndUserPhoneNumber(Long.valueOf(userIdHeader), phoneNumber);

        if (userPhoto == null) {
            throw new ResourceNotFoundException(
                    MessagesLangValues.CAR_PHOTO_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        String fileType = userPhoto.getFileType();
        log.info("file type ============================== {}", fileType);
        if (fileType == null || fileType.isBlank()) {
            fileType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        if (!fileType.contains("/")) {
            fileType = "image/" + fileType.toLowerCase();
        }

        MediaType mediaType = MediaType.parseMediaType(fileType);
        log.info("media type file type ================= {}", mediaType.getType());

        redisCacheService.putUserPhoto(userIdHeader, mediaType, userPhoto.getImageData());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(userPhoto.getImageData());
    }

    /**
     * tr: Partner logosunu yükler; mevcut fotoğraf varsa siler, Tika ile içerik tipini kontrol eder (görsel değilse InvalidStatusException) ve yenisini kaydeder. file/partnerId eksikse MissingFieldException, partner bulunamazsa ResourceNotFoundException, IO hatasında FileStorageException fırlatır.
     * en: Uploads the partner logo; deletes the existing photo if present, verifies the content type with Tika (InvalidStatusException if not an image), and saves the new one. Throws MissingFieldException when file/partnerId is missing, ResourceNotFoundException when the partner is not found, and FileStorageException on IO errors.
     */
    @Override
    @Transactional
    public PhotoResponse uploadPartnerPhoto(MultipartFile file, Long partnerId) {
        if (file == null || partnerId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Avto Servis tapilmadi"));

        try {
            checkAttack(file, null);

            PartnerPhoto existPhoto = partnerPhotoRepository.findByPartnerId(partner.getId());

            if (existPhoto != null) {
                partnerPhotoRepository.delete(existPhoto);
            }

            Tika tika = new Tika();
            String detectedType = tika.detect(file.getBytes());

            if (!detectedType.startsWith("image/")) {
                throw new InvalidStatusException(MessagesLangValues.INVALID_PHOTO_FORMAT.getMessageByLang(null));
            }

            String fileType = detectedType.substring("image/".length());

            PartnerPhoto partnerPhoto = PartnerPhoto.builder()
                    .fileName("partner " + partner.getId() + " image")
                    .fileType(fileType)
                    .partnerId(partner.getId())
                    .imageData(file.getBytes())
                    .build();

            partnerPhotoRepository.save(partnerPhoto);
            redisCacheService.evictPartnerPhotoAfterCommit(partner.getId());

            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    /**
     * tr: Verilen partnerId'ye ait partner logosunu uygun Content-Type ile döner. partnerId null ise MissingFieldException, fotoğraf yoksa ResourceNotFoundException fırlatır.
     * en: Returns the partner logo for the given partnerId with the proper Content-Type. Throws MissingFieldException if partnerId is null and ResourceNotFoundException if the photo does not exist.
     */
    @Override
    public ResponseEntity<byte[]> getPartnerPhotoById(Long partnerId) {
        if (partnerId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }

        ResponseEntity<byte[]> cached = redisCacheService.getPartnerPhoto(partnerId);
        if (cached != null) {
            return cached;
        }

        PartnerPhoto partnerPhoto = partnerPhotoRepository.findByPartnerId(partnerId);

        if (partnerPhoto == null) {
            throw new ResourceNotFoundException(
                    MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(null));
        }

        String fileType = partnerPhoto.getFileType();
        log.info("file type ============================== {}", fileType);

        if (fileType == null || fileType.isBlank()) {
            fileType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        if (!fileType.contains("/")) {
            fileType = "image/" + fileType.toLowerCase();
        }

        MediaType mediaType = MediaType.parseMediaType(fileType);
        log.info("media type file type ================= {}", mediaType.getType());

        byte[] bytes = partnerPhoto.getImageData();
        if (bytes != null && bytes.length > 0) {
            redisCacheService.putPartnerPhoto(partnerId, mediaType, bytes);
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(bytes);
    }

    /**
     * tr: adds veya info banner fotoğrafını değiştirir. service tipine yazılmaz.
     *     Redis: commit sonrası photo:banner:{bannerId} silinir. Put yok.
     * en: Replaces the adds or info banner photo. A service banner is rejected.
     *     Redis: photo:banner:{bannerId} is deleted after commit. No put.
     */
    @Override
    @Transactional
    public PhotoResponse replaceBannerPhoto(MultipartFile file, Long bannerId) {
        if (file == null || file.isEmpty() || bannerId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("banner not found"));
        String type = banner.getType() == null || banner.getType().isBlank() ? Banner.SERVICE : banner.getType();
        if (Banner.SERVICE.equals(type)) {
            throw new InvalidStatusException("banner photo is only for adds and info");
        }
        try {
            checkAttack(file, null);
            BannerPhoto existing = bannerPhotoRepository.findByBannerId(bannerId);
            if (existing != null) {
                bannerPhotoRepository.delete(existing);
            }
            Tika tika = new Tika();
            String detectedType = tika.detect(file.getBytes());
            if (!detectedType.startsWith("image/")) {
                throw new InvalidStatusException(MessagesLangValues.INVALID_PHOTO_FORMAT.getMessageByLang(null));
            }
            BannerPhoto photo = BannerPhoto.builder()
                    .fileName("banner " + bannerId + " image")
                    .fileType(detectedType.substring("image/".length()))
                    .bannerId(bannerId)
                    .imageData(file.getBytes())
                    .build();
            bannerPhotoRepository.save(photo);
            redisCacheService.evictBannerPhotoAfterCommit(bannerId);
            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    /**
     * tr: Banner fotoğrafını döner. Cache photo:banner:{bannerId}. Miss DB'den dolar.
     * en: Returns the banner photo. Cache photo:banner:{bannerId}. A miss loads from the DB.
     */
    @Override
    public ResponseEntity<byte[]> getBannerPhotoById(Long bannerId) {
        if (bannerId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        ResponseEntity<byte[]> cached = redisCacheService.getBannerPhoto(bannerId);
        if (cached != null) {
            return cached;
        }
        BannerPhoto photo = bannerPhotoRepository.findByBannerId(bannerId);
        if (photo == null) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(null));
        }
        String fileType = photo.getFileType();
        if (fileType == null || fileType.isBlank()) {
            fileType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        if (!fileType.contains("/")) {
            fileType = "image/" + fileType.toLowerCase();
        }
        MediaType mediaType = MediaType.parseMediaType(fileType);
        byte[] bytes = photo.getImageData();
        if (bytes != null && bytes.length > 0) {
            redisCacheService.putBannerPhoto(bannerId, mediaType, bytes);
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(bytes);
    }

    /**
     * tr: Şube rozetini yükler; varsa eskisini siler. file/branchId yoksa MissingFieldException, şube yoksa ResourceNotFoundException, görsel değilse InvalidStatusException.
     * en: Uploads the branch badge and replaces the previous one. MissingFieldException when file/branchId is missing, ResourceNotFoundException when the branch is missing, InvalidStatusException when the file is not an image.
     */
    @Override
    @Transactional
    public PhotoResponse uploadBranchBadgeLogo(MultipartFile file, Long branchId) {
        if (file == null || branchId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }

        if (branchRepository.findById(branchId).isEmpty()) {
            throw new ResourceNotFoundException("Şöbə tapılmadı");
        }

        try {
            checkAttack(file, null);

            Tika tika = new Tika();
            String detectedType = tika.detect(file.getBytes());
            if (!detectedType.startsWith("image/")) {
                throw new InvalidStatusException(MessagesLangValues.INVALID_PHOTO_FORMAT.getMessageByLang(null));
            }

            BranchBadgeLogo badge = branchBadgeLogoRepository.findByBranchId(branchId);
            if (badge == null) {
                badge = BranchBadgeLogo.builder().branchId(branchId).build();
            }
            badge.setFileName("branch " + branchId + " badge logo");
            badge.setFileType(detectedType.substring("image/".length()));
            badge.setImageData(file.getBytes());
            branchBadgeLogoRepository.save(badge);

            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    /**
     * tr: Şube rozetini Content-Type ile döner. branchId null ise MissingFieldException, logo yoksa ResourceNotFoundException.
     * en: Returns the branch badge with its Content-Type. MissingFieldException when branchId is null, ResourceNotFoundException when the logo is missing.
     */
    @Override
    public ResponseEntity<byte[]> getBranchBadgeLogoById(Long branchId) {
        if (branchId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }

        BranchBadgeLogo badge = branchBadgeLogoRepository.findByBranchId(branchId);
        if (badge == null) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(null));
        }

        String fileType = badge.getFileType();
        if (fileType == null || fileType.isBlank()) {
            fileType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        if (!fileType.contains("/")) {
            fileType = "image/" + fileType.toLowerCase();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileType))
                .body(badge.getImageData());
    }

    /**
     * tr: Sahiplik kontrolü yapmadan verilen carId'ye ait araç fotoğrafını siler (admin/dahili kullanım). acceptLanguage null ise MissingFieldException, araç veya fotoğraf bulunamazsa ResourceNotFoundException fırlatır.
     * en: Deletes the car photo for the given carId without ownership checks (admin/internal use). Throws MissingFieldException if acceptLanguage is null and ResourceNotFoundException if the car or photo is not found.
     */
    @Override
    public PhotoResponse deleteOtherCarPhoto(Long carId, String acceptLanguage) {
        if ( acceptLanguage == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }



        Car car = carRepository.findByCarId(carId);

        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        CarPhoto carPhoto = carPhotoRepository.findByCarId(carId);

        if (carPhoto == null) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        carPhotoRepository.delete(carPhoto);
        redisCacheService.evictCarPhoto(carId);
        redisCacheService.evictCarListAfterCommit(redisCacheService.ownerUserId(car));

        return PhotoResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }


    /**
     * tr: Kullanıcının kendi aracına fotoğraf yükler; rolün USER olmasını şart koşar (değilse InvalidStatusException), aktif müşteriyi ve müşteriye ait aracı doğrular (UserNotFoundException/ResourceNotFoundException), mevcut fotoğrafı silip Tika kontrolünden geçen yenisini kaydeder. Parametreler eksikse MissingFieldException, IO hatasında FileStorageException fırlatır.
     * en: Uploads a photo to the user's own car; requires the USER role (InvalidStatusException otherwise), validates the active customer and that the car belongs to them (UserNotFoundException/ResourceNotFoundException), deletes any existing photo and saves the new Tika-verified one. Throws MissingFieldException for missing parameters and FileStorageException on IO errors.
     */
    @Override
    @Transactional
    public PhotoResponse uploadCarPhoto(MultipartFile file, Long carId, String role, String phoneNumber, String userIdHeader,
                                        String timezone, String acceptLanguage) {
        if (file == null || role == null || phoneNumber == null || userIdHeader == null || acceptLanguage == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        if (!role.equals(UserRoles.USER.name())) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }

        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader),
                phoneNumber, UserStatus.ACTIVE.name());

        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        Car car = carRepository.findByCarIdAndCustomer(carId, customer);

        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        CarPhoto existPhoto = carPhotoRepository.findByCarId(carId);
        if (existPhoto != null && CarPhotoStatus.isPending(existPhoto.getPhotoStatus())) {
            throw new ConflictException(MessagesLangValues.PHOTO_AI_UPLOAD_BLOCKED.getMessageByLang(acceptLanguage));
        }

        try {
            checkAttack(file, acceptLanguage);

            if (existPhoto != null) {
                carPhotoRepository.delete(existPhoto);
            }

            Tika tika = new Tika();
            String detectedType = tika.detect(file.getBytes());

            if (!detectedType.startsWith("image/")) {
                throw new InvalidStatusException(MessagesLangValues.INVALID_PHOTO_FORMAT
                        .getMessageByLang(acceptLanguage));
            }

            String fileType = detectedType.substring("image/".length());


//            byte[] processedImageBytes = CustomImageCrop.resizeAndCropImage(file.getBytes(), fileType);

            CarPhoto carPhoto = CarPhoto.builder()
                    .fileName("car " + carId + " image")
                    .fileType(fileType)
                    .carId(carId)
                    .imageData(file.getBytes())
                    .photoStatus(CarPhotoStatus.READY)
                    .photoSource(CarPhotoSource.USER)
                    .promptKey(null)
                    .build();

            carPhotoRepository.save(carPhoto);
            redisCacheService.evictCarPhoto(carId);
            redisCacheService.evictCarListAfterCommit(userIdHeader);

            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(acceptLanguage));
        }
    }

    /**
     * tr: Kullanıcının kendi aracının fotoğrafını siler; USER rolü şarttır (InvalidStatusException), aktif müşteri ve müşteriye ait araç doğrulanır (UserNotFoundException/ResourceNotFoundException), fotoğraf yoksa ResourceNotFoundException fırlatır. Header'lar eksikse MissingFieldException oluşur.
     * en: Deletes the photo of the user's own car; requires the USER role (InvalidStatusException), validates the active customer and car ownership (UserNotFoundException/ResourceNotFoundException), and throws ResourceNotFoundException if no photo exists. Missing headers cause a MissingFieldException.
     */
    @Override
    public PhotoResponse deleteCarPhoto(String role, Long carId, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {

        if (role == null || phoneNumber == null || userIdHeader == null || acceptLanguage == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        if (!role.equals(UserRoles.USER.name())) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }

        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader),
                phoneNumber, UserStatus.ACTIVE.name());

        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        Car car = carRepository.findByCarIdAndCustomer(carId, customer);

        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        CarPhoto carPhoto = carPhotoRepository.findByCarId(carId);

        if (carPhoto == null) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        if (CarPhotoStatus.isPending(carPhoto.getPhotoStatus())) {
            throw new ConflictException(MessagesLangValues.PHOTO_AI_UPLOAD_BLOCKED.getMessageByLang(acceptLanguage));
        }

        carPhotoRepository.delete(carPhoto);
        redisCacheService.evictCarPhoto(carId);
        redisCacheService.evictCarListAfterCommit(userIdHeader);

        return PhotoResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    /**
     * tr: Kullanıcının profil fotoğrafını yükler; mevcut fotoğraf varsa siler, Tika ile içerik tipini doğrular (görsel değilse InvalidStatusException) ve yenisini kaydeder. Parametreler eksikse MissingFieldException, IO hatasında FileStorageException fırlatır.
     * en: Uploads the user's profile photo; deletes the existing one if present, verifies the content type with Tika (InvalidStatusException if not an image), and saves the new photo. Throws MissingFieldException for missing parameters and FileStorageException on IO errors.
     */
    @Override
    public PhotoResponse uploadUserPP(MultipartFile file, String role, String phoneNumber, String userIdHeader, String timezone, String acceptLanguage) {
        if (file == null || role == null || phoneNumber == null || userIdHeader == null || acceptLanguage == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }
        try {
            checkAttack(file, acceptLanguage);

            UserPhoto userPhoto = userPhotoRepository.findByUserIdAndUserPhoneNumber(Long.valueOf(userIdHeader), phoneNumber);
            if (userPhoto != null) {
                userPhotoRepository.delete(userPhoto);
            }
            Tika tika = new Tika();
            String detectedType = tika.detect(file.getBytes());

            if (!detectedType.startsWith("image/")) {
                throw new InvalidStatusException(MessagesLangValues.INVALID_PHOTO_FORMAT
                        .getMessageByLang(acceptLanguage));
            }

            String fileType = detectedType.substring("image/".length());


//            byte[] processedImageBytes = CustomImageCrop.resizeAndCropImage(file.getBytes(), fileType);

            UserPhoto newPhoto = UserPhoto.builder()
                    .fileName("user " + userIdHeader + " image")
                    .fileType(fileType)
                    .imageData(file.getBytes())
                    .userId(Long.valueOf(userIdHeader))
                    .userPhoneNumber(phoneNumber)
                    .build();

            userPhotoRepository.save(newPhoto);
            redisCacheService.evictUserPhoto(userIdHeader);

            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                    .build();

        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(acceptLanguage));
        }
    }

    /**
     * tr: Kullanıcının profil fotoğrafını siler. Header'lar eksikse MissingFieldException, fotoğraf yoksa ResourceNotFoundException fırlatır; başarıda mesaj içeren PhotoResponse döner.
     * en: Deletes the user's profile photo. Throws MissingFieldException for missing headers and ResourceNotFoundException if no photo exists; returns a PhotoResponse with a success message.
     */
    @Override
    public PhotoResponse deleteUserPP(String role, String phoneNumber, String userIdHeader, String timezone,
                                      String acceptLanguage) {
        if (role == null || phoneNumber == null || userIdHeader == null || acceptLanguage == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        UserPhoto userPhoto = userPhotoRepository.findByUserIdAndUserPhoneNumber(Long.valueOf(userIdHeader), phoneNumber);

        if (userPhoto == null) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(acceptLanguage));
        }

        userPhotoRepository.delete(userPhoto);
        redisCacheService.evictUserPhoto(userIdHeader);

        return PhotoResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }


    /**
     * tr: Servis kalemi ikonunu yükler; services satırı yoksa 404, görsel değilse InvalidStatusException.
     *     Mevcut satır varsa fileName/fileType/imageData güncellenir (INSERT yok); yoksa INSERT.
     *     Redis key commit sonrası DEL.
     * en: Uploads a maintenance-item icon; 404 when the services row is missing. Updates
     *     fileName/fileType/imageData in place when a row exists; otherwise INSERT. DELs Redis after commit.
     */
    @Override
    @Transactional
    public PhotoResponse uploadPercentagePhoto(MultipartFile file, Long serviceId) {
        if (file == null || serviceId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        if (!serviceEntityRepository.existsById(serviceId)) {
            throw new ResourceNotFoundException(MessagesLangValues.SERVICE_NOT_FOUND.getMessageByLang(null));
        }
        try {
            DetectedImage image = detectImage(file);
            PercentagePhoto photo = percentagePhotoRepository.findByServiceId(serviceId);
            if (photo == null) {
                photo = PercentagePhoto.builder().serviceId(serviceId).build();
            }
            photo.setFileName("percentage service " + serviceId + " image");
            photo.setFileType(image.fileType());
            photo.setImageData(image.bytes());
            percentagePhotoRepository.save(photo);
            redisCacheService.evictPercentagePhotoAfterCommit(serviceId);
            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    /**
     * tr: Servis ikonunu döner. Kendi fotosu varsa onu (ve Redis'e koyar); yoksa empty-state'i
     *     serviceId key'ine yazmadan döner. İkisi de yoksa 404. Olmayan serviceId de 404.
     * en: Returns the service icon. Caches a real photo under serviceId; empty fallback is never
     *     written to that key. 404 when the service is missing or both photos are missing.
     */
    @Override
    public ResponseEntity<byte[]> getPercentagePhoto(Long serviceId) {
        if (serviceId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        if (!serviceEntityRepository.existsById(serviceId)) {
            throw new ResourceNotFoundException(MessagesLangValues.SERVICE_NOT_FOUND.getMessageByLang(null));
        }

        ResponseEntity<byte[]> cached = redisCacheService.getPercentagePhoto(serviceId);
        if (cached != null) {
            return cached;
        }

        PercentagePhoto photo = percentagePhotoRepository.findByServiceId(serviceId);
        if (photo != null && photo.getImageData() != null && photo.getImageData().length > 0) {
            MediaType mediaType = mediaTypeOf(photo.getFileType());
            redisCacheService.putPercentagePhoto(serviceId, mediaType, photo.getImageData());
            return ResponseEntity.ok().contentType(mediaType).body(photo.getImageData());
        }

        return getPercentageEmptyPhoto();
    }

    /**
     * tr: Empty-state placeholder yükler. Satır varsa fileName/fileType/imageData güncellenir; yoksa INSERT.
     *     Redis empty key commit sonrası DEL.
     * en: Uploads the empty-state placeholder. Updates fileName/fileType/imageData in place when a row
     *     exists; otherwise INSERT. DELs the Redis empty key after commit.
     */
    @Override
    @Transactional
    public PhotoResponse uploadPercentageEmptyPhoto(MultipartFile file) {
        if (file == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        try {
            DetectedImage image = detectImage(file);
            PercentageEmptyPhoto photo = percentageEmptyPhotoRepository.findFirstByOrderByImageIdAsc()
                    .orElseGet(PercentageEmptyPhoto::new);
            photo.setFileName("percentage empty state image");
            photo.setFileType(image.fileType());
            photo.setImageData(image.bytes());
            percentageEmptyPhotoRepository.save(photo);
            redisCacheService.evictPercentageEmptyPhotoAfterCommit();
            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    /**
     * tr: Empty-state ikonunu Redis sonra DB'den döner; yoksa 404.
     * en: Returns the empty-state icon from Redis then DB; 404 when missing.
     */
    @Override
    public ResponseEntity<byte[]> getPercentageEmptyPhoto() {
        ResponseEntity<byte[]> cached = redisCacheService.getPercentageEmptyPhoto();
        if (cached != null) {
            return cached;
        }
        PercentageEmptyPhoto empty = percentageEmptyPhotoRepository.findFirstByOrderByImageIdAsc()
                .orElse(null);
        if (empty == null || empty.getImageData() == null || empty.getImageData().length == 0) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(null));
        }
        MediaType mediaType = mediaTypeOf(empty.getFileType());
        redisCacheService.putPercentageEmptyPhoto(mediaType, empty.getImageData());
        return ResponseEntity.ok().contentType(mediaType).body(empty.getImageData());
    }

    /**
     * tr: Kategori ikonunu Redis sonra DB'den döner. Kategori veya foto yoksa 404. Empty-state yok.
     * en: Returns the category icon from Redis then DB. 404 when the category or photo is missing. No empty-state.
     */
    @Override
    public ResponseEntity<byte[]> getServiceCategoryPhoto(Long categoryId) {
        if (categoryId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        if (!serviceCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("category not found");
        }
        ResponseEntity<byte[]> cached = redisCacheService.getServiceCategoryPhoto(categoryId);
        if (cached != null && cached.getBody() != null && cached.getBody().length > 0) {
            return cached;
        }
        ServiceCategoryPhoto photo = serviceCategoryPhotoRepository.findByCategoryId(categoryId);
        if (photo == null || photo.getImageData() == null || photo.getImageData().length == 0) {
            throw new ResourceNotFoundException("category photo not found");
        }
        MediaType mediaType = mediaTypeOf(photo.getFileType());
        redisCacheService.putServiceCategoryPhoto(categoryId, mediaType, photo.getImageData());
        return ResponseEntity.ok().contentType(mediaType).body(photo.getImageData());
    }

    /**
     * tr: Kategori ikonunu yükler. Satır varsa byte güncellenir. Redis commit sonrası DEL.
     * en: Uploads a category icon. Updates bytes when a row exists. DELs Redis after commit.
     */
    @Override
    @Transactional
    public PhotoResponse uploadServiceCategoryPhoto(MultipartFile file, Long categoryId) {
        if (file == null || categoryId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        if (!serviceCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("category not found");
        }
        try {
            DetectedImage image = detectImage(file);
            ServiceCategoryPhoto photo = serviceCategoryPhotoRepository.findByCategoryId(categoryId);
            if (photo == null) {
                photo = ServiceCategoryPhoto.builder().categoryId(categoryId).build();
            }
            photo.setFileName("service category " + categoryId + " image");
            photo.setFileType(image.fileType());
            photo.setImageData(image.bytes());
            serviceCategoryPhotoRepository.save(photo);
            redisCacheService.evictServiceCategoryPhotoAfterCommit(categoryId);
            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    /**
     * tr: Kategori ikon satırını siler. Foto yoksa 404. Redis commit sonrası DEL.
     * en: Deletes the category icon row. 404 when no photo. DELs Redis after commit.
     */
    @Override
    @Transactional
    public PhotoResponse deleteServiceCategoryPhoto(Long categoryId) {
        if (categoryId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        if (!serviceCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("category not found");
        }
        if (!serviceCategoryPhotoRepository.existsByCategoryId(categoryId)) {
            throw new ResourceNotFoundException("category photo not found");
        }
        serviceCategoryPhotoRepository.deleteByCategoryId(categoryId);
        redisCacheService.evictServiceCategoryPhotoAfterCommit(categoryId);
        return PhotoResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                .build();
    }

    /**
     * tr: Offered service ikonunu döner. Redis HIT ise offered_services count yok.
     *     Miss'te servis veya foto yoksa 404. Key photo:offered-service:{id}.
     *     Yükleme commit sonrası key'i siler; silinmiş servis en fazla TTL kadar eski ikonu dönebilir.
     * en: Returns the offered-service icon. A Redis HIT skips the offered_services count.
     *     On a miss, 404 when the service or the photo is missing. Key photo:offered-service:{id}.
     *     Upload deletes the key after commit; a removed service can still serve the icon until TTL.
     */
    @Override
    public ResponseEntity<byte[]> getOfferedServicePhoto(Long offeredServiceId) {
        if (offeredServiceId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        ResponseEntity<byte[]> cached = redisCacheService.getOfferedServicePhoto(offeredServiceId);
        if (cached != null && cached.getBody() != null && cached.getBody().length > 0) {
            return cached;
        }
        if (!offeredServiceRepository.existsById(offeredServiceId)) {
            throw new ResourceNotFoundException("service not found");
        }
        OfferedServicePhoto photo = offeredServicePhotoRepository.findByOfferedServiceId(offeredServiceId);
        if (photo == null || photo.getImageData() == null || photo.getImageData().length == 0) {
            throw new ResourceNotFoundException("offered service photo not found");
        }
        MediaType mediaType = mediaTypeOf(photo.getFileType());
        redisCacheService.putOfferedServicePhoto(offeredServiceId, mediaType, photo.getImageData());
        return ResponseEntity.ok().contentType(mediaType).body(photo.getImageData());
    }

    /**
     * tr: Offered service ikonunu yükler. Satır varsa byte güncellenir. Redis commit sonrası DEL.
     * en: Uploads an offered-service icon. Updates bytes when a row exists. DELs Redis after commit.
     */
    @Override
    @Transactional
    public PhotoResponse uploadOfferedServicePhoto(MultipartFile file, Long offeredServiceId) {
        if (file == null || offeredServiceId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        if (!offeredServiceRepository.existsById(offeredServiceId)) {
            throw new ResourceNotFoundException("service not found");
        }
        try {
            DetectedImage image = detectImage(file);
            OfferedServicePhoto photo = offeredServicePhotoRepository.findByOfferedServiceId(offeredServiceId);
            if (photo == null) {
                photo = OfferedServicePhoto.builder().offeredServiceId(offeredServiceId).build();
            }
            photo.setFileName("offered service " + offeredServiceId + " image");
            photo.setFileType(image.fileType());
            photo.setImageData(image.bytes());
            offeredServicePhotoRepository.save(photo);
            redisCacheService.evictOfferedServicePhotoAfterCommit(offeredServiceId);
            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    /**
     * tr: Yüklenen dosyanın adında ".." (path traversal) olup olmadığını kontrol eder; varsa MissingFieldException fırlatır.
     * en: Checks whether the uploaded file's name contains ".." (path traversal); throws MissingFieldException if it does.
     */
    public void checkAttack(MultipartFile file, String acceptLanguage) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains("..")) {
            throw new MissingFieldException(MessagesLangValues.INVALID_PHOTO_NAME.getMessageByLang(acceptLanguage));

        }
    }

    private Car requireOwnedCar(Long carId, String phoneNumber, String userIdHeader, String acceptLanguage) {
        Customer customer = customerRepository.findByUserIdAndPhoneNumberAndStatus(Long.valueOf(userIdHeader),
                phoneNumber, UserStatus.ACTIVE.name());
        if (customer == null) {
            throw new UserNotFoundException(MessagesLangValues.USER_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        Car car = carRepository.findByCarIdAndCustomer(carId, customer);
        if (car == null) {
            throw new ResourceNotFoundException(MessagesLangValues.CAR_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        return car;
    }

    private static GeneratePhotoResponse pendingResponse(Car car, String acceptLanguage, String photoSource) {
        return GeneratePhotoResponse.builder()
                .carId(car.getCarId())
                .photoStatus(CarPhotoStatus.PENDING)
                .photoSource(photoSource)
                .message(MessagesLangValues.PHOTO_AI_PREPARING.getMessageByLang(acceptLanguage))
                .lockedUntil(car.getAiPhotoGenerateLockedUntil())
                .remainingSeconds(CarAiPhotoGenerateLock.remainingSeconds(car.getAiPhotoGenerateLockedUntil()))
                .build();
    }

    private void enqueueGenerateAfterCommit(Long carId, String userIdHeader, String attemptKey) {
        Runnable job = () -> carAiPhotoWorker.generate(carId, userIdHeader, attemptKey);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
        }
    }

    /**
     * tr: Marka logosu. Cache miss DB'den dolar. Logo yoksa 404.
     * en: Brand logo. A cache miss loads from the database. 404 when the logo is missing.
     */
    @Override
    public ResponseEntity<byte[]> getBrandLogo(Long brandId) {
        if (brandId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        if (!brandRepository.existsById(brandId)) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(null));
        }
        ResponseEntity<byte[]> cached = redisCacheService.getBrandLogo(brandId);
        if (cached != null) {
            return cached;
        }
        BrandLogo photo = brandLogoRepository.findByBrandId(brandId);
        if (photo == null || photo.getImageData() == null || photo.getImageData().length == 0) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(null));
        }
        MediaType mediaType = mediaTypeOf(photo.getFileType());
        redisCacheService.putBrandLogo(brandId, mediaType, photo.getImageData());
        return ResponseEntity.ok().contentType(mediaType).body(photo.getImageData());
    }

    /**
     * tr: Marka logosu yükler. Eski satır silinir, yeni satır yazılır, Redis commit sonrası düşer.
     * en: Uploads a brand logo. Deletes the old row, writes a new row, and evicts Redis after commit.
     */
    @Override
    @Transactional
    public PhotoResponse uploadBrandLogo(MultipartFile file, Long brandId) {
        if (file == null || brandId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(null)));
        try {
            DetectedImage image = detectImage(file);
            BrandLogo existing = brandLogoRepository.findByBrandId(brandId);
            if (existing != null) {
                brandLogoRepository.delete(existing);
                brandLogoRepository.flush();
            }
            brandLogoRepository.save(BrandLogo.builder()
                    .brandId(brand.getBrandId())
                    .brandName(brand.getBrandName())
                    .fileName(brand.getBrandName())
                    .fileType(image.fileType())
                    .imageData(image.bytes())
                    .build());
            redisCacheService.evictBrandLogoAfterCommit(brandId);
            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    private DetectedImage detectImage(MultipartFile file) throws IOException {
        checkAttack(file, null);
        byte[] bytes = file.getBytes();
        Tika tika = new Tika();
        String detectedType = tika.detect(bytes);
        if (!detectedType.startsWith("image/")) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_PHOTO_FORMAT.getMessageByLang(null));
        }
        return new DetectedImage(detectedType.substring("image/".length()), bytes);
    }

    private static MediaType mediaTypeOf(String fileType) {
        String type = fileType;
        if (type == null || type.isBlank()) {
            type = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        if (!type.contains("/")) {
            type = "image/" + type.toLowerCase();
        }
        return MediaType.parseMediaType(type);
    }

    private record DetectedImage(String fileType, byte[] bytes) {
    }
}
