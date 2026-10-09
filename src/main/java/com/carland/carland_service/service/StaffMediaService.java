package com.carland.carland_service.service;

import com.carland.carland_service.dto.response.PhotoResponse;
import com.carland.carland_service.entity.BookingStaff;
import com.carland.carland_service.entity.Branch;
import com.carland.carland_service.entity.BranchPhoto;
import com.carland.carland_service.entity.StaffPhoto;
import com.carland.carland_service.entity.UserPhoto;
import com.carland.carland_service.enums.BookingStaffRole;
import com.carland.carland_service.enums.UserRoles;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.FileStorageException;
import com.carland.carland_service.exceptions.ForbiddenException;
import com.carland.carland_service.exceptions.InvalidStatusException;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.exceptions.ResourceNotFoundException;
import com.carland.carland_service.repository.BookingRepository;
import com.carland.carland_service.repository.BranchPhotoRepository;
import com.carland.carland_service.repository.BranchRepository;
import com.carland.carland_service.repository.StaffPhotoRepository;
import com.carland.carland_service.repository.UserPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * tr: Şube fotoğrafı ve staff fotoğrafı. Partner logosu değildir.
 *     Şube yüklemesi yalnızca şube adminidir. Staff fotoğrafı photo:staff:{userId} cache'ini commit sonrası siler.
 * en: Branch photo and staff photo. Not the partner logo.
 *     Only a branch admin uploads the branch photo. Staff photo evicts photo:staff:{userId} after commit.
 */
@Service
@RequiredArgsConstructor
public class StaffMediaService {

    private final BookingStaffAccess bookingStaffAccess;
    private final BranchRepository branchRepository;
    private final BranchPhotoRepository branchPhotoRepository;
    private final StaffPhotoRepository staffPhotoRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final BookingRepository bookingRepository;
    private final RedisCacheService redisCacheService;

    /**
     * tr: Şubenin tek fotoğrafını yazar. Partner admin yasaktır.
     * en: Writes the branch's single photo. Partner admin is rejected.
     */
    @Transactional
    public PhotoResponse uploadBranchPhoto(Long userId, boolean mustChangePassword, MultipartFile file,
                                            String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        if (BookingStaffRole.PARTNER_ADMIN.name().equals(staff.getRole())) {
            throw new ForbiddenException("Partner admin cannot upload a branch photo");
        }
        if (staff.getBranch() == null) {
            throw new ForbiddenException("branch required");
        }
        byte[] bytes = imageBytes(file);
        String fileType = fileType(bytes, acceptLanguage);
        BranchPhoto existing = branchPhotoRepository.findByBranchId(staff.getBranch().getId());
        if (existing != null) {
            branchPhotoRepository.delete(existing);
            branchPhotoRepository.flush();
        }
        branchPhotoRepository.save(BranchPhoto.builder()
                .branchId(staff.getBranch().getId())
                .fileName("branch " + staff.getBranch().getId())
                .fileType(fileType)
                .imageData(bytes)
                .build());
        Branch branch = staff.getBranch();
        branch.setPhoto("/api/v1/photo/for/branch/get/" + branch.getId());
        branchRepository.save(branch);
        redisCacheService.evictBranchPhotoAfterCommit(branch.getId());
        return PhotoResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getBranchPhoto(Long userId, String role, Long branchId, String acceptLanguage) {
        if (!bookingStaffAccess.isActiveStaff(userId)
                && (userId == null || role == null || !UserRoles.USER.name().equalsIgnoreCase(role.trim()))) {
            throw new InvalidStatusException(MessagesLangValues.INVALID_ROLE_PERMISSION.getMessageByLang(acceptLanguage));
        }
        ResponseEntity<byte[]> cached = redisCacheService.getBranchPhoto(branchId);
        if (cached != null) {
            return cached;
        }
        BranchPhoto photo = branchPhotoRepository.findByBranchId(branchId);
        if (photo == null || photo.getImageData() == null) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        MediaType mediaType = mediaType(photo.getFileType());
        redisCacheService.putBranchPhoto(branchId, mediaType, photo.getImageData());
        return ResponseEntity.ok().contentType(mediaType).body(photo.getImageData());
    }

    /**
     * tr: Giriş yapan staff'ın fotoğrafını yazar ve cache'i commit sonrası düşürür.
     * en: Writes the signed-in staff photo and evicts the cache after commit.
     */
    @Transactional
    public PhotoResponse uploadStaffPhoto(Long userId, boolean mustChangePassword, MultipartFile file,
                                           String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        byte[] bytes = imageBytes(file);
        String fileType = fileType(bytes, acceptLanguage);
        StaffPhoto existing = staffPhotoRepository.findByUserId(staff.getUserId());
        if (existing != null) {
            staffPhotoRepository.delete(existing);
            staffPhotoRepository.flush();
        }
        staffPhotoRepository.save(StaffPhoto.builder()
                .userId(staff.getUserId())
                .fileName("staff " + staff.getUserId())
                .fileType(fileType)
                .imageData(bytes)
                .build());
        redisCacheService.evictStaffPhotoAfterCommit(staff.getUserId());
        return PhotoResponse.builder()
                .message(MessagesLangValues.SUCCESS.getMessageByLang(acceptLanguage))
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getStaffPhoto(Long userId, boolean mustChangePassword, String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        ResponseEntity<byte[]> cached = redisCacheService.getStaffPhoto(staff.getUserId());
        if (cached != null) {
            return cached;
        }
        StaffPhoto photo = staffPhotoRepository.findByUserId(staff.getUserId());
        if (photo != null && photo.getImageData() != null) {
            MediaType mediaType = mediaType(photo.getFileType());
            redisCacheService.putStaffPhoto(staff.getUserId(), mediaType, photo.getImageData());
            return ResponseEntity.ok().contentType(mediaType).body(photo.getImageData());
        }
        return userProfilePhoto(staff, acceptLanguage);
    }

    /**
     * tr: Staff fotoğrafı yoksa user_photos döner. Miss olunca photo:user dolar.
     *     Silen: PhotoServiceImpl kullanıcı foto upload/delete.
     * en: Falls back to user_photos. A miss fills photo:user. PhotoServiceImpl evicts that key.
     */
    private ResponseEntity<byte[]> userProfilePhoto(BookingStaff staff, String acceptLanguage) {
        String userKey = String.valueOf(staff.getUserId());
        ResponseEntity<byte[]> cached = redisCacheService.getUserPhoto(userKey);
        if (cached != null) {
            return cached;
        }
        UserPhoto userPhoto = null;
        if (staff.getPhoneNumber() != null && !staff.getPhoneNumber().isBlank()) {
            userPhoto = userPhotoRepository.findByUserIdAndUserPhoneNumber(staff.getUserId(), staff.getPhoneNumber());
        }
        if (userPhoto == null || userPhoto.getImageData() == null) {
            userPhoto = userPhotoRepository.findFirstByUserIdOrderByImageIdDesc(staff.getUserId());
        }
        if (userPhoto == null || userPhoto.getImageData() == null) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        MediaType mediaType = mediaType(userPhoto.getFileType());
        redisCacheService.putUserPhoto(userKey, mediaType, userPhoto.getImageData());
        return ResponseEntity.ok().contentType(mediaType).body(userPhoto.getImageData());
    }

    /**
     * tr: Müşteri profil fotoğrafı. Yalnız bu şubenin booking'i olan kullanıcı. Cache photo:user.
     *     Silen: PhotoServiceImpl kullanıcı foto upload/delete.
     * en: Customer profile photo, only when that user has a booking on this staff's branch. Cache photo:user.
     *     Evict: PhotoServiceImpl user photo upload/delete.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getCustomerPhoto(Long userId, boolean mustChangePassword, Long customerUserId,
                                                   String acceptLanguage) {
        BookingStaff staff = bookingStaffAccess.requireStaff(userId, mustChangePassword, acceptLanguage);
        if (customerUserId == null) {
            throw MissingFieldException.required("userId");
        }
        boolean allowed = BookingStaffRole.BRANCH_ADMIN.name().equals(staff.getRole())
                ? staff.getBranch() != null && bookingRepository.existsByCustomerUserIdAndBranch_Id(
                        customerUserId, staff.getBranch().getId())
                : staff.getPartner() != null && bookingRepository.existsByCustomerUserIdAndBranch_Partner_Id(
                        customerUserId, staff.getPartner().getId());
        if (!allowed) {
            throw new ForbiddenException("customer photo is not on your branch");
        }
        String userKey = String.valueOf(customerUserId);
        ResponseEntity<byte[]> cached = redisCacheService.getUserPhoto(userKey);
        if (cached != null) {
            return cached;
        }
        UserPhoto userPhoto = userPhotoRepository.findFirstByUserIdOrderByImageIdDesc(customerUserId);
        if (userPhoto == null || userPhoto.getImageData() == null) {
            throw new ResourceNotFoundException(MessagesLangValues.PHOTO_NOT_FOUND.getMessageByLang(acceptLanguage));
        }
        MediaType mediaType = mediaType(userPhoto.getFileType());
        redisCacheService.putUserPhoto(userKey, mediaType, userPhoto.getImageData());
        return ResponseEntity.ok().contentType(mediaType).body(userPhoto.getImageData());
    }

    private static byte[] imageBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(null));
        }
        String name = file.getOriginalFilename();
        if (name != null && name.contains("..")) {
            throw new MissingFieldException(MessagesLangValues.INVALID_PHOTO_NAME.getMessageByLang(null));
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new FileStorageException(MessagesLangValues.FILE_CANT_SET.getMessageByLang(null));
        }
    }

    private static final Set<String> ALLOWED_PHOTOS = Set.of("jpeg", "png", "webp", "gif");

    private static String fileType(byte[] bytes, String acceptLanguage) {
        String kind = photoKind(new Tika().detect(bytes));
        if (!ALLOWED_PHOTOS.contains(kind)) {
            throw new InvalidStatusException(rejectedPhoto(kind, acceptLanguage));
        }
        return kind;
    }

    private static String photoKind(String detected) {
        if (detected == null || detected.isBlank() || !detected.startsWith("image/")) {
            return "file";
        }
        String sub = detected.substring("image/".length()).toLowerCase(Locale.ROOT);
        int plus = sub.indexOf('+');
        if (plus > 0) {
            sub = sub.substring(0, plus);
        }
        return sub;
    }

    private static String rejectedPhoto(String kind, String acceptLanguage) {
        String lang = acceptLanguage == null ? "az" : acceptLanguage.toLowerCase(Locale.ROOT);
        if (lang.startsWith("en")) {
            return kind + " format is not accepted, only JPEG, PNG, WebP, GIF";
        }
        if (lang.startsWith("ru")) {
            return "Формат " + kind + " не принимается, только JPEG, PNG, WebP, GIF";
        }
        return kind + " formatı qəbul edilmir, yalnız JPEG, PNG, WebP, GIF";
    }

    private static MediaType mediaType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        if (!fileType.contains("/")) {
            return MediaType.parseMediaType("image/" + fileType.toLowerCase());
        }
        return MediaType.parseMediaType(fileType);
    }
}
