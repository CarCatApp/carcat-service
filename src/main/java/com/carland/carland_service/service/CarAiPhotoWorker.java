package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CarPhoto;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.enums.CarPhotoSource;
import com.carland.carland_service.enums.CarPhotoStatus;
import com.carland.carland_service.repository.CarPhotoRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
import com.carland.carland_service.util.PlateNumbers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * tr: Generate 202 döndükten sonra OpenAI'yi arka planda çağırır. Retry yok; hata → failed.
 * en: Calls OpenAI in the background after generate returns 202. No retry; error → failed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarAiPhotoWorker {

    private final CarPhotoRepository carPhotoRepository;
    private final CarRepository carRepository;
    private final ColorRepository colorRepository;
    private final OpenAiImageClient openAiImageClient;
    private final RedisCacheService redisCacheService;
    private final PlatformTransactionManager transactionManager;

    @Async
    public void generate(Long carId, String userIdHeader, String attemptKey) {
        if (!ownsAttempt(carId, attemptKey)) {
            return;
        }
        Car car = carRepository.findById(carId).orElse(null);
        if (car == null) {
            markFailed(carId, userIdHeader, attemptKey);
            return;
        }

        String prompt = buildPrompt(car);
        byte[] bytes;
        try {
            bytes = openAiImageClient.generateWebp(prompt);
        } catch (Exception ex) {
            log.warn("AI car photo OpenAI failed | carId={}, reason={}", carId, ex.getMessage());
            markFailed(carId, userIdHeader, attemptKey);
            return;
        }
        if (bytes == null || bytes.length == 0) {
            log.warn("AI car photo empty bytes | carId={}", carId);
            markFailed(carId, userIdHeader, attemptKey);
            return;
        }
        persistReady(carId, userIdHeader, attemptKey, bytes);
    }

    private void persistReady(Long carId, String userIdHeader, String attemptKey, byte[] bytes) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Boolean wrote = tx.execute(status -> {
            CarPhoto photo = carPhotoRepository.findByCarId(carId);
            if (!owns(photo, attemptKey)) {
                return false;
            }
            Car car = carRepository.findById(carId).orElse(null);
            if (car == null) {
                return false;
            }
            photo.setImageData(bytes);
            photo.setFileType("webp");
            photo.setFileName("car " + carId + " image");
            photo.setPhotoStatus(CarPhotoStatus.READY);
            photo.setPhotoSource(CarPhotoSource.AI_GENERATED);
            photo.setPromptKey(CarAiPhotoPromptKey.of(car));
            carPhotoRepository.save(photo);

            int used = car.getAiPhotoGenerateCount() == null ? 0 : car.getAiPhotoGenerateCount();
            car.setAiPhotoGenerateCount(used + 1);
            carRepository.save(car);
            return true;
        });
        if (!Boolean.TRUE.equals(wrote)) {
            return;
        }
        redisCacheService.evictCarPhoto(carId);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        log.info("AI car photo ready | carId={}", carId);
    }

    private void markFailed(Long carId, String userIdHeader, String attemptKey) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Boolean wrote = tx.execute(status -> {
            CarPhoto photo = carPhotoRepository.findByCarId(carId);
            if (!owns(photo, attemptKey)) {
                return false;
            }
            photo.setPhotoStatus(CarPhotoStatus.FAILED);
            carPhotoRepository.save(photo);
            return true;
        });
        if (!Boolean.TRUE.equals(wrote)) {
            return;
        }
        redisCacheService.evictCarPhoto(carId);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
    }

    private boolean ownsAttempt(Long carId, String attemptKey) {
        return owns(carPhotoRepository.findByCarId(carId), attemptKey);
    }

    /** A newer generate replaces the attempt key. A stale job must not write ready or failed. */
    private static boolean owns(CarPhoto photo, String attemptKey) {
        return photo != null
                && attemptKey != null
                && attemptKey.equals(photo.getPromptKey())
                && CarPhotoStatus.isPending(photo.getPhotoStatus());
    }

    private String buildPrompt(Car car) {
        String year = car.getModelYear() == null ? "" : String.valueOf(car.getModelYear());
        String brand = car.getBrand() == null ? "" : car.getBrand().trim();
        String model = car.getModel() == null ? "" : car.getModel().trim();
        String colorPart = colorForPrompt(car);
        String colorClause = car.getColorId() == null
                ? CarIddaColor.azerbaijaniClause(car.getIddaColor())
                : "";
        if (!colorClause.isEmpty()) {
            colorPart = "";
        }
        String rawPlate = car.getPlateNumber() == null ? "" : car.getPlateNumber().trim();
        String plate = rawPlate.isEmpty() ? "" : PlateNumbers.format(rawPlate);
        String body = car.getBodyType() == null ? "" : car.getBodyType().trim();
        String vehicle = (year + " " + colorPart + " " + brand + " " + model + (body.isEmpty() ? "" : " " + body))
                .replaceAll(" +", " ")
                .trim();
        return "Photorealistic studio product photo of a " + vehicle
                + colorClause
                + " , shown in a front three-quarter view angled toward the front — the front fascia mostly facing the camera with only a slight turn to reveal the right-side profile, roughly 20° from head-on, camera at roughly bumper height, entire vehicle centered and fully in frame. Transparent background, even soft studio lighting, subtle contact shadow and faint reflection directly beneath the car, no other reflections. Clean catalog/marketing style, sharp detail, no people, no text or watermarks, no background objects. License plate '"
                + plate + "' in Azerbaijani format";
    }

    private String colorForPrompt(Car car) {
        if (car.getColorId() == null) {
            return "unknown";
        }
        Color color = colorRepository.findByColorId(car.getColorId());
        if (color == null) {
            return "unknown";
        }
        if (color.getHex() != null && !color.getHex().isBlank()) {
            return color.getHex().trim();
        }
        return color.getColor() == null ? "unknown" : color.getColor();
    }
}
