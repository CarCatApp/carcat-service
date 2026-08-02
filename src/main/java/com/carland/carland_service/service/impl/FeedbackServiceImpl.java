package com.carland.carland_service.service.impl;

import com.carland.carland_service.dto.request.FeedbackRequest;
import com.carland.carland_service.dto.response.FeedbackResponse;
import com.carland.carland_service.entity.Feedback;
import com.carland.carland_service.enums.FeedbackSuccessResponse;
import com.carland.carland_service.enums.FeedbackTypes;
import com.carland.carland_service.enums.MessagesLangValues;
import com.carland.carland_service.exceptions.MissingFieldException;
import com.carland.carland_service.repository.FeedbackRepository;
import com.carland.carland_service.service.MailService;
import com.carland.carland_service.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.stream.Stream;

/**
 * tr: Geri bildirim akışının implementasyonudur: geri bildirimi veritabanına kaydeder ve
 *     MailService üzerinden e-posta olarak iletir.
 * en: Implementation of the feedback flow: persists the feedback to the database and forwards
 *     it by e-mail via MailService.
 */
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MailService mailService;

    /**
     * tr: Geri bildirimi kaydeder, ticket id üretir ve maili (varsa ekiyle) gönderir; ticket id ve tahmini
     *     yanıt süresini döner. Tip feedback/support/bug_report dışında ise MissingFieldException fırlatır.
     *     İşlem transactional'dır; mail gönderimi başarısız olursa kayıt da geri alınır.
     * en: Persists the feedback, generates a ticket id, and sends the mail (with attachment if any);
     *     returns the ticket id and estimated response time. Throws MissingFieldException when the type is
     *     not one of feedback/support/bug_report. The operation is transactional; a mail failure rolls back
     *     the saved record as well.
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

        mailService.sendFeedbackMail(feedbackRequest, file, feedback.getFeedbackId().toString(), phoneNumber);

        return FeedbackResponse.builder()
                .ticketId(feedback.getFeedbackId())
                .message(FeedbackSuccessResponse.SUCCESS_MESSAGE.getMessageByLang(acceptLanguage))
                .estimatedResponseTime(FeedbackSuccessResponse.ESTIMATED_TIME.getMessageByLang(acceptLanguage))
                .build();
    }
}
