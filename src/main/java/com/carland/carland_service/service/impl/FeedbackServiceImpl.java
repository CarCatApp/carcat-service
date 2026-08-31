package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.FeedbackRequest;
import com.carland.carland_service.dto.response.FeedbackResponse;
import com.carland.carland_service.entity.Feedback;
import com.carland.carland_service.entity.FeedbackPhoto;
import com.carland.carland_service.enums.FeedbackSuccessResponse;
import com.carland.carland_service.enums.FeedbackTypes;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.FeedbackPhotoRepository;
import com.carland.carland_service.repository.FeedbackRepository;
import com.carland.carland_service.service.MailService;
import com.carland.carland_service.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * tr: Geri bildirim akışının implementasyonudur: geri bildirimi veritabanına kaydeder,
 *     görsel eki varsa feedback_photos'a yazar ve MailService üzerinden e-posta olarak iletir.
 * en: Implementation of the feedback flow: persists the feedback, stores an image attachment
 *     in feedback_photos when present, and forwards e-mail via MailService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackPhotoRepository feedbackPhotoRepository;
    private final MailService mailService;

    /**
     * tr: Geri bildirimi kaydeder, ticket id üretir ve maili (varsa ekiyle) gönderir; ticket id ve tahmini
     *     yanıt süresini döner. Tip feedback/support/bug_report dışında ise MissingFieldException fırlatır.
     *     Görsel ek Tika ile doğrulanır ve feedback_photos'a yazılır. Mail başarısız olursa kayıt da geri alınır.
     * en: Persists the feedback, generates a ticket id, and sends the mail (with attachment if any);
     *     returns the ticket id and estimated response time. Throws MissingFieldException when the type is
     *     not one of feedback/support/bug_report. Image attachments are Tika-checked and stored in
     *     feedback_photos. The operation is transactional; a mail failure rolls back the saved records.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackResponse pushFeedback(MultipartFile file, FeedbackRequest feedbackRequest, String phoneNumber,
            String userIdHeader, String acceptLanguage) {

        Feedback feedback = Feedback.builder()
                .type(feedbackRequest.getType())
                .subject(feedbackRequest.getSubject())
                .description(feedbackRequest.getDescription())
                .rating(feedbackRequest.getRating())
                .customerId(Long.valueOf(userIdHeader))
                .customerPhone(phoneNumber)
                .build();

        if (Stream.of(FeedbackTypes.feedback, FeedbackTypes.support, FeedbackTypes.bug_report)
                .noneMatch(e -> e.name().equalsIgnoreCase(feedbackRequest.getType()))) {
            throw new MissingFieldException(MessagesLangValues.MISSING_BODY.getMessageByLang(acceptLanguage));
        }

        feedback = feedbackRepository.save(feedback);
        storeImageIfPresent(file, feedback.getFeedbackId());

        mailService.sendFeedbackMail(feedbackRequest, file, feedback.getFeedbackId().toString(), phoneNumber);

        return FeedbackResponse.builder()
                .ticketId(feedback.getFeedbackId())
                .message(FeedbackSuccessResponse.SUCCESS_MESSAGE.getMessageByLang(acceptLanguage))
                .estimatedResponseTime(FeedbackSuccessResponse.ESTIMATED_TIME.getMessageByLang(acceptLanguage))
                .build();
    }

    private void storeImageIfPresent(MultipartFile file, Long feedbackId) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String original = file.getOriginalFilename();
        if (original != null && original.contains("..")) {
            log.warn("feedback photo skipped, path traversal in name, feedbackId={}", feedbackId);
            return;
        }
        try {
            byte[] bytes = file.getBytes();
            String detectedType = new Tika().detect(bytes);
            if (detectedType == null || !detectedType.startsWith("image/")) {
                log.info("feedback attachment is not an image ({}), stored only on mail, feedbackId={}",
                        detectedType, feedbackId);
                return;
            }
            String fileType = detectedType.substring("image/".length());
            String fileName = (original == null || original.isBlank())
                    ? ("feedback-" + feedbackId)
                    : original.trim();
            if (fileName.length() > 255) {
                fileName = fileName.substring(0, 255);
            }
            feedbackPhotoRepository.save(FeedbackPhoto.builder()
                    .feedbackId(feedbackId)
                    .fileName(fileName)
                    .fileType(fileType)
                    .imageData(bytes)
                    .build());
        } catch (IOException ex) {
            throw new RuntimeException("Feedback photo could not be read", ex);
        }
    }
}
