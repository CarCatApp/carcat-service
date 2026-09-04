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
import java.time.Duration;
import java.time.LocalDateTime;


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
    private final PartnerBadgeLogoRepository partnerBadgeLogoRepository;
    private final RedisCacheService redisCacheService;
    private final CarAiPhotoWorker carAiPhotoWorker;

    private static final Duration GENERATE_RATE_LIMIT = Duration.ofMinutes(5);
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

        var response = ResponseEntity.ok()
                .contentType(mediaType)
                .header("X-Photo-Status", status)
                .header("X-Photo-Source", source);
        if (CarPhotoStatus.FAILED.equalsIgnoreCase(status)) {
            response.header("X-Photo-Message",
                    MessagesLangValues.PHOTO_AI_FAILED.getMessageByLang(acceptLanguage));
        }
        return response.body(CarPhotoStatus.READY.equalsIgnoreCase(status) ? bytes : new byte[0]);
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

        if (photo != null && CarPhotoStatus.isPending(photo.getPhotoStatus())) {
            return pendingResponse(carId, acceptLanguage);
        }

        LocalDateTime last = car.getAiPhotoLastGenerateAt();
        if (last != null && last.isAfter(LocalDateTime.now().minus(GENERATE_RATE_LIMIT))) {
            throw new TooManyRequestsException(
                    MessagesLangValues.PHOTO_AI_GENERATE_LIMIT.getMessageByLang(acceptLanguage));
        }

        if (photo == null) {
            photo = CarPhoto.builder()
                    .carId(carId)
                    .fileName("car " + carId + " image")
                    .fileType("webp")
                    .imageData(null)
                    .build();
        }
        photo.setPhotoStatus(CarPhotoStatus.PENDING);
        carPhotoRepository.save(photo);
        car.setAiPhotoLastGenerateAt(LocalDateTime.now());
        carRepository.save(car);
        redisCacheService.evictCarPhoto(carId);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        enqueueGenerateAfterCommit(carId, userIdHeader);
        return pendingResponse(carId, acceptLanguage);
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

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(partnerPhoto.getImageData());
    }

    /**
     * tr: Partner badge (rozet) logosunu yükler; mevcut logo varsa siler, Tika ile içerik tipini kontrol eder (görsel değilse InvalidStatusException) ve yenisini kaydeder. file/partnerId eksikse MissingFieldException, partner bulunamazsa ResourceNotFoundException, IO hatasında FileStorageException fırlatır.
     * en: Uploads the partner badge logo; deletes the existing logo if present, verifies the content type with Tika (InvalidStatusException if not an image), and saves the new one. Throws MissingFieldException when file/partnerId is missing, ResourceNotFoundException when the partner is not found, and FileStorageException on IO errors.
     */
    @Override
    @Transactional
    public PhotoResponse uploadPartnerBadgeLogo(MultipartFile file, Long partnerId) {
        if (file == null || partnerId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }

        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Avto Servis tapilmadi"));

        try {
            checkAttack(file, null);

            PartnerBadgeLogo existLogo = partnerBadgeLogoRepository.findByPartnerId(partner.getId());

            if (existLogo != null) {
                partnerBadgeLogoRepository.delete(existLogo);
            }

            Tika tika = new Tika();
            String detectedType = tika.detect(file.getBytes());

            if (!detectedType.startsWith("image/")) {
                throw new InvalidStatusException(MessagesLangValues.INVALID_PHOTO_FORMAT.getMessageByLang(null));
            }

            String fileType = detectedType.substring("image/".length());

            PartnerBadgeLogo partnerBadgeLogo = PartnerBadgeLogo.builder()
                    .fileName("partner " + partner.getId() + " badge logo")
                    .fileType(fileType)
                    .partnerId(partner.getId())
                    .imageData(file.getBytes())
                    .build();

            partnerBadgeLogoRepository.save(partnerBadgeLogo);

            return PhotoResponse.builder()
                    .message(MessagesLangValues.SUCCESS.getMessageByLang(null))
                    .build();
        } catch (IOException e) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    /**
     * tr: Verilen partnerId'ye ait badge logosunu uygun Content-Type ile döner. partnerId null ise MissingFieldException, logo yoksa ResourceNotFoundException fırlatır.
     * en: Returns the badge logo for the given partnerId with the proper Content-Type. Throws MissingFieldException if partnerId is null and ResourceNotFoundException if the logo does not exist.
     */
    @Override
    public ResponseEntity<byte[]> getPartnerBadgeLogoById(Long partnerId) {
        if (partnerId == null) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }

        PartnerBadgeLogo partnerBadgeLogo = partnerBadgeLogoRepository.findByPartnerId(partnerId);

        if (partnerBadgeLogo == null) {
            throw new ResourceNotFoundException(
                    MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(null));
        }

        String fileType = partnerBadgeLogo.getFileType();
        log.info("file type ============================== {}", fileType);

        if (fileType == null || fileType.isBlank()) {
            fileType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        if (!fileType.contains("/")) {
            fileType = "image/" + fileType.toLowerCase();
        }

        MediaType mediaType = MediaType.parseMediaType(fileType);
        log.info("media type file type ================= {}", mediaType.getType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(partnerBadgeLogo.getImageData());
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
     * tr: Yüklenen dosyanın adında ".." (path traversal) olup olmadığını kontrol eder; varsa MissingFieldException fırlatır.
     * en: Checks whether the uploaded file's name contains ".." (path traversal); throws MissingFieldException if it does.
     */
    public void checkAttack(MultipartFile file, String acceptLanguage) {
        if (file.getOriginalFilename().contains("..")) {
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

    private static GeneratePhotoResponse pendingResponse(Long carId, String acceptLanguage) {
        return GeneratePhotoResponse.builder()
                .carId(carId)
                .photoStatus(CarPhotoStatus.PENDING)
                .message(MessagesLangValues.PHOTO_AI_PREPARING.getMessageByLang(acceptLanguage))
                .build();
    }

    private void enqueueGenerateAfterCommit(Long carId, String userIdHeader) {
        Runnable job = () -> carAiPhotoWorker.generate(carId, userIdHeader);
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
}
