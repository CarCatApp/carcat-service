package com.carland.carland_service.service;

import com.carland.carland_service.entity.Car;
import com.carland.carland_service.entity.CarPhoto;
import com.carland.carland_service.entity.Color;
import com.carland.carland_service.enums.CarPhotoSource;
import com.carland.carland_service.enums.CarPhotoStatus;
import com.carland.carland_service.repository.CarPhotoRepository;
import com.carland.carland_service.repository.CarRepository;
import com.carland.carland_service.repository.ColorRepository;
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
    public void generate(Long carId, String userIdHeader) {
        CarPhoto photo = carPhotoRepository.findByCarId(carId);
        if (photo == null || !CarPhotoStatus.isPending(photo.getPhotoStatus())) {
            return;
        }
        Car car = carRepository.findById(carId).orElse(null);
        if (car == null) {
            markFailed(carId, userIdHeader);
            return;
        }

        String prompt = buildPrompt(car);
        byte[] bytes;
        try {
            bytes = openAiImageClient.generateWebp(prompt);
        } catch (Exception ex) {
            log.warn("AI car photo OpenAI failed | carId={}, reason={}", carId, ex.getMessage());
            markFailed(carId, userIdHeader);
            return;
        }
        if (bytes == null || bytes.length == 0) {
            log.warn("AI car photo empty bytes | carId={}", carId);
            markFailed(carId, userIdHeader);
            return;
        }
        persistReady(carId, userIdHeader, bytes);
    }

    private void persistReady(Long carId, String userIdHeader, byte[] bytes) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            CarPhoto photo = carPhotoRepository.findByCarId(carId);
            if (photo == null || !CarPhotoStatus.isPending(photo.getPhotoStatus())) {
                return;
            }
            Car car = carRepository.findById(carId).orElse(null);
            if (car == null) {
                return;
            }
            photo.setImageData(bytes);
            photo.setFileType("webp");
            photo.setFileName("car " + carId + " image");
            photo.setPhotoStatus(CarPhotoStatus.READY);
            photo.setPhotoSource(CarPhotoSource.AI_GENERATED);
            carPhotoRepository.save(photo);

            int used = car.getAiPhotoGenerateCount() == null ? 0 : car.getAiPhotoGenerateCount();
            car.setAiPhotoGenerateCount(used + 1);
            carRepository.save(car);
        });
        redisCacheService.evictCarPhoto(carId);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
        log.info("AI car photo ready | carId={}", carId);
    }

    private void markFailed(Long carId, String userIdHeader) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            CarPhoto photo = carPhotoRepository.findByCarId(carId);
            if (photo == null || !CarPhotoStatus.isPending(photo.getPhotoStatus())) {
                return;
            }
            photo.setPhotoStatus(CarPhotoStatus.FAILED);
            carPhotoRepository.save(photo);
        });
        redisCacheService.evictCarPhoto(carId);
        redisCacheService.evictCarListAfterCommit(userIdHeader);
    }

    private String buildPrompt(Car car) {
        String year = car.getModelYear() == null ? "" : String.valueOf(car.getModelYear());
        String brand = car.getBrand() == null ? "" : car.getBrand().trim();
        String model = car.getModel() == null ? "" : car.getModel().trim();
        String colorPart = colorForPrompt(car);
        String plate = car.getPlateNumber() == null ? "" : car.getPlateNumber().trim();
        return "Stylized 3D miniature model of a " + year + " " + brand + " " + model + " in " + colorPart + ". "
                + "Front-left 45° three-quarter view. Preserve the real vehicle's recognizable design and proportions, "
                + "especially the body shape, grille, headlights, wheels, windows and trim. "
                + "Include an Azerbaijani license plate displaying '" + plate + "' clearly on the front of the vehicle. "
                + "Clean toy-like 3D appearance with smooth surfaces, simplified realistic details, slightly exaggerated but accurate proportions, "
                + "soft studio lighting and subtle shadow. Full vehicle visible, centered, no cropping. "
                + "Transparent background. No people, additional text or watermark.";
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
